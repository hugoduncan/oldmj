(ns makejack.invoke.babashka
  "Makejack tool to invoke babashka"
  (:require [makejack.api.core :as makejack]))

(defn babashka
  "Invoke babashka"
  [_args target-kw {:keys [mj project] :as _config} options]
  (let [target-config (get-in mj [:targets target-kw])
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
                               (:deps target-config))))
        args          (cond-> []
                        (not= "" cp)          (into ["-cp" cp])
                        form                  (into ["-e" (str form)])
                        (:args target-config) (into (:args target-config)))]
    (makejack/babashka
      args
      (merge
        (if (seq args)
          {}
          {:out :inherit                ; run a bb repl
           :err :inherit
           :in  :inherit})
        (:options target-config)))))
