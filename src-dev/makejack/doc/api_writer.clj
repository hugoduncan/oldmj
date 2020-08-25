(ns makejack.doc.api-writer
  "A codox writer for output to hugo."
  (:use [hiccup core page element])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [codox.writer.html :as html]
            [makejack.api.util :as util]))

(defn var-data [var-map]
  (-> var-map
     (select-keys [:name])
     (update :name str)))

(defn- namespace-data [namespace]
  (-> namespace
     (select-keys [:name :publics])
     (update :publics #(mapv var-data %))
     (update :name str)))

(defn- write-namespaces-data [output-dir project]
  (util/mkdirs (util/path output-dir "data"))
  (spit (io/file (util/path output-dir "data" "namespaces.yml"))
        (yaml/generate-string
          (mapv namespace-data (:namespaces project)))))

(defn- namespace-content [project namespace]
  (str
    "---\n"
    (yaml/generate-string
      {:title (str (:name namespace))
       :layout "api-docs"
       :group "api-docs"
       :toc true}
      :dumper-options {:flow-style :block})
    "---\n"
    (html
      [:div#content.namespace-docs
       [:h1#top.anchor (h (:name namespace))]
       (#'html/added-and-deprecated-docs namespace)
       [:div.doc (#'html/format-docstring project namespace namespace)]
       (for [var (#'html/sorted-public-vars namespace)]
         (#'html/var-docs project namespace var))])))

(defn- write-namespaces [output-dir project]
  (let [version (re-find #"\d+\.\d+" (:version project))]
    (doseq [namespace (:namespaces project)]
      (let [p (util/path output-dir "content" "docs" version "api-docs")]
        (spit
          (io/file (util/path p (str (:name namespace) ".html")))
          (namespace-content project namespace))))))

(defn write-docs
  "Take raw documentation info and turn it into hugo pages."
  [{:keys [output-path] :as project}]
  (prn "writing to " output-path)
  (doto output-path
    (write-namespaces-data project)
    ;; (write-index project)
    (write-namespaces project)
    ;; (write-documents project)
    ))
