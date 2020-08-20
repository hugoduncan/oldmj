(ns makejack.uberscript
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn uberscript
  "Output a babashka uberscript."
  [_args target-kw {:keys [:makejack/project] :as config} options]
  (let [target-config (get-in config [:targets target-kw])
        aliases       (-> []
                         (into (:aliases project))
                         (into (:aliases target-config))
                         (into (:aliases options)))
        cp            (makejack/classpath aliases nil)
        main          (:main project)
        script-name   (:scritp-name project (:name project))
        mode          (:script-mode project "750")
        path          (util/path (:target-path config) script-name)]

    (makejack/babashka
      (-> ["-cp" cp "-m" (str main)]
         (into ["--uberscript" (str path)]))
      {})

    (when (:script-shebang? project)
      (let [raw (slurp (str path))]
        (spit (str path)
              (str
                "#!/usr/bin/env bb\n\n"
                ";; Generated with makejack uberscript. Do not edit directly.\n\n"
                raw))))

    (util/chmod path mode)))
