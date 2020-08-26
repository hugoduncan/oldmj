(ns makejack.impl.resolve
  (:require [makejack.impl.invokers :as invokers]))

(defn resolve-invoker
  "Return an invoker function, given an invoker keyword."
  [invoker-kw]
  (invokers/invokers invoker-kw))

;; (defn resolve-target [kw config]
;;   (let [target (get-in config [:targets kw])
;;         invoker (:invoker target (str "makejack." (name kw)))]
;;     (when-not target
;;       (makejack/error
;;         (str "No target specified in mj.edn for " kw)))
;;     (resolve-invoker (str invoker))))

;; (defn target-invoker [target-kw config]
;;   (let [target (get-in config [:targets target-kw])
;;         invoker (:invoker target (str "makejack." (subs (name target-kw) 1)))]
;;     (when-not target
;;       (makejack/error
;;         (str "No target specified in mj.edn for " target-kw)))
;;     invoker))

(defn resolve-target [target-kw config]
  (get-in config [:targets target-kw]))

(defn resolve-target-invoker [target]
  (let [invoker (:invoker target)]
    (if invoker
      (resolve-invoker invoker))))
