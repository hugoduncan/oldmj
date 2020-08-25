(ns makejack.pom
  "Create a pom file"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util])
  (:import [org.apache.maven.model
            Model]
           [org.apache.maven.model.io.xpp3
            MavenXpp3Reader
            MavenXpp3Writer]))

(defn- set-details [^Model pom group-id artifact-id name version]
  (doto pom
    (.setGroupId group-id)
    (.setArtifactId artifact-id)
    (.setName name)
    (.setVersion version)))

(defn- update-or-create-pom [group-id artifact-id name version]
  (let [model (if (util/file-exists? "pom.xml")
                (with-open [in (io/input-stream "pom.xml")]
                  (.read (MavenXpp3Reader.) in false))
                (Model.))]
    (set-details model group-id artifact-id name version)
    (with-open [out (io/output-stream "pom.xml")]
      (.write (MavenXpp3Writer.) out model))))


(defn pom
  "Pom file creation or update."
  [_args target-kw {:keys [:makejack/project] :as config} options]
  (let [target-config (get-in config [:targets target-kw])
        aliases        (-> []
                          (into (:aliases project))
                          (into (:aliases target-config))
                          (into (:aliases options)))
        version       (:version project)
        name          (:name project)
        group-id      (:group-id project name)
        artifact-id   (:artifact-id project name)]
    (update-or-create-pom group-id artifact-id name version)
    (makejack/clojure aliases nil ["-Spom"] {})))
