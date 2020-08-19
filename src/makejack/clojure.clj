(ns makejack.clojure
  "Makejack tool to invoke clojure"
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

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
          (assoc-in res [:options kw] (ffirst args)))
        (assoc res :args args)))))

(defn clojure
  "Execute clojure"
  [args target-kw config _options]
  (let [{:keys [options]} (parse-args args)
        ;; project           (makejack/load-project)
        ;; deps              (util/deep-merge
        ;;                     (makejack/load-deps)
        ;;                     (:sdeps options))
        target-config     (get-in config [:targets target-kw])
        aliases           (-> []
                             (into (:aliases target-config))
                             (into (:aliases options)))
        deps-edn          (select-keys target-config [:deps])
        res               (makejack/clojure
                            aliases
                            (merge deps-edn
                                   (:Sdeps options))
                            (:main-opts target-config)
                            (:options target-config))]
    ;; (if (pos? (:exit res))
    ;;   (makejack/error (:err res))
    ;;   (println (:out res)))
    nil))
