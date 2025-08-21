(ns eca.llm-api
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [eca.config :as config]
   [eca.llm-providers.anthropic :as llm-providers.anthropic]
   [eca.llm-providers.ollama :as llm-providers.ollama]
   [eca.llm-providers.openai :as llm-providers.openai]
   [eca.llm-providers.openai-chat :as llm-providers.openai-chat]
   [eca.logger :as logger]))

(set! *warn-on-reflection* true)

(def ^:private logger-tag "[LLM-API]")

;; TODO ask LLM for the most relevant parts of the path
(defn refine-file-context [path lines-range]
  (cond
    (not (fs/exists? path))
    "File not found"

    (not (fs/readable? path))
    "File not readable"

    :else
    (let [content (slurp path)]
      (if lines-range
        (let [lines (string/split-lines content)
              start (dec (:start lines-range))
              end (min (count lines) (:end lines-range))]
          (string/join "\n" (subvec lines start end)))
        content))))

(defn ^:private provider-api-key [provider config]
  (or (get-in config [:providers (name provider) :key])
      (config/get-env (str (-> provider
                               (string/replace "-" "_")
                               string/upper-case) "_API_KEY"))))

(defn ^:private provider-api-url [provider config]
  (or (get-in config [:providers (name provider) :url])
      (config/get-env (str (-> provider
                               (string/replace "-" "_")
                               string/upper-case) "_API_URL"))))

(defn extra-models [config]
  (let [ollama-api-url (provider-api-url "ollama" config)]
    (mapv
     (fn [{:keys [model] :as ollama-model}]
       (let [capabilities (llm-providers.ollama/model-capabilities {:api-url ollama-api-url :model model})]
         (assoc ollama-model
                :tools (boolean (some #(= % "tools") capabilities))
                :reason? (boolean (some #(= % "thinking") capabilities)))))
     (llm-providers.ollama/list-models {:api-url ollama-api-url}))))

(defn default-model
  "Returns the default LLM model checking this waterfall:
  - Any custom provider with defaultModel set
  - Anthropic api key set
  - Openai api key set
  - Github copilot login done
  - Ollama first model if running
  - Anthropic default model."
  [db config]
  (let [[decision model]
        (or (when-let [custom-provider-default-model (first (keep (fn [[model config]]
                                                                    (when (and (:custom-provider? config)
                                                                               (:default-model? config))
                                                                      model))
                                                                  (:models db)))]
              [:custom-provider-default-model custom-provider-default-model])
            (when (provider-api-key "anthropic" config)
              [:api-key-found "anthropic/claude-sonnet-4-20250514"])
            (when (provider-api-key "openai" config)
              [:api-key-found "openai/gpt-5"])
            (when (get-in db [:auth "github-copilot" :api-key])
              [:api-key-found "github-copilot/gpt-4.1"])
            (when-let [ollama-model (first (filter #(string/starts-with? % config/ollama-model-prefix) (keys (:models db))))]
              [:ollama-running ollama-model])
            [:default "anthropic/claude-sonnet-4-20250514"])]
    (logger/info logger-tag (format "Default LLM model '%s' decision '%s'" model decision))
    model))

(defn ^:private tool->llm-tool [tool]
  (assoc (select-keys tool [:name :description :parameters])
         :type "function"))

(defn complete!
  [{:keys [provider model model-config instructions user-messages config on-first-response-received
           on-message-received on-error on-prepare-tool-call on-tools-called on-reason on-usage-updated
           past-messages tools provider-auth]}]
  (let [first-response-received* (atom false)
        emit-first-message-fn (fn [& args]
                                (when-not @first-response-received*
                                  (reset! first-response-received* true)
                                  (apply on-first-response-received args)))
        on-message-received-wrapper (fn [& args]
                                      (apply emit-first-message-fn args)
                                      (apply on-message-received args))
        on-reason-wrapper (fn [& args]
                            (apply emit-first-message-fn args)
                            (apply on-reason args))
        on-prepare-tool-call-wrapper (fn [& args]
                                       (apply emit-first-message-fn args)
                                       (apply on-prepare-tool-call args))
        on-error-wrapper (fn [{:keys [exception] :as args}]
                           (when-not (:silent? (ex-data exception))
                             (logger/error args)
                             (on-error args)))
        tools (when (:tools model-config)
                (mapv tool->llm-tool tools))
        web-search (:web-search model-config)
        max-output-tokens (:max-output-tokens model-config)
        custom-providers (:customProviders config)
        custom-models (set (mapcat (fn [[k v]]
                                     (map #(str (name k) "/" %) (:models v)))
                                   custom-providers))
        extra-payload (get-in config [:models (keyword model) :extraPayload])
        provider-api-key (provider-api-key provider config)
        provider-api-url (provider-api-url provider config)
        callbacks {:on-message-received on-message-received-wrapper
                   :on-error on-error-wrapper
                   :on-prepare-tool-call on-prepare-tool-call-wrapper
                   :on-tools-called on-tools-called
                   :on-reason on-reason-wrapper
                   :on-usage-updated on-usage-updated}]
    (try
      (cond
        (= "openai" provider)
        (llm-providers.openai/completion!
         {:model model
          :instructions instructions
          :user-messages user-messages
          :max-output-tokens max-output-tokens
          :reason? (:reason? model-config)
          :past-messages past-messages
          :tools tools
          :web-search web-search
          :extra-payload extra-payload
          :api-url provider-api-url
          :api-key provider-api-key}
         callbacks)

        (= "anthropic" provider)
        (llm-providers.anthropic/completion!
         {:model model
          :instructions instructions
          :user-messages user-messages
          :max-output-tokens max-output-tokens
          :reason? (:reason? model-config)
          :past-messages past-messages
          :tools tools
          :web-search web-search
          :extra-payload extra-payload
          :api-url provider-api-url
          :api-key provider-api-key}
         callbacks)

        (= "github-copilot" provider)
        (llm-providers.openai-chat/completion!
         {:model model
          :instructions instructions
          :user-messages user-messages
          :max-output-tokens max-output-tokens
          :reason? (:reason? model-config)
          :past-messages past-messages
          :tools tools
          :extra-payload extra-payload
          :api-url provider-api-url
          :api-key (:api-token provider-auth)
          :extra-headers {"openai-intent" "conversation-panel"
                          "x-request-id" (str (random-uuid))
                          "vscode-sessionid" ""
                          "vscode-machineid" ""
                          "copilot-integration-id" "vscode-chat"}}
         callbacks)

        (= "ollama" provider)
        (llm-providers.ollama/completion!
         {:api-url provider-api-url
          :reason? (:reason? model-config)
          :model model
          :instructions instructions
          :user-messages user-messages
          :past-messages past-messages
          :tools tools
          :extra-payload extra-payload}
         callbacks)

        (contains? custom-models (str provider "/" model))
        (let [provider-config (get custom-providers (keyword provider))
              provider-fn (case (:api provider-config)
                            ("openai-responses"
                             "openai") llm-providers.openai/completion!
                            "anthropic" llm-providers.anthropic/completion!
                            "openai-chat" llm-providers.openai-chat/completion!
                            (on-error-wrapper {:message (format "Unknown custom model %s for provider %s" (:api provider-config) provider)}))
              url (or (:url provider-config) (config/get-env (:urlEnv provider-config)))
              key (or (:key provider-config) (config/get-env (:keyEnv provider-config)))
              url-relative-path (:completionUrlRelativePath provider-config)]
          (provider-fn
           {:model model
            :instructions instructions
            :user-messages user-messages
            :max-output-tokens max-output-tokens
            :reason? (:reason? model-config)
            :past-messages past-messages
            :tools tools
            :extra-payload extra-payload
            :api-url url
            :url-relative-path url-relative-path
            :api-key key}
           callbacks))

        :else
        (on-error-wrapper {:message (str "ECA Unsupported model: " model)}))
      (catch Exception e
        (on-error-wrapper {:exception e})))))
