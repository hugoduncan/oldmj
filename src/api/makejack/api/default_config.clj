(ns ^:no-doc makejack.api.default-config)

(def config
  "{:project #include \"project.edn\"
    :target-path \"target\"
    :targets {
     :compile {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.compile\"
                           \"--profiles\" \":compile\"]}
     :clean   {:doc #join [\"Remove the \"
                           #ref [:target-path]
                           \" directory\"]
               :invoker :shell
               :args [\"rm\" \"-rf\" #ref [:target-path]]}

     :init    {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.init\"]}

     :pom     {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.pom\"
                           \"--profiles\" \":pom\"]}

     :jar     {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.jar\"
                           \"--profiles\" \":jar\"]}

     :uberjar {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.jar\"
                           \"--profiles\" \":uberjar\"]}

     :javac   {:invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.javac\"
                           \"--profiles\" \":javac\"]}

     :uberscript {:invoker :clojure
                  :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
                  :main-opts [\"-m\" \"makejack.tools.uberscript\"
                              \"--profiles\" \":uberscript\"]}

     :bin     {:doc \"Build a binary from the uberjar.
                     Uses the GraalVM native-image builder.\"
               :invoker :clojure
               :deps {makejack/makejack.tools {:mvn/version \"0.0.1-alpha1\"}}
               :main-opts [\"-m\" \"makejack.tools.graal-compile\"
                           \"--profiles\" \":binary:uberjar\"]}}}")
