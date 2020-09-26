(ns makejack.api.util-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.api.util :as util]))

(deftest clojure-source-file?-test
  (is (util/clj-source-file? "abc.clj"))
  (is (not (util/clj-source-file? "abc.clja")))
  (is (not (util/clj-source-file? "abcclj"))))

(deftest file-hashes-test
  (filesystem/with-temp-path [p "file-hashes-test"]
    (spit (path/as-file p) "something to hash")
    (let [hashes (util/file-hashes p)]
      (is (= "6f4815fdf1f1fd3f36ac295bf39d26b4" (:md5 hashes))
          "md5 matches")
      (is (= "72668bc961b0a78bfa1633f6141bcea69ca37468" (:sha1 hashes))
          "sha1 matches"))))
