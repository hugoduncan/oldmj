(ns makejack.api.core
  "Main API namespace for tools designed to work with project.edn and mj.edn"
  (:require [aero.core :as aero]
            [clojure.string :as str]
            [babashka.process :as process]
            makejack.api.aero           ; for defmethod
            [makejack.api.default-config :as default-config]
            [makejack.api.project :as project]
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

(defn load-project* [& [options]]
  ;; (prn :loading-project)
  ;;(prn :load-project :options options)
  (:project
   (aero/read-config
     (java.io.StringReader. (pr-str default-config/project-with-defaults))
     (merge
       {:resolver aero/root-resolver}
       ;; {:resolver {"project.edn" "./project.edn"
       ;;             "mj.edn" "./mj.edn"}}
       ;; {:resolver #(do
       ;;               (prn :resolver %1 %2)
       ;;               (clojure.java.io/file %2))}
       options))))

(defn resolve-source [{:keys [resolver] :as _options} value]
  (cond
    (map? resolver) (get resolver value)
    resolver        (resolver nil value)
    :else           value))

(defn load-mj* [& [options]]
  ;; (prn :loading-mj :options options)
  (let [res (aero/read-config
              (if (util/file-exists? "mj.edn")
                (resolve-source options "mj.edn")
                (java.io.StringReader. (pr-str default-config/default-mj)))
              (merge
                {:resolver aero/root-resolver}
                options)
              ;; (merge
              ;;   ;; {:resolver aero/root-resolver}
              ;;   ;; {:resolver {"project.edn" "./project.edn"
              ;;   ;;             "mj.edn" "./mj.edn"}}
              ;;   ;; {:resolver #(do
              ;;   ;;               (prn :resolver %1 %2)
              ;;   ;;               (clojure.java.io/file %2))}
              ;;   options)
              )]
    ;; (prn :mj-res res)
    res)
  )

(def load-mj
  "Load the mj.edn file."
  (memoize load-mj*))

(defn load-config* [& [options]]
  (let [mj      (load-mj* options)
        ;; _       (prn :load-config* :loaded-mj)
        project (if (util/file-exists? "project.edn")
                  (load-project* options)
                  {})
        ;; _       (prn :load-config* :loaded-project)
        ]
    ;; (prn :load-config* :done)
    {:mj      mj
     :project project})
  ;; (aero/read-config
  ;;   (java.io.StringReader. (pr-str (default-config/config options)))
  ;;   options
  ;;   ;; (merge
  ;;   ;;   ;; {:resolver aero/root-resolver}
  ;;   ;;   ;; {:resolver {"project.edn" "./project.edn"
  ;;   ;;   ;;             "mj.edn" "./mj.edn"}}
  ;;   ;;   {:resolver #(do
  ;;   ;;                 (prn :resolver %1 %2)
  ;;   ;;                 (clojure.java.io/file %2))}
  ;;   ;;   options)
  ;;   )
  )

(def load-config
  (memoize load-config*))

;; (defn load-config
;;   "Load the mj.edn config file.
;;    The profect.edn file is made available on the :project key."
;;   []
;;   (util/deep-merge
;;     (load-default-config)
;;     (if (util/file-exists? "mj.edn")
;;       (aero/read-config "mj.edn"))))

;; (defn apply-options [{:keys [project] :as config} options target-kw]
;;   (let [profiles (cond-> []
;;                    target-kw (into (some-> config :targets target-kw :profiles))
;;                    true (into (:profiles options)))]
;;     (assoc config :makejack/project (project/with-profiles project profiles))))

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
        (if *verbose* {:out :inherit})
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
        (if *verbose* {:out :inherit})
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
      (if *verbose* {:out :inherit})
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
