`makejack` is actively looking for contributors.

# Prerequisites

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
