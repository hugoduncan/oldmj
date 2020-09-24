(ns makejack.impl.run
  (:require [makejack.api.core :as makejack]
            [makejack.impl.resolve :as resolve]))

(defn run-command [cmd args options]
  (let [config       (makejack/load-config {:profile (:profile options)
                                            :dir (:dir options)})
        target-kw     (keyword cmd)
        target        (resolve/resolve-target target-kw config)
        f             (resolve/resolve-target-invoker target)]
    (when-not target
      (makejack/error (str "Unknown target: " cmd)))
    (when-not f
      (makejack/error (str "Invalid invoker for target: " cmd)))
    (f args target-kw config options)))
