(ns makejack.api.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file
            Files
            LinkOption Path Paths];
           [java.nio.file.attribute FileAttribute PosixFilePermission]))

;; (extend-protocol io/Coercions
;;   Path
;;   (as-file [p] (.toFile p)))

(defprotocol Coercions
  ;; "Coerce between various 'resource-namish' things."
  (^{:tag java.nio.file.Path} as-path [x] "Coerce argument to a path."))


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

(defn ^String str-path
  "Return a path string, passing each argument to as-path.

  Multiple-arg versions treat the first argument as parent and subsequent args as
  children relative to the parent."
  (^String [path-like]
   (str (path path-like)))
  (^String [parent child]
   (str (path parent child)))
  (^String [parent child & more]
   (str (apply path parent child more))))

(defn- char-to-int
  [c]
  (- (int c) 48))

(def POSIX-PERMS
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
  (let [[owner group others :as specs] (map char-to-int mode)
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


;; (def visit-options (doto (java.util.HashSet.)
;;                      (.add (FileVisitOption/FOLLOW_LINKS))))

;; (defn visit-root [^Path root visit-file-fn]
;;   (let [visitor (reify FileVisitor
;;                   (visitFile [_ path attrs]
;;                     (let [f (.relativize root path)]
;;                       (visit-file-fn root f))
;;                     FileVisitResult/CONTINUE)
;;                   (visitFileFailed [_ path e]
;;                     (throw (ex-info "Problem visting File" {:path path} e))))]
;;     (Files/walkFileTree root visit-options Integer/MAX_VALUE visitor)))

;; (defn source-file-filter []
;;   (let [paths (volatile! [])]
;;     [(fn [_ ^Path p]
;;        (when (str/ends-with? (str p) ".clj")
;;          (vswap! paths conj p)))
;;      paths]))

(defn clj-source-file? [p]
  (str/ends-with? (str-path p) ".clj"))

(defn relative-to ^Path [root]
  (let [root (path root)]
    (fn [p] (.relativize root (path p)))))

(defn source-files
  "Return all source files under root.
  Return a sequence of File objects."
  [path-like]
  (->> (file-seq (io/file path-like))
     (filter clj-source-file?)
     (mapv (relative-to path-like))))

(defn path->namespace [path-like]
  (-> (str-path path-like)
     (str/replace ".clj" "")
     (str/replace "_" "-")
     (str/replace "/" ".")
     symbol))

(defn mkdirs [path-like]
  (Files/createDirectories (path path-like) (make-array FileAttribute 0)))

(defn cwd
  ^Path []
  (.toAbsolutePath (path ".")))

(def link-options (make-array LinkOption 0))

(defn filename [path-like]
  (.getFileName (.toRealPath (path path-like) link-options)))


(defn file-exists? [path-like]
  (Files/exists (path path-like) link-options))

;; (filename (cwd))

;; (defn- buffer-size [opts]
;;   (or (:buffer-size opts) 1024))

;; (defn copy-with-flush
;;   [^InputStream input ^Writer output opts]
;;   (let [^"[C" buffer (make-array Character/TYPE (buffer-size opts))
;;         in (InputStreamReader. input (encoding opts))]
;;     (loop []
;;       (let [size (.read in buffer 0 (alength buffer))]
;;         (if (pos? size)
;;           (do (.write output buffer 0 size)
;;               (recur)))))))

(defn deep-merge
  "Merge maps recursively."
  [& ms]
  (letfn [(merge* [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with merge* xs)
              (last xs)))]
    (reduce merge* ms)))
