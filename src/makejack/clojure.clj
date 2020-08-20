(ns makejack.clojure
  "Makejack tool to invoke clojure"
  (:require [makejack.api.core :as makejack]))

(def option-str->keyword
  {"-Sdeps" :sdeps
   "-A"     :aliases})

(defn parse-args [args]
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
  [args target-kw {:keys [:makejack/project] :as config} options]
  (let [{:keys [tool-options]} (parse-args args)
        aliases                (-> []
                                  (into (:aliases project))
                                  (into (:aliases options))
                                  (into (:aliases tool-options)))
        _                      (prn :aliases aliases)
        target-config          (some-> config :targets target-kw)
        deps-edn               (select-keys target-config [:deps])]
    (makejack/clojure
      aliases
      (merge deps-edn
             (:Sdeps tool-options))
      (:main-opts target-config)
      (:options target-config))
    nil))
