---
layout: docs
title: Config Files
group: guide
---

makejack uses two configuration files, `project.edn` and `mj.edn`.

All the makejack specific configuration takes place in `mj.edn`, while
`project.edn` describes your project in a tool agnostic fashion.

The `project.edn` file *could* be used by other build tools, should they
decide to support it.

Both files are loaded using [aero][aero], and can use all [aero][aero]'s
default tags.


## `project.edn`

The `project.edn` file contains keys that describe your project.  The
keys are shown in [sample.project.edn]({{< relref "../reference/project-edn.md" >}}).

These keys can use [aero profiles](https://github.com/juxt/aero#profile)
to provide variants for these values.  The targets can specify which
profile to use.  makejack's default targets specify target specific
profiles, named after the target itself.

The `:aliases` key is used to specify the `deps.edn` aliases to use.


## `mj.edn`

The `mj.edn` file is used to specify build targets on the `:targets` key
, and to configure makejack.  It does not use profiles.  See
[sample.mj.edn]({{< relref "../reference/mj-edn.md" >}})

## Aero Tags Defined by Makejack

makejack defines several new [aero][aero] tags, mainly for use in `mj.edn`.

### `#mj`

This is used as the top level tag in `mj.edn`.

```clojure
#mj {}
```

It is used inject default values into the makejack configuration.  At
present it injects the `:target-path` and the `:classes-path` keys.
The provided map is merged into these default values.

```clojure
{:target-path "target"
:classes-path "target/classes"}
```

### `#project`

The `#project` tage is used to inject the `project.edn` file into
`mj.edn`.  It is passed a map with a `:profile` key, specifying which
profile to load.


### `#default-targets`

The `#default-targets` tag is used to inject makejack's default
targets. See [Targets]({{ relref "targets.md" }}).

### `#regex`

The `#regex` key can be used to define a regex from a string pattern.

### `#opt-ref`

Like [`#ref`](https://github.com/juxt/aero#ref), but evaluates to
nil if the specified path is not found.

[aero]: https://github.com/juxt/aero "Aero, a small library for explicit, intentful configuration."
