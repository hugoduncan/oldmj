(ns makejack.invoke.shell
  "Makejack tool to invoke shell"
  (:require [makejack.api.core :as makejack]
            [makejack.impl.util :as util]))

(defn shell
  "Invoke shell command."
  [_args target-kw config _options]
  (let [target-config (get-in config [:mj :targets target-kw])
        args          (:args target-config)]
    (try
      (makejack/process args (:options target-config))
      nil
      (catch clojure.lang.ExceptionInfo e
        (util/handle-invoker-exception e)))))
