(ns eca.llm-providers.openai
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [eca.llm-util :as llm-util]
   [eca.logger :as logger]
   [hato.client :as http]))

(set! *warn-on-reflection* true)

(def ^:private logger-tag "[OPENAI]")

(def ^:private openai-url "https://api.openai.com")
(def ^:private responses-path "/v1/responses")

(defn ^:private url [path]
  (format "%s%s"
          (or (System/getenv "OPENAI_API_URL")
              openai-url)
          path))

(defn ^:private base-completion-request! [{:keys [rid body api-key on-error on-response]}]
  (let [api-key (or api-key
                    (System/getenv "OPENAI_API_KEY"))
        url (url responses-path)]
    (llm-util/log-request logger-tag rid url body)
    (http/post
     url
     {:headers {"Authorization" (str "Bearer " api-key)
                "Content-Type" "application/json"}
      :body (json/generate-string body)
      :throw-exceptions? false
      :async? true
      :as :stream}
     (fn [{:keys [status body]}]
       (try
         (if (not= 200 status)
           (let [body-str (slurp body)]
             (logger/warn logger-tag "Unexpected response status: %s body: %s" status body-str)
             (on-error {:message (format "OpenAI response status: %s body: %s" status body-str)}))
           (with-open [rdr (io/reader body)]
             (doseq [[event data] (llm-util/event-data-seq rdr)]
               (llm-util/log-response logger-tag rid event data)
               (on-response event data))))
         (catch Exception e
           (on-error {:exception e}))))
     (fn [e]
       (on-error {:exception e})))))

(defn ^:private past-messages->input [past-messages]
  (mapv (fn [{:keys [role content] :as msg}]
          (case role
            "tool_call" {:type "function_call"
                         :name (:name content)
                         :call_id (:id content)
                         :arguments (json/generate-string (:arguments content))}
            "tool_call_output"
            {:type "function_call_output"
             :call_id (:id content)
             :output (llm-util/stringfy-tool-result content)}
            msg))
        past-messages))

(defn completion! [{:keys [model user-prompt context temperature api-key past-messages tools web-search]
                    :or {temperature 1.0}}
                   {:keys [on-message-received on-error on-prepare-tool-call on-tool-called on-reason]}]
  (let [input (conj (past-messages->input past-messages)
                    {:role "user" :content user-prompt})
        tools (cond-> tools
                web-search (conj {:type "web_search_preview"}))
        body {:model model
              :input input
              :user (str (System/getProperty "user.name") "@ECA")
              :instructions context
              :temperature temperature
              :tools tools
              :stream true}
        mcp-call-by-item-id* (atom {})
        on-response-fn
        (fn handle-response [event data]
          (case event
            ;; text
            "response.output_text.delta"
            (on-message-received {:type :text
                                  :text (:delta data)})
            ;; tools
            "response.function_call_arguments.delta" (let [call (get @mcp-call-by-item-id* (:item_id data))]
                                                       (on-prepare-tool-call {:id (:id call)
                                                                              :name (:name call)
                                                                              :argumentsText (:delta data)}))

            "response.output_item.done"
            (case (:type (:item data))
              "function_call" (let [function-name (-> data :item :name)
                                    function-args (-> data :item :arguments)
                                    {:keys [result past-messages]} (on-tool-called {:id (-> data :item :call_id)
                                                                                    :name function-name
                                                                                    :arguments (json/parse-string function-args)})]
                                (base-completion-request!
                                 {:rid (llm-util/gen-rid)
                                  :body (assoc body :input (concat (past-messages->input past-messages)
                                                                   [{:type "function_call"
                                                                     :call_id (-> data :item :call_id)
                                                                     :name function-name
                                                                     :arguments function-args}]
                                                                   (mapv
                                                                    (fn [{:keys [_type content]}]
                                                                       ;; TODO handle different types
                                                                      {:type "function_call_output"
                                                                       :call_id (-> data :item :call_id)
                                                                       :output content})
                                                                    (:contents result))))
                                  :api-key api-key
                                  :on-error on-error
                                  :on-response handle-response})
                                (swap! mcp-call-by-item-id* dissoc (-> data :item :id)))
              "reasoning" (on-reason {:status :finished})
              nil)

            ;; URL mentioned
            "response.output_text.annotation.added"
            (case (-> data :annotation :type)
              "url_citation" (on-message-received
                              {:type :url
                               :title (-> data :annotation :title)
                               :url (-> data :annotation :url)})
              nil)

            ;; reasoning / tools
            "response.output_item.added"
            (case (-> data :item :type)
              "reasoning" (on-reason {:status :started})
              "function_call" (let [call-id (-> data :item :call_id)
                                    item-id (-> data :item :id)
                                    name (-> data :item :name)]
                                (swap! mcp-call-by-item-id* assoc item-id {:name name :id call-id})
                                (on-prepare-tool-call {:id (-> data :item :call_id)
                                                       :name (-> data :item :name)
                                                       :argumentsText (-> data :item :arguments)}))
              nil)

            ;; done
            "response.completed"
            (when-not (= "function_call" (-> data :response :output last :type))
              (on-message-received {:type :finish
                                    :finish-reason (-> data :response :status)}))
            nil))]
    (base-completion-request!
     {:rid (llm-util/gen-rid)
      :body body
      :api-key api-key
      :on-error on-error
      :on-response on-response-fn})))
