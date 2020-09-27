(ns makejack.tools.compile
  "AOT compilation"
  (:refer-clojure :exclude [compile])
  (:require [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.filesystem :as filesystem]
            [makejack.api.tool :as tool]
            [makejack.api.util :as util]))

(defn- compile-ns-form [ns-sym]
  `(clojure.core/compile '~ns-sym))

(defn compile
  "AOT compilation of clojure sources."
  [options _args {:keys [mj project]}]
  (let [aliases      (-> []
                         (into (:aliases project))
                         (into (:aliases options)))
        deps         (makejack/load-deps)
        paths        (distinct
                      (reduce
                       (fn [paths alias]
                         (into paths (some-> deps :aliases alias :extra-paths)))
                       (:paths deps ["src"])
                       aliases))
        classes-path (:classes-path mj "target/classes")
        source-files (mapcat
                      (partial util/source-files util/clj-source-file?)
                      paths)
        nses         (mapv util/path->namespace source-files)
        form         `(binding [~'*compile-path* ~classes-path]
                        ~@(map compile-ns-form nses))]
    (when-not (->> paths
                   (into (makejack/deps-paths deps aliases))
                   (filter #(= classes-path %))
                   first)
      (makejack/error
       (str "Target path, "
            classes-path
            " must be in the deps.edn :paths")))

    (filesystem/mkdirs classes-path)
    (clojure-cli/process
     (concat
      (clojure-cli/args {:repro true})
      (clojure-cli/main-args {:aliases aliases :expr form}))
     {})))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool/parse-kw-stringlist]])

(defn -main [& args]
  (tool/with-shutdown-agents
    (tool/dispatch-main "compile" "[options]" compile extra-options args)))
