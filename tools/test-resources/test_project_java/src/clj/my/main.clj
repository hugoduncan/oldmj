(ns my.main
  (:import [my Hello])
  (:gen-class))

(defn -main [& args]
  (apply println (. (Hello.) hello) args))
