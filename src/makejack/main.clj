(ns makejack.main
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.builtins :as builtins]
            [makejack.api.core :as makejack]
            [makejack.api.resolve :as resolve]
            makejack.chain)
  (:gen-class))

(defn run-command [cmd args options]
  (let [config (makejack/load-config)
        cmd (read-string cmd)
        [kw tool-sym] (resolve/resolve-tool-sym cmd config)
        f (resolve/resolve-tool tool-sym)]
    (f args kw config options)))

(defn tool-doc-string [f]
  (first (str/split-lines (:doc (meta f) ""))))

(defn- target-doc-string [target-map]
  (first
    (str/split-lines
      (:doc target-map
            (tool-doc-string
              (get builtins/builtins
                   (:tool target-map)))))))

(defn target-doc
  "Construct odc for available tools."
  []
  (str/join
    "\n"
    (map
      (fn [[kw m]]
        (format "%25s   %s" kw (target-doc-string m)))
      (sort-by
        key
        (:targets (makejack/load-config))))))

(defn tool-doc
  "Construct odc for available tools."
  []
  (str/join
    "\n"
    (map
      (fn [[sym f]]
        (format
          "%25s   %s"
          (name sym)
          (tool-doc-string f)))
      builtins/builtins)))

(defn usage [summary]
  (println
    (str/join
      "\n"
      ["makejack [options ...] target"
       ""
       summary
       ""
       "Project targets:"
       (target-doc)
       ""
       "Available tools:"
       (tool-doc)])))

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
      (usage summary)

      (:pprint options)
      (pprint/pprint
        (makejack/load-config))

      (not (seq args))
      (usage summary)

      :else
      (binding [makejack/*verbose* (:verbose options)]
        (run-command (first arguments) (rest arguments) options)
        (shutdown-agents)))))
