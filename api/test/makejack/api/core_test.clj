(ns makejack.api.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.default-config :as default-config]))

(deftest relative-to-resolver-test
  (testing "relative-to-resolver with no directory"
    (let [resolver (makejack/relative-to-resolver nil)]
      (testing "relative to working directory"
        (is (= (io/file "project.edn")
               (resolver nil "project.edn"))))
      (testing "relative to source file"
        (is (= (io/file "project.edn")
               (resolver "mj.edn" "project.edn"))))))
  (testing "relative-to-resolver with a directory"
    (let [resolver (makejack/relative-to-resolver "test-resources/project/sub")]
      (testing "relative to working directory"
        (is (= (io/file "test-resources/project/sub/project.edn")
               (resolver nil "project.edn"))))
      (testing "relative to source file"
        (is (= (io/file "project.edn")
               (resolver "mj.edn" "project.edn"))))
      (testing "relative to source file"
        (is (= (io/file "test-resources/project/sub/project.edn")
               (resolver "test-resources/project/sub/mj.edn" "project.edn")))))))

(deftest load-project*-test
  (let [project (makejack/load-project*)]
    (is (map? project))
    (is (= "makejack.api" (:name project))))
  (let [project (makejack/load-project* {:dir "test-resources/project/sub"})]
    (is (map? project))
    (is (= "sub" (:name project)))))

(defn resolver [proj-name]
  {"project.edn" (io/resource (str proj-name "/project.edn"))
   "mj.edn"      (io/resource (str proj-name "/mj.edn"))})

(deftest load-basic-config-test
  (let [dir      (str (.getPath (io/resource "project")))
        resolver (makejack/relative-to-resolver dir)]
    (let [config (makejack/load-config*
                  {:resolver resolver})]
      (is (= {:name        "a-project"
              :artifact-id "a-project"
              :group-id    "a-project"
              :version     "0.0.1"
              :jar-type    :jar
              :jar-name    "a-project-0.0.1.jar"
              :aliases     []}
             (:project config)))
      (is (= {:target-path  "target"
              :classes-path "target/classes"
              :project-root (System/getProperty "user.dir")}
             (dissoc (:mj config) :targets)))
      (is (= (keys default-config/default-targets)
             (keys (-> config :mj :targets)))))
    (is (= {:name        "a-project"
            :version     "0.0.1"
            :artifact-id "a-project"
            :group-id    "a-project"
            :jar-type    :uberjar
            :jar-name    "a-project-0.0.1-standalone.jar"
            :aliases     [:uberjar]}
           (:project (makejack/load-config*
                      {:resolver resolver
                       :profile  :uberjar}))))))
