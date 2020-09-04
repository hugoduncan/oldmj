((clojure-mode
  (cider-preferred-build-tool . "clojure-cli")
  (cider-clojure-cli-parameters . "-A:dev:api-docs -m nrepl.cmdline --middleware '%s'")))
