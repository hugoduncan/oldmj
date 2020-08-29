(ns makejack.api.tool-options
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]))



(defn parse-kw-stringlist [profiles-str]
  (->> (str/split profiles-str #":")
     (filter (complement str/blank?))
     (mapv keyword)))


(def cli-options
  "Recommended CLI options for tools."
  [["-h" "--help" "Show this help message."]
   ["-o" "--options OPTIONS"
    "Options as an EDN map.
    This will contain the options parsed by makejack."
    :parse-fn edn/read-string]
   ["-P" "--profiles PROFILES"
    "Project profiles to apply when executing the command."
    :parse-fn parse-kw-stringlist]
   ["-v" "--verbose" "Show command execution"]
   ])


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
  [args extra-options intro]
  (let [{:keys [arguments errors options summary]}
        (parse-options args extra-options)
        options (merge (:options options) (dissoc options :options))
        config  (makejack/load-config)
        config  (makejack/apply-options config options nil)]
    (cond
      errors
      (makejack/error
        (str/join \newline errors))

      (:help options)
      (usage summary intro)

      :else
      {:arguments arguments
       :options   options
       :errors    errors
       :config    config
       :summary   summary})))
