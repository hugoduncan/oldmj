(ns java-test
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]))

(defn resolver [proj-name]
  {"project.edn" (io/resource (str proj-name "/project.edn"))
   "mj.edn"      (io/resource (str proj-name "/mj.edn"))})

(deftest java-test
  (testing "mj javac"
    (makejack/process ["../../target/mj-script" "--verbose" "javac"]
                      {:dir "test-resources/test_project_java"})
    (let [class-file (path/path
                      "test-resources" "test_project_java"
                      "target" "classes" "my" "Hello.class")]
      (testing "creates class file"
        (is (filesystem/file-exists? class-file)))))
  (testing "mj compile"
    (makejack/process ["../../target/mj-script" "--verbose" "compile"]
                      {:dir "test-resources/test_project_java"})
    (let [class-file (path/path
                      "test-resources" "test_project_java"
                      "target" "classes" "my" "main__init.class")]
      (testing "creates class file"
        (is (filesystem/file-exists? class-file)))))
  (testing "mj pom"
    (makejack/process ["../../target/mj-script" "--verbose" "pom"]
                      {:dir "test-resources/test_project_java"})
    (let [pom-file (path/path "test-resources" "test_project_java" "pom.xml")]
      (testing "creates pom"
        (is (filesystem/file-exists? pom-file)))))
  (testing "mj uberjar"
    (makejack/process ["../../target/mj-script" "--verbose" "uberjar"]
                      {:dir "test-resources/test_project_java"})
    (let [jar (path/path "test-resources" "test_project_java" "target"
                         "java-0.1.0-standalone.jar")]
      (testing "creates uberjar file"
        (is (filesystem/file-exists? jar)))
      (testing "contains src"
        (with-open [jar-file (java.util.jar.JarFile. (str jar))]
          (is (.getJarEntry jar-file "my/main.clj"))))
      (testing "contains clojure class file"
        (with-open [jar-file (java.util.jar.JarFile. (str jar))]
          (is (.getJarEntry jar-file "my/main__init.class"))))
      (testing "contains java class file"
        (with-open [jar-file (java.util.jar.JarFile. (str jar))]
          (is (.getJarEntry jar-file "my/Hello.class"))))
      (testing "can be invoked by clojure"
        (let [res (makejack/process
                   ["clojure" "-Scp" "target/java-0.1.0-standalone.jar"
                    "-m" "my.main" "world"]
                   {:dir "test-resources/test_project_java"})]
          (= "Hello world" (:out res))))
      (testing "can be invoked by java"
        (let [res (makejack/process
                   ["java" "-jar" "target/java-0.1.0-standalone.jar" "world"]
                   {:dir "test-resources/test_project_java"})]
          (= "hello" (:out res)))))))
