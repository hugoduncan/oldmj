(ns makejack.clojure
  "Makejack tool to invoke clojure"
  (:require [clojure.java.shell :as shell]
            [makejack.api.core :as makejack]))

(defn clojure [args target-kw config options]
  (let [project       (makejack/load-project)
        deps          (makejack/load-deps)
        target-config (get-in config [:targets target-kw])
        deps-edn      (select-keys target-config [:deps])
        res           (makejack/clojure
                        (:aliases target-config)
                        deps-edn
                        (:main-opts target-config))]
    (if (pos? (:exit res))
      (makejack/error (:err res))
      (println (:out res)))))
