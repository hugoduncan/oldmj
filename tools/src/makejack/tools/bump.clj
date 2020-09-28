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
  (let [components (mapv maybe-long (str/split version #"[.-]"))]
    (zipmap
     [:major :minor :incremental :qualifier]
     components)))

(defmulti current-version
  (fn [version-source _project] (:type version-source)))

(defmethod current-version :project-edn
  [_version-source {:keys [version]}]
  (read-version-map version))

(defmethod current-version :version-edn
  [{:keys [path]} {:keys [version]}]
  (edn/read-string (slurp path)))

(defn next-version [version-map [part value]]
  (let [part-kw (keyword part)]
    (cond
      value                           (assoc version-map part-kw (maybe-long value))
      (number? (part-kw version-map)) (update version-map part-kw inc)
      :else                           (throw
                                       (ex-info
                                        "Must supply a value for non-numeric part"
                                        {:version-map version-map
                                         :part        part-kw})))))


(defmulti update-version-source
  (fn [version-source _new-version _options]
    (:type version-source)))

(defmethod update-version-source :project-edn
  [_version-source version-map {:keys [dir]}]
  (let [project-file    (path/path-for dir "project.edn")
        project-str     (slurp (path/as-file project-file))
        version-str     (re-find #":version\s+\".*\"" project-str)
        new-version-str (str/replace
                         version-str
                         #"\".*\""
                         (pr-str (util/format-version-map version-map)))
        project-str     (str/replace project-str version-str new-version-str)]
    (spit (path/as-file project-file) project-str)))

(defmethod update-version-source :version-edn
  [{:keys [path]} version-map _options]
  (spit path (pr-str version-map)))

(defn bump
  "Bump project version"
  [options args {:keys [mj project]}]
  (let [version-source (infer-source options)
        version-map    (current-version version-source project)
        new-version    (next-version version-map args)]
    (update-version-source version-source new-version options)
    ;; (doseq [update (:updates options)]
    ;;   (update-version update new-version))
    ))

(def extra-options
  [])

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main
     "bump"
     "[options] [:major|:minor|:incremental] [qualifier]"
     bump extra-options args)))
