(ns makejack.babashka
  "Makejack tool to invoke babashka"
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn babashka
  "Invoke babashka"
  [args target-kw config options]
  (let [project       (makejack/load-project)
        deps          (makejack/load-deps)
        target-config (get-in config [:targets target-kw])
        aliases       (:aliases target-config)
        form          (:form target-config)
        cp            (cond-> ""
                        (:with-project-deps? target-config)
                        (str ":"
                             (makejack/classpath
                               aliases
                               {}))
                        (:with-mj-deps? target-config)
                        (str ":"
                             (makejack/classpath
                               aliases
                               (:deps config))))
        args          (cond-> []
                        (not= "" cp)          (into ["-cp" cp])
                        form                  (into ["-e" (str form)])
                        (:args target-config) (into (:args target-config)))
        res           (makejack/babashka args (:options target-config))]
    (if (pos? (:exit res))
      (makejack/error (:err res))
      (println (:out res)))))
