(ns makejack.graal-compile
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [makejack.core :as makejack]
            [makejack.util :as util]))

(def default-args
  ["-H:+TraceClassInitialization"
   "-H:+ReportExceptionStackTraces"
   "-J-Dclojure.spec.skip-macros=true"
   "-J-Dclojure.compiler.direct-linking=true"
   "-H:ReflectionConfigurationFiles=reflection.json"
   "--initialize-at-run-time=java.lang.Math$RandomNumberGeneratorHolder"
   "--initialize-at-build-time"
   "-H:Log=registerResource:"
   "-H:EnableURLProtocols=http,https"
   "--enable-all-security-services"
   "-H:+JNI"
   "--verbose"
   "--no-fallback"
   "--no-server"
   "--report-unsupported-elements-at-runtime"
   "-J-Xmx6500m"])

(defn graal-compile [args config-kw config options]
  (let [project (makejack/load-project)
        uberjar (:uberjar project)
        graal   (:graal-compile project)
        aliases (:aliases graal)
        main     (:main project)
        bin-name (:bin-name graal (:name project))
        _ (println :project project)
        jar-path (:path uberjar (makejack/default-uberjar-name project))
        args (into ["-jar" jar-path
                    (str "-H:Name=" bin-name)]
                   default-args )
        graalvm-home (System/getenv "GRAALVM_HOME")
        _ (println "running " (str graalvm-home "/bin/native-image") (pr-str args))
        res (apply sh
                   (str graalvm-home "/bin/native-image")
                   args)]
    (println "res" res)
    (if (pos? (:exit res))
      (println (:err res)))
    (println "Done")))

(defn -main [& args]
  (graal-compile args {}))
