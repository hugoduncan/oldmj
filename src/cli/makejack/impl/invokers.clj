(ns makejack.impl.invokers
  (:require makejack.invoke.babashka
            makejack.invoke.clojure
            makejack.invoke.no-op
            makejack.invoke.shell))

(def invokers
  {:babashka #'makejack.invoke.babashka/babashka
   :clojure  #'makejack.invoke.clojure/clojure
   :no-op    #'makejack.invoke.no-op/no-op
   :shell    #'makejack.invoke.shell/shell})
