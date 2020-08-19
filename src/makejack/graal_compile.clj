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
        ;; target-config     (get config target-kw)
        ;; main              (:main project)
        target-path       (:target-path config)
        bin-name          (:name bin (-> config :project :name))
        bin-path          (str (util/path target-path bin-name))
        jar-path          (str (util/path target-path uberjar-name))
        ;; init-at-runtime   (:initalize-at-runtime target-config)
        ;; init-at-rt-args   (mapv
        ;;                   #(str "--initialize-at-run-time=" (name %))
        ;;                   init-at-runtime)
        ;; init-at-buildtime (:initalize-at-buildtime target-config)
        ;; init-at-bt-args   (mapv
        ;;                   #(str "--initialize-at-build-time=" (name %))
        ;; init-at-buildtime)

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




;; (def default-args
;;   ["-H:+TraceClassInitialization"
;;    "-H:+ReportExceptionStackTraces"
;;    "-J-Dclojure.spec.skip-macros=true"
;;    "-J-Dclojure.compiler.direct-linking=true"
;;    "-H:ReflectionConfigurationFiles=reflection.json"
;;    "--initialize-at-run-time=java.lang.Math$RandomNumberGeneratorHolder"
;;    "--initialize-at-build-time"
;;    "-H:Log=registerResource:"
;;    "-H:EnableURLProtocols=http,https"
;;    "--enable-all-security-services"
;;    "-H:+JNI"
;;    "--verbose"
;;    "--no-fallback"
;;    "--no-server"
;;    "--report-unsupported-elements-at-runtime"
;;    "-J-Xmx6500m"])

;; -H:DeadlockWatchdogInterval=10
;; -H:+DeadlockWatchdogExitOnTimeout
