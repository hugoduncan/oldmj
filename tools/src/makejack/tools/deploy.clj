(ns makejack.tools.deploy
  "Deploy to repository"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]
            [org.httpkit.client :as client]
            [org.httpkit.sni-client :as sni-client])
  (:import [java.net URL]
           [org.apache.maven.artifact.repository.metadata Metadata]
           [org.apache.maven.artifact.repository.metadata.io.xpp3 MetadataXpp3Writer]))

;; Change default client for your whole application:
(alter-var-root
  #'org.httpkit.client/*default-client*
  (fn [_] sni-client/default-client))

(defn paths-to-deploy
  [mj project]
  (let [target-path (:target-path mj)
        version     (:version project)
        artifact-id (:artifact-id project)]
    [{:source-path (path/path "pom.xml")
      :target-path (str artifact-id "-" version ".pom")}
     {:source-path (path/path target-path (:jar-name project))
      :target-path (:jar-name project)}]))

(defn prepare-path-deploy! [path-map dir]
  (let [source-path (:source-path path-map)
        target-path (path/path dir (:target-path path-map))]
    (filesystem/copy-file! source-path target-path)
    (let [{:keys [md5 sha1]} (util/file-hashes source-path)]
      (spit (.toFile (path/path-with-extension target-path ".md5")) md5)
      (spit (.toFile (path/path-with-extension target-path ".sha1")) sha1))))

(defn multipart-map
  "Return a multipart map for the given paths"
  [path-like]
  {:name     (str (path/filename path-like))
   :filename (str (path/filename path-like))
   :content  (.toFile (path/path path-like))})

(defn url-with-path [url url-path]
  (if (.isAbsolute (path/path url-path))
    (str url url-path)
    (str url "/" url-path)))

(defn put!
  "Put the multiparts to the specified url.
  Return a promise."
  [request url url-path]
  (let [url (url-with-path url url-path)]
    (client/put url request)))

(defn new-url
  ^URL [url]
  (URL. url))

(defn url-credentials
  [^URL url]
  (let [user-info (some-> url (.getUserInfo))
        [username password] (some-> user-info (.split ":"))]
    (if username
      {:username username :password password})))

(defn read-with-prompt [prompt]
  (print prompt)
  (flush)
  (read-line))

(defn read-password
  []
  (if-let [console (System/console)]
    (String. (.readPassword console "%s" (into-array ["password: "])))
    (do
      (println "WARN: Unable to turn off echoing,"
               "WARN: password is printed to the console")
      (read-with-prompt "Password: "))))

(defn env-credentials
  [repo-name]
  (if-let [credentials (or (System/getenv (str "REPO_" (str/upper-case repo-name)))
                           (System/getenv "REPO_CREDENTIALS"))]
    (let [[username password] ((str/split credentials ":"))]
      {:username username
       :password password})))

(defn interactive-credentials
  []
  {:username (read-with-prompt "username: ")
   :password (read-password)})

(defn gpg-program
  []
  (or (System/getenv "MAKEJACK_GPG") "gpg"))

(defn gpg
  [args]
  (let [res (makejack/process (into [(gpg-program)] args) {:out :string})]
    (:out res)))

(defn gpg-decrypt [path-like]
  (gpg ["--quiet" "--batch" "--decrypt" "--" (str path-like)]))

(defn read-gpg-file [path-like]
  (edn/read-string (gpg-decrypt path-like)))

(defn gpg-credentials-path
  []
  (path/path
    (System/getProperty "user.home")
    ".makejack"
    "credentials.edn.gpg"))

(defn gpg-credentials
  [repo-name]
  (let [path (gpg-credentials-path)]
    (if (filesystem/file-exists? path)
      (if-let [config (read-gpg-file path)]
        (get config repo-name)))))

(defn credentials
  [repository]
  (let [url         (:url repository)
        credentials (or (env-credentials (:name repository))
                        (url-credentials (new-url url))
                        (gpg-credentials (:name repository))
                        (interactive-credentials))]
    (when-not credentials
      (throw (ex-info (str "No credentials for " url) {})))
    credentials))

(defn base-request
  [credentials]
  {:user-agent "makejack.tools.deploy"
   :keepalive  100
   :basic-auth [(:username credentials) (:password credentials)]})

(defn request-with-cookie
  [request response]
  (if-let [cookie (:set-cookie (:headers response))]
    (let [cookie (first (str/split cookie #";"))]
      (update-in request [:headers] (fnil assoc {}) "Cookie" cookie))
    request))

(defn check-response [response]
  (let [status (:status response)]
    (prn response)
    (when (>= status 400)
      (throw (ex-info "Upload failed" {:response response}))))
  response)

(defn group-path [project]
  (str/replace (:group-id project) "." "/"))

(defn url-path [project]
  (str (path/path
         (group-path project)
         (:artifact-id project)
         (:version project))))

(defn metadata-url-path [project]
  (str (path/path
         (group-path project)
         (:artifact-id project)
         "maven-metadata.xml")))

(defn deploy-dir [project dir repository request]
  (let [url            (:url repository)
        url-path       (url-path project)
        [path & paths] (filterv filesystem/file? (filesystem/list-paths dir))
        response       (check-response
                         @(put!
                            (assoc request :body (path/as-file path))
                            url
                            (path/path url-path (path/filename path))))
        request        (request-with-cookie request response)
        uploads        (for [path paths]
                         (put!
                           (assoc request :body (path/as-file path))
                           url
                           (path/path url-path (path/filename path))))]
    (doseq [upload uploads]
      (check-response @upload))
    request))

(defn generate-metadata-model
  [project dir]
  (let [metadata (Metadata.)
        writer (MetadataXpp3Writer.)]
    (.setGroupId metadata (:group-id project))
    (.setArtifactId metadata (:artifact-id project))
    (.setVersion metadata (:version project))
    (.setFileComment writer "Written by Makejack")
    (with-open [^java.io.OutputStream out (io/output-stream
                                            (path/as-file
                                              (path/path dir "maven-metadata.xml")))]
      (.write writer out metadata ))))

(defn deploy-metadata [project dir repository request]
  (check-response
    @(put!
       (merge
         request
         {:body (.toFile (path/path dir "maven-metadata.xml"))})
       (:url repository)
       (metadata-url-path project))))

(def default-repos
  {"clojars" {:url "https://clojars.org/repo"}
   "central" {:url "https://repo1.maven.org/maven2/"}})

(defn repository [repo-name]
  (let [deps-edn (makejack/load-deps)
        repos    (merge default-repos
                        (:mvn/repos deps-edn))
        repo-name (or repo-name "clojars")]
    (assoc
      (or (get repos repo-name)
          (throw (ex-info "Could not find repository"
                          {:repository repo-name
                           :repos      repos
                           :deps-edn   deps-edn})))
      :name repo-name)))

(defn deploy
  "Deploy a project to a maven repository."
  [[repo-name] {:keys [mj project] :as _config} _options]
  (let [repository  (repository repo-name)
        credentials (credentials repository)
        paths       (paths-to-deploy mj project)]
    (filesystem/with-temp-dir [dir "mj-deploy"]
      (doseq [path paths]
        (prepare-path-deploy! path dir))
      (let [request  (base-request credentials)
            request  (deploy-dir project dir repository request)]
        (generate-metadata-model project dir)
        (deploy-metadata project dir repository request))))
  nil)

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments options] {:keys [project] :as config} :config}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "pom [options]")]
    (makejack/with-makejack-tool ["deploy" options project]
      (deploy arguments config options))
    (shutdown-agents)))
