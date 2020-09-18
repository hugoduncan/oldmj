(ns makejack.tools.install-test
  (:require [makejack.api.path :as path]
            [makejack.tools.install :as install]
            [clojure.test :refer [deftest is]]))


(deftest group-path-test
  (is (= (path/path "org" "hugoduncan")
         (install/group-path {:group-id "org.hugoduncan"}))))
