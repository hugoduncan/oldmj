(ns makejack.tools.pom
  "Create a pom file"
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util])
  (:import [org.apache.maven.model
            Build Model Scm]
           [org.apache.maven.model.io.xpp3
            MavenXpp3Reader
            MavenXpp3Writer]))

(defn- scm [{:keys [connection developer-connection url
                    tag location]}]
  (let [scm (Scm.)]
    (.setTag tag)
    (.setConnection connection)
    (.setDeveloperConnection developer-connection)
    (.setUrl url)))

(defn- set-details [^Model pom group-id artifact-id name version scm target-path]
  (doto pom
    (.setModelVersion "4.0.0")
    (.setGroupId group-id)
    (.setArtifactId artifact-id)
    (.setName name)
    (.setVersion version)
    (.setBuild (doto (or (.getBuild pom) (Build.))
                 (.setOutputDirectory target-path)))))

(defn- update-or-create-pom [group-id artifact-id name version scm target-path]
  (let [model (if (util/file-exists? "pom.xml")
                (with-open [in (io/input-stream "pom.xml")]
                  (.read (MavenXpp3Reader.) in false))
                (Model.))]
    (set-details model group-id artifact-id name version scm target-path)
    (with-open [out (io/output-stream "pom.xml")]
      (.write (MavenXpp3Writer.) out model))))


(defn pom
  "Pom file creation or update."
  [_args {:keys [mj project] :as _config} options]
  (prn :aliases (:aliases project) (:aliases options))
  (let [aliases        (-> []
                          (into (:aliases project))
                          (into (:aliases options)))
        version       (:version project)
        name          (:name project)
        group-id      (:group-id project name)
        artifact-id   (:artifact-id project name)
        scm           (:scm project)
        target-path   (:target-path mj)]
    (makejack/clojure aliases nil ["-Spom"] {})
    (update-or-create-pom group-id artifact-id name version scm target-path)))



(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (prn :args args)
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "pom [options]")]
    (prn :options options)
    (binding [makejack/*verbose* (:verbose options)]
      (pom arguments config options))
    (shutdown-agents)))
