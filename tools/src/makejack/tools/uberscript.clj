(ns makejack.tools.uberscript
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]))

(defn uberscript
  "Output a babashka uberscript."
  [_args {:keys [:makejack/project] :as config} options]
  (let [aliases       (-> []
                         (into (:aliases project))
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

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "uberscript [options]")]
    (binding [makejack/*verbose* (:verbose options)]
      (uberscript arguments config options))
    (shutdown-agents)))
