(ns ^:no-doc makejack.api.aero
  (:require [aero.alpha.core :as aero-alpha]))

;; private in alpha, so redefine here
(defn- rewrap
  [tl]
  (fn [v]
    (tagged-literal (:tag tl) v)))

;; optional leaf value
(defmethod aero-alpha/eval-tagged-literal 'opt-ref
  [tl opts env ks]
  (let [{:keys [:aero.core/incomplete? :aero.core/env :aero.core/value
                :aero.core/incomplete]
         :or   {env env}
         :as   expansion}
        (aero-alpha/expand (:form tl) opts env ks)]
    (if (or incomplete? (and (not (contains? env value))
                             (not (contains? env (pop value)))))
      (-> expansion
         (assoc :aero.core/incomplete? true)
         (update :aero.core/value (rewrap tl))
         (assoc :aero.core/incomplete (or incomplete
                                          {:aero.core/path  (pop ks)
                                           :aero.core/value tl})))
      (let [v (get env value ::none)
            v (if (= ::none v)
                (let [p (get env (pop value))]
                  (if (map? p)
                    (get p (last value))))
                v)]
        (assoc expansion :aero.core/value v)))))

(defmethod aero-alpha/eval-tagged-literal 'regex
  [tl opts env ks]
  (let [{:keys [:aero.core/incomplete? :aero.core/value :aero.core/incomplete]
         :as   expansion}
        (aero-alpha/expand (:form tl) opts env ks)]
    (if incomplete?
      (-> expansion
         (assoc :aero.core/incomplete? true)
         (update :aero.core/value (rewrap tl))
         (assoc :aero.core/incomplete incomplete))
      (assoc expansion :aero.core/value (re-pattern value)))))
