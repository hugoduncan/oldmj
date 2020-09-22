(ns makejack.api.clojure-cli
  "Helpers for working with Clojure CLI"
  (:require [babashka.process :as process]
            [clojure.string :as str]
            [makejack.api.core :as makejack]))

(defn version
  "Return the clojure cli version string"
  []
  (let [res (process/process ["clojure" "--help"] {:out :string})]
    (-> (:out res)
        str/split-lines
        first
        (str/split #"\s+")
        second)))

(defn ^:no-doc version-less [v1 v2]
  (let [pairs (mapv vector v1 v2)]
    (loop [pairs pairs]
      (let [[[a b] & rest] pairs]
        (if a
          (if (< a b)
            true
            (if (= a b)
              (recur rest)
              false))
          false)))))

(defn ^:no-doc in-version-range?
  [version-string {:keys [min max]}]
  (let [version (mapv #(Integer/valueOf ^String %) (str/split version-string #"\."))]
    (and (or (not min) (not (version-less version min)))
         (or (not max) (version-less version max)))))

(def feature-ranges
  "Version ranges for features, min inclusive, max exclusive."
  {:single-alias-exec-fn   {:min [1 10 1 600] :max [1 10 1 672]}
   :exec-fn                {:min [1 10 1 672]}
   :clojure-basis-property {:min [1 10 1 672]}
   :explicit-main          {:min [1 10 1 672]}})

(defn features*
  ([] (features* (version)))
  ([version]
   (set
     (filterv
       #(in-version-range? version (feature-ranges %))
       (keys feature-ranges)))))

(def ^{:doc (:doc (meta #'features*))
       :arglists (:arglists (meta #'features*))}
  features
  (memoize features*))

(defn args
  "Return a cli arguments vector given a map of cli options."
  [{:keys [cp deps force repro threads verbose]}]
  (cond-> []
    cp      (into ["-Scp" cp])
    deps    (into ["-Sdeps" (str deps)])
    force   (conj "-Sforce")
    repro   (conj "-Srepro")
    threads (into ["-Sthreads" (str threads)])
    verbose (conj "-Sverbose")))

(defn aliases-arg
  [option aliases {:keys [elide-when-no-aliases] :or {elide-when-no-aliases false}}]
  (if (or (seq aliases) (not elide-when-no-aliases))
    (str option (str/join (mapv pr-str aliases)))))

(defn ^:no-doc keypaths-in [m]
  (if (or (not (map? m))
          (empty? m))
    '(())
    (for [[k v] m
          subkey (keypaths-in v)]
      (cons k subkey))))

(defn ^:no-doc keypath-values [m]
  (let [keypaths (mapv vec (keypaths-in m))]
    (vec (mapcat
           vector
           keypaths
           (map (partial get-in m) keypaths)))))

(defn exec-args
  "Return a cli arguments vector given an exec function to execute."
  ([options]
   (exec-args options (features)))
  ([{:keys [aliases exec-fn exec-args]} features]
   (cond
     (:single-alias-exec-fn features)
     (cond-> [(aliases-arg "-X" (first aliases) {})]
       exec-fn   (conj (str exec-fn))
       exec-args (into (map str (keypath-values exec-args))))

     (:exec-fn features)
     (cond-> [(aliases-arg "-X" aliases {})]
       exec-fn   (conj (str exec-fn))
       exec-args (into (map str (keypath-values exec-args))))

     :elae
     (throw (ex-info "exec-fn not supported in clojure cli version"
                     {:version (version)})))))

(defn main-args
  "Return a cli arguments vector given an main function to execute."
  ([options]
   (main-args options (features)))
  ([{:keys [aliases expr main main-args report]} features]
   (cond-> []
     (or
       (seq aliases)
       (:explicit-main features)) (conj (aliases-arg
                                          (if (:explicit-main features)
                                            "-M"
                                            "-A")
                                          aliases
                                          {:elide-when-no-aliases
                                           (not (:explicit-main features))}))
     report                       (into ["--report" report])
     expr                         (into ["-e" (str expr)])
     main                         (into ["-m" (str main)])
     main-args                    (into main-args))))

(defn read-clojure-basis
  []
  (some-> (System/getProperty "clojure.basis")
          slurp
          clojure.edn/read-string))

(defn clojure-basis-form
  []
  `(-> (System/getProperty "clojure.basis")
      slurp
      clojure.edn/read-string
      prn))

(defn process
  "Execute clojure process.

  deps ia s map with external dependencies, as specifed on the :deps key
  of deps.edn.

  args is a vector of arguments to pass.

  options is a map of options, as specifed in babashka.process/process.
  Defaults to {:err :inherit}."
  [args options]
  (makejack/process (into ["clojure"] args) options))

(defn classpath
  "Returns the project classpath, with the given extra deps map.

  aliases is a vector of keywords with deps.edn aliases to use.

  deps ia s map with external dependencies, as specifed on the :deps key
  of deps.edn."
  [aliases deps options]
  (let [args (cond-> []
               (not-empty aliases) (conj (aliases-arg
                                           "-A" aliases
                                           {:elide-when-no-aliases true}))
               deps                (into ["-Sdeps" (str {:deps deps})])
               true                (conj "-Spath"))
        res  (process args (merge options {:err :inherit :out :string}))]
    (-> (:out res)
       (str/replace "\n" ""))))
