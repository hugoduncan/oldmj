(ns makejack.api.core
  "Main API namespace for tools designed to work with project.edn and mj.edn"
  (:require [aero.core :as aero]
            [babashka.process :as process]
            makejack.api.aero           ; for defmethod
            [makejack.api.default-config :as default-config]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]))

(set! *warn-on-reflection* true)

(def ^:dynamic *verbose*
  "Bound to true when `--verbose` is specified."
  nil)

(def ^:dynamic *debug*
  "Bound to true when `--debug` is specified."
  nil)

(defn error
  "Exit with the given error message and exit code.
   The default exit-code is 1,"
  ([message] (error message 1))
  ([message exit-code]
   (binding [*out* *err*]
     (println message))
   (shutdown-agents)
   (System/exit exit-code)))

(defn- load-deps* []
  (try
    (aero/read-config "deps.edn")
    (catch Exception e
      (println "Failed to read deps file deps.edn: " (str e))
      (throw e))))

(def load-deps
  "Load the deps.edn file."
  (memoize load-deps*))

(defn- maybe-path
  "Return a path from source if source can be understood as a path."
  [source]
  (try
    (path/as-path source)
    (catch java.lang.IllegalArgumentException _ nil)))

(defn relative-to-resolver
  "Resolves relative to the source file, or to the given directory."
  [dir]
  (fn relative-to-resolver [source include]
    (let [fl (if (path/absolute? include)
               include
               (if-let [source-path (maybe-path source)]
                 (if-let [parent (path/parent source-path)]
                   (path/path parent include)
                   include)
                 (if dir
                   (path/path dir include)
                   include)))]
      (if (and fl (filesystem/file-exists? fl))
        (path/as-file fl)
        (java.io.StringReader. (pr-str {:aero/missing-include include}))))))

(defn project-path [dir]
  (if dir
    (path/path dir "project.edn")
    (path/path "project.edn")))

(defn load-project* [& [{:keys [dir] :as options}]]
  (:project
   (aero/read-config
    (java.io.StringReader.
     (pr-str default-config/project-with-defaults))
    (merge
     {:resolver (relative-to-resolver dir)}
     options))))

(defn resolve-source [{:keys [resolver] :as _options} value]
  (cond
    (map? resolver) (get resolver value)
    resolver        (resolver nil value)
    :else           value))

(defn mj-path [dir]
  (if dir
    (path/path dir "mj.edn")
    (path/path "mj.edn")))

(defn load-mj* [& [{:keys [dir] :as options}]]
  (aero/read-config
   (if (filesystem/file-exists? (mj-path nil))
     (resolve-source options (path/as-file (mj-path dir)))
     (java.io.StringReader. (pr-str default-config/default-mj)))
   (merge
    {:resolver (relative-to-resolver dir)}
    options)))

(def load-mj
  "Load the mj.edn file."
  (memoize load-mj*))

(defn load-config* [& [{:keys [dir] :as options}]]
  (let [mj      (load-mj* options)
        project (if (filesystem/file-exists? (project-path dir))
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
  (when *debug*
    (apply println (into args (when-let [dir (:dir options)] ["in" dir]))))
  (process/process
   (map str args)
   (merge
    {:err :inherit}
    (when *debug* {:out :inherit})
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

(defmacro with-output-bindings [[options] & body]
  `(binding [*verbose* (:verbose ~options)
             *debug*   (:debug ~options)]
     ~@body))

(defn project-description
  [{:keys [group-id name version] :as _project}]
  (str group-id  "/" name " " version))

(defn verbose-println
  [& args]
  (when *verbose*
    (apply println args)))

(defmacro with-makejack-tool [[tool-name options project] & body]
  `(with-output-bindings [~options]
     (verbose-println ~tool-name (project-description ~project))
     ~@body))
