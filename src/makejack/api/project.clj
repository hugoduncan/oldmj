(ns makejack.api.project
  (:require [makejack.api.util :as util]))

(defn project-merge [vals]
  (cond
    (every? vector? vals) (reduce into vals)
    :else (last vals)))

(defn profiles
  [project profiles]
  (reduce
    (fn [res profile-kw]
      (if-let [profile (some-> project :profiles profile-kw)]
        (util/deep-merge-with project-merge res profile)
        res))
    {}
    profiles))

(defn with-profiles
  [project profiles]
  (reduce
    (fn [project profile-kw]
      (if-let [profile (some-> project :profiles profile-kw)]
        (util/deep-merge-with project-merge project profile)
        project))
    project
    profiles))
