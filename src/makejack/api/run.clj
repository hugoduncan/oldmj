(ns makejack.api.run
  (:require [makejack.api.core :as makejack]
            [makejack.api.project :as project]
            [makejack.api.resolve :as resolve]))


(defn apply-options [{:keys [project] :as config} options target-kw]
  (let [profiles (cond-> []
                   target-kw (into (some-> config :targets target-kw :profiles))
                   true (into (:profiles options)))]
    (prn :target-kw target-kw)
    (assoc config :makejack/project (project/with-profiles project profiles))))

(defn run-command [cmd args options]
  (let [[config e]    (try
                       [(makejack/load-config) nil]
                       (catch Exception e
                         [nil e]))
        [kw tool-sym] (resolve/resolve-tool-sym cmd config)
        config        (apply-options config options kw)
        f             (resolve/resolve-tool tool-sym)]
    (when (and e (not (:no-config-required (meta f))))
      (makejack/error (str "Bad configuration: " e)))
    (f args kw config options)))
