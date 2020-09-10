(ns makejack.invoke.clojure
  "Makejack tool to invoke clojure"
  (:require [makejack.api.core :as makejack]))

(def ^:private option-str->keyword
  {"-Sdeps" :sdeps
   "-A"     :aliases})

(defn- parse-args [args]
  (loop [args args
         res {}]
    (let [arg (first args)]
      (if-let [kw (option-str->keyword arg)]
        (recur
          (fnext args)
          (assoc-in res [:tool-options kw] (ffirst args)))
        (assoc res :tool-args args)))))

(defn clojure
  "Execute clojure"
  [args target-kw {:keys [mj project] :as _config} options]
  (let [tool-options     (:tool-options (parse-args args))
        target-config    (some-> mj :targets target-kw)
        aliases          (-> []
                                  (into (:aliases target-config))
                                  (into (:aliases project))
                                  (into (:aliases options))
                                  (into (:aliases tool-options)))
        deps-edn         (select-keys target-config [:deps])
        options          (merge options (:options target-config))
        forward-options? (:forward-options options true)
        repro?           (:repro options true)
        report           (:report options "stderr")
        args             (cond-> (makejack/clojure-cli-args
                                         {:deps (merge deps-edn
                                                       (:Sdeps tool-options))
                                          :repro repro?})
                           (:main target-config)
                           (into
                             (makejack/clojure-cli-main-args
                               {:report    report
                                :aliases   aliases
                                :main      (:main target-config)
                                :main-args (cond-> []
                                             forward-options?
                                             (into ["-o" options])
                                             true (into
                                                    (:main-args target-config)))})))]
    (makejack/clojure
      args
      options)))
