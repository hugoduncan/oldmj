`makejack` is actively looking for contributors.

# Prerequisites

Install [babashka][babashka].

# Build the makejacl babashka script

```shell
bin/bootstrap-script
```

This will create a `target/mj-script`, which is a babashka uberscript,
and is fully functional.

If you have GraalVM install, you can use this to build the binary using
the `target/mj build` command.


# Optional

To build a makejack binary you will need GraalVM.

## Install GraalVM

Download the binaries for your platform at
https://github.com/graalvm/graalvm-ce-builds/releases.

Unpack and add it to the path:

``` bash
$ export GRAALVM_HOME=/path/to/graalvm/Contents/Home
$ export PATH=$GRAALVM_HOME/bin:$PATH
```

Install the `native-image` component:

``` bash
$ gu install native-image
```

For Mac OSX, remove quarantine on the GraalVM directory.

``` bash
sudo xattr -r -d com.apple.quarantine ${GRAALVM_HOME}/../..
```

[babashka] https://github.com/borkdude/babashka "A Clojure babushka for the grey areas of Bash."
