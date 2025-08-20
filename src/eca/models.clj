(ns eca.models
  (:require
   [cheshire.core :as json]
   [eca.logger :as logger]
   [eca.shared :refer [assoc-some]]))

(set! *warn-on-reflection* true)

(def ^:private logger-tag "[MODELS]")

(defn ^:private models-dev* []
  (try
    (let [response (slurp "https://models.dev/api.json")
          data (json/parse-string response keyword)]
      data)
    (catch Exception e
      (logger/error logger-tag " Error fetching models from models.dev:" (.getMessage e))
      {})))

(def ^:private models-dev (memoize models-dev*))

(def ^:private one-million 1000000)

(def ^:private models-with-web-search-support
  #{"openai/gpt-4.1"
    "openai/gpt-5"
    "openai/gpt-5-mini"
    "openai/gpt-5-nano"
    "anthropic/claude-sonnet-4"
    "anthropic/claude-opus-4"
    "anthropic/claude-opus-4.1"
    "anthropic/claude-3.5-haiku"})

(defn all
  "Return all known existing models with their capabilities and configs."
  []
  (reduce
   (fn [m [provider provider-config]]
     (merge m
            (reduce
             (fn [p [model model-config]]
               (let [provider-name (or (namespace model) (name provider))]
                 (assoc p (str provider-name "/" (name model))
                        (assoc-some
                         {:provider provider-name
                          :reason? (:reasoning model-config)
                          ;; TODO how to check for web-search mode dynamically
                          :web-search (contains? models-with-web-search-support (name model))
                          :tools (:tool_call model-config)
                          :max-output-tokens (-> model-config :limit :output)}
                         :input-token-cost (some-> (:input (:cost model-config)) float (/ one-million))
                         :output-token-cost (some-> (:output (:cost model-config)) float (/ one-million))
                         :input-cache-creation-token-cost (some-> (:cache_write (:cost model-config)) float (/ one-million))
                         :input-cache-read-token-cost (some-> (:cache_read (:cost model-config)) float (/ one-million))))))
             {}
             (:models provider-config))))
   {}
   (models-dev)))

(comment
  (require '[clojure.pprint :as pprint])
  (pprint/pprint (all)))
