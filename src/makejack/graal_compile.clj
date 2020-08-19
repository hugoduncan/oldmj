(ns makejack.graal-compile
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn graal-compile
  "GraalVM native-image compilation of jar file.
  Specify options using .properties file in the uberjar.
  See https://www.graalvm.org/reference-manual/native-image/Configuration/."
  [_args _target-kw config _options]
  (let [bin               (-> config :project :bin)
        jar-kw            (:jar bin :uberjar)
        uberjar           (-> config :project :jars jar-kw)
        uberjar-name      (or (:name uberjar
                                     (makejack/default-uberjar-name (:project config))))
        target-path       (:target-path config)
        bin-name          (:name bin (-> config :project :name))
        bin-path          (str (util/path target-path bin-name))
        jar-path          (str (util/path target-path uberjar-name))
        graalvm-home      (System/getenv "GRAALVM_HOME")
        _ (when-not graalvm-home
            (makejack/error "GRAALVM_HOME not set"))
        args              [(str (util/path graalvm-home "bin/native-image"))
                           "-jar" jar-path
                           (str "-H:Name=" bin-path)]]
    (makejack/sh
      args
      (when makejack/*verbose*
        {:out :inherit}))))
