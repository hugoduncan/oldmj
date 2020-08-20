(ns makejack.main
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]
            [makejack.api.help :as help]
            [makejack.api.run :as run]
            makejack.chain)
  (:gen-class))

(defn apply-command [cmd args options]
  (let [cmd        (read-string cmd)]
    (run/run-command cmd args options)))

(defn error-msg [errors]
  (str "makejack error:\n" (str/join \newline errors)))

(defn parse-profiles [profiles-str]
  (->> (str/split profiles-str #":")
     (filter (complement str/blank?))
     (mapv keyword)))

(def cli-options
  [["-h" "--help" "Show this help message."]
   ["-p" "--pprint" "Pretty print the makejack config."]
   ["-v" "--verbose" "Show command execution"]
   ["-P" "--profiles PROFILES" "Project profiles to apply"
    :parse-fn parse-profiles]])


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
        (run/apply-options (makejack/load-config) options nil))

      (not (seq args))
      (help/usage summary)

      (= "help" (first args))
      (if-let [cmd (fnext args)]
        (help/help-on cmd)
        (help/usage summary))

      :else
      (binding [makejack/*verbose* (:verbose options)]
        (apply-command (first arguments) (rest arguments) options)
        (shutdown-agents)))))
