(ns makejack.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [makejack.builtins :as builtins]
            [makejack.core :as makejack])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-h" "--help"]])

(defn resolve-tool [tool-ns]
  (let [;; ns-str (if (str/includes? ns-str "." )
        ;;          ns-str
        ;;          (str "makejack." ns-str))
        ;; tool-ns (symbol ns-str)
        ]
    (println "tool-ns" tool-ns)
    (try
      (require tool-ns)
      (catch Exception _))
    (or (builtins/builtins tool-ns)
        (ns-resolve tool-ns (symbol (last (str/split (name tool-ns) #"\.")))))))

(defn resolve-target [kw-str config]
  (let [kw (read-string kw-str)
        target (get-in config [:targets kw])
        tool (:tool target (str "makejack." (subs kw-str 1)))]
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

;; (defn target-kw [cmd]
;;   (if (str/starts-with? cmd ":")
;;     (read-string kw-str)
;;     (keyword cmd)))

(defn resolve-tool-sym [cmd config]
  (if (str/starts-with? cmd ":")
    (let [kw   (read-string cmd)
          tool (target-tool kw config)]
      [kw tool])
    (let [kw     (keyword cmd)
          target (get-in config [:targets kw])]
      (if target
        [kw (:tool target)]
        (let [ns-segs (str/split cmd #"\.")
              ns-segs (if (= 1 (count ns-segs))
                        (into ["makejack"] ns-segs)
                        ns-segs)]
          [(keyword (last ns-segs))
           (symbol (str/join "." ns-segs))])))))

(defn run-command [cmd args options]
  (let [config (makejack/load-config)
        [kw tool-sym] (resolve-tool-sym cmd config)
        f (resolve-tool tool-sym)
        ;; f (if (str/starts-with? cmd ":")
        ;;     (resolve-target cmd config)
        ;;     (resolve-tool cmd))
        ]
    (f args kw config options)))

(defn -main [& args]
  (let [{:keys [arguments] :as options} (cli/parse-opts
                                          args cli-options)]
    (println options)
    (run-command (first arguments) (rest arguments) options)
    (shutdown-agents)))
