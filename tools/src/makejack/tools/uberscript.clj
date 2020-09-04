(ns makejack.tools.uberscript
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]))

(defn uberscript
  "Output a babashka uberscript."
  [_args {:keys [mj project] :as _config} options]
  (let [aliases     (-> []
                       (into (:aliases project))
                       (into (:aliases options)))
        main        (:main project)
        script-name (:script-name project (:name project))
        mode        (:script-mode project "750")
        path        (util/path (:target-path mj) script-name)]

    (makejack/babashka
      aliases
      {}
      (-> ["-m" (str main)]
         (into ["--uberscript" (str path)]))
      {:with-project-deps? true})

    (when (:script-shebang? project)
      (println "Adding shebang")
      (let [raw (slurp (str path))]
        (spit (str path)
              (str
                "#!/usr/bin/env bb\n\n"
                ";; Generated with makejack uberscript. Do not edit directly.\n\n"
                raw))))

    (util/chmod path mode)
    (when makejack/*verbose*
      (println {:script-mode mode}))))

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
