(ns makejack.uberscript
  (:require [clojure.string :as str]
            [makejack.core :as makejack]
            [makejack.util :as util]))

(defn uberscript [args kw config options]
  (let [project (makejack/load-project)
        deps (makejack/load-deps)
        uberscript (:uberscript project)
        aliases (:aliases uberscript)
        cp (makejack/classpath aliases nil)
        ;; cp (str/replace
        ;;      (makejack/deps aliases  ["-Spath"])
        ;;      "\n" "")
        main (:main uberscript(:main project))
        path (:path uberscript (:name project))
        mode (:mode uberscript "750")
        paths (:paths deps)
        ;; source-files (mapcat util/source-files paths)
        ;; nses (mapv util/path->namespace source-files)
        ;; form `(require ~@(mapv #(list 'quote %) nses))
        res     (makejack/babashka
                  (-> ["-cp" cp "-m" (str main)]
                     ;;(into (mapcat vector (repeat "-f" ) (map str source-files)))
                     ;; (into ["-e" (str form)])
                     (into ["--uberscript" path])))]
    ;; (println :project project)
    ;; (prn :paths paths)

    (when (pos? (:exit res))
      (makejack/error
        (str "Problem creating uberscript: "
             (pr-str res))))

    (let [raw (slurp path)]
      (spit path (str
                   "#!/usr/bin/env bb\n\n"
                   ";; Generated with makejack uberscript. Do not edit directly.\n\n"
                   raw)))
    (util/chmod path mode)))
