(ns makejack.depstar
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn depstar
  "Build a jar with depstar.
  If `:jar-type` is `:uberjar`, then build an uberjar, else a thin jar."
  [_args target-kw {:keys [:makejack/project] :as config} options]
  (let [target-config (get config target-kw)
        aliases       (-> []
                          (into (:aliases project))
                          (into (:aliases target-config))
                          (into (:aliases options)))
        deps          '{:deps {seancorfield/depstar {:mvn/version "1.0.97"}}}
        main          (:main project)
        target-path   (:target-path config)
        jar-name      (or (:jar-name project)
                          (makejack/default-jar-name project))
        jar-path      (str (util/path target-path jar-name ))
        args          ["-m"
                       (if (= :uberjar (:jar-type project))
                         "hf.depstar.uberjar"
                         "hf.depstar.jar")
                       jar-path]
        args          (into args (if main ["-m" (str main)]))]
    (makejack/clojure aliases deps args (:options target-config))))
