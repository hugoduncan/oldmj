(ns makejack.chain
  "Makejack tool to invoke mu;tiple targets"
  (:require [makejack.impl.run :as run]
            [makejack.impl.builtins :as builtins]))

(defn chain
  "Chain execution of multiple targets"
  [args target-kw config options]
  (let [target-config (get-in config [:targets target-kw])
        targets       (:targets target-config)]
    (doseq [target targets]
      (run/run-command target args options)
      ;; (let [[kw tool-sym] (resolve/resolve-tool-sym target config)
      ;;       f (resolve/resolve-tool tool-sym)]
      ;;   (f args kw config options))
      )))

(alter-var-root
  #'builtins/builtins
  assoc  'makejack.chain #'chain)
