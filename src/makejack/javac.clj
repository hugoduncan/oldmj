(ns makejack.javac
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))


(defn javac
  "Compile java sources"
  [_args target-kw config _options]
  (let [target-config   (get config target-kw)
        java-compile-kw (:name target-config :default)
        compile-config  (-> config :project :java-compile java-compile-kw)
        paths           (:paths compile-config)
        javac-options   (:javac-options compile-config)
        source-files    (mapcat
                          (partial util/source-files util/java-source-file?)
                          paths)
        args            (-> ["javac"]
                           (into javac-options)
                           (into source-files))]
    (makejack/sh args {})))
