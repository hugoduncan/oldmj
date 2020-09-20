(ns makejack.invoke.modules
  "Makejack tool to invoke targets on sub projects"
  (:require [makejack.api.core :as makejack]
            [makejack.impl.run :as run]
            [makejack.impl.invokers :as invokers]))

(defn modules
  "Execute target on sub-projects.

  Sub-project directories are specified on the target's :modules key."
  [args target-kw config options]
  (let [target-config (get-in config [:mj :targets target-kw])
        modules       (:modules target-config)]
    (when-not (sequential? modules)
      (makejack/error
        (str "ERROR: the :modules key of the "
             target-kw
             " target must specify a vector of subprojects")))
    (doseq [module modules]
      (when makejack/*verbose*
        (println "Running" (first args) "in" module))
      (run/run-command
        (first args)
        (rest args)
        (assoc options :dir module)))
    (run/run-command
      (first args)
      (rest args)
      options)))

(alter-var-root
  #'invokers/invokers
  assoc :modules #'modules)
