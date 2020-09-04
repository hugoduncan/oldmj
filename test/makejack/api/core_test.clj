(ns makejack.api.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [makejack.api.core :as makejack]
            [makejack.api.default-config :as default-config]))

(defn resolver [proj-name]
  {"project.edn" (io/resource (str proj-name "/project.edn"))
   "mj.edn"      (io/resource (str proj-name "/mj.edn"))})

(deftest load-basic-config-test
  (let [config (makejack/load-config*
                 {:resolver (resolver "test_project_basic")})]
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
            :tp "target"}
           (dissoc(:mj config) :targets)))
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
                     {:resolver (resolver "test_project_basic")
                      :profile  :uberjar})))))
