(ns ^:no-doc makejack.api.aero
  (:require [aero.alpha.core :as aero-alpha]
            [aero.core :as aero]
            ;; [clojure.pprint]
            ;; [fipp.ednize]
            [makejack.api.default-config :as default-config]
            [makejack.api.util :as util]))

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

(defmethod aero-alpha/eval-tagged-literal 'version-string
  [tl opts env ks]
  (let [{:keys [:aero.core/incomplete? :aero.core/value :aero.core/incomplete]
         :as   expansion}
        (aero-alpha/expand (:form tl) opts env ks)]
    (if incomplete?
      (-> expansion
         (assoc :aero.core/incomplete? true)
         (update :aero.core/value (rewrap tl))
         (assoc :aero.core/incomplete incomplete))
      (assoc expansion :aero.core/value (util/format-version-map value)))))

;; internal, to inject the default jar name depending on the :jar-type
(defmethod aero/reader 'default-jar-name
  [_opts _tag [artifact-id version jar-type]]
  (if (= jar-type :uberjar)
    (str artifact-id "-" version "-standalone.jar")
    (str artifact-id "-" version ".jar")))

;; top level #mj tag
(defmethod aero-alpha/eval-tagged-literal 'mj
  [tl opts env ks]
  (aero-alpha/expand
    (merge default-config/default-mj-config (:form tl))
    opts
    env
    ks))

;; target to inject the default targets
;; Use :all, or a vector of keywords to select targets.
(defmethod aero-alpha/eval-tagged-literal 'default-targets
  [tl opts env ks]
  {:aero.core/value
   (if (= :all (:form tl))
     default-config/default-targets
     (select-keys default-config/default-targets (:form tl)))})


;; include project with given read-config options
(defmethod aero-alpha/eval-tagged-literal 'project
  [tl opts env ks]
  (let [value (:project
               (aero.core/read-config
                 (java.io.StringReader.
                   (pr-str default-config/project-with-defaults))
                 (merge opts (:form tl))))]
    {:aero.core/value value
     :aero.core/env (assoc env ks value)}))

(defmethod print-method java.util.regex.Pattern
  [^java.util.regex.Pattern v ^java.io.Writer w]
  (.write w (pr-str (tagged-literal 'regex (.pattern v)))))
