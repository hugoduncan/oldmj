(ns makejack.impl.help
  "Help messages"
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.impl.invokers :as invokers]
            [makejack.impl.resolve :as resolve]))

(defn invoker-doc-string [f]
  (first (str/split-lines (:doc (meta f) ""))))

(defn- target-doc-string [target-map]
  (first
   (str/split-lines
    (:doc target-map
          (invoker-doc-string
           (get invokers/invokers
                (:invoker target-map)))))))

(defn target-doc
  "Construct doc for available tools."
  []
  (let [mj-config (makejack/load-mj)]
    (str/join
     "\n"
     (map
      (fn [[kw m]]
        (format "%25s   %s" (name kw) (target-doc-string m)))
      (sort-by
       key
       (:targets mj-config))))))

(defn invoker-doc
  "Construct doc for available invokers."
  []
  (str/join
   "\n"
   (map
    (fn [[kw f]]
      (format
       "%25s   %s"
       kw
       (invoker-doc-string f)))
    invokers/invokers)))

(defn usage [summary]
  (println
   (str/join
    "\n"
    ["mj [options ...] target"
     ""
     summary
     ""
     "Project targets:"
     (target-doc)
     ""
     "Available invokers:"
     (invoker-doc)])))

(defn help-on-invoker
  "Return help on the specified command."
  [invoker-kw]
  (let [invoker (resolve/resolve-invoker invoker-kw)
        doc     (some-> invoker meta :doc)]
    (if invoker
      (do (println "makejack invoker" invoker-kw "\n")
          (println doc))
      (binding [*out* *err*]
        (makejack/error (str "Unknown invoker: " invoker-kw))))))

(defn help-on-target
  "Return help on the specified command."
  [target-kw]
  (let [config (try
                 (makejack/load-config)
                 (catch Exception _))
        target (resolve/resolve-target target-kw config)
        doc    (:doc target)]
    (if target
      (do (println "mj" (name target-kw) "\n")
          (if doc
            (println doc)
            (println "Undocumented")))
      (binding [*out* *err*]
        (makejack/error (str "Unknown target: " (name target-kw)))))))

(defn help-on
  "Return help on the specified command."
  [cmd]
  (if (str/starts-with? cmd ":")
    (help-on-invoker (read-string cmd))
    (help-on-target (keyword cmd))))
