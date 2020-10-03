# Changelog

Notable changes will be documented in this file. The format is based on
[Keep a Changelog](http://keepachangelog.com).

## [Unreleased]

## 0.0.1-alpha2

### Added
- bump version tool
- :jar-exclusions in project.edn
- no-op invoker
- --debug outputs invoked command line and command output
- modules invoker to invoke targets in sub-projects
- install tool for local maven repository install
- :project-root in mj.edn

### Changed
- change makejack.api.tool-options to makejack.api.tool and add
  dispatch-main function
- support clojure tools cli 1.10.1.697
- require babashka 0.2.1
- --verbose flag outputs logical description of executing target


## 0.0.1-alpha1
### Added
- initial version



[Unreleased]: https://github.com/hugoduncan/makejack/compare/v0.0.1-alpha2...HEAD
