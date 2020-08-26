(ns makejack.javac
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn javac
  "Compile java sources"
  [_args _target-kw {:keys [:makejack/project]} _options]
  (let [java-paths      (:java-paths project)
        javac-options   (:javac-options project)
        source-files    (mapcat
                          (partial util/source-files util/java-source-file?)
                          java-paths)
        args            (-> ["javac"]
                           (into javac-options)
                           (into source-files))]
    (makejack/sh args {})))
