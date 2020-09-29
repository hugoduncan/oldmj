(ns makejack.tools.bump-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.tools.bump :as bump]))

(deftest infer-source-test
  (testing "infer-source with no options uses version.edn if it exists"
    (is (= {:type :version-edn
            :path "version.edn"}
           (bump/infer-source {}))))
  (testing "infer-source with no options uses project.edn if version.edn doesnt exist"
    (is (= {:type :project-edn            }
           (bump/infer-source {:dir "test-resources/test-hello-world"})))))

(deftest read-version-map-test
  (is (= {:major       0
          :minor       1
          :incremental 0}
         (bump/read-version-map "0.1.0"))))

(deftest current-version-test
  (testing "current-version"
    (testing "for a :project-edn type, converts the project version to a version map"
      (is (= {:major       0
              :minor       1
              :incremental 0}
             (bump/current-version
              {:type :project-edn}
              {:version "0.1.0"}
              {}))))
    (testing "cfor a :version-edn type, reads the version map"
      (is (= {:major       0
              :minor       1
              :incremental 0}
             (bump/current-version
              {:type :version-edn
               :path (.getPath (io/resource "version.edn"))}
              {}
              {}))))))

(deftest update-version-source-test
  (testing "update-version-source"
    (filesystem/with-temp-dir [dir "update-version-source-test"]
      (testing "for a :project-edn type, updates the version in the project file"
        (let [project-file (path/as-file (path/path dir "project.edn"))]
          (spit project-file (str ";; comment\n" (pr-str {:version "0.1.0"})))
          (bump/update-version-source
           {:type :project-edn}
           {:major 0 :minor 1 :incremental 0}
           {:major 1 :minor 2 :incremental 3}
           {:dir (str dir)})
          (is (= ";; comment\n{:version \"1.2.3\"}"
                 (slurp project-file)))))
      (testing "for a :version-edn type, writes the version to the version.edn file"
        (let [version-file (path/as-file (path/path dir "version.edn"))]
          (spit version-file "")
          (bump/update-version-source
           {:type :version-edn
            :path (str version-file)}
           {:major 0 :minor 1 :incremental 0}
           {:major 1 :minor 2 :incremental 3}
           {})
          (is (= {:major 1 :minor 2 :incremental 3}
                 (edn/read-string (slurp version-file)))))))))

(deftest update-version-test
  (testing "update-version"
    (filesystem/with-temp-dir [dir "update-version-test"]
      (testing "for a path, updates the old version with the new version"
        (let [path (path/path dir "project.edn")]
          (spit
           (path/as-file path)
           (str ";; comment\n" (pr-str {:version "0.1.0"})))
          (bump/update-version
           path
           {:major 0 :minor 1 :incremental 0}
           {:major 1 :minor 2 :incremental 3}
           {})
          (is (= ";; comment\n{:version \"1.2.3\"}"
                 (slurp (path/as-file path))))))
      (testing "for a map, updates the old version with the new version in a search"
        (let [path    (path/path dir "version.edn")
              filedef {:path path :search #":abc \"0.1.0\""}]
          (spit (path/as-file path) "{:abc \"0.1.0\" :def \"0.1.0\"}")
          (bump/update-version
           filedef
           {:major 0 :minor 1 :incremental 0}
           {:major 1 :minor 2 :incremental 3}
           {})
          (is (= "{:abc \"1.2.3\" :def \"0.1.0\"}"
                 (slurp (path/as-file path)))))))))

(deftest next-version-test
  (testing "next-version"
    (testing "given a part, increments the numeric part"
      (is (= {:major 1 :minor 1 :incremental 0}
             (bump/next-version
              {:major 0 :minor 1 :incremental 0}
              ["major"]))))
    (testing "given a part and a numeric value, sets the part to the value as a number"
      (is (= {:major 2 :minor 1 :incremental 0}
             (bump/next-version
              {:major 0 :minor 1 :incremental 0}
              ["major" "2"]))))
    (testing "given a part and a string value, sets the part to the value"
      (is (= {:major "abc"}
             (bump/next-version
              {:major "def"}
              ["major" "abc"]))))
    (testing "given a part, throws if the part is not numeric"
      (is (thrown? clojure.lang.ExceptionInfo
                   (bump/next-version
                    {:major "x"}
                    ["major"]))))))

(deftest bump-test
  (testing "bump"
    (testing "for a :project-edn type, updates the version in the project file"
      (filesystem/with-temp-dir [dir "bump-test"]
        (filesystem/copy-files!
         (.getPath (io/resource "test_bump_project_edn"))
         dir)
        (bump/bump
         {:dir     dir
          :updates ["README.md"
                    {:path "deps.edn" :search #"/related\s+\{.*\}"}]}
         ["minor"]
         (makejack/load-config {:dir dir}))
        (let [project-edn-path (path/path dir "project.edn")
              readme-path      (path/path dir "README.md")
              deps-edn-path    (path/path dir "deps.edn")]
          (is (= "0.2.0"
                 (:version (edn/read-string (slurp (path/as-file project-edn-path))))))
          (is (= "current \"0.2.0\"\n"
                 (slurp (path/as-file readme-path))))
          (let [deps-edn (edn/read-string (slurp (path/as-file deps-edn-path)))]
            (is (= "0.2.0"
                   (-> deps-edn :deps (get 'related/related) :mvn/version)))
            (is (= "0.1.0"
                   (-> deps-edn :deps (get 'unrelated/unrelated) :mvn/version)))))))
    (testing "for a :version-edn type, writes the version to the version.edn file"
      (filesystem/with-temp-dir [dir "bump-test"]
        (filesystem/copy-files!
         (.getPath (io/resource "test_bump_version_edn"))
         dir)
        (bump/bump
         {:dir     dir
          :updates ["README.md"
                    {:path "deps.edn" :search #"/related\s+\{.*\}"}]}
         ["minor"]
         (makejack/load-config {:dir dir}))
        (let [version-edn-path (path/path dir "version.edn")
              project-edn-path (path/path dir "project.edn")
              readme-path      (path/path dir "README.md")
              deps-edn-path    (path/path dir "deps.edn")]
          (is (= {:major 1 :minor 3 :incremental 3}
                 (edn/read-string (slurp (path/as-file version-edn-path)))))
          (is (re-find #":version\s+#version-string"
                       (slurp (path/as-file project-edn-path))))
          (is (= "current \"1.3.3\"\n"
                 (slurp (path/as-file readme-path))))
          (let [deps-edn (edn/read-string (slurp (path/as-file deps-edn-path)))]
            (is (= "1.3.3"
                   (-> deps-edn :deps (get 'related/related) :mvn/version)))
            (is (= "1.2.3"
                   (-> deps-edn :deps (get 'unrelated/unrelated) :mvn/version)))))))))
