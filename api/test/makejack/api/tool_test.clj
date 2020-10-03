(ns makejack.api.tool-test
  (:require [makejack.api.core :as makejack]
            [makejack.api.tool :as tool]
            [clojure.test :refer [deftest is testing]]))

(deftest dispatch-main-test
  (let [options  (volatile! nil)
        args     (volatile! nil)
        config   (volatile! nil)
        bindings (volatile! nil)
        tool-fn  (fn [options_ args_ config_]
                   (vreset! options options_)
                   (vreset! args args_)
                   (vreset! config config_)
                   (vreset! bindings {:verbose makejack/*verbose*
                                      :debug   makejack/*debug*}))]
    (testing "dispatch-main"
      (testing "parses cli-options"
        (tool/dispatch-main "" "" tool-fn [] ["--profile" "aprofile"])
        (is (= 'aprofile (:profile @options))))
      (testing "exits on unrecognised options"
        (let [error (volatile! nil)]
          (with-redefs [makejack/error (fn [message] (vreset! error message))]
            (tool/dispatch-main "" "" tool-fn [] ["--xyz"])
            (is @error))))
      (testing "forwards un-parsed options and arguments"
        (tool/dispatch-main "" "" tool-fn [] ["--profile" "p" "else" "--something"])
        (is (= ["else" "--something"] @args)))
      (testing "loads the mj.edn file in config"
        (tool/dispatch-main "" "" tool-fn [] [])
        (is (= "target" (-> @config :mj :target-path))))
      (testing "loads the project.edn file in config"
        (tool/dispatch-main "" "" tool-fn [] [])
        (is (= "makejack.api" (-> @config :project :name))))
      (testing "binds *verbose* and *debug* according to --verbose and --debug options"
        (tool/dispatch-main "" "" tool-fn [] [])
        (is (= {:verbose nil :debug nil} @bindings))
        (tool/dispatch-main "" "" tool-fn [] ["--verbose"])
        (is (= {:verbose true :debug nil} @bindings))
        (tool/dispatch-main "" "" tool-fn [] ["--debug"])
        (is (= {:verbose nil :debug true} @bindings))
        (tool/dispatch-main "" "" tool-fn [] ["--debug" "--verbose"])
        (is (= {:verbose true :debug true} @bindings))))))
