(ns makejack.tools.javac
  (:require [clojure.string :as str]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]))

(defn javac
  "Compile java sources."
  [_args {:keys [mj project]} options]
  (let [java-paths    (:java-paths project)
        javac-options (:javac-options project)
        source-files  (->> java-paths
                         (mapcat
                           (partial util/project-source-files util/java-source-file?))
                         (mapv str))
        aliases       (-> []
                         (into (:aliases project))
                         (into (:aliases options)))
        deps          (:deps options)
        classpath     (clojure-cli/classpath aliases deps)
        args          (-> ["javac"
                          "-classpath" classpath
                          "-sourcepath" (str/join ":" java-paths)
                          "-d" (:classes-path mj)]
                         (into javac-options)
                         (into source-files))]
    (filesystem/mkdirs (:classes-path mj))
    (makejack/process args {})))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "javac [options]")]
    (binding [makejack/*verbose* (:verbose options)]
      (try
        (javac arguments config options)
        (finally
          (shutdown-agents))))))
