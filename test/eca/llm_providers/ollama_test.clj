(ns eca.llm-providers.ollama-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [eca.llm-providers.ollama :as llm-providers.ollama]
   [matcher-combinators.test :refer [match?]]))

(deftest ->messages-with-history-test
  (testing "no previous history"
    (is (match?
         [{:role "system" :content "You are ECA"}]
         (#'llm-providers.ollama/past-messages->messages [] "You are ECA"))))
  (testing "With basic text history"
    (is (match?
         [{:role "system" :content "You are ECA"}
          {:role "user" :content "Count with me: 1"}
          {:role "assistant" :content "2"}]
         (#'llm-providers.ollama/past-messages->messages
          [{:role "user" :content "Count with me: 1"}
           {:role "assistant" :content "2"}]
          "You are ECA"))))
  (testing "With tool_call history"
    (is (match?
         [{:role "system" :content "You are ECA"}
          {:role "user" :content "List the files you are allowed"}
          {:role "assistant" :content "Ok!"}
          {:role "assistant" :tool-calls [{:type "function"
                                           :function {:name "list_allowed_directories"
                                                      :arguments {}}}]}
          {:role "tool" :content "Allowed directories: /foo/bar\n"}
          {:role "assistant" :content "I see /foo/bar"}]
         (#'llm-providers.ollama/past-messages->messages
          [{:role "user" :content "List the files you are allowed"}
           {:role "assistant" :content "Ok!"}
           {:role "tool_call" :content {:id "call-1" :name "list_allowed_directories" :arguments {}}}
           {:role "tool_call_output" :content {:id "call-1"
                                               :name "list_allowed_directories"
                                               :arguments {}
                                               :output {:contents [{:type :text
                                                                    :error false
                                                                    :content "Allowed directories: /foo/bar"}]}}}
           {:role "assistant" :content "I see /foo/bar"}]
          "You are ECA")))))
