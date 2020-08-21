(ns makejack.api.core
  (:require [aero.core :as aero]
            [clojure.string :as str]
            [babashka.process :as process]
            makejack.api.aero           ; for defmethod
            [makejack.api.default-config :as default-config]
            [makejack.api.util :as util]))

(def ^:dynamic *verbose* nil)

(defn error [s]
  (binding [*out* *err*]
    (println s)
    (System/exit 1)))

(defn load-deps* []
  (try
    (aero/read-config "deps.edn")
    (catch Exception e
      (println "Failed to read deps file deps.edn: " (str e))
      (throw e))))

(def load-deps (memoize load-deps*))

(defn load-project []
  (try
    (aero/read-config "project.edn")
    (catch Exception e
      (println "Failed to read project file project.edn: " (str e))
      (throw e))))

(defn load-default-config* []
  (aero/read-config
    (java.io.StringReader. default-config/config)
    {:resolver aero/root-resolver}))

(def load-default-config (memoize load-default-config*))

(defn load-config []
  (util/deep-merge
    (load-default-config)
    (if (util/file-exists? "mj.edn")
      (aero/read-config "mj.edn"))))

(defn clojure
  "Execute clojure"
  [aliases deps args options]
  (let [args (cond-> ["clojure"]
               (not-empty aliases) (conj (str "-A:" (str/join ":" aliases)))
               deps                (into ["-Sdeps" (str deps)])
               args                (into args))]
    (when *verbose*
      (apply println args))
    (process/process
      args
      (merge
        {:err :inherit}
        (select-keys options [:throw :out :err :in :wait])))))

(defn babashka [args options]
  (let [args (cond-> ["bb"]
               args (into args))]
    (when *verbose*
      (apply println args))
    (process/process
      args
      (merge
        {:err :inherit}
        (select-keys options [:throw :out :err :in :wait])))))

(defn sh [args options]
  (when *verbose*
    (apply println args))
  (process/process
    args
    (merge
      {:err :inherit}
      (select-keys options [:throw :out :err :in :wait]))))

(defn classpath [aliases deps]
  (let [args (cond-> ["clojure"]
               aliases (conj (str "-A:" (str/join ":" aliases)))
               deps    (into ["-Sdeps" (str deps)])
               true    (conj "-Spath"))
        _    (when *verbose* (apply println args))
        res  (process/process args {:err :inherit})]

    (-> (:out res)
       (str/replace "\n" ""))))

(defn default-jar-name
  [{:keys [jar-type] :as project}]
  (str (:name project)
       "-" (:version project)
       (if (= :uberjar jar-type) "-standalone" "")
       ".jar"))


(defn deps-paths [deps aliases]
  (mapcat
    #(some-> deps :aliases % :extra-paths)
    aliases))
