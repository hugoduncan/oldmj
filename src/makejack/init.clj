(ns makejack.init
  "Initialise project"
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]))


(defn init
  "Initialise a project for use with makejack.
  Creates project.edn and mj.edn files if they do not exist."
  [args target-kw config options]
  (when-not (util/file-exists? "project,edn")
    (let [dir-name (util/filename (util/cwd))
          default-project {:name (str dir-name)
                           :version "0.1.0"}]
      (spit "project.edn" (pr-str default-project))))
  (when-not (util/file-exists? "mj,edn")
    (let [default-config
          (str "{:project #include \"project.edn\"\n"
               " :targets {}}")]
      (spit "mj.edn" default-config))))
