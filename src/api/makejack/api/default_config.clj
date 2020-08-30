(ns ^:no-doc makejack.api.default-config)

(def default-mj-config
  {:target-path "target"
   :classes-path "target/classes"})

(def mj-tools {'makejack/makejack.tools {:mvn/version "0.0.1-alpha1"}})

#_(def config
  ;; project-project is the project's project.edn, as is
  (array-map
    :project-project (tagged-literal 'include "project.edn")
    ;; project is the project's project.edn, with some defaults
    :project-p1      (tagged-literal
                       'merge
                       [(tagged-literal 'ref [:project-project])
                        {:group-id
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :group-id])
                            (tagged-literal 'ref [:project-project :name])])
                         :artifact-id
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :artifact-id])
                            (tagged-literal 'ref [:project-project :name])])
                         :jar-type
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :jar-type])
                            :jar])}])
    :project         (tagged-literal
                       'merge
                       [(tagged-literal 'ref [:project-p1])
                        {:jar-name
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-p1 :jar-name])
                            (tagged-literal
                              'default-jar-name
                              [(tagged-literal 'ref [:project-p1 :artifact-id])
                               (tagged-literal 'ref [:project-p1 :version])
                               (tagged-literal 'ref [:project-p1 :jar-type])])])}])
    :mj (tagged-literal 'include "mj.edn")))

;; (defn config
;;   "This uses the resolver to resolve mj.edn and project.edn"
;;   [options]
;;   {:mj (tagged-literal 'include "mj.edn")
;;    :project (tagged-literal 'project options)})


(def project-with-defaults
  ;; project-project is the project's project.edn, as is
  (array-map
    :project-project (tagged-literal 'include "project.edn")
    ;; project is the project's project.edn, with some defaults
    :project-p1      (tagged-literal
                       'merge
                       [(tagged-literal 'ref [:project-project])
                        {:group-id
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :group-id])
                            (tagged-literal 'ref [:project-project :name])])
                         :artifact-id
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :artifact-id])
                            (tagged-literal 'ref [:project-project :name])])
                         :jar-type
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-project :jar-type])
                            :jar])}])
    :project         (tagged-literal
                       'merge
                       [(tagged-literal 'ref [:project-p1])
                        {:jar-name
                         (tagged-literal
                           'or
                           [(tagged-literal 'opt-ref [:project-p1 :jar-name])
                            (tagged-literal
                              'default-jar-name
                              [(tagged-literal 'ref [:project-p1 :artifact-id])
                               (tagged-literal 'ref [:project-p1 :version])
                               (tagged-literal 'ref [:project-p1 :jar-type])])])}])))


(def default-targets
  {:compile {:doc       "AOT compilation of clojure sources."
             :invoker   :clojure
             :deps      {'makejack/makejack.tools {:mvn/version "0.0.1-alpha1"}}
             :main-opts ["-m" "makejack.tools.compile"
                         "--profile" ":compile"]}

   :clean {:doc     (tagged-literal
                      'join ["Remove the "
                             (tagged-literal 'ref [:target-path])
                             " directory"])
           :invoker :shell
           :args    ["rm" "-rf" (tagged-literal 'ref [:target-path])]}

   :init {:doc       "Initialise a   project for   use with makejack.
                Creates    project.edn  and mj.edn  files if  they do not exist."
          :invoker   :clojure
          :deps      mj-tools
          :main-opts ["-m" "makejack.tools.init"]}

   :pom {:doc       "Pom file creation or update."
         :invoker   :clojure
         :deps      mj-tools
         :main-opts ["-m" "makejack.tools.pom"
                     "--profile" ":pom"]}

   :jar {:doc       "Build a jar, "
         :invoker   :clojure
         :deps      mj-tools
         :main-opts ["-m" "makejack.tools.jar"
                     "--profile" ":jar"]}

   :uberjar {:doc       "Build an uberjar, "
             :invoker   :clojure
             :deps      mj-tools
             :main-opts ["-m" "makejack.tools.jar"
                         "--profile" ":uberjar"]}

   :javac {:doc       "Compile java sources."
           :invoker   :clojure
           :deps      mj-tools
           :main-opts ["-m" "makejack.tools.javac"
                       "--profile" ":javac"]}

   :uberscript {:doc       "Output a babashka uberscript."
                :invoker   :clojure
                :deps      mj-tools
                :main-opts ["-m" "makejack.tools.uberscript"
                            "--profile" ":uberscript"]}

   :binary
   {:doc       "GraalVM native-image compilation of jar file.
         Specify options using .properties file in the uberjar.
         See https://www.graalvm.org/reference-manual/native-image/Configuration/."
    :invoker   :clojure
    :deps      mj-tools
    :main-opts ["-m" "makejack.tools.graal-compile"
                "--profile" ":binary"]}})

(def default-mj
  {:targets (dissoc default-targets :clean)})