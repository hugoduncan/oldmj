---
layout: docs
title: Project Initilisation
group: getting-started
---
## Initialising Your Project

To use makejack in your project, you need to create `project.edn` and
`mj.edn` files.  You can do this using makejack itself:

```shell
mj init
```

You'll need to set at least the `:version` key in the `project.edn`
file.

The created `mj.edn` just imports the default targets.  To see the
defined targets run:

```shell
mj help
```
