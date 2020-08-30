(ns makejack.tools.init
  "Initialise project"
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]
            [makejack.api.tool-options :as tool-options]))

(def default-mj
  (tagged-literal 'mj {:targets (tagged-literal 'default-targets :all)}))

(defn init
  "Initialise a project for use with makejack.
  Creates project.edn and mj.edn files if they do not exist.  "
  [_args _config _options]
  (when-not (util/file-exists? "project,edn")
    (let [dir-name (util/filename (util/cwd))
          default-project {:name (str dir-name)
                           :version "0.1.0"}]
      (spit "project.edn" (pr-str default-project))))
  (when-not (util/file-exists? "mj,edn")
    (spit "mj.edn" (pr-str default-mj))))


(def extra-options
  [])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "init [options]")]
    (binding [makejack/*verbose* (:verbose options)]
      (init arguments config options))
    (shutdown-agents)))