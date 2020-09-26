(ns makejack.invoke.clojure-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.invoke.clojure :as clojure]))

(deftest clojure-test
  (let [res (clojure/clojure
             []
             :x
             {:mj      {:targets {:x {:main      'makejack.invoke.clojure-test
                                      :main-args ["a" 1]
                                      :options   {:forward-options false}}}}
              :project {:aliases [:with-tests]}}
             {:verbose true})]
    (is (= ":test-main (\"a\" \"1\")\n" (:out res)))))

(defn -main [& args]
  (prn :test-main args))
