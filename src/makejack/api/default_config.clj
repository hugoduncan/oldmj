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
     :jar     {:tool makejack.depstar
               :profiles [:jar]}
     :uberjar {:tool makejack.depstar
               :profiles [:uberjar]}
     :bin     {:tool makejack.graal-compile
               :profiles [:binary :uberjar]}}}")
