(ns makejack.impl.resolve
  (:require [makejack.impl.invokers :as invokers]))

(defn resolve-invoker
  "Return an invoker function, given an invoker keyword."
  [invoker-kw]
  (invokers/invokers invoker-kw))

(defn resolve-target [target-kw config]
  (get-in config [:mj :targets target-kw]))

(defn resolve-target-invoker [target]
  (let [invoker (:invoker target)]
    (if invoker
      (resolve-invoker invoker))))
