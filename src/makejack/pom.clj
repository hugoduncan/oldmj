(ns makejack.pom
  "Create a pom file"
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]))

(defn basic-pom [group-id artifact-id name version]
  (str/join
    "\n"
    ["<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
     "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">
"
     "  <modelVersion>4.0.0</modelVersion>"
     (str"  <groupId>" group-id "</groupId>")
     (str"  <artifactId>" artifact-id "</artifactId>")
     (str "  <version>" version "</version>")
     (str "  <name>" name "</name>")
     "</project>"]))

(defn pom
  "Pom file creation or update."
  [args target-kw config options]
  (let [project       (makejack/load-project)
        target-config (get-in config [:targets target-kw])
        aliases       (:aliases target-config)
        version       (:version project)
        name          (:name project)
        group-id      (:group-id project name)
        artifact-id   (:artifact-id project name)]
    (spit "pom.xml" (basic-pom group-id artifact-id name version))
    (makejack/clojure aliases nil ["-Spom"] {})))
