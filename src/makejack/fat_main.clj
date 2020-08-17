(ns makejack.fat-main
  (:require ;; makejack.default-config
            ;; makejack.util
            ;; makejack.core
            ;; makejack.builtins
            makejack.main
            ;; makejack.compile
            ;; makejack.babashka
            ;; makejack.depstar
            ;; makejack.graal-compile
            ))

(defn -main [& args]
  (apply makejack.main/-main args))
