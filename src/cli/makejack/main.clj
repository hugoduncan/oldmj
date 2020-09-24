(ns makejack.main
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]
            [makejack.impl.version :as version]
            [makejack.impl.help :as help]
            [makejack.impl.run :as run]
            makejack.invoke.chain
            makejack.invoke.modules)
  (:gen-class))

(defn apply-command [cmd args options]
  (let [cmd (read-string cmd)]
    (run/run-command cmd args options)
    nil))

(defn error-msg [errors]
  (str "makejack error:\n" (str/join \newline errors)))

(def cli-options
  [["-h" "--help" "Show this help message."]
   ["-p" "--pprint" "Pretty print the makejack config."]
   ["-P" "--profile PROFILE" "Project profile to apply"
    :parse-fn read-string]
   ["-d" "--debug" "Output command executions"]
   ["-v" "--verbose" "Show target execution"]
   ["-V" "--version" "Show makejack version and exit"]])


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
        (makejack/load-config {:profile (:profile options)}))

      (:version options)
      (pprint/pprint
        version/info)

      (not (seq arguments))
      (help/usage summary)

      (= "help" (first arguments))
      (if-let [cmd (fnext arguments)]
        (help/help-on cmd)
        (help/usage summary))

      :else
      (binding [makejack/*verbose* (:verbose options)
                makejack/*debug* (:debug options)]
        (try
          (apply-command (first arguments) (rest arguments) options)
          (finally
            (shutdown-agents)))))))
