(ns makejack.api.glam
  (:require [clojure.string :as str]
            [makejack.api.clojure-cli :as clojure-cli]
            [makejack.api.core :as makejack]
            [makejack.api.path :as path]))

(def deps {:deps
           {'borkdude/glam
            {:git/url "https://github.com/borkdude/glam"
             :sha     "6abf4c0c17848df1c41108978c31566ac8f9b42b"}}})

(defonce glam-cp (volatile! nil))

(defn resolved-glam-cp
  []
  (try
    (->
     (clojure-cli/process
      (clojure-cli/args {:deps deps :repro true :path true})
      {:err :string
       :out :string})
     :out
     str/trim)
    (catch Exception e
      (throw (ex-info "Failed to install glam" {} e)))))

(defn strip-clj-from-cp
  [cp]
  (->>
   (str/split cp #":")
   (remove #(re-find #"clojure-|core.specs.alpha|spec.alpha" %))
   (str/join ":")))

(defn set-glam-cp!
  []
  (let [cp (resolved-glam-cp)]
    (vreset! glam-cp {:clojure  cp
                      :babashka (strip-clj-from-cp cp)})))

(defn glam-classpath [target]
  (when-not @glam-cp
    (set-glam-cp!))
  (@glam-cp target))

(defn install-and-setup-glam
  []
  (try
    (clojure-cli/process
     (-> []
         (into (clojure-cli/args {:cp (glam-classpath :clojure)}))
         (into (clojure-cli/main-args {:main "glam.main"}))
         (into ["setup" "--force"]))
     {:err :string
      :out :string})
    (catch Exception e
      (throw (ex-info "Failed to install glam" {} e)))))

(defn name-and-version
  [package-name package-version]
  (if package-version
    (str package-name "@" package-version)
    package-name))

(defonce package-path-cache
  (volatile! {}))

(defn cache-set!
  [package-name-version package-path]
  (vswap! package-path-cache assoc package-name-version package-path))

(defn cache-get
  [package-name-version]
  (get @package-path-cache package-name-version))

(defn install-package-using-clojure
  [package-name]
  (try
    (let [{:keys [out]}
          (clojure-cli/process
           (-> []
               (into (clojure-cli/args {:cp (glam-classpath :clojure)}))
               (into (clojure-cli/main-args {:main "glam.main"}))
               (into ["install" package-name]))
           {:err :string
            :out :string})
          _            (println out)
          package-path (str/trim out)]
      (cache-set! package-name package-path)
      package-path)
    (catch Exception e
      (throw (ex-info "Failed to install babashka" {} e)))))

(defn install
  "Install and setuo glam to run with babashka"
  []
  (set-glam-cp!)
  (install-and-setup-glam)
  (install-package-using-clojure "org.babashka/babashka"))

(defn install-babashka
  []
  (try
    (install-package-using-clojure "org.babashka/babashka")
    (catch clojure.lang.ExceptionInfo _
      (install)
      (install-package-using-clojure "org.babashka/babashka"))))

(defn install-package
  [package-name package-version]
  (let [package-name-version (name-and-version package-name package-version)
        package-path         (or (cache-get "org.babashka/babashka")
                                 (install-babashka))
        _                    (assert package-path)
        path
        (-> (makejack/process
             [(str (path/path package-path "bb"))
              "-cp" (glam-classpath :babashka) "-m" "glam.main"
              "install" package-name-version]
             {:out :string
              :err :string})
            :out
            str/trim)]
    (cache-set! package-name-version path)
    path))

(defn package-path
  [package-name package-version]
  (let [package-name-version (name-and-version package-name package-version)]
    (or (cache-get package-name-version)
        (install-package package-name package-version))))

(defn resolve-tool
  "Resolve a tool path from the given package"
  [package-name package-version tool-name]
  (let [p (package-path package-name package-version)]
    (str (path/path p tool-name))))
