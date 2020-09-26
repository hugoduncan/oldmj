(ns makejack.impl.util
  (:require [makejack.api.core :as makejack]))

(defn maybe-string [x]
  (if (string? x)
    x))

(defn handle-invoker-exception
  [e]
  (let [data (ex-data e)]
    (if (= :babashka.process/error (:type data))
      (makejack/error
       (or
        (maybe-string (:err data))
        (maybe-string (:out data))
        "Failed")
       (:exit data 1))
      (throw e))))
