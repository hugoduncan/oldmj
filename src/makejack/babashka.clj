(ns makejack.babashka
  "Makejack tool to invoke babashka"
  (:require [makejack.api.core :as makejack]))

(defn babashka
  "Invoke babashka"
  [_args target-kw {:keys [:makejack/project] :as config} options]
  (let [target-config (get-in config [:targets target-kw])
        aliases       (-> []
                         (into (:aliases project))
                         (into (:aliases target-config))
                         (into (:aliases options)))
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
