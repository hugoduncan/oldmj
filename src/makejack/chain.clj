(ns makejack.chain
  "Makejack tool to invoke mu;tiple targets"
  (:require [makejack.api.resolve :as resolve]
            [makejack.api.builtins :as builtins]))

(defn chain [args target-kw config options]
  (let [target-config (get-in config [:targets target-kw])
        targets       (:targets target-config)]
    (prn "chain" targets)
    (doseq [target targets]
      (let [[kw tool-sym] (resolve/resolve-tool-sym target config)
            f (resolve/resolve-tool tool-sym)]
        (prn "chain run" kw f)
        (f args kw config options)))))

(alter-var-root #'builtins/builtins assoc 'makejack.chain chain)
