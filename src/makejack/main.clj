(ns makejack.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]
            [makejack.api.resolve :as resolve])
  (:gen-class))

(def cli-options
  [["-h" "--help"]])

(defn run-command [cmd args options]
  (let [config (makejack/load-config)
        cmd (read-string cmd)
        [kw tool-sym] (resolve/resolve-tool-sym cmd config)
        f (resolve/resolve-tool tool-sym)]
    (f args kw config options)))

(defn usage [summary]
  (println "makejack [options ...] target")
  (println)
  (println summary))

(defn error-msg [errors]
  (str "makejack error:\n" (str/join \newline errors)))

(defn -main [& args]
  (let [{:keys [arguments errors options summary]} (cli/parse-opts args cli-options)]
    (cond
      errors ; errors => exit with description of errors
      (makejack/error (error-msg errors))

      (:help options)
      (usage summary)

      :else
      (do
        (run-command (first arguments) (rest arguments) options)
        (shutdown-agents)))))
