(ns makejack.tools.jar
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool-options :as tool-options]
            [makejack.api.util :as util])
  (:gen-class))

(defn depstar
  "Build a jar with depstar.
  If `:jar-type` is `:uberjar`, then build an uberjar, else a thin jar."
  [_args {:keys [mj project] :as _config} options]
  (let [aliases     (-> []
                         (into (:aliases project))
                         (into (:aliases options)))
        deps        '{:deps {seancorfield/depstar {:mvn/version "1.0.97"}}}
        main        (:main project)
        target-path (:target-path mj)
        jar-name    (or (:jar-name project)
                          (makejack/default-jar-name project))
        jar-path    (str (util/path target-path jar-name ))
        uberjar?    (= :uberjar (:jar-type project))
        args        ["-m"
                       (if uberjar?
                         "hf.depstar.uberjar"
                         "hf.depstar.jar")
                       jar-path]
        args        (cond-> args
                      (and
                        uberjar?
                        (:main project)) (into ["-m" (str (:main project))])
                      (:verbose options) (conj "--verbose"))]
    (makejack/clojure aliases deps args (if (:verbose options)
                                          {:out :inherit}))))


(def extra-options
  [["-a" "--aliases ALIASES" "Aliases to use."
    :parse-fn tool-options/parse-kw-stringlist]
   ])

(defn -main [& args]
  (let [{:keys [arguments config options]}
        (tool-options/parse-options-and-apply-to-config
          args extra-options "jar [options]")]
    (binding [makejack/*verbose* (:verbose options)]
      (depstar arguments config options))
    (shutdown-agents)))
