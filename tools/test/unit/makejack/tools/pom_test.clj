(ns makejack.tools.pom-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.tools.pom :as pom]))

(deftest pom-test
  (is pom/pom))
