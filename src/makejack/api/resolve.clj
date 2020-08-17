(ns makejack.api.resolve
  (:require [clojure.string :as str]
            [makejack.api.builtins :as builtins]
            [makejack.api.core :as makejack]))

(defn resolve-tool [tool-ns]
  (try
    (require tool-ns)
    (catch Exception _))
  (or (builtins/builtins tool-ns)
      (ns-resolve tool-ns (symbol (last (str/split (name tool-ns) #"\."))))))

(defn resolve-target [kw config]
  (let [target (get-in config [:targets kw])
        tool (:tool target (str "makejack." (name kw)))]
    (when-not target
      (makejack/error
        (str "No target specified in mj.edn for " kw)))
    (resolve-tool (str tool))))

(defn target-tool [target-kw config]
  (let [target (get-in config [:targets target-kw])
        tool (:tool target (str "makejack." (subs (name target-kw) 1)))]
    (when-not target
      (makejack/error
        (str "No target specified in mj.edn for " target-kw)))
    tool))

(defn resolve-tool-sym [cmd config]
  (if (keyword? cmd)
    (let [tool (target-tool cmd config)]
      [cmd tool])
    (let [kw     (keyword (name cmd))
          target (get-in config [:targets kw])]
      (println "target" target)
      (if target
        [kw (:tool target)]
        (let [ns-segs (str/split (name cmd) #"\.")
              ns-segs (if (= 1 (count ns-segs))
                        (into ["makejack"] ns-segs)
                        ns-segs)]
          [(keyword (last ns-segs))
           (symbol (str/join "." ns-segs))])))))
