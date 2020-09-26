(ns makejack.api.filesystem-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path])
  (:import [java.nio.file
            CopyOption
            Files
            StandardCopyOption];
           [java.nio.file.attribute FileAttribute]
           [java.util Arrays]))

(deftest make-temp-path-path-test
  (testing "with default options creates a path with tmp prefix and .tmp suffix"
    (let [path (filesystem/make-temp-path {})]
      (is (filesystem/file-exists? path))
      (is (str/starts-with? (str (path/filename path)) "tmp"))
      (is (str/ends-with? (str (path/filename path)) ".tmp"))
      (filesystem/delete-file! path)))
  (testing "with sxplicit options creates a path with the given prefix and suffix"
    (let [path (filesystem/make-temp-path {:prefix "pre" :suffix ".sfx"})]
      (is (filesystem/file-exists? path))
      (is (str/starts-with? (str (path/filename path)) "pre"))
      (is (str/ends-with? (str (path/filename path)) ".sfx"))
      (filesystem/delete-file! path)))
  (testing "with string option creates a path with the given prefix prefix"
    (let [path (filesystem/make-temp-path "pref")]
      (is (filesystem/file-exists? path))
      (is (str/starts-with? (str (path/filename path)) "pref"))
      (filesystem/delete-file! path)))
  (testing "with directory option creates a path in the given directory"
    (let [dir  (Files/createTempDirectory "xyz" filesystem/empty-file-attributes)
          path (filesystem/make-temp-path {:prefix "pref" :dir dir})]
      (is (filesystem/file-exists? path))
      (is (= dir (path/parent path)))
      (is (str/starts-with? (str (path/filename path)) "pref"))
      (filesystem/delete-file! path))))

(deftest with-temp-path-test
  (let [paths (volatile! [])]
    (filesystem/with-temp-path [path {}]
      (vreset! paths path)
      (is (filesystem/file-exists? path))
      (is (str/starts-with? (str (path/filename path)) "tmp"))
      (is (str/ends-with? (str (path/filename path)) ".tmp")))
    (is (not (filesystem/file-exists? @paths)))

    (filesystem/with-temp-path [path {}
                                path2 "pref"]
      (is (filesystem/file-exists? path))
      (is (filesystem/file-exists? path2))
      (vreset! paths [path path2])
      (is (str/starts-with? (str (path/filename path2)) "pref")))
    (is (not (filesystem/file-exists? (first @paths))))
    (is (not (filesystem/file-exists? (second @paths))))))

(deftest delete-recursive-test
  (let [file-attributes (into-array FileAttribute [])
        dir             (Files/createTempDirectory
                         "delete-recursive-test" file-attributes)]
    (doseq [sub-dir (range 3)]
      (filesystem/mkdirs (path/path dir (str sub-dir)))
      (doseq [file (range 3)]
        (Files/createFile
         (path/path dir (str sub-dir) (str file))
         (into-array FileAttribute []))))
    (filesystem/delete-recursively! (str dir))
    (is (not (filesystem/file-exists? dir))
        (str dir))))

(deftest wirh-temp-dir-test
  (let [capture-dir (volatile! nil)]
    (filesystem/with-temp-dir [dir "wirh-temp-dir-test"]
      (vreset! capture-dir dir)
      (filesystem/file-exists? dir)
      (Files/createFile
       (path/path dir "xx")
       (into-array FileAttribute []))
      (is (filesystem/file-exists? (path/path dir "xx"))))
    (is (not (filesystem/file-exists? (path/path @capture-dir "xx"))))
    (is (not (filesystem/file-exists? @capture-dir)))))

(deftest copy-options-test
  (is (Arrays/equals
       ^"[Ljava.nio.file.CopyOption;" (into-array CopyOption [])
       (filesystem/copy-options {})))
  (is (Arrays/equals
       ^"[Ljava.nio.file.CopyOption;" (into-array
                                       CopyOption
                                       [StandardCopyOption/COPY_ATTRIBUTES])
       (filesystem/copy-options {:copy-attributes true})))
  (is (Arrays/equals
       ^"[Ljava.nio.file.CopyOption;" (into-array
                                       CopyOption
                                       [StandardCopyOption/REPLACE_EXISTING])
       (filesystem/copy-options {:replace-existing true})))
  (is (Arrays/equals
       ^"[Ljava.nio.file.CopyOption;" (into-array
                                       CopyOption
                                       [StandardCopyOption/COPY_ATTRIBUTES
                                        StandardCopyOption/REPLACE_EXISTING])
       (filesystem/copy-options {:copy-attributes true :replace-existing true}))))
