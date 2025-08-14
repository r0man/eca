(ns llm-mock.mocks)

(def ^:dynamic *case* nil)

(defn set-case! [case]
  (alter-var-root #'*case* (constantly case)))
