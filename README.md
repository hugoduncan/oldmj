**makejack, a clojure build tool invoker**

makejack is a build tool that aims to be simpler and faster than boot
and leiningen.  Also more flexible than leiningen, and more focused than
boot.

Think of it as a `make` for clojure.  The tooling specific
configuration, such as build targets, get added to a `mj.edn` file,
which is a little like a `Makefile`.

It embraces `deps.edn` to describe dependencies.  It extends this with a
`project.edn` file for a declarative, tooling agnostic, description of
the project.

**ALPHA - Not yet released**

**Note: currently only supports `.clj` files**

## Install

Install the `mj` binary using homebrew:

```shell
brew install hugoduncan/brew/makejack
```

## Project Initialisation

In your clojure project, run `mj init` to create `project.edn`
and `mj.edn` filesx.

## Default targets

makejack has some default targets built in.  Run `mj help` to see the
targets.


See [sample.project.edn](sample.project.edn) for project description
options.

See [sample.mk.edn](sample.mj.edn) for makejack options.

See [DEVELOPMENT.md](docs/DEVELOPMENT.md)
