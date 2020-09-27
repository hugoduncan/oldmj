(ns makejack.impl.build-version
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(def ns-def
  '(ns makejack.impl.version))

(defn current-date-time
  []
  (str (java.time.Instant/now)))

(defn- build-lib-map-from-stree [s]
  (let [[coord ver-or-repo sha] (str/split s #"\s+")]
    (if sha
      {(symbol coord) {:git/url ver-or-repo :sha sha}}
      {(symbol coord) {:mvn/version ver-or-repo}})))

(defn resolved-deps-with-stree
  []
  (let [deps-str (:out (clojure-cli/process ["-Srepro" "-Stree"] {:out :string}))]
    (->> deps-str
         str/split-lines
         (mapv str/trim)
         (mapv build-lib-map-from-stree)
         (into {}))))

(defn filter-basis-dep
  [[coord spec]]
  [coord (dissoc spec :paths :dependents :deps/manifest :deps/root :parents)])

(defn resolved-deps-with-clojure-basis
  []
  (let [deps-str (:out
                  (clojure-cli/process
                   (concat
                    (clojure-cli/args {:repro true})
                    (clojure-cli/main-args
                     {:expr (clojure-cli/clojure-basis-form)}))
                   {}))]
    (->> deps-str
         edn/read-string
         :libs
         (mapv filter-basis-dep)
         (into {}))))

(defn resolved-deps
  []
  (if (:clojure-basis-property (clojure-cli/features))
    (resolved-deps-with-clojure-basis)
    (resolved-deps-with-stree)))

(defn vm-info
  []
  (let [props (System/getProperties)
        props (zipmap
               (map keyword (keys props))
               (vals props))]
    (select-keys props [:java.runtime.name
                        :java.vm.version
                        :java.vm.vendor
                        :java.vm.name
                        :java.runtime.version
                        :os.name
                        :os.version
                        :java.specification.version
                        :java.vm.specification.version
                        :java.version
                        :java.vendor])))

(defn build-version
  "Create the makejack.impl.version namespace source"
  []
  (let [project   (makejack/load-project* {})
        version   (:version project)
        git-sha   (util/git-sha)
        date-time (current-date-time)
        deps      (resolved-deps)
        vm        (vm-info)
        info      {:version  version
                   :git-sha  git-sha
                   :built-at date-time
                   :deps     deps
                   :vm-info  vm}]
    (spit "src/makejack/impl/version.clj"
          (str ns-def "\n"
               `(def ~'info '~info) "\n"))))

(defn info []
  (require 'makejack.impl.version :reload)
  (let [info-var (ns-resolve 'makejack.impl.version 'info)]
    @info-var))

(defn -main [& _args]
  (build-version)
  (shutdown-agents))
