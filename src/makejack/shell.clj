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
        res           (makejack/sh args (:options config))]
    (if (pos? (:exit res))
      (makejack/error "makejack.shell tool failed"))))
