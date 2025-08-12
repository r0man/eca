(ns integration.initialize-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [integration.eca :as eca]
   [integration.fixture :as fixture]
   [matcher-combinators.test :refer [match?]]))

(eca/clean-after-test)

(deftest simple-text
  (eca/start-process!)

  (testing "initialize request with default config"
    (is (match?
         {:models ["o4-mini"
                   "claude-opus-4-20250514"
                   "gpt-5"
                   "gpt-4.1"
                   "claude-sonnet-4-20250514"
                   "gpt-5-mini"
                   "claude-3-5-haiku-20241022"
                   "gpt-5-nano"
                   "claude-opus-4-1-20250805"
                   "o3"]
          :chatDefaultModel "claude-sonnet-4-20250514"
          :chatBehaviors ["agent" "plan"]
          :chatDefaultBehavior "plan"
          :chatWelcomeMessage "Welcome to ECA!\n\nType '/' for commands\n\n"}
         (eca/request! (fixture/initialize-request
                        {:initializationOptions (merge fixture/default-init-options
                                                       {:chatBehavior "plan"})
                         :capabilities {:codeAssistant {:chat {}}}}))))))
