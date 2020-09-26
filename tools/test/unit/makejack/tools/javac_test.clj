(ns makejack.tools.javac-test
  (:require [makejack.tools.javac :as javac]
            [clojure.test :refer [deftest is]]))

(deftest compile-test
  (is javac/javac))
