Targets are invoked as the first argument to the `mj` command line
tool.  For example, to invoke the `clean` target:

```
mj clean
```

Targets are defined in the `:targets` key of `mj.edn`. Let's look at the
definition of the `clean` target, which is a default target in makejack.

```clojure
{…
 :targets
  {:clean {:doc #join ["Remove the " #ref [:target-path] " directory"]
           :type :shell
           :args ["rm" "-rf" #ref [:target-path]]
 …}}
```

The `:type` key is mandatory, and defines the way makejack invokes the
build tool.  Here we will be invoking the `:shell` tool, which executes
the shell command specified in the `:args` key.

The `:doc` key provides a docstring that is used to describe the target
in the `mj help` anf `mj help clean` output.

Notice the use of [aero](https://github.com/juxt/aero) tags to refer to
other values in the `mj.edn` file. The `project.edn` is also injected
and made available on the `:project` key.
