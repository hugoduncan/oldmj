(ns makejack.graal-compile
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

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

;; -H:DeadlockWatchdogInterval=10
;; -H:+DeadlockWatchdogExitOnTimeout

(defn graal-compile
  "GraalVM native-image compilation of jar file."
  [args target-kw config options]
  (let [project           (makejack/load-project)
        uberjar           (:uberjar project)
        graal             (:graal-compile project)
        target-config     (get config target-kw)
        aliases           (:aliases graal)
        main              (:main project)
        bin-name          (:bin-name graal (:name project))
        _                 (println :project project)
        jar-path          (:path uberjar (makejack/default-uberjar-name project))
        init-at-runtime   (:initalize-at-runtime target-config)
        init-at-rt-args   (mapv
                          #(str "--initialize-at-run-time=" (name %))
                          init-at-runtime)
        init-at-buildtime (:initalize-at-buildtime target-config)
        init-at-bt-args   (mapv
                          #(str "--initialize-at-build-time=" (name %))
                          init-at-buildtime)
        args              (-> ["-jar" jar-path
                            (str "-H:Name=" bin-name)]
                           (into default-args)
                           (into init-at-rt-args)
                           (into init-at-bt-args))
        graalvm-home      (System/getenv "GRAALVM_HOME")
        _                 (println "running "
                                 (str graalvm-home "/bin/native-image")
                                 (pr-str args))
        res               (apply sh
                               (str graalvm-home "/bin/native-image")
                               args)]
    (println "res" res)
    (if (pos? (:exit res))
      (println (:err res)))
    (println "Done")))

(defn -main [& args]
  (graal-compile args {}))
