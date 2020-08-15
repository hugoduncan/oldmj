(ns makejack.uberscript
  (:require [clojure.string :as str]
            [makejack.core :as makejack]
            [makejack.util :as util]))

(defn uberscript [args options]
  (let [project (makejack/load-project)
        uberscript (:uberscript project)
        aliases (:aliases uberscript)
        cp (str/replace
             (makejack/deps aliases  ["-Spath"])
             "\n" "")
        main (:main project)
        path (:path uberscript (:name project))
        mode (:mode uberscript "750")]
    (println :project project)
    (makejack/babashka
      ["-cp" cp "-m" main "--uberscript" path])
    (let [raw (slurp path)]
      (spit path (str
                   "#!/usr/bin/env bb\n\n"
                   ";; Generated with makejack uberscript. Do not edit directly.\n\n"
                   raw)))
    (util/chmod path mode)))
