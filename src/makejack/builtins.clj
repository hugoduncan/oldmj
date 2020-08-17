(ns makejack.builtins
  (:require makejack.compile
            makejack.depstar
            makejack.graal-compile))

(def builtins
  {'makejack.compile       makejack.compile/compile
   'makejack.depstar       makejack.depstar/depstar
   'makejack.graal-compile makejack.graal-compile/graal-compile})
