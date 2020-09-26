(ns makejack.api.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.default-config :as default-config]))

(deftest relative-to-resolver-test
  (let [resolver (makejack/relative-to-resolver nil)]
    (testing "relative to working directory"
      (is (= (io/file "project.edn")
             (resolver nil "project.edn"))))
    (testing "relative to source file"
      (is (= (io/file "project.edn")
             (resolver "mj.edn" "project.edn")))))
  (let [resolver (makejack/relative-to-resolver "tools")]
    (testing "relative to working directory"
      (is (= (io/file "tools/project.edn")
             (resolver nil "project.edn"))))
    (testing "relative to source file"
      (is (= (io/file "project.edn")
             (resolver "mj.edn" "project.edn"))))
    (testing "relative to source file"
      (is (= (io/file "tools/project.edn")
             (resolver "tools/mj.edn" "project.edn"))))))

(deftest load-project*-test
  (let [project (makejack/load-project*)]
    (is (map? project))
    (is (= "makejack" (:name project))))
  (let [project (makejack/load-project* {:dir "tools"})]
    (is (map? project))
    (is (= "makejack.tools" (:name project)))))

(defn resolver [proj-name]
  {"project.edn" (io/resource (str proj-name "/project.edn"))
   "mj.edn"      (io/resource (str proj-name "/mj.edn"))})

(deftest load-basic-config-test
  (let [dir      (str (.getPath (io/resource "test_project_basic")))
        resolver (makejack/relative-to-resolver dir)]
    (let [config (makejack/load-config*
                  {:resolver resolver})]
      (is (= {:name        "basic"
              :version     "0.0.1"
              :artifact-id "basic"
              :group-id    "basic"
              :jar-type    :jar
              :jar-name    "basic-0.0.1.jar"
              :aliases     []}
             (:project config)))
      (is (= {:target-path  "target"
              :classes-path "target/classes"
              :project-root (System/getProperty "user.dir")
              :tp           "target"}
             (dissoc (:mj config) :targets)))
      (is (= (keys default-config/default-targets)
             (keys (-> config :mj :targets)))))
    (is (= {:name        "basic"
            :version     "0.0.1"
            :artifact-id "basic"
            :group-id    "basic"
            :jar-type    :uberjar
            :jar-name    "basic-0.0.1-standalone.jar"
            :aliases     [:uberjar]}
           (:project (makejack/load-config*
                      {:resolver resolver
                       :profile  :uberjar}))))))
