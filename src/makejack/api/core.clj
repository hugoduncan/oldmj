(ns makejack.api.core
  "Main API namespace for tools designed to work with project.edn and mj.edn"
  (:require [aero.core :as aero]
            [clojure.string :as str]
            [babashka.process :as process]
            makejack.api.aero           ; for defmethod
            [makejack.api.default-config :as default-config]
            [makejack.api.util :as util]))

(def ^:dynamic *verbose*
  "Bound to true when `--verbose` is specified."
  nil)

(defn error
  "Exit with the given error message.
   Exits with error code 1,"
  [s]
  (binding [*out* *err*]
    (println s))
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

(defn- load-default-config* []
  (aero/read-config
    (java.io.StringReader. default-config/config)
    {:resolver aero/root-resolver}))

(def ^:private load-default-config
  (memoize load-default-config*))

(defn load-config
  "Load the mj.edn config file.
   The profect.edn file is made available on the :project key."
  []
  (util/deep-merge
    (load-default-config)
    (if (util/file-exists? "mj.edn")
      (aero/read-config "mj.edn"))))

(defn clojure
  "Execute clojure process.

  aliases is a vector of keywords with deps.edn aliases to use.

  deps ia s map with external dependencies, as specifed on the :deps key
  of deps.edn.

  args is a vector of arguments to pass.

  options is a map of options, as specifed in babashka.process/process.
  Defaults to {:err :inherit}."
  [aliases deps args options]
  (let [args (cond-> ["clojure"]
               (not-empty aliases) (conj (str "-A:" (str/join ":" aliases)))
               deps                (into ["-Sdeps" (str deps)])
               args                (into args))
        args (mapv str args)]
    (when *verbose*
      (apply println args))
    (process/process
      args
      (merge
        {:err :inherit}
        (select-keys options [:throw :out :err :in :wait])))))

(defn babashka
  "Execute babashka process.

  args is a vector of arguments to pass.

  options is a map of options, as specifed in babashka.process/process.
  Defaults to {:err :inherit}."
  [args options]
  (let [args (cond-> ["bb"]
               args (into args))]
    (when *verbose*
      (apply println args))
    (process/process
      args
      (merge
        {:err :inherit}
        (select-keys options [:throw :out :err :in :wait])))))

(defn sh
  "Execute a shell process.

  args is a vector of shell arguments.

  options is a map of options, as specifed in babashka.process/process.
  Defaults to {:err :inherit}."
  [args options]
  (when *verbose*
    (apply println args))
  (process/process
    args
    (merge
      {:err :inherit}
      (select-keys options [:throw :out :err :in :wait]))))

(defn classpath
  "Returns the project classpath, with the given extra deps map.

  aliases is a vector of keywords with deps.edn aliases to use.

  deps ia s map with external dependencies, as specifed on the :deps key
  of deps.edn."
  [aliases deps]
  (let [args (cond-> ["clojure"]
               aliases (conj (str "-A:" (str/join ":" aliases)))
               deps    (into ["-Sdeps" (str deps)])
               true    (conj "-Spath"))
        _    (when *verbose* (apply println args))
        res  (process/process args {:err :inherit})]

    (-> (:out res)
       (str/replace "\n" ""))))

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
