(ns makejack.tools.deploy-test
  (:require [makejack.tools.deploy :as deploy]
            [clojure.test :refer [deftest is]]))

(deftest reository-test
  (is (= "clojars" (:name (deploy/repository nil))))
  (is (= "clojars" (:name (deploy/repository "clojars"))))
  (is (= "central" (:name (deploy/repository "central")))))
