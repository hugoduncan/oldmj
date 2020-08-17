(ns makejack.compile
  "AOT compilation"
  (:refer-clojure :exclude [compile])
  (:require [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(defn compile-ns-form [ns-sym]
  `(clojure.core/compile '~ns-sym))

(defn compile [args config-kw config options]
  (let [project      (makejack/load-project)
        deps         (makejack/load-deps)
        paths        (:paths deps)
        compile-config (get-in config [:targets config-kw])
        target       (:target compile-config "target")
        aliases      (:aliases compile-config)
        source-files (mapcat util/source-files paths)
        nses         (mapv util/path->namespace source-files)
        form         `(binding [*compile-path* ~target]
                        ~@(map compile-ns-form nses))]

    (when-not (first (filter #(= target %) paths))
      (makejack/error
        ("Target path, " target " must be in the deps.edn :paths")))

    (util/mkdirs target)
    (makejack/clojure
      aliases
      nil
      ["-e" (str form)])))
