(ns makejack.api.tool
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]))

(set! *warn-on-reflection* true)

(defn parse-kw-stringlist [kwlist-str]
  (->> (str/split kwlist-str #":")
       (filter (complement str/blank?))
       (mapv keyword)))

(def cli-options
  "Recommended CLI options for tools."
  [["-h" "--help" "Show this help message."]
   ["-o" "--options OPTIONS"
    "Options as an EDN map.
    This will contain the options parsed by makejack."
    :parse-fn edn/read-string]
   ["-P" "--profile "
    "Project profile to apply when executing the command."
    :parse-fn read-string
    :default :default]
   ["-d" "--debug" "Output command executions"]
   ["-v" "--verbose" "Show target execution"]])

(defn parse-options
  "Parse the cli options, with the given extra options."
  [args extra-options]
  (let [options (into cli-options extra-options)]
    (cli/parse-opts args options :in-order true)))

(defn usage [intro summary]
  (println
   (str/join
    "\n"
    [intro
     ""
     summary]))
  (System/exit 0))

(defn parse-options-and-apply-to-config
  "Return a map with :arguments, :options, :errors, :config keys."
  [args extra-options tool-name tool-syntax]
  (let [{:keys [arguments errors options summary]}
        (parse-options args extra-options)
        options (merge (:options options) (dissoc options :options))
        config  (makejack/load-config {:profile (:profile options)})]
    (cond
      errors
      (makejack/error
       (str/join \newline errors))

      (:help options)
      (usage summary (str tool-name " " tool-syntax))

      :else
      {:arguments arguments
       :options   options
       :errors    errors
       :config    config
       :summary   summary})))

(defmacro with-makejack-tool
  "When verbose, output a message with the tool and the project co-ordinates."
  [[tool-name options project] & body]
  `(makejack/with-output-bindings [~options]
     (makejack/verbose-println ~tool-name (makejack/project-description ~project))
     ~@body))

(defn dispatch-main
  "Dispatch main to tool-fn, parsing command-line args with extra-options.

  Uses cli-options/cli-options with extra-options to parse an options
  map and args.  Loads the mj config file and the project.edn file with
  the profile specified by any --profile option.

  Executes tool-fn with *verbose* and *debug* nound according to the
  parsed options.

  The tool-fn should have the following signature:
     (fn [options args {:keys [mj project] :as config}])"
  [tool-name tool-syntax tool-fn extra-options args]
  (let [{:keys [arguments options] {:keys [project] :as config} :config}
        (parse-options-and-apply-to-config
         args extra-options tool-name tool-syntax)]
    (with-makejack-tool [tool-name options project]
      (tool-fn options arguments config))))

(defmacro with-shutdown-agents
  "Ensure that (shutdown-agents) is called after the block."
  [& body]
  `(try
     ~@body
     (finally
       (shutdown-agents))))
