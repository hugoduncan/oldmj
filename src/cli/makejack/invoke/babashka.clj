(ns makejack.invoke.babashka
  "Makejack tool to invoke babashka"
  (:require [makejack.api.core :as makejack]))

(defn babashka
  "Invoke babashka"
  [_args target-kw {:keys [mj project] :as _config} options]
  (let [target-config    (get-in mj [:targets target-kw])
        aliases          (-> []
                            (into (:aliases project))
                            (into (:aliases target-config))
                            (into (:aliases options)))
        form             (:form target-config)
        deps             (:deps target-config)
        forward-options? (:forward-options options true)
        args             (cond-> []
                           form                  (into ["-e" (str form)])
                           (:main target-config) (into ["-m" (:main target-config)])
                           true                  (into (:main-args target-config))
                           forward-options?      (into ["-o" options])
                           (:args target-config) (into (:args target-config)))
        options          (merge options
                                  (select-keys [:with-project-deps?] target-config))]
    (makejack/babashka
      aliases
      deps
      args
      (merge
        (if (seq args)
          {}
          {:out :inherit ; run a bb repl
           :err :inherit
           :in  :inherit})
        options))))
