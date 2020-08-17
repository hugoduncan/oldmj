(ns makejack.babashka
  (:require [makejack.core :as makejack]
            [makejack.util :as util]))

(defn babashka [args config-kw config options]
  (let [project      (makejack/load-project)
        deps         (makejack/load-deps)
        bb-config    (get-in config [:targets config-kw])

        aliases      (:aliases bb-config)
        form         (:form bb-config)
        cp           (cond-> ""
                       (:with-project-deps? bb-config)
                       (str ":"
                            (makejack/classpath
                              aliases
                              {}))
                       (:with-mj-deps? bb-config)
                       (str ":"
                            (makejack/classpath
                              aliases
                              (:deps config))))
        args         (cond-> []
                       (not= "" cp) (into ["-cp" cp])
                       form (into ["-e" (str form)])
                       (:args bb-config) (into (:args bb-config)))
        res (makejack/babashka args)]
    (if (pos? (:exit res))
      (makejack/error (:err res))
      (println (:out res)))))
