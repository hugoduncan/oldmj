(ns makejack.impl.invokers
  (:require makejack.invoke.babashka
            makejack.invoke.clojure
            makejack.invoke.shell))

(def invokers
  {:babashka #'makejack.invoke.babashka/babashka
   :clojure  #'makejack.invoke.clojure/clojure
   :shell    #'makejack.invoke.shell/shell})
