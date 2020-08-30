(ns makejack.tools.javac
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]))

(defn javac
  "Compile java sources."
  [_args {:keys [mj project]} _options]
  (let [java-paths      (:java-paths project)
        javac-options   (:javac-options project)
        source-files    (mapcat
                          (partial util/source-files util/java-source-file?)
                          java-paths)
        args            (-> ["javac"]
                           (into javac-options)
                           (into source-files))]
    (makejack/sh args {})))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "javac [options]")]
    (binding [makejack/*verbose* (:verbose options)]
      (javac arguments config options))
    (shutdown-agents)))
