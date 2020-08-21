(ns makejack.api.builtins
  (:require makejack.babashka
            makejack.clojure
            makejack.compile
            makejack.depstar
            makejack.graal-compile
            makejack.init
            makejack.javac
            makejack.pom
            makejack.shell
            makejack.uberscript))

(def builtins
  {'makejack.babashka      #'makejack.babashka/babashka
   'makejack.clojure       #'makejack.clojure/clojure
   'makejack.compile       #'makejack.compile/compile
   'makejack.depstar       #'makejack.depstar/depstar
   'makejack.graal-compile #'makejack.graal-compile/graal-compile
   'makejack.init          #'makejack.init/init
   'makejack.javac         #'makejack.javac/javac
   'makejack.pom           #'makejack.pom/pom
   'makejack.shell         #'makejack.shell/shell
   'makejack.uberscript    #'makejack.uberscript/uberscript})
