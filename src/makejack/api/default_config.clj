(ns makejack.api.default-config)

(def config
  "{:project #include \"project.edn\"
    :target-path \"target\"
    :targets {
     :compile {:tool makejack.compile
               :profiles [:compile]
               :classes-path \"target/classes\"}
     :clean   {:doc #join [\"Remove the \"
                           #ref [:target-path]
                           \" directory\"]
               :tool makejack.shell
               :args [\"rm\" \"-rf\" #ref [:target-path]]}
     :jar     {:tool makejack.clojure
               :deps {seancorfield/depstar {:mvn/version \"1.0.97\"}}
               :profiles [:jar]
               :main-opts [\"-m\" \"hf.depstar.jar\"
                           #join [#ref [:target-path]
                                  \"/\"
                                  #ref [:project :name]
                                  \"-\"
                                  #ref [:project :version]
                                  \".jar\"]]}
     :uberjar {:tool makejack.clojure
               :deps {seancorfield/depstar {:mvn/version \"1.0.97\"}}
               :profiles [:uberjar]
               :main-opts [\"-m\" \"hf.depstar.uberjar\"
                           #join [#ref [:target-path]
                                  \"/\"
                                  #ref [:project :name]
                                  \"-\"
                                  #ref [:project :version]
                                  \"-standalone.jar\"]
                           \"--main\" #opt-ref [:project :main]]}
     :bin     {:tool makejack.graal-compile
               :profiles [:binary]}}}")
