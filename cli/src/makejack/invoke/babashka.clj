(ns makejack.invoke.babashka
  "Makejack tool to invoke babashka"
  (:require [makejack.api.babashka :as babashka]
            [makejack.impl.util :as util]))

(defn babashka
  "Invoke babashka"
  [args target-kw {:keys [mj project] :as _config} options]
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
                           forward-options?      (into ["-o" (dissoc options :dir)])
                           (:args target-config) (into (:args target-config))
                           args                  (into args))
        options          (merge options
                                (select-keys [:with-project-deps?] target-config))]
    (try
      (babashka/process
       aliases
       deps
       args
       (merge
        (if (seq args)
          {}
          {:out :inherit ; run a bb repl
           :err :inherit
           :in  :inherit})
        options))
      (catch clojure.lang.ExceptionInfo e
        (util/handle-invoker-exception e)))))
