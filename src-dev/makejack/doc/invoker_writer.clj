(ns makejack.doc.invoker-writer
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

(defn- write-tools-data [output-dir project]
  (util/mkdirs (util/path output-dir "data"))
  (spit (io/file (str (util/path output-dir "data" "invokers.yml")))
        (yaml/generate-string
          (mapv namespace-data (:namespaces project)))))

(defn- var-docs [project namespace var]
  [:div.public.anchor {:id (h (#'html/var-id (:name var)))}
   [:h3 (h (str (:name namespace) "/" (:name var)))]
   (if-not (= (:type var) :var)
     [:h4.type (name (:type var))])
   (if (:dynamic var)
     [:h4.dynamic "dynamic"])
   (#'html/added-and-deprecated-docs var)
   (if (:type-sig var)
     [:div.type-sig
      [:pre (h (#'html/type-sig namespace var))]])
   [:div.usage
    (for [form (#'html/var-usage var)]
      [:code (h (pr-str form))])]
   [:div.doc (#'html/format-docstring project namespace var)]
   (if-let [members (seq (:members var))]
     [:div.members
      [:h4 "members"]
      [:div.inner
       (let [project (dissoc project :source-uri)]
         (map (partial var-docs project namespace) members))]])
   (if (:source-uri project)
     (if (:path var)
       [:div.src-link (link-to (#'html/var-source-uri project var) "view source")]
       (println "Could not generate source link for" (:name var))))])


(defn- namespace-content [project namespace]
  (str
    "---\n"
    (yaml/generate-string
      {:title (str (:name namespace))
       :layout "tool-docs"
       :group "invoker-docs"
       :toc true}
      :dumper-options {:flow-style :block})
    "---\n"
    (html
      [:div#content.namespace-docs
       ;; [:h1#top.anchor (h (:name namespace))]
       (#'html/added-and-deprecated-docs namespace)
       ;; [:div.doc (#'html/format-docstring project namespace namespace)]
       (for [var (#'html/sorted-public-vars namespace)]
         (var-docs project namespace var))])))

(defn- write-namespaces [output-dir project]
  (let [version (re-find #"\d+\.\d+" (:version project))]
    (util/mkdirs (util/path output-dir "content" "docs" version "invoker-docs"))
    (doseq [namespace (:namespaces project)]
      (let [p (util/path output-dir "content" "docs" version "invoker-docs")]
        (spit
          (io/file (str (util/path p (str (:name namespace) ".html"))))
          (namespace-content project namespace))))))

(defn write-docs
  "Take raw documentation info and turn it into hugo pages."
  [{:keys [output-path] :as project}]
  (prn "writing to" output-path "from" (util/cwd))
  (doto output-path
    (write-tools-data project)
    ;; (write-index project)
    (write-namespaces project)
    ;; (write-documents project)
    ))
