(ns llm-mock.openai
  (:require
   [cheshire.core :as json]
   [llm-mock.mocks :as llm.mocks]
   [org.httpkit.server :as hk]))

(defn ^:private sse-send!
  "Send one SSE event line pair: event + data, followed by a blank line.
  Keep the connection open until the scenario finishes."
  [ch event m]
  (hk/send! ch (str "event: " event "\n") false)
  (hk/send! ch (str "data: " (json/generate-string m) "\n\n") false))

(defn ^:private simple-text-0 [ch]
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta "Knock"})
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta " knock!"})
  (sse-send! ch "response.completed"
             {:type "response.completed"
              :response {:output []
                         :usage {:input_tokens 10
                                 :output_tokens 20}
                         :status "completed"}})
  (hk/close ch))

(defn ^:private simple-text-1 [ch]
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta "Foo"})
  (sse-send! ch "response.completed"
             {:type "response.completed"
              :response {:output []
                         :usage {:input_tokens 10
                                 :output_tokens 5}
                         :status "completed"}})
  (hk/close ch))

(defn ^:private simple-text-2 [ch]
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta "Foo"})
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta " bar!"})
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta "\n\n"})
  (sse-send! ch "response.output_text.delta"
             {:type "response.output_text.delta" :delta "Ha!"})
  (sse-send! ch "response.completed"
             {:type "response.completed"
              :response {:output []
                         :usage {:input_tokens 5
                                 :output_tokens 15}
                         :status "completed"}})
  (hk/close ch))

(defn handle-openai-responses [req]
  (hk/as-channel
   req
   {:on-open (fn [ch]
                ;; initial SSE handshake
               (hk/send! ch {:status 200
                             :headers {"Content-Type" "text/event-stream; charset=utf-8"
                                       "Cache-Control" "no-cache"
                                       "Connection" "keep-alive"}}
                         false)
               (case llm.mocks/*case*
                 :simple-text-0 (simple-text-0 ch)
                 :simple-text-1 (simple-text-1 ch)
                 :simple-text-2 (simple-text-2 ch)))}))
