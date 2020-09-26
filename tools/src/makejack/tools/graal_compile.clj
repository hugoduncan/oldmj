(ns makejack.tools.graal-compile
  (:require [makejack.api.core :as makejack]
            [makejack.api.path :as path]
            [makejack.api.tool-options :as tool-options]))

(defn graal-compile
  "GraalVM native-image compilation of jar file.
  Specify options using .properties file in the uberjar.
  See https://www.graalvm.org/reference-manual/native-image/Configuration/."
  [_args {:keys [mj project]} _options]
  (when (= :jar (:jar-type project))
    (throw (ex-info
            "GraalVM compilation requires an uberjar"
            {})))
  (let [uberjar-name (or (:jar-name project
                                    (makejack/default-jar-name project)))
        target-path  (:target-path mj)
        bin-name     (:binary-name project)
        bin-path     (str (path/path target-path bin-name))
        jar-path     (str (path/path target-path uberjar-name))
        graalvm-home (System/getenv "GRAALVM_HOME")
        _            (when-not graalvm-home
                       (makejack/error "GRAALVM_HOME not set"))
        args         [(str (path/path graalvm-home "bin/native-image"))
                      "-jar" jar-path
                      (str "-H:Name=" bin-path)]]
    (makejack/process
     args
     (when makejack/*verbose*
       {:out :inherit}))))

(def extra-options
  [])

(defn -main [& args]
  (let [{:keys [arguments options] {:keys [project] :as config} :config}
        (tool-options/parse-options-and-apply-to-config
         args extra-options "graal-compile options")]
    (makejack/with-makejack-tool ["graalVM native-image" options project]
      (graal-compile arguments config options))
    (shutdown-agents)))
