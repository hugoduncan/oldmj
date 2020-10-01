(ns makejack.invoke.shell
  "Makejack tool to invoke shell"
  (:require [makejack.api.core :as makejack]
            [makejack.impl.util :as util]))

(defn shell
  "Invoke shell command."
  [args target-kw config options]
  (let [target-config (get-in config [:mj :targets target-kw])
        args          (into (:args target-config) args)]
    (try
      (makejack/process args (merge options (:options target-config)))
      nil
      (catch clojure.lang.ExceptionInfo e
        (util/handle-invoker-exception e)))))
