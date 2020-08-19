(ns makejack.main
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]
            [makejack.api.help :as help]
            [makejack.api.resolve :as resolve]
            makejack.chain)
  (:gen-class))

(defn run-command [cmd args options]
  (let [config (makejack/load-config)
        cmd (read-string cmd)
        [kw tool-sym] (resolve/resolve-tool-sym cmd config)
        f (resolve/resolve-tool tool-sym)]
    (f args kw config options)))


(defn error-msg [errors]
  (str "makejack error:\n" (str/join \newline errors)))

(def cli-options
  [["-h" "--help" "Show this help message."]
   ["-p" "--pprint" "Pretty print the makejack config."]
   ["-v" "--verbose" "Show command execution"]])


(defn -main [& args]
  (let [{:keys [arguments errors options summary]}
        (cli/parse-opts args cli-options :in-order true)]
    (cond
      errors ; errors => exit with description of errors
      (makejack/error (error-msg errors))

      (:help options)
      (help/usage summary)

      (:pprint options)
      (pprint/pprint
        (makejack/load-config))

      (not (seq args))
      (help/usage summary)

      (= "help" (first args))
      (if-let [cmd (fnext args)]
        (help/help-on cmd)
        (help/usage summary))

      :else
      (binding [makejack/*verbose* (:verbose options)]
        (run-command (first arguments) (rest arguments) options)
        (shutdown-agents)))))
