(ns makejack.tools.pom
  "Create a pom file"
  (:require [clojure.java.io :as io]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.tool-options :as tool-options])
  (:import [org.apache.maven.model
            Build Model #_Scm]
           [org.apache.maven.model.io.xpp3
            MavenXpp3Reader
            MavenXpp3Writer]))

(set! *warn-on-reflection* true)

;; (defn- scm [{:keys [connection developer-connection url
;;                     tag location]}]
;;   (doto (Scm.)
;;     (.setTag tag)
;;     (.setConnection connection)
;;     (.setDeveloperConnection developer-connection)
;;     (.setUrl url)))

(defn- set-details [^Model pom group-id artifact-id name version _scm target-path]
  (doto pom
    (.setModelVersion "4.0.0")
    (.setGroupId group-id)
    (.setArtifactId artifact-id)
    (.setName name)
    (.setVersion version)
    (.setBuild (doto (or (.getBuild pom) (Build.))
                 (.setOutputDirectory target-path)))))

(defn- update-or-create-pom [group-id artifact-id name version scm target-path]
  (let [^Model model (if (filesystem/file-exists? "pom.xml")
                       (with-open [^java.io.InputStream in (io/input-stream "pom.xml")]
                         (.read (MavenXpp3Reader.) in false))
                       (Model.))]
    (set-details model group-id artifact-id name version scm target-path)
    (with-open [^java.io.OutputStream out (io/output-stream "pom.xml")]
      (.write (MavenXpp3Writer.) out model))))

(defn pom
  "Pom file creation or update."
  [_args {:keys [mj project] :as _config} options]
  (let [aliases     (-> []
                        (into (:aliases project))
                        (into (:aliases options)))
        version     (:version project)
        name        (:name project)
        group-id    (:group-id project name)
        artifact-id (:artifact-id project name)
        scm         (:scm project)
        target-path (:target-path mj)]
    (clojure-cli/process
     (into
      (clojure-cli/args {:repro true})
      (cond-> []
        (seq aliases) (clojure-cli/aliases-arg "-A" aliases)
        true          (conj "-Spom")))
     (merge options
            {:out :inherit :err :inherit}))
    (update-or-create-pom group-id artifact-id name version scm target-path)))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]])

(defn -main [& args]
  (let [{:keys [arguments options] {:keys [project] :as config} :config}
        (tool-options/parse-options-and-apply-to-config
         args extra-options "pom [options]")]
    (makejack/with-makejack-tool ["pom" options project]
      (pom arguments config options))
    (shutdown-agents)))
