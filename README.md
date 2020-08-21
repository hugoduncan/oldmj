makejack, a clojure build tool invoker

**ALPHA - Not yet released**

makejack is a build tool that aims to be simpler and faster than boot
and leiningen.  Also more flexible than leiningen, and more focused than
boot.

It embraces `deps.edn` to describe dependencies.  It extends this with a
`project.edn` file for a declarative, tooling agnostic, description of the
project.  The tooling specific configuration, such as build targets,
then get added to a `mj.edn` file.

To get started, [install the graalvm native-image
builder](docs/DEVELOPMENT.md), clone the repo, and run `bin/bootstrap`.
This will create the `target/mj` binary, which you should put on your
PATH.

In your clojure project, run `mj init` to create a `project.edn` file
and a `mj.edn` file.

See [sample.project.edn](sample.project.edn) for project description
options.

See [sample.mk.edn](sample.mj.edn) for makejack options.

See [DEVELOPMENT.md](docs/DEVELOPMENT.md)
