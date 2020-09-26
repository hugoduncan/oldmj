(ns makejack.api.path-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.api.path :as path])
  (:import [java.io File]))

(deftest path-test
  (is (path/path? (path/path ".")))
  (is (path/path? (path/path (File. ".")))))

(deftest filename-test
  (is (= (path/path "fn") (path/filename (path/path "a/b/fn"))))
  (is (path/path? (path/filename (path/path "a/b/fn")))))

(deftest path-with-extension-test
  (is (= (path/path "a/b/fn.abc") (path/path-with-extension "a/b/fn" ".abc")))
  (is (= (path/path "fn.abc") (path/path-with-extension "fn" ".abc"))))
