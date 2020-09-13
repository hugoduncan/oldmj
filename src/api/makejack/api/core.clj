(ns makejack.api.core
  "Main API namespace for tools designed to work with project.edn and mj.edn"
  (:require [aero.core :as aero]
            [babashka.process :as process]
            [clojure.string :as str]
            makejack.api.aero           ; for defmethod
            [makejack.api.default-config :as default-config]
            [makejack.api.filesystem :as filesystem]))

(set! *warn-on-reflection* true)

(def ^:dynamic *verbose*
  "Bound to true when `--verbose` is specified."
  nil)

(defn error
  "Exit with the given error message.
   Exits with error code 1,"
  [s]
  (binding [*out* *err*]
    (println s))
  (shutdown-agents)
  (System/exit 1))

(defn- load-deps* []
  (try
    (aero/read-config "deps.edn")
    (catch Exception e
      (println "Failed to read deps file deps.edn: " (str e))
      (throw e))))

(def load-deps
  "Load the deps.edn file."
  (memoize load-deps*))

(defn load-project* [& [options]]
  (:project
   (aero/read-config
     (java.io.StringReader. (pr-str default-config/project-with-defaults))
     (merge
       {:resolver aero/root-resolver}
       options))))

(defn resolve-source [{:keys [resolver] :as _options} value]
  (cond
    (map? resolver) (get resolver value)
    resolver        (resolver nil value)
    :else           value))

(defn load-mj* [& [options]]
  (aero/read-config
    (if (filesystem/file-exists? "mj.edn")
      (resolve-source options "mj.edn")
      (java.io.StringReader. (pr-str default-config/default-mj)))
    (merge
      {:resolver aero/root-resolver}
      options)))

(def load-mj
  "Load the mj.edn file."
  (memoize load-mj*))

(defn load-config* [& [options]]
  (let [mj      (load-mj* options)
        project (if (filesystem/file-exists? "project.edn")
                  (load-project* options)
                  {})]
    {:mj      mj
     :project project}))

(def load-config
  "Load a map containing the project and the mj config."
  (memoize load-config*))

(def ^:no-doc process-option-keys
  [:dir :err :in :throw :out :wait])

(defn process
  "Execute a process.

  args is a vector of arguments, the first of which is the program to
  execute.  The arguments are used as strings.

  options is a map of options, as specifed in babashka.process/process.
  Defaults to {:err :inherit}."
  [args options]
  (when *verbose*
    (apply println args))
  (process/process
    (map str args)
    (merge
      {:err :inherit}
      (if *verbose* {:out :inherit})
      (select-keys options process-option-keys))))

(defn default-jar-name
  "Helper to return the default jar file name.

  When the :jar-type key of the project map specifies :uberjar, then the
  name will be for an uberjar."
  [{:keys [jar-type] :as project}]
  (str (:name project)
       "-" (:version project)
       (if (= :uberjar jar-type) "-standalone" "")
       ".jar"))

(defn deps-paths
  "Helper to return the paths for deps with the given aliases applied.

  aliases is a vector of keywords with deps.edn aliases to use.

  Returns the :paths value, with :extra-paths from the specified aliases."
  [deps aliases]
  (mapcat
    #(some-> deps :aliases % :extra-paths)
    aliases))

(def ^:dynamic *print-edn-tagged-literals* nil)

(def orgininal-pattern-print-method
  (get-method print-method java.util.regex.Pattern))

(defmethod print-method java.util.regex.Pattern
  [^java.util.regex.Pattern value ^java.io.Writer writer]
  (if *print-edn-tagged-literals*
    (.write writer ^String (pr-str (tagged-literal 'regex (.pattern value))))
    (orgininal-pattern-print-method value writer)))
