(ns makejack.tools.bump
  "Bump version"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.api.tool :as tool]
            [makejack.api.util :as util]))

(defn infer-source
  [{:keys [dir] :as options}]
  (if (filesystem/file-exists?
       (path/path-for dir "version.edn"))
    {:type :version-edn
     :path "version.edn"}
    {:type :project-edn}))

(defn maybe-long [x]
  (try
    (Long/parseLong x)
    (catch Exception _e
      x)))

(defn read-version-map
  [version]
  {:pre [(string? version)]}
  (let [components (mapv maybe-long (str/split version #"[.-]"))]
    (zipmap
     [:major :minor :incremental :qualifier]
     components)))

(defmulti current-version
  (fn [version-source _project _options] (:type version-source)))

(defmethod current-version :project-edn
  [_version-source {:keys [version]} _options]
  (read-version-map version))

(defmethod current-version :version-edn
  [{:keys [path]} {:keys [version]} {:keys [dir]}]
  (edn/read-string (slurp (path/as-file (path/path-for dir path)))))

(defn next-version [version-map [part value]]
  (let [part-kw (keyword part)]
    (cond
      value
      (assoc version-map part-kw (maybe-long value))

      (number? (part-kw version-map))
      (update version-map part-kw inc)

      :else
      (throw
       (ex-info
        "Must supply a value for non-numeric part"
        {:version-map version-map
         :part        part-kw})))))


(defn- update-file-with-regex
  [path search-regex old-str new-str]
  (let [path            (path/path path)
        content-str     (slurp (path/as-file path))
        _               (prn :search-regex search-regex :content-str content-str)
        found-str       (re-find search-regex content-str)
        new-found-str   (str/replace found-str old-str new-str)
        new-content-str (str/replace content-str found-str new-found-str)]
    (spit (path/as-file path) new-content-str)))

(defmulti update-version-source
  (fn [version-source _old-version-map _new-version-map _options]
    (:type version-source)))

(defmethod update-version-source :project-edn
  [_version-source old-version-map new-version-map {:keys [dir]}]
  (let [project-file (path/path-for dir "project.edn")]
    (update-file-with-regex
     project-file
     #":version\s+\".*\""
     (util/format-version-map old-version-map)
     (util/format-version-map new-version-map))))

(defmethod update-version-source :version-edn
  [{:keys [path]} _old-version-map new-version-map {:keys [dir]}]
  (spit
   (path/as-file (path/path-for dir path))
   (pr-str new-version-map)))

(defmulti update-version
  (fn [filedef _old-version-map _new-version-map _options]
    (cond
      (string? filedef)    :file-literal
      (path/path? filedef) :file-literal
      {:search filedef}    :file-search)))

(defmethod update-version :file-literal
  [filedef old-version-map new-version-map {:keys [dir]}]
  (let [path            (path/path-for dir filedef)
        content-str     (slurp (path/as-file path))
        new-version-str (str/replace
                         content-str
                         (util/format-version-map old-version-map)
                         (util/format-version-map new-version-map))]
    (spit (path/as-file path) new-version-str)))

(defmethod update-version :file-search
  [{:keys [search path]} old-version-map new-version-map {:keys [dir]}]
  (update-file-with-regex
   (path/path-for dir path)
   search
   (util/format-version-map old-version-map)
   (util/format-version-map new-version-map)))

(defn bump
  "Bump project version"
  [options args {:keys [mj project]}]
  (let [version-source (infer-source options)
        version-map    (current-version version-source project options)
        new-version    (next-version version-map args)]
    (update-version-source version-source version-map new-version options)
    (doseq [update (:versioned-files project)]
      (update-version update version-map new-version options))))

(def extra-options
  [])

(defn bump-xfun [{:keys [updates dir profiles verbpse debug args] :as options}]
  (let [{:keys [mj project] :as config} (makejack/load-config options)]
    (tool/with-shutdown-agents
      (tool/with-makejack-tool ["bump" "[options]" project]
        (bump
         (dissoc options :args)
         args
         config)))))

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main
     "bump"
     "[options] [:major|:minor|:incremental] [qualifier]"
     bump extra-options args)))
