(ns eca.llm-providers.copilot
  (:require
   [cheshire.core :as json]
   [eca.config :as config]
   [hato.client :as http]))

(def ^:private client-id "Iv1.b507a08c87ecfe98")

(defn ^:private auth-headers []
  {"Content-Type" "application/json"
   "Accept" "application/json"
   "editor-plugin-version" "eca/*"
   "editor-version" (str "eca/" (config/eca-version))})

(defn oauth-url []
  (let [{:keys [body]} (http/post
                        "https://github.com/login/device/code"
                        {:headers (auth-headers)
                         :body (json/generate-string {:client_id client-id
                                                      :scope "read:user"})
                         :as :json})]
    {:user-code (:user_code body)
     :device-code (:device_code body)
     :url (:verification_uri body)}))

(defn oauth-access-token [device-code]
  (let [{:keys [status body]} (http/post
                               "https://github.com/login/oauth/access_token"
                               {:headers (auth-headers)
                                :body (json/generate-string {:client_id client-id
                                                             :device_code device-code
                                                             :grant_type "urn:ietf:params:oauth:grant-type:device_code"})
                                :throw-exceptions? false
                                :as :json})]
    (if (= 200 status)
      (:access_token body)
      (throw (ex-info (format "Github auth failed: %s" (pr-str body))
                      {:status status
                       :body body})))))

(defn oauth-renew-token [access-token]
  (let [{:keys [status body]} (http/get
                               "https://api.github.com/copilot_internal/v2/token"
                               {:headers (merge (auth-headers)
                                                {"authorization" (str "token " access-token)})
                                :throw-exceptions? false
                                :as :json})]
    (if-let [token (:token body)]
      {:api-token token
       :expires-at (:expires_at body)}
      (throw (ex-info (format "Error on copilot login: %s" body)
                      {:status status
                       :body body})))))

(comment
  (def a (oauth-url))
  (:user-code a)
  (:device-code a)
  (:url a)

  (def access-token (oauth-access-token (:device-code a)))

  (def credentials (oauth-renew-token access-token))

  (:api-token credentials)
  (:expires-at credentials))
