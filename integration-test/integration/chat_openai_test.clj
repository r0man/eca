(ns integration.chat-openai-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [integration.eca :as eca]
   [integration.fixture :as fixture]
   [matcher-combinators.matchers :as m]
   [matcher-combinators.test :refer [match?]]))

(eca/clean-after-test)

(deftest simple-text
  (eca/start-process!)

  (eca/request! (fixture/initialize-request))
  (eca/notify! (fixture/initialized-notification))
  (testing "simple hello message with reply"
    (let [resp (eca/request! (fixture/chat-prompt-request
                              {:request-id 0
                               :message "Hello there!"}))
          chat-id (:chatId resp)]

      (is (match?
           {:chatId (m/pred string?)
            :model "claude-sonnet-4-20250514"
            :status "success"}
           resp))

      (is (match?
           {:chatId chat-id
            :requestId 0
            :role "user"
            :content {:type "text" :text "Hello there!\n"}}
           (eca/client-awaits-server-notification :chat/contentReceived)))
      (is (match?
           {:chatId chat-id
            :requestId 0
            :role "system"
            :content {:type "progress" :state "running" :text "Waiting model"}}
           (eca/client-awaits-server-notification :chat/contentReceived))))))
