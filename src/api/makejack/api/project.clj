(ns makejack.api.project
  "Helpers for working with project.edn project maps."
  (:require [makejack.api.util :as util]))

;; (defn- default-jar-name
;;   "Helper to return the default jar file name."
;;   [project jar-type]
;;   (str (:artifact-id project)
;;        "-" (:version project)
;;        (if (= :uberjar jar-type) "-standalone" "")
;;        ".jar"))

;; (defn project-with-defaults
;;   "Return project with defaults for derived project keys."
;;   [project]
;;   (let [project (merge
;;                   {:group-id    (:name project)
;;                    :artifact-id (:name project)}
;;                   project)
;;         project (update-in project [:profiles :jar]
;;                            #(merge {:jar-name (default-jar-name project :jar)}
;;                                    %))
;;         project (update-in project [:profiles :uberjar]
;;                            #(merge {:jar-name (default-jar-name project :uberjar)
;;                                     :jar-type :uberjar}
;;                                    %))]
;;     project))

;; (project-with-defaults {:name "x" :version "0.1"})


(defn- project-merge
  [vals]
  (cond
    (every? vector? vals) (reduce into vals)
    :else (last vals)))

(defn with-profiles
  "Return the project with the given profiles applied.

  project is a project.edn map.

  profiles is a vector of profile keywords to apply."
  [project profiles]
  (reduce
    (fn [project profile-kw]
      (if-let [profile (some-> project :profiles profile-kw)]
        (util/deep-merge-with project-merge project profile)
        project))
    project
    profiles))
