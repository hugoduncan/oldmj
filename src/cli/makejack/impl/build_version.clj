(ns makejack.impl.build-version
  (:require [clojure.string :as str]
            [makejack.api.core :as makejack]
            [makejack.api.util :as util]))

(def ns-def
  '(ns makejack.impl.version))

(defn current-date-time
  []
  (str (java.time.Instant/now)))

(defn resolved-deps
  []
  (let [deps-str (:out (makejack/clojure [] nil ["-Stree"] {}))]
    (->> deps-str
       str/split-lines
       (mapv str/trim)
       (mapv #(str/split % #"\s+")))))

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
    (spit "src/cli/makejack/impl/version.clj"
          (str ns-def "\n"
               `(def ~'info ~info) "\n"))))

(defn info []
  (require 'makejack.impl.version :reload)
  (let [info-var (ns-resolve 'makejack.impl.version 'info)]
    @info-var))

(defn -main [& args]
  (build-version)
  (shutdown-agents))
