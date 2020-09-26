(ns makejack.invoke.no-op)

(defn no-op
  "When the :message key is specified in the target config, prints the message.

  Otherwise, does nothing.

  Useful for specifying an :undefined target to make missing targets not be an error,
  or as a no-op specification for a specific target."
  [_args target-kw config _options]
  (let [target-config (get-in config [:mj :targets target-kw])
        message       (:message target-config)]
    (when message
      (println message))))
