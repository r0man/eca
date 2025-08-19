(ns eca.features.context-test
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [eca.features.context :as f.context]
   [eca.features.index :as f.index]
   [eca.features.tools.mcp :as f.mcp]
   [eca.test-helper :as h]))

(deftest all-contexts-test
  (testing "includes repoMap, root directories, files/dirs, and mcp resources"
    (let [root (h/file-path "/fake/repo")
          ;; Fake filesystem entries under the root
          fake-paths [(str root "/dir")
                      (str root "/foo.txt")
                      (str root "/dir/nested.txt")
                      (str root "/bar.txt")]
          db* (atom {:workspace-folders [{:uri (h/file-uri "file:///fake/repo")}]})]
      (with-redefs [fs/glob (fn [_root-filename pattern]
                              ;; Very simple glob: filter by substring present in pattern ("**<q>**")
                              (let [q (string/replace pattern #"\*" "")]
                                (filter #(string/includes? (str %) q) fake-paths)))
                    fs/directory? (fn [p] (string/ends-with? (str p) "/dir"))
                    fs/canonicalize identity
                    f.index/filter-allowed (fn [file-paths _root _config] file-paths)
                    f.mcp/all-resources (fn [_db] [{:uri "mcp://r1"}])]
        (let [result (f.context/all-contexts nil db* {})]
          ;; Starts with repoMap
          (is (= "repoMap" (:type (first result))))
          ;; Contains root directory entries
          (is (some #(= {:type "directory" :path root} %) result))
          ;; Contains file and directory entries from the fake paths
          (is (some #(= {:type "directory" :path (str root "/dir")} %) result))
          (is (some #(= {:type "file" :path (str root "/foo.txt")} %) result))
          (is (some #(= {:type "file" :path (str root "/dir/nested.txt")} %) result))
          ;; MCP resources appended with proper type
          (is (some #(= {:type "mcpResource" :uri "mcp://r1"} %) result))))))

  (testing "respects the query by limiting glob results"
    (let [root (h/file-path "/fake/repo")
          fake-paths [(str root "/foo.txt")
                      (str root "/bar.txt")]
          db* (atom {:workspace-folders [{:uri (h/file-uri "file:///fake/repo")}]})]
      (with-redefs [fs/glob (fn [_root-filename pattern]
                              (let [q (string/replace pattern #"\*" "")]
                                (filter #(string/includes? (str %) q) fake-paths)))
                    fs/directory? (constantly false)
                    fs/canonicalize identity
                    f.index/filter-allowed (fn [file-paths _root _config] file-paths)
                    f.mcp/all-resources (fn [_db] [])]
        (let [result (f.context/all-contexts "foo" db* {})]
          ;; Should include foo.txt but not bar.txt
          (is (some #(= {:type "file" :path (str root "/foo.txt")} %) result))
          (is (not (some #(= {:type "file" :path (str root "/bar.txt")} %) result)))))))

  (testing "aggregates entries across multiple workspace roots"
    (let [root1 (h/file-path "/fake/repo1")
          root2 (h/file-path "/fake/repo2")
          db* (atom {:workspace-folders [{:uri (h/file-uri "file:///fake/repo1")}
                                         {:uri (h/file-uri "file:///fake/repo2")}]})]
      (with-redefs [fs/glob (fn [root-filename pattern]
                              (let [q (string/replace pattern #"\*" "")]
                                (cond
                                  (string/includes? (str root-filename) (h/file-path "/fake/repo1"))
                                  (filter #(string/includes? (str %) q)
                                          [(str root1 "/a.clj")])

                                  (string/includes? (str root-filename) (h/file-path "/fake/repo2"))
                                  (filter #(string/includes? (str %) q)
                                          [(str root2 "/b.clj")])

                                  :else [])))
                    fs/directory? (constantly false)
                    fs/canonicalize identity
                    f.index/filter-allowed (fn [file-paths _root _config] file-paths)
                    f.mcp/all-resources (fn [_db] [])]
        (let [result (f.context/all-contexts nil db* {})]
          ;; Root directories present
          (is (some #(= {:type "directory" :path root1} %) result))
          (is (some #(= {:type "directory" :path root2} %) result))
          ;; Files from both roots present
          (is (some #(= {:type "file" :path (str root1 "/a.clj")} %) result))
          (is (some #(= {:type "file" :path (str root2 "/b.clj")} %) result)))))))

(deftest case-insensitive-query-test
  (testing "Should find README.md when searching for 'readme' (case-insensitive)"
    (let [readme (h/file-path "/fake/repo/README.md")
          core (h/file-path "/fake/repo/src/core.clj")]
      (with-redefs [fs/glob (fn [_root-filename pattern]
                              (cond
                                (= pattern "**") [readme core]
                                (= pattern "**readme**") []
                                :else []))
                    fs/directory? (constantly false)
                    fs/canonicalize identity
                    f.index/filter-allowed (fn [files _root _config] files)]
        (let [db* (atom {:workspace-folders [{:uri (h/file-uri "file:///fake/repo")}]})
              config {}
              results (f.context/all-contexts "readme" db* config)
              file-paths (->> results (filter #(= "file" (:type %))) (map :path) set)]
          (is (contains? file-paths readme)))))))
