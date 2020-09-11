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


(deftest clojure-cli-args-test
  (is (= [] (makejack/clojure-cli-args {})))
  (is (= ["-Scp" "abc"] (makejack/clojure-cli-args {:cp "abc"})))
  (is (= ["-Sdeps" "{a {:b \"s\"}}"]
         (makejack/clojure-cli-args {:deps '{a {:b "s"}}})))
  (is (= [] (makejack/clojure-cli-args {:force false})))
  (is (= ["-Sforce"] (makejack/clojure-cli-args {:force true})))
  (is (= [] (makejack/clojure-cli-args {:repro false})))
  (is (= ["-Srepro"] (makejack/clojure-cli-args {:repro true})))
  (is (= ["-Sthreads" "1"]
         (makejack/clojure-cli-args {:threads 1})))
  (is (= [] (makejack/clojure-cli-args {:verbose false})))
  (is (= ["-Sverbose"] (makejack/clojure-cli-args {:verbose true}))))

(deftest clojure-cli-aliases-arg-test
  (is (nil? (makejack/clojure-cli-aliases-arg "-A" nil {:elide-when-no-aliases true})))
  (is (= "-A" (makejack/clojure-cli-aliases-arg "-A" nil {})))
  (is (= "-A:a" (makejack/clojure-cli-aliases-arg "-A" [:a] {})))
  (is (= "-A:a:b" (makejack/clojure-cli-aliases-arg "-A" [:a :b] {}))))

(deftest clojure-cli-exec-args-test
  (is (= ["-X"] (makejack/clojure-cli-exec-args {})))
  (is (= ["-X:a:b"] (makejack/clojure-cli-exec-args {:aliases [:a :b]})))
  (is (= ["-X" "a/b"] (makejack/clojure-cli-exec-args {:exec-fn 'a/b})))
  (is (= ["-X" "a/b" "[:a]" "1"]
         (makejack/clojure-cli-exec-args {:exec-fn 'a/b :exec-args {:a 1}})))
  (is (= ["-X" "[:a :b]" "c" "[:a :d]" "1"]
         (makejack/clojure-cli-exec-args {:exec-args {:a {:b "c" :d 1}}}))))

(deftest clojure-cli-main-args-test
  (is (= [] (makejack/clojure-cli-main-args {})))
  (is (= ["-A:a"] (makejack/clojure-cli-main-args {:aliases [:a]})))
  (is (= ["-A:a:b"] (makejack/clojure-cli-main-args {:aliases [:a:b]})))
  (is (= ["-m" "a.b"] (makejack/clojure-cli-main-args {:main 'a.b})))
  (is (= ["a" "b"] (makejack/clojure-cli-main-args {:main-args ["a" "b"]})))
  (is (= ["-m" "a.b" "a" "b"]
         (makejack/clojure-cli-main-args {:main 'a.b :main-args ["a" "b"]})))
  (is (= ["-e" "(+ 1)"] (makejack/clojure-cli-main-args {:expr '(+ 1)})))
  )
