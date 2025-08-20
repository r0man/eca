(ns eca.features.login
  (:require
   [eca.db :as db]
   [eca.llm-api :as llm-api]
   [eca.messenger :as messenger]))

(defn start-login [chat-id provider db*]
  (let [{:keys [error-message auth-type] :as result} (llm-api/auth-start {:provider provider})]
    (cond
      error-message
      (do
        (swap! db* assoc-in [:chats chat-id :status] :idle)
        {:error true
         :message error-message})

      (= :oauth/simple auth-type)
      (do
        (swap! db* assoc-in [:chats chat-id :login-provider] provider)
        (swap! db* assoc-in [:auth provider] {:step :login/waiting-user-confirmation
                                              :device-code (:device-code result)})
        {:message (format "Open your browser at `%s` and authenticate using the code: `%s`\nThen type anything in the chat and send it to continue the authentication."
                          (:url result)
                          (:user-code result))}))))

(defn continue [{:keys [chat-id request-id]} db* messenger]
  (let [provider (get-in @db* [:chats chat-id :login-provider])
        step (get-in @db* [:auth provider :step])]
    (case step
      :login/waiting-user-confirmation
      (case provider
        "github-copilot" (let [{:keys [api-token expires-at error-message]} (llm-api/auth-continue {:provider provider
                                                                                                    :db* db*})
                               msg (or error-message "Login successful! You can now use the 'github-copilot' models.")]
                           (when-not error-message
                             (swap! db* update-in [:auth provider] merge {:step :login/done
                                                                          :api-token api-token
                                                                          :expires-at expires-at}))
                           (swap! db* update-in [:chats chat-id :status] :idle)
                           (messenger/chat-content-received
                            messenger
                            {:chat-id chat-id
                             :request-id request-id
                             :role "system"
                             :content {:type :text
                                       :text msg}})
                           (messenger/chat-content-received
                            messenger
                            {:chat-id chat-id
                             :request-id request-id
                             :role "system"
                             :content {:type :progress
                                       :state :finished}}))))
    (db/update-workspaces-cache! @db*)))
