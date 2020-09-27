(ns makejack.tools.init
  "Initialise project"
  (:require [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.api.tool :as tool]))

(def default-mj
  (tagged-literal 'mj {:targets (tagged-literal 'default-targets :all)}))

(defn init
  "Initialise a project for use with makejack.
  Creates project.edn and mj.edn files if they do not exist.  "
  [_options _args _config]
  (when-not (filesystem/file-exists? "project,edn")
    (let [dir-name        (path/filename (filesystem/real-path (filesystem/cwd)))
          default-project {:name    (str dir-name)
                           :version "0.1.0"}]
      (spit "project.edn" (pr-str default-project))))
  (when-not (filesystem/file-exists? "mj,edn")
    (spit "mj.edn" (pr-str default-mj))))

(def extra-options
  [])

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main "init" "[options]" init extra-options args)))
