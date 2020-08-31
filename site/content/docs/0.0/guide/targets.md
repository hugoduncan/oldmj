---
layout: docs
title: Targets
group: guide
---
Targets are invoked as the first argument to the `mj` command line
tool.  For example, to invoke the `clean` target:

```
mj clean
```

## Using Default Targets


makejack comes with several default targets/  To use these, you have to
import them into your `mj.edn` file.

```clojure
#mj {:targets
     #default-targets [:clean :pom :jar]}}
```

The targets are specified as a vector of keywords.  To import all
available targets pass the `:all` keyword, instead of the vector.

## Defining New Targets

Targets are defined in the `:targets` key of `mj.edn`. Let's look at the
definition of the `clean` target, which is a default target in makejack.

```clojure
#mj {…
 :targets
  {:clean {:doc #join ["Remove the " #ref [:target-path] " directory"]
           :invoker :shell
           :args ["rm" "-rf" #ref [:target-path]]
 …}}
```

The `:invoker` key is mandatory, and defines the way makejack invokes the
build tool.  Here we use the `:shell` invoker, which executes
the shell command specified in the `:args` key.

The `:doc` key provides a docstring that is used to describe the target.
The first line appears in the `mj help` output, and `mj help clean`
shows the full doc string.

### Referring to values in `mj.edn` and `project.edn`

Notice the use of [aero](https://github.com/juxt/aero) `#ref` tag above
to refer to other values in the `mj.edn` file.

The `project.edn` can also injected using the `#project` tag, should you
want to refernce values defined there.  For example, you could define an
`install` target like this:

```clojure
#mj {:project #project {:profile :jar}
     :targets {:install {:doc     "Install the API jar into the local repository."
                         :invoker :shell
                         :args    ["mvn" "install-file"
                                   #join ["-Dfile="
                                          #ref [:target-path]
                                          "/"
                                          #ref [:project :jar-name]]
                                   #join ["-DgroupId="
                                          #ref [:project :group-id]]
                                   #join ["-DartifactId="
                                          #ref [:project :artifact-id]]
                                   #join ["-Dversion="
                                          #ref [:project :version]]
                                   "-Dpackaging=jar"]}}}
```
