(ns makejack.api.builtins
  (:require makejack.babashka
            makejack.clojure
            makejack.init
            makejack.shell
            makejack.compile
            makejack.depstar
            makejack.graal-compile
            makejack.pom))

(def builtins
  {'makejack.compile       makejack.compile/compile
   'makejack.depstar       makejack.depstar/depstar
   'makejack.graal-compile makejack.graal-compile/graal-compile})
