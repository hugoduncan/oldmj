(ns makejack.tools.pom-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.tools.pom :as pom]))

(deftest compile-test
  (is pom/pom))
