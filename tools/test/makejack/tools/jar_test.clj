(ns makejack.tools.jar-test
  (:require [makejack.api.filesystem :as filesystem]
            [makejack.api.path :as path]
            [makejack.tools.jar :as jar]
            [clojure.test :refer [deftest is]]))

(deftest jar-test
  (filesystem/with-temp-dir [dir "jat-test"]
    (filesystem/mkdirs (path/path dir "src"))
    (spit (str (path/path dir "src" "my.ns")) (str '(ns my)))
    (jar/depstar [] {:mj {:target-path (str dir)}
                     :project {:jar-name "my-0.1.0.jar"}} {})
    (is (filesystem/file-exists? (path/path dir "my-0.1.0.jar")))))
