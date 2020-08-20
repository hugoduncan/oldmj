(ns makejack.compile
  "AOT compilation"
  (:refer-clojure :exclude [compile])
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn compile-ns-form [ns-sym]
  `(clojure.core/compile '~ns-sym))

(defn compile
  "AOT compilation of clojure sources."
  [args target-kw config options]
  (let [deps           (makejack/load-deps)
        paths          (:paths deps)
        compile-config (-> config :project :compile)
        target-config  (get-in config [:targets target-kw])
        classes-path   (:classes-path target-config "target/classes")
        aliases        (:aliases compile-config)
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
    (let [res (makejack/clojure
                aliases
                nil
                ["-e" (str form)]
                {})]
      (when (pos? (:exit res))
        (makejack/error (:err res))))))
