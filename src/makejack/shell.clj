(ns makejack.shell
  "Makejack tool to invoke shell"
  (:require [clojure.java.shell :as shell]
            [makejack.api.core :as makejack]))

(defn shell
  "Invoke shell command."
  [args target-kw config options]
  (let [project       (makejack/load-project)
        deps          (makejack/load-deps)
        target-config (get-in config [:targets target-kw])
        args          (:args target-config)
        res           (apply shell/sh args)]
    (if (pos? (:exit res))
      (makejack/error (:err res))
      (println (:out res)))))
