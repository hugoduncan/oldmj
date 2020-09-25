(ns makejack.api.clojure-cli-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.api.core :as makejack]
            [makejack.api.clojure-cli :as clojure-cli]))

(deftest args-test
  (is (= [] (clojure-cli/args {})))
  (is (= ["-Scp" "abc"] (clojure-cli/args {:cp "abc"})))
  (is (= ["-Sdeps" "{a {:b \"s\"}}"]
         (clojure-cli/args {:deps '{a {:b "s"}}})))
  (is (= [] (clojure-cli/args {:force false})))
  (is (= ["-Sforce"] (clojure-cli/args {:force true})))
  (is (= [] (clojure-cli/args {:repro false})))
  (is (= ["-Srepro"] (clojure-cli/args {:repro true})))
  (is (= ["-Sthreads" "1"]
         (clojure-cli/args {:threads 1})))
  (is (= [] (clojure-cli/args {:verbose false})))
  (is (= ["-Sverbose"] (clojure-cli/args {:verbose true}))))

(deftest aliases-arg-test
  (is (nil? (clojure-cli/aliases-arg "-A" nil {:elide-when-no-aliases true})))
  (is (= "-A" (clojure-cli/aliases-arg "-A" nil {})))
  (is (= "-A:a" (clojure-cli/aliases-arg "-A" [:a] {})))
  (is (= "-A:a:b" (clojure-cli/aliases-arg "-A" [:a :b] {}))))

(deftest exec-args-test
  (is (thrown? clojure.lang.ExceptionInfo
               (clojure-cli/exec-args {} #{})))
  (is (= ["-X"]
         (clojure-cli/exec-args {} #{:exec-fn})))
  (is (= ["-X:a:b"]
         (clojure-cli/exec-args {:aliases [:a :b]} #{:exec-fn})))
  (is (= ["-X" "a/b"]
         (clojure-cli/exec-args {:exec-fn 'a/b} #{:exec-fn})))
  (is (= ["-X" "a/b" "[:a]" "1"]
         (clojure-cli/exec-args {:exec-fn 'a/b :exec-args {:a 1}} #{:exec-fn})))
  (is (= ["-X" "[:a :b]" "c" "[:a :d]" "1"]
         (clojure-cli/exec-args {:exec-args {:a {:b "c" :d 1}}} #{:exec-fn}))))

(deftest main-args-test
  (is (= [] (clojure-cli/main-args {} #{})))
  (is (= ["-M"] (clojure-cli/main-args {} #{:explicit-main})))
  (is (= ["-A:a"]
         (clojure-cli/main-args {:aliases [:a]} #{})))
  (is (= ["-M:a"]
         (clojure-cli/main-args {:aliases [:a]} #{:explicit-main})))
  (is (= ["-A:a:b"]
         (clojure-cli/main-args {:aliases [:a:b]} #{})))
  (is (= ["-M:a:b"]
         (clojure-cli/main-args {:aliases [:a:b]} #{:explicit-main})))
  (is (= ["-m" "a.b"]
         (clojure-cli/main-args {:main 'a.b} #{})))
  (is (= ["a" "b"]
         (clojure-cli/main-args {:main-args ["a" "b"]} #{})))
  (is (= ["-m" "a.b" "a" "b"]
         (clojure-cli/main-args {:main 'a.b :main-args ["a" "b"]} #{})))
  (is (= ["-e" "(+ 1)"] (clojure-cli/main-args {:expr '(+ 1)} #{}))))

(deftest version-test
  (is (re-matches #"\d+\.\d+\.\d+\.\d+" (clojure-cli/version))))

(deftest version-less-test
  (is (not (clojure-cli/version-less [1 0 0 0] [1 0 0 0])))
  (is (clojure-cli/version-less [1 0 0 0] [1 0 0 1]))
  (is (not (clojure-cli/version-less [1 1 0 0] [1 0 0 1])))
  (is (clojure-cli/version-less [1 1 0 0] [1 3 0 1]))
  (is (clojure-cli/version-less [1 1 0 0] [1 1 3 1])))

(deftest known-version-test
  (is (set? (clojure-cli/features*)))
  (is (= #{} (clojure-cli/features* "1.10.1.561")))
  (is (= #{:single-alias-exec-fn}
         (clojure-cli/features* "1.10.1.600")))
  (is (= #{:clojure-basis-property :exec-fn :explicit-main}
         (clojure-cli/features* "1.10.1.672")))
  (is (= #{:clojure-basis-property :exec-fn :explicit-main}
         (clojure-cli/features* "1.10.1.681"))))
