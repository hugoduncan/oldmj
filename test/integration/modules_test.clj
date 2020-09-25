(ns modules-test
  (:require [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]))

(deftest modules-test
  (testing "mj modules pom"
    (makejack/process ["../../target/mj-script" "--verbose" "modules" "pom"]
                      {:dir "test-resources/test_modules"})
    (let [pom-files [(path/path "test-resources" "test_modules" "pom.xml")
                     (path/path "test-resources" "test_modules" "a" "pom.xml")
                     (path/path "test-resources" "test_modules" "b" "pom.xml")]]
      (testing "creates pom files"
        (doseq [pom-file pom-files]
          (is (filesystem/file-exists? pom-file)))))))
