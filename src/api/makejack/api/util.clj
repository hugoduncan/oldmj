(ns makejack.api.util
  "Helper functions to implement tools for makejack."
  (:require [babashka.process :as process]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [makejack.api.path :as path])
  (:import [java.security #_DigestInputStream MessageDigest]))


(defn source-files
  "Return all source files under path-like.
  Return a sequence of File objects."
  [filter-fn path-like]
  (->> (file-seq (io/file (str path-like)))
     (filter filter-fn)
     (mapv (path/relative-to path-like))))

(defn project-source-files
  "Return all source files under path-like.
  Return a sequence of File objects relative to the current directory."
  [filter-fn path-like]
  (->> (file-seq (io/file (str path-like)))
     (filterv filter-fn)))

(defn clj-source-file?
  "Predicate for the given path being a clojure source file."
  [p]
  (str/ends-with? (str (path/path p)) ".clj"))

(defn java-source-file?
  "Predicate for the given path being a java source file."
  [p]
  (str/ends-with? (str (path/path p)) ".java"))


(defn path->namespace
  "Return namespaces found under the given root path."
  [path-like]
  (-> (path/path path-like)
     str
     (str/replace ".clj" "")
     (str/replace "_" "-")
     (str/replace "/" ".")
     symbol))

(defn deep-merge
  "Merge maps recursively."
  [& ms]
  (letfn [(merge* [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with merge* xs)
              (last xs)))]
    (reduce merge* ms)))

(defn deep-merge-with
  "Merge maps recursively, using f to merge non-map keys"
  [f & ms]
  (apply
    (fn m [& ms]
      (if (every? map? ms)
        (apply merge-with m ms)
        (apply f ms)))
    ms))

(defn format-version-map
  "Format a version map as a string."
  [{:keys [major minor incremental qualifier]}]
  (cond-> (str major)
    minor (str "." minor)
    incremental (str "." incremental)
    qualifier (str "-" qualifier)))

(defn git-sha
  "Return the current git sha as a string."
  []
  (-> ["git" "rev-parse" "--verify" "HEAD"]
     (process/process
        {:err   :inherit
         :wait  true
         :throw true})
     :out
     str/trim))

(defn- digest-string
  [^MessageDigest algorithm]
  (let [length  (* 2 (.getDigestLength algorithm))
        hex-str (.toString (BigInteger. 1 (.digest algorithm)) 16)
        pad     (str/join (repeat (- length (count hex-str)) "0"))]
    (str pad hex-str)))

(def ^:private buffer-size (* 1024 1024))

(defn- byte-buffer
  ^bytes []
  (make-array Byte/TYPE buffer-size))

(defn- read-bytes
  ^bytes [^java.io.InputStream stream]
  (let [^bytes buffer (byte-buffer)
        num-bytes (.read stream buffer)]
    (if (pos? num-bytes)
      (if (= buffer-size num-bytes)
        buffer
        (java.util.Arrays/copyOf buffer num-bytes)))))

(defn- byte-seq
  [^java.io.InputStream stream]
  (take-while some? (repeatedly (partial read-bytes stream))))

(defn- byte-seq-digest
  [digests byte-seq]
  (doseq [^MessageDigest digest digests]
    (.reset digest))
  (doseq [^bytes bytes byte-seq]
    (doseq [^MessageDigest digest digests]
      (.update digest bytes)))
  digests)

(defn file-hashes
  "Return a map with hash strings for the contents of the given path.
  The returned map has :md5 and :sha1 hash strings."
  [path]
  (let [md5  (MessageDigest/getInstance "MD5")
        sha1 (MessageDigest/getInstance "SHA1")]
    ;; (with-open [md5-digest (DigestInputStream.
    ;;                          (io/input-stream (.toFile (as-path path))) md5)
    ;;             sha1-digest (DigestInputStream. md5-digest sha1)]
    ;;   (while (> (.read sha1-digest) -1)))
    (with-open [^java.io.InputStream stream (io/input-stream (path/as-file path))]
      (byte-seq-digest [md5 sha1] (byte-seq stream))
      {:md5  (digest-string md5)
       :sha1 (digest-string sha1)})))
