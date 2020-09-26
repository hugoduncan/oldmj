(ns makejack.tools.uberscript
  (:require [makejack.api.babashka :as babashka]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.api.tool-options :as tool-options]))

(defn uberscript
  "Output a babashka uberscript."
  [_args {:keys [mj project] :as _config} options]
  (let [aliases     (-> []
                        (into (:aliases project))
                        (into (:aliases options)))
        main        (:main project)
        script-name (:script-name project (:name project))
        mode        (:script-mode project "750")
        path        (path/path (:target-path mj) script-name)]

    (babashka/process
     aliases
     {}
     ["--uberscript" (str path) "-m" (str main)]
     {:with-project-deps? true})

    (when (:script-shebang? project)
      (println "Adding shebang")
      (let [raw (slurp (str path))]
        (spit (str path)
              (str
               "#!/usr/bin/env bb\n\n"
               ";; Generated with makejack uberscript. Do not edit directly.\n\n"
               raw))))

    (filesystem/chmod path mode)
    (when makejack/*verbose*
      (println {:script-mode mode}))))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]])

(defn -main [& args]
  (let [{:keys [arguments options] {:keys [project] :as config} :config}
        (tool-options/parse-options-and-apply-to-config
         args extra-options "uberscript [options]")]
    (makejack/with-makejack-tool ["uberscript" options project]
      (uberscript arguments config options))
    (shutdown-agents)))
