#!/usr/bin/env bb
;; -*- mode: clojure -*-

;; bootstrap the mj binary
(require '[aero.core :as aero])
(require '[clojure.edn :as edn])
(require '[clojure.java.shell :as shell])
(require '[clojure.string :as str])
(require '[clojure.tools.cli :as cli])

(def bootstrap-options-spec
  [["-v" "--verbose" "Show command execution"]])

(def bootstrap-options
  (:options (cli/parse-opts *command-line-args* bootstrap-options-spec)))

(def verbose (:verbose bootstrap-options))


(def API-POM "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
  <project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" >
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.hugoduncan</groupId>
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

(defn sh [args]
  (when verbose
    (prn args))
  (apply shell/sh args))

(def clojure-cli-version
  (let [res (sh ["clojure" "--help"])]
    (-> (:out res)
        str/split-lines
        first
        (str/split #"\s+")
        second)))

(if verbose
  (println "Bootstrap with clojure CLI version" clojure-cli-version))

(def verbose-args (if (:verbose bootstrap-options) ["--verbose"] []))

(def explicit-main (pos? (compare clojure-cli-version "1.10.1.600")))

(defn main-switches
  ([] (if explicit-main
        ["-M" "--report" "stderr" "-m"]
        ["--report" "stderr" "-m"]))
  ([aliases] (if explicit-main
               [(str "-M" aliases) "--report" "stderr" "-m"]
               [(str "-A" aliases) "--report" "stderr" "-m"])))


(defn format-version-map
  "Format a version map as a string."
  [{:keys [major minor incremental qualifier]}]
  (cond-> (str major)
    minor (str "." minor)
    incremental (str "." incremental)
    qualifier (str "-" qualifier)))

;;; Get the project version
(println "Get version")
(let [version-map (aero/read-config "version.edn")
      version (format-version-map version-map)]
  (when verbose
    (println "Bootstrapping makejack version" version))


  (println "building version source namespace")
  (let [res (sh (-> ["clojure"]
                   (into (main-switches))
                   (into ["makejack.impl.build-version"])
                   (into verbose-args)))]
    (when verbose
      (println (:out res))
      (println (:err res)))
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res))))

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
  (let [res (sh (-> ["clojure"]
                   (into ["-Sdeps"
                          "{:deps {seancorfield/depstar {:mvn/version \"1.1.104\"}}}"
                          ])
                   (into (main-switches ":jar"))
                   (into ["hf.depstar.jar"])
                   (into verbose-args)
                   (conj (str "target/makejack-" version ".jar"))))]
    (when verbose
      (println (:out res))
      (println (:err res)))
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res))))

  (println "Install API jar")
  (let [res (sh ["mvn"
                 "org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file"
                 (str "-Dfile=target/makejack-" version ".jar")
                 "-DgroupId=org.hugoduncan"
                 "-DartifactId=makejack"
                 (str "-Dversion=" version)
                 "-Dpackaging=jar"])]
    (when verbose
      (println (:out res)))
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res))))

;;; Build mj-script

  (println "Get main classpath")
  (let [res     (sh ["clojure" "-Srepro" "-Spath"])
        main-cp (str/trim (:out res))]
    (when (pos? (:exit res))
      (binding [*out* *err*]
        (println "failed")
        (println (:err res)))
      (System/exit (:exit res)))


    (println "building mj-script")
    (let [res (sh ["bb"
                   "-cp" main-cp
                   "-m" "makejack.main"
                   "--uberscript" "target/mj-script"])]
      (when verbose
        (println (:out res)))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

;;; Build tools jar

    (println "build tools pom")
    (let [res (sh (-> ["clojure"]
                     (into (main-switches))
                     (into ["makejack.tools.pom"])
                     (into [ :dir "tools"])))]
      (when verbose
        (println (:out res)))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

    (println "build tools jar")
    (let [res (sh (-> ["clojure"]
                     (into (main-switches))
                     (into ["makejack.tools.jar"])
                     (into verbose-args)
                     (into [(str "target/makejack.tools-" version ".jar")
                            :dir "tools"])))]
      (when verbose
        (println (:out res)))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

    (println "install tools jar")
    (let [res (sh (concat
                    ["bb" "../target/mj-script"]
                    verbose-args
                    ["install"
                     :dir "tools"]))]
      (when verbose
        (println (:out res)))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))

    (println "rebuild script for shebang")
    (let [res (sh (concat
                    ["bb" "target/mj-script"]
                    verbose-args
                    ["uberscript"]))]
      (when verbose
        (println (:out res))
        (println (:err res)))
      (when (pos? (:exit res))
        (binding [*out* *err*]
          (println "failed")
          (println (:err res)))
        (System/exit (:exit res))))))
