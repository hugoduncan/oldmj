(ns makejack.uberscript
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn uberscript
  "Output a babashka uberscript."
  [args kw config options]
  (let [project    (makejack/load-project)
        deps       (makejack/load-deps)
        uberscript (:uberscript project)
        aliases    (:aliases uberscript)
        cp         (makejack/classpath aliases nil)
        main       (:main uberscript (:main project))
        name       (:name uberscript (:name project))
        mode       (:mode uberscript "750")
        path       (util/path (:target-path config) name)
        res        (makejack/babashka
                     (-> ["-cp" cp "-m" (str main)]
                        (into ["--uberscript" (str path)]))
                     {})]

    (when (:shebang? uberscript)
      (let [raw (slurp (str path))]
        (spit (str path)
              (str
                "#!/usr/bin/env bb\n\n"
                ";; Generated with makejack uberscript. Do not edit directly.\n\n"
                raw))))

    (util/chmod path mode)))
