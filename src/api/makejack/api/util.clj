(ns makejack.api.util
  "Helper functions to implement tools for makejack."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file
            Files
            LinkOption Path Paths];
           [java.nio.file.attribute FileAttribute PosixFilePermission]))

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
