(ns makejack.clojure
  "Makejack tool to invoke clojure"
  (:require [clojure.java.shell :as shell]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]))

(def cli-options
  [["-Sdeps" "Additional deps map"]])

(defn clojure
  "Execute clojure"
  [args target-kw config _options]
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        project           (makejack/load-project)
        deps              (makejack/load-deps)
        target-config     (get-in config [:targets target-kw])
        deps-edn          (select-keys target-config [:deps])
        res               (makejack/clojure
                            (:aliases target-config)
                            (merge deps-edn
                                   (:Sdeps options))
                            (:main-opts target-config))]
    (if (pos? (:exit res))
      (makejack/error (:err res))
      (println (:out res)))))
