(ns makejack.impl.invokers
  (:require makejack.invoke.babashka
            makejack.invoke.clojure
            makejack.invoke.glam
            makejack.invoke.no-op
            makejack.invoke.shell))

(def invokers
  {:babashka #'makejack.invoke.babashka/babashka
   :clojure  #'makejack.invoke.clojure/clojure
   :glam     #'makejack.invoke.glam/glam
   :no-op    #'makejack.invoke.no-op/no-op
   :shell    #'makejack.invoke.shell/shell})
