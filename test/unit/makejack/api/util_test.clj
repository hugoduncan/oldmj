(ns makejack.api.util-test
  (:require [clojure.test :refer [deftest is]]
            [makejack.api.util :as util])
  (:import [java.io File]
           [java.nio.file
            Files
            LinkOption Path Paths];
           [java.nio.file.attribute FileAttribute PosixFilePermission]))


(deftest delete-recursive-test
  (let [file-attributes (into-array FileAttribute [])
        dir (Files/createTempDirectory "delete-recursive-test" file-attributes)]
    (doseq [sub-dir (range 3)]
      (util/mkdirs (util/path dir (str sub-dir)))
      (doseq [file (range 3)]
        (Files/createFile
          (util/path dir (str sub-dir) (str file))
          (into-array FileAttribute []))))
    (util/delete-recursively (str dir))
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
  (util/with-temp-file [f "file-hashes-test"]
    (spit f "something to hash")
    (let [hashes (util/file-hashes f)]
      (is (= "6F4815FDF1F1FD3F36AC295BF39D26B4" (:md5 hashes))
          "md5 matches")
      (is (= "72668BC961B0A78BFA1633F6141BCEA69CA37468" (:sha1 hashes))
          "sha1 matches"))))
