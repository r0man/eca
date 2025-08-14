(ns llm-mock.anthropic
  (:require
   [cheshire.core :as json]
   [org.httpkit.server :as hk]))

(defn ^:private sse-send!
  "Send one SSE event line pair: event + data, followed by a blank line.
  Keep the connection open until the scenario finishes."
  [ch event m]
  (hk/send! ch (str "event: " event "\n") false)
  (hk/send! ch (str "data: " (json/generate-string m) "\n\n") false))

(defn ^:private anthropic-simple-text! [ch]
  ;; Stream minimal text and then finish with usage via message_delta
  (sse-send! ch "content_block_delta"
             {:type "content_block_delta"
              :index 0
              :delta {:type "text_delta" :text "Hello"}})
  (sse-send! ch "content_block_delta"
             {:type "content_block_delta"
              :index 0
              :delta {:type "text_delta" :text " world!"}})
  (sse-send! ch "message_delta"
             {:type "message_delta"
              :delta {:stop_reason "end_turn"}
              :usage {:input_tokens 10
                      :output_tokens 3}})
  (hk/close ch))

(defn handle-anthropic-messages [req]
  (hk/as-channel
   req
   {:on-open (fn [ch]
               (hk/send! ch {:status 200
                             :headers {"Content-Type" "text/event-stream; charset=utf-8"
                                       "Cache-Control" "no-cache"
                                       "Connection" "keep-alive"}}
                         false)
               (anthropic-simple-text! ch))}))
