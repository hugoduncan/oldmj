makejack, a clojure build tool invoker

**ALPHA - Not yet released**

makejack is a build tool that aims to be simpler and faster than boot
and leiningen.  Also more flexible than leiningen, and more focused than
boot.

It embraces `deps.edn` to describe dependencies.  It extends this with a
`project.edn` file for a declarative, tooling agnostic, description of the
project.  The tooling specific configuration, such as build targets,
then get added to a `mj.edn` file.

To get started, install babashka, clone the repo, and run
`bin/bootstrap`.  This will create the `target/mj1` script and the
`target/mj` binary if `GRAALVM_HOME` is set.  Either can be used
interchangeably.

In your clojure project, run `mj init` to create a `project.edn` file
and a `mj.edn` file.

See [sample.project.edn](./sample.project.edn) for project description
options.

See [sample.mk.edn](./sample.mj.edn) for makejack options.

See [DEVELOPMENT.md](docs/DEVELOPMENT.md)
