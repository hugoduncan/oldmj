(ns makejack.impl.run
  (:require [makejack.api.core :as makejack]
            [makejack.api.project :as project]
            [makejack.impl.resolve :as resolve]))


(defn apply-options [{:keys [project] :as config} options target-kw]
  (let [profiles (cond-> []
                   target-kw (into (some-> config :targets target-kw :profiles))
                   true (into (:profiles options)))]
    (assoc config :makejack/project (project/with-profiles project profiles))))

(defn run-command [cmd args options]
  (let [[config e]    (try
                       [(makejack/load-config) nil]
                       (catch Exception e
                         [nil e]))
        target-kw     (keyword cmd)
        target        (resolve/resolve-target target-kw config)
        f             (resolve/resolve-target-invoker target)
        config        (apply-options config options target-kw)]
    (when (and e (not (:no-config-required (meta f))))
      (makejack/error (str "Bad configuration: " e)))
    (when-not target
      (makejack/error (str "Unknown target: " cmd)))
    (when-not f
      (makejack/error (str "Invalid invoker for target: " cmd)))
    (f args target-kw config options)))
