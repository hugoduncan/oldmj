(ns makejack.core
  (:require [aero.core :as aero]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [makejack.default-config :as default-config]))

(defn error [s]
  (binding [*out* *err*]
    (println s)
    (System/exit 1)))

(defn load-deps []
  (try
    (aero/read-config "deps.edn")
    (catch Exception e
      (println "Failed to read deps file deps.edn: " (str e))
      (throw e))))

(defn load-project []
  (try
    (aero/read-config "project.edn")
    (catch Exception e
      (println "Failed to read project file project.edn: " (str e))
      (throw e))))

(defn load-config []
  (try
    (merge (aero/read-config "mj.edn")
           default-config/config)
    (catch Exception e
      (println "Failed to read makejack file mj.edn: " (str e))
      (throw e))))

(defn clojure [aliases deps args]
  (let [args (cond-> ["clojure"]
               (not-empty aliases) (conj (str "-A:" (str/join ":" aliases)))
               deps (into ["-Sdeps" (str deps)])
               args (into args))]
    (println (pr-str args))
    (apply shell/sh args)))

(defn babashka [args]
  (let [args (cond-> ["bb"]
               args (into args))]
    (prn args)
    (apply shell/sh args)))

(defn deps [aliases args]
  (let [args (cond-> []
               aliases (conj (str "-A:" (str/join ":" aliases)))
               args (into args))]
    (apply println "deps" args)
    ;; deps.clj would be better here
    (-> (clojure aliases nil args)
       :out
       (str/replace "\n" ""))))

(defn sh [args]
  (let [{:keys [exit out err]} (apply shell/sh args)]
    (when (pos? exit)
      (throw (ex-info "Failed"
                      {:args args
                       :err err
                       :out out})))
    out))

(defn classpath [aliases deps]
  (let [args (cond-> ["deps.exe"]
               aliases (conj (str "-A:" (str/join ":" aliases)))
               deps    (into ["-Sdeps" (str deps)])
               true    (conj "-Spath"))]
    (-> (sh args)
       (str/replace "\n" "")) ))

(defn default-uberjar-name [project]
  (str (:name project)
       "-" (str/join "." (:version project))
       ".jar"))
