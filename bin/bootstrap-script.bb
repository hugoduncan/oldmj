#!/usr/bin/env bb
;; -*- mode: clojure -*-

;; bootstrap the mj binary
(require '[aero.core :as aero])
(require '[clojure.edn :as edn])
(require '[clojure.java.shell :refer [sh]])


(def API-POM "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
  <project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" >
  <modelVersion>4.0.0</modelVersion>
  <groupId>makejack</groupId>
  <artifactId>makejack</artifactId>
  <version>%s</version>
  <name>makejack</name>
  <dependencies>
    <dependency>
      <groupId>org.clojure</groupId>
      <artifactId>clojure</artifactId>
      <version>1.10.2-alpha1</version>
    </dependency>
    <dependency>
      <groupId>aero</groupId>
      <artifactId>aero</artifactId>
      <version>1.1.6</version>
    </dependency>
    <dependency>
      <groupId>fipp</groupId>
      <artifactId>fipp</artifactId>
      <version>0.6.18</version>
    </dependency>
    <dependency>
      <groupId>org.clojure</groupId>
      <artifactId>tools.cli</artifactId>
      <version>1.0.194</version>
    </dependency>
  </dependencies>
  <build>
    <sourceDirectory>src/api</sourceDirectory>
  </build>
  <repositories>
    <repository>
      <id>clojars</id>
      <url>https://repo.clojars.org/</url>
    </repository>
  </repositories>
  </project>")

;;; Get the project version
(println "Get version")
(let [project (aero/read-config "project.edn")
      version (:version project)]
  (println "version" version)


;;;  Build and install API jar

  (println "building pom")
  ;; clojure -Spom produces an invalid pom, with nested <build>
  ;; (let [res (sh "clojure" "-A:jar" "-Spom")]
  ;;   (when (pos? (:exit res))
  ;;     (binding [*out* *err*]
  ;;       (println "failed")
  ;;       (println (:err res)))
  ;;     (System/exit (:exit res))))
  (spit "pom.xml" (format API-POM version))


  (println "building API jar")
  (let [res (sh "clojure"
                "-A:jar"
                "-Sdeps" "{:deps {seancorfield/depstar {:mvn/version \"1.1.104\"}}}"
                "-m" "hf.depstar.jar"
                " --verbose"
                (str "target/makejack-" version ".jar"))]
    (println (:out res))
    (println (:err res))
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res))))

  (println "Install API jar")
  (let [res (sh "mvn"
                "org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file"
                (str "-Dfile=target/makejack-" version ".jar")
                "-DgroupId=makejack"
                "-DartifactId=makejack"
                (str "-Dversion=" version)
                "-Dpackaging=jar")]
    (println (:out res))
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res))))

;;; Build mj-script

  (println "Get main classpath")
  (let [res     (sh "clojure" "-Spath")
        main-cp (:out res)]
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res)))


    (println "building mj-script")
    (let [res (sh "bb"
                  "-cp" main-cp
                  "-m" "makejack.main"
                  "--uberscript" "target/mj-script")]
      (println (:out res))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

;;; Build tools jar

    (println "build tools pom")
    (let [res (sh "clojure"  "-m" "makejack.tools.pom"
                  :dir "tools")]
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

    (println "build tools jar")
    (let [res (sh "clojure"
               "-m" "makejack.tools.jar"
               "target/makejack.tools-" version ".jar"
               :dir "tools")]
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

    (println "install tools jar")
    (let [res (sh "bb" "../target/mj-script"
                  "--verbose"
                  "install"
               :dir "tools")]
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))))
