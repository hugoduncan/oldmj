(ns makejack.impl.help
  "Help messages"
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.impl.builtins :as builtins]
            [makejack.impl.resolve :as resolve]))


(defn tool-doc-string [f]
  (first (str/split-lines (:doc (meta f) ""))))

(defn- target-doc-string [target-map]
  (first
    (str/split-lines
      (:doc target-map
            (tool-doc-string
              (get builtins/builtins
                   (:tool target-map)))))))

(defn target-doc
  "Construct odc for available tools."
  []
  (let [config (try
                 (makejack/load-config)
                 (catch Exception _))]
    (str/join
      "\n"
      (map
        (fn [[kw m]]
          (format "%25s   %s" kw (target-doc-string m)))
        (sort-by
          key
          (:targets config))))))

(defn tool-doc
  "Construct odc for available tools."
  []
  (str/join
    "\n"
    (map
      (fn [[sym f]]
        (format
          "%25s   %s"
          (name sym)
          (tool-doc-string f)))
      builtins/builtins)))

(defn usage [summary]
  (println
    (str/join
      "\n"
      ["makejack [options ...] target"
       ""
       summary
       ""
       "Project targets:"
       (target-doc)
       ""
       "Available tools:"
       (tool-doc)])))

(defn help-on
  "Return help on the specified command."
  [cmd]
  (let [config (try
                 (makejack/load-config)
                 (catch Exception _))
        cmd (read-string cmd)
        [kw tool-sym] (resolve/resolve-tool-sym cmd config)
        f (resolve/resolve-tool tool-sym)
        doc (->> (meta f)
                :doc
                str/split-lines
                (mapv str/trim)
                (str/join "\n"))
        ]
    (println "makejack" cmd "\n")
    (println doc)))
