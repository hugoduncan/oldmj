(ns makejack.depstar
  (:require [clojure.string :as str]
            [makejack.core :as makejack]
            [makejack.util :as util]))

(defn depstar [args config-kw config options]
  (let [project (makejack/load-project)
        uberjar (:uberjar project)
        uberjar-config (get config config-kw)
        aliases (:aliases uberjar-config)
        deps '{:deps {seancorfield/depstar {:mvn/version "1.0.97"}}}
        ;; cp (str/replace
        ;;      (makejack/deps aliases  ["-Spath" "-Sdeps" (str depstar-dep)])
        ;;      "\n" "")
        main (:main project)
        jar-path (:path uberjar (makejack/default-uberjar-name project))
        args ["-m" "hf.depstar.uberjar" jar-path]
        args (into args (if main ["-m" (str main)]))
        ;; cmd (into
        ;;       ["-cp" cp "-m" "hf.depstar.uberjar" jar-path]
        ;;       args)
        ]
    ;; (println :cmd cmd)
    ;; (makejack/babashka cmd)
    (makejack/clojure aliases deps args)
    ))

(defn -main [& args]
  (depstar args {}))
