(ns makejack.invoke.chain
  "Makejack tool to invoke multiple targets"
  (:require [makejack.impl.run :as run]
            [makejack.impl.invokers :as invokers]))

(defn chain
  "Chain execution of multiple targets"
  [args target-kw config options]
  (let [target-config (get-in config [:mj :targets target-kw])
        targets       (:targets target-config)]
    (doseq [target targets]
      (run/run-command target args options))))

(alter-var-root
  #'invokers/invokers
  assoc :chain #'chain)
