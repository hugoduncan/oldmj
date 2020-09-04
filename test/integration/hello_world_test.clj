(ns hello-world-test
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]
            [makejack.api.default-config :as default-config]))

(defn resolver [proj-name]
  {"project.edn" (io/resource (str proj-name "/project.edn"))
   "mj.edn"      (io/resource (str proj-name "/mj.edn"))})

(deftest hello-world-test
  (testing "mj init"
    (makejack/sh ["../../target/mj-script" "init"]
                 {:dir "test-resources/test_hello_world"})
    (let [project-edn (util/path "test-resources" "test_hello_world" "project.edn")
          mj-edn      (util/path "test-resources" "test_hello_world" "mj.edn")]
      (testing "creates mj.edn and project.edn"
        (is (util/file-exists? project-edn))
        (is (util/file-exists? mj-edn)))
      (testing "injects default targets"
        (let [config (aero/read-config
                       (java.io.StringReader.
                         (:out
                          (makejack/sh
                            ["../../target/mj-script" "--pprint"]
                            {:dir "test-resources/test_hello_world"})))
                       (resolver "test_hello_world"))]
          (is (= "test_hello_world" (-> config :project :name)))
          (is (= "0.1.0" (-> config :project :version)))
          (is (= (keys default-config/default-targets)
                 (keys (-> config :mj :targets))))))))
  (testing "mj jar"
    (makejack/sh ["../../target/mj-script" "--verbose" "jar"]
                 {:dir "test-resources/test_hello_world"})
    (let [jar (util/path "test-resources" "test_hello_world" "target"
                         "test_hello_world-0.1.0.jar")]
      (testing "creates jar file"
        (is (util/file-exists? jar)))
      (testing "contains src"
        (with-open [jar-file (java.util.jar.JarFile. (str jar))]
          (is (.getJarEntry jar-file "hello/world.clj")))))))
