(ns makejack.tools.javac
  (:require [clojure.string :as str]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.tool :as tool]
            [makejack.api.util :as util]))

(defn javac
  "Compile java sources."
  [options _args {:keys [mj project]}]
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
        classpath     (clojure-cli/classpath aliases deps options)
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
    :parse-fn tool/parse-kw-stringlist]])

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main "javac" "[options]" javac extra-options args)))
