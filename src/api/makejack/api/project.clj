(ns makejack.api.project
  "Helpers for working with project.edn project maps."
  (:require [makejack.api.util :as util]))

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
