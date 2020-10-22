(ns makejack.invoke.glam
  "Makejack tool to invoke shell"
  (:require [makejack.api.core :as makejack]
            [makejack.api.glam :as glam]
            [makejack.impl.util :as util]))

(defn glam
  "Invoke shell command using glam."
  [args target-kw config options]
  (let [target-config (get-in config [:mj :targets target-kw])
        package-name  (:package-name target-config)
        tool-name     (:tool-name target-config)
        tool-path     (glam/resolve-tool package-name tool-name)
        args          (-> [tool-path]
                          (into (:args target-config))
                          (into args))]
    (try
      (makejack/process args (merge options (:options target-config)))
      nil
      (catch clojure.lang.ExceptionInfo e
        (util/handle-invoker-exception e)))))
