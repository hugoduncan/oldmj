(ns makejack.codox
  (:require [makejack.core :as makejack]))

(defn -main [& _args]
  (let [project (makejack/load-project)]
    (makejack/clojure
      ["codox" "clj-xchart" "test.check"]
      `(do
         (:require [codox.main :as codox])
         (codox/generate-docs
           ~(:codox project))))))

(defn codox [args options]
  (println "codox")
  (let [project (makejack/load-project)
        codox (:codox project)
        deps `{:deps {~'codox {:mvn/version ~(:codox-version codox "0.10.6")}}}]
    (makejack/clojure
      (:aliases codox)
      deps
      ["-e" `(do
               (require '[~'codox.main :as ~'codox])
               (codox/generate-docs
                 ~(:config codox)))])))
