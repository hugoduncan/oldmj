(ns makejack.api.util-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.util :as util])
  (:import [java.io File]
           [java.nio.file
            Files
            LinkOption Path Paths];
           [java.nio.file.attribute FileAttribute PosixFilePermission]))


(deftest make-temp-path-path-test
  (testing "with default options creates a path with tmp prefix and .tmp suffix"
    (let [path (util/make-temp-path {})]
      (is (util/file-exists? path))
      (is (str/starts-with? (str (util/filename path)) "tmp"))
      (is (str/ends-with? (str (util/filename path)) ".tmp"))
      (util/delete-file! path)))
  (testing "with sxplicit options creates a path with the given prefix and suffix"
    (let [path (util/make-temp-path {:prefix "pre" :suffix ".sfx"})]
      (is (util/file-exists? path))
      (is (str/starts-with? (str (util/filename path)) "pre"))
      (is (str/ends-with? (str (util/filename path)) ".sfx"))
      (util/delete-file! path)))
  (testing "with string option creates a path with the given prefix prefix"
    (let [path (util/make-temp-path "pref")]
      (is (util/file-exists? path))
      (is (str/starts-with? (str (util/filename path)) "pref"))
      (util/delete-file! path)))
  (testing "with directory option creates a path in the given directory"
    (let [dir (Files/createTempDirectory "xyz" util/empty-file-attributes)
          path (util/make-temp-path "pref")]
      (is (util/file-exists? path))
      (is (str/starts-with? (str (util/filename path)) "pref"))
      (util/delete-file! path))))

(deftest with-temp-path-test
  (let [paths (volatile! [])]
    (util/with-temp-path [path {}]
      (vreset! paths path)
      (is (util/file-exists? path))
      (is (str/starts-with? (str (util/filename path)) "tmp"))
      (is (str/ends-with? (str (util/filename path)) ".tmp")))
    (is (not (util/file-exists? @paths)))

    (util/with-temp-path [path {}
                          path2 "pref"]
      (is (util/file-exists? path))
      (is (util/file-exists? path2))
      (vreset! paths [path path2])
      (is (str/starts-with? (str (util/filename path2)) "pref")))
    (is (not (util/file-exists? (first @paths))))
    (is (not (util/file-exists? (second @paths))))))

(deftest delete-recursive-test
  (let [file-attributes (into-array FileAttribute [])
        dir (Files/createTempDirectory "delete-recursive-test" file-attributes)]
    (doseq [sub-dir (range 3)]
      (util/mkdirs (util/path dir (str sub-dir)))
      (doseq [file (range 3)]
        (Files/createFile
          (util/path dir (str sub-dir) (str file))
          (into-array FileAttribute []))))
    (util/delete-recursively! (str dir))
    (is (not (util/file-exists? dir))
        (str dir))))


(deftest wirh-temp-dir-test
  (let [capture-dir (volatile! nil)]
    (util/with-temp-dir [dir "wirh-temp-dir-test"]
      (vreset! capture-dir dir)
      (util/file-exists? dir)
      (Files/createFile
        (util/path dir "xx")
        (into-array FileAttribute []))
      (is (util/file-exists? (util/path dir "xx"))))
    (is (not (util/file-exists? (util/path @capture-dir "xx"))))
    (is (not (util/file-exists? @capture-dir)))))


(deftest file-hashes-test
  (util/with-temp-path [p "file-hashes-test"]
    (spit (util/as-file p) "something to hash")
    (let [hashes (util/file-hashes p)]
      (is (= "6f4815fdf1f1fd3f36ac295bf39d26b4" (:md5 hashes))
          "md5 matches")
      (is (= "72668bc961b0a78bfa1633f6141bcea69ca37468" (:sha1 hashes))
          "sha1 matches"))))
