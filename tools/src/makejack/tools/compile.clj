(ns makejack.tools.compile
  "AOT compilation"
  (:refer-clojure :exclude [compile])
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util]))

(defn- compile-ns-form [ns-sym]
  `(clojure.core/compile '~ns-sym))

(defn compile
  "AOT compilation of clojure sources."
  [_args {:keys [mj project] :as config} options]
  (let [aliases        (-> []
                          (into (:aliases project))
                          (into (:aliases options)))
        deps           (makejack/load-deps)
        paths          (distinct
                         (reduce
                           (fn [paths alias]
                             (into paths (some-> deps :aliases alias :extra-paths)))
                           (:paths deps)
                           aliases))
        classes-path   (:classes-path mj "target/classes")
        source-files   (mapcat
                         (partial util/source-files util/clj-source-file?)
                         paths)
        nses           (mapv util/path->namespace source-files)
        form           `(binding [~'*compile-path* ~classes-path]
                          ~@(map compile-ns-form nses))]
    (when-not (->> paths
                 (into (makejack/deps-paths deps aliases))
                 (filter #(= classes-path %))
                 first)
      (makejack/error
        (str "Target path, "
             classes-path
             " must be in the deps.edn :paths")))

    (util/mkdirs classes-path)
    (makejack/clojure
      aliases
      nil
      ["-e" (str form)]
      {})))

(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "compile options")]
    (binding [makejack/*verbose* (:verbose options)]
      (compile arguments config options))
    (shutdown-agents)))
