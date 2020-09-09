(ns makejack.api.util
  "Helper functions to implement tools for makejack."
  (:require [babashka.process :as process]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file
            CopyOption
            Files
            LinkOption Path Paths
            StandardCopyOption]
           [java.nio.file.attribute FileAttribute PosixFilePermission];
           [java.security #_DigestInputStream MessageDigest]))

;; bb doesn't like this
;; (extend-protocol io/Coercions
;;   Path
;;   (as-file [p] (.toFile p)))

(defprotocol Coercions
  ;; "Coerce between various 'resource-namish' things."
  (^{:tag java.nio.file.Path} as-path [x]
   "Coerce argument to a Path."))

(def ^:private empty-strings (make-array String 0))

(extend-protocol Coercions
  nil
  (as-path [_] nil)

  Path
  (as-path [p] p)

  String
  (as-path [s] (Paths/get s empty-strings))

  File
  (as-path [f] (.toPath f)))

(defn as-file ^File [^Path path-like]
  (.toFile (as-path path-like)))

(defn ^Path path
  "Return a java.nio.file.Path, passing each argument to as-path.

  Multiple-arg versions treat the first argument as parent and subsequent args as
  children relative to the parent."
  (^Path [path-like]
   (as-path path-like))
  (^Path [parent child]
   (.resolve (as-path parent) (str child)))
  (^Path [parent child & more]
   (reduce path (path parent child) more)))

(defn- char-to-int
  [c]
  (- (int c) 48))

(def ^:private POSIX-PERMS
  {:owner  [PosixFilePermission/OWNER_EXECUTE
            PosixFilePermission/OWNER_WRITE
            PosixFilePermission/OWNER_READ]
   :group  [PosixFilePermission/GROUP_EXECUTE
            PosixFilePermission/GROUP_WRITE
            PosixFilePermission/GROUP_READ]
   :others [PosixFilePermission/OTHERS_EXECUTE
            PosixFilePermission/OTHERS_WRITE
            PosixFilePermission/OTHERS_READ]})

(defn chmod
  "Change file mode, given octal mode specification as string."
  [path-like mode]
  (let [specs (map char-to-int mode)
        perms (reduce
                (fn [perms [who spec]]
                  (cond-> perms
                    (pos? (bit-and spec 1)) (conj (first (POSIX-PERMS who)))
                    (pos? (bit-and spec 2)) (conj (second (POSIX-PERMS who)))
                    (pos? (bit-and spec 4)) (conj (last (POSIX-PERMS who)))))
                #{}
                (map vector [:owner :group :others] specs))]
    (Files/setPosixFilePermissions
      (path path-like)
      perms)))

(defn clj-source-file?
  "Predicate for the given path being a clojure source file."
  [p]
  (str/ends-with? (str (path p)) ".clj"))

(defn java-source-file?
  "Predicate for the given path being a java source file."
  [p]
  (str/ends-with? (str (path p)) ".java"))

(defn relative-to
  "Return a function that will return its argument path relative to the given root."
  [root]
  (let [root (path root)]
    (fn ^Path [p] (.relativize root (path p)))))

(defn source-files
  "Return all source files under root.
  Return a sequence of File objects."
  [filter-fn path-like]
  (->> (file-seq (io/file (str path-like)))
     (filter filter-fn)
     (mapv (relative-to path-like))))

(defn path->namespace
  "Return namespaces found under the given root path."
  [path-like]
  (-> (path path-like)
     str
     (str/replace ".clj" "")
     (str/replace "_" "-")
     (str/replace "/" ".")
     symbol))

(defn mkdirs
  "Ensure the given path exists."
  [path-like]
  (Files/createDirectories (path path-like) (make-array FileAttribute 0)))

(defn cwd
  "Rturn the current working directory as a Path."
  ^Path []
  (.toAbsolutePath (path ".")))

(def ^:private link-options (make-array LinkOption 0))

(defn filename
  "Return the filename segment of the given path as a Path."
  ^Path [path-like]
  (.getFileName (.toRealPath (path path-like) link-options)))


(defn file-exists?
  "Predicate for the given path existing."
  [path-like]
  (Files/exists (path path-like) link-options))

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

(defn git-sha []
  (-> ["git" "rev-parse" "--verify" "HEAD"]
     (process/process
        {:err   :inherit
         :wait  true
         :throw true})
     :out
     str/trim))

(defn file?
  "Predicate for path referring to a file."
  [path-like]
  (.isFile (.toFile (path path-like))))

(def copy-options
  (into-array
    CopyOption
    [StandardCopyOption/COPY_ATTRIBUTES]))

(defn copy-file!
  [source-path target-path]
  (Files/copy (as-path source-path) (as-path target-path) copy-options))

(defn list-paths
  "Return a lazy sequence of paths under path in depth first order."
  [path]
  ;; (->> (Files/walk (as-path path) fvos)
  ;;    (.iterator)
  ;;    iterator-seq)
  (->> (file-seq (io/file path))
     (map as-path)))

(defn delete-file!
  "Delete the file at the specified path-like.
  Semantics as for java.nio.file.Files/delete."
  [path-like]
  (Files/delete (as-path path-like)))

(defn delete-recursively!
  [path]
  (let [paths (->> (list-paths path)
                 (sort-by identity (comp - compare))
                 vec)]
    (doseq [^Path path paths]
      (.delete (.toFile path)))))

(defn delete-on-exit-if-exists! [path-like]
  (let [path (as-path path-like)]
    (-> (java.lang.Runtime/getRuntime)
       (.addShutdownHook
         (Thread.
           (fn []
             (when (file-exists? path)
               (delete-recursively! path)
               (delete-file! path))))))))

(def empty-file-attributes (into-array FileAttribute []))

(defn ^:no-doc with-bindings-macro
  [bindings body macro-sym macro-fn]
  {:pre [(vector? bindings) (even? (count bindings))]}
  (cond
    (not (seq bindings))
    `(do ~@body)

    (symbol? (bindings 0))
    (macro-fn
      (subvec bindings 0 2)
      [`(~macro-sym ~(subvec bindings 2)
         ~@body)])

    :else
    (throw (IllegalArgumentException.
             (str (name macro-sym) " only allows [symbol value] pairs in bindings")))))


(defn make-temp-path
  "Return a temporary file path.

  The options map can pass the keys:

  :delete-on-exit - delete the file on JVM exit (default false)
  :dir - the directory in which to create the file (defaults to the system temp dir).
         Must be a path-like.
  :prefix - prefix for the file name (default \"tmp\").
            Must be at elast three characters long.
  :suffix - suffix for the file name (default \".tmp\")

  As a shortcut, a prefix string can be passed instead of the options mao."
  ^Path [options-or-prefix]
  (let [{:keys [delete-on-exit dir prefix suffix] :or {suffix ".tmp"}}
        (if (map? options-or-prefix)
          options-or-prefix)
        prefix (or prefix
                   (and (string? options-or-prefix) options-or-prefix)
                   "tmp")
        path   (if dir
                 (Files/createTempFile
                   (as-path dir)
                   prefix
                   suffix
                   empty-file-attributes)
                 (Files/createTempFile
                   prefix
                   suffix
                   empty-file-attributes))]
    (when delete-on-exit
      (delete-on-exit-if-exists! path))
    path))


(defn ^:no-doc with-temp-path-fn
  [[sym prefix-or-options] body]
  `(let [~sym    (make-temp-path ~prefix-or-options)
         delete# (if (map? ~prefix-or-options)
                   (:delete ~prefix-or-options true)
                   true)]
     (try
       ~@body
       (finally
         (if delete#
           (delete-file! ~sym))))))


(defmacro with-temp-path
  "A scope with sym bound to a java.io.File object for a temporary
  file in the system's temporary directory.

  Options is a map with the keys:

  :delete - delete file when leaving scope (default true)
  :delete-on-exit - delete the file on JVM exit (default false)
  :dir - directory to create the file in (default is the system temp dir).
         Must be of type that can be passed to clojure.java.io/file.
  :prefix - prefix for the file name (default \"tmp\")
            Must be at elast three characters long.
  :suffix - suffix for the file name (default \".tmp\")"
  [[sym prefix-or-options & more :as bindings] & body]
  (with-bindings-macro bindings body `with-temp-path with-temp-path-fn))

(defn ^File make-temp-dir
  "Return a newly created temporary directory.
  Prefix is an arbitary string that is used to name the directory.
  Options is a map with the keys:
  :delete-on-exit - delete the dir on JVM exit (default true).
  :dir - directory to create the dir in (default is the system temp dir).
         Must be of type that can be passed to clojure.java.io/dir.
  :prefix - a string that is used to name the directory."
  [prefix-or-options]
  ^Path {:pre [(or (string? prefix-or-options) (map? prefix-or-options))]}
  (let [prefix (if (string? prefix-or-options)
                 prefix-or-options
                 (:prefix prefix-or-options))
        {:keys [delete-on-exit dir]
         :or   {delete-on-exit true}
         :as   options} (if (map? prefix-or-options) prefix-or-options {})
        _ (assert (string? prefix))
        _ (assert (map? options))
        dir (if dir (.toPath (io/file dir)))
        file-attributes (into-array FileAttribute [])
        file (..
               (if dir
                 (Files/createTempDirectory dir prefix file-attributes)
                 (Files/createTempDirectory prefix file-attributes))
               (toFile))]
    (when delete-on-exit
      (-> (java.lang.Runtime/getRuntime)
          (.addShutdownHook
            (Thread.
              (fn []
                (when (file-exists? file)
                  (delete-recursively! file)
                  (.delete file)))))))
    file))


(defn ^:no-doc with-temp-dir-fn
  [[sym prefix-or-options] body]
  `(let [~sym (make-temp-dir ~prefix-or-options)]
     (try
       ~@body
       (finally
         (delete-recursively! ~sym)
         (.delete ~sym)))))


(defmacro with-temp-dir
  "bindings => [name prefix-or-options ...]

  Evaluate body with names bound to java.io.File
  objects of newly created temporary directories, and a finally clause
  that deletes them recursively in reverse order.

  Prefix is a string that is used to name the directory.
  Options is a map with the keys:
  :delete-on-exit - delete the dir on JVM exit (default true)
  :dir - directory to create the dir in (default is the system temp dir).
         Must be of type that can be passed to clojure.java.io/dir.
  :prefix - a string that is used to name the directory."
  [bindings & body]
  {:pre [(vector? bindings) (even? (count bindings))]}
  (with-bindings-macro bindings body `with-temp-dir with-temp-dir-fn))

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
  (let [buffer (byte-buffer)
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
    (with-open [stream (io/input-stream (.toFile (as-path path)))]
      (byte-seq-digest [md5 sha1] (byte-seq stream))
      {:md5  (digest-string md5)
       :sha1 (digest-string sha1)})))

(defn path-with-extension
  "Return the path with extension added to it.
  The extension is a string, including any required dot."
  ^Path [path-like extension]
  (let [base-path (as-path path-like)
        parent-path (.getParent base-path)
        filename (str (.getFileName base-path) extension)]
    (if parent-path
      (path parent-path filename)
      (path filename))))

;; (file-hashes "/etc/hosts")
;; "04F186E74288A10E09DFBF8A88D64A1F33C0E698AAA6B75CDB0AC3ABA87D5644"
