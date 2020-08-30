(ns makejack.invoke.shell
  "Makejack tool to invoke shell"
  (:require [makejack.api.core :as makejack]))

(defn shell
  "Invoke shell command."
  [_args target-kw config _options]
  (let [target-config (get-in config [:mj :targets target-kw])
        args          (:args target-config)
        res           (makejack/sh args (:options target-config))]
    (if (pos? (:exit res))
      (makejack/error "makejack.shell tool failed"))))
