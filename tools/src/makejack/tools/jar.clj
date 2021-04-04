(ns makejack.tools.jar
  (:require [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.tool :as tool]
            [makejack.api.path :as path])
  (:gen-class))

(defn depstar
  "Build a jar with depstar.
  If `:jar-type` is `:uberjar`, then build an uberjar, else a thin jar."
  [options _args {:keys [mj project] :as _config}]
  (let [aliases      (-> []
                         (into (:aliases project))
                         (into (:aliases options)))
        deps         '{:aliases
                       {:mj-depstar
                        {:replace-deps
                         {seancorfield/depstar
                          {:mvn/version "2.0.211"}}}}}
        main         (:main project)
        target-path  (:target-path mj)
        jar-name     (or (:jar-name project)
                         (makejack/default-jar-name project))
        jar-path     (str (path/path target-path jar-name))
        uberjar?     (= :uberjar (:jar-type project))
        exclusions   (:jar-exclusions project)
        depstar-fn   (if uberjar?
                       "hf.depstar/uberjar"
                       "hf.depstar/jar")
        depstar-args (cond-> {}
                       (and uberjar? main) (assoc :main-class (str main))
                       true                (assoc :jar jar-path)
                       (:verbose options)  (assoc :verbose true)
                       exclusions          (assoc :exclude exclusions)
                       (seq aliases)       (assoc :aliases aliases))]
    (clojure-cli/process
     (concat
      (clojure-cli/args {:repro true
                         :deps  deps})
      (clojure-cli/exec-args
       {:aliases   [:mj-depstar]
        :exec-fn   depstar-fn
        :exec-args depstar-args}))
     options)))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool/parse-kw-stringlist]])

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main "jar" "[options]" depstar extra-options args)))
