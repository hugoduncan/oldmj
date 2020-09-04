((clojure-mode
  (cider-preferred-build-tool . "clojure-cli")
  (cider-clojure-cli-parameters . "-A:dev:api-docs:test -m nrepl.cmdline --middleware '%s'")))
