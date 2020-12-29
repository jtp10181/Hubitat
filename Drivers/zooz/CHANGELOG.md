# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- None


## [1.3.0] - 2020-12-22 (@jtp10181)
### Added
- Saving model number in deviceModel for quick access
- Code to remove params when not available for certain models
- Brightness Correction - to convert full range to set between min/max (dimmers only)

### Changed
-  Started to unify the switch and dimmer code between models

### Fixed
- Bugs with the groups associations commands
- Refresh will actually update firmware version now
- Comparison in SendEventIfNew to handle when value is a number


## [1.2.0] - 2020-12-18 (@jtp10181)
### Added
- Added Group3 Associations
- Added Fingerprint for ZEN23/24 (for ZEN21/22 drivers)


## [1.1.0] - 2020-12-14 (@jtp10181)
_New release of ZEN21/22/26 drivers, all 1.0.0 changes included_
### Added
- Parameter 7 for associations
- Parameter 20 for Smart Bulb Dimming (dimmers only)

### Fixed
- Corrected Fingerprints for Hubitat
- Cleaned up some parameter wording and ordering
- Reverted Up/Down fix per Zooz (except firmware 3.01 due to a bug)


## [1.0.0] - 2020-12-10 (@jtp10181)
_ZEN27 Only, all changes rolled into other models as added_
### Added
- SupervisionGet Event
- Parameter 17 ZWave Ramp Rate (dimmers only)
- Command to flash the light from Hubitat example driver

### Changed
- Ported from ST to HE
- Reset / synced version numbers
- Upgraded command classes when possible
- Debug and info logging to match Hubitat standards
- Moved storage of config variables to Data (in a Map)
 
### Fixed
- Some default designations to match zooz documentation
- Up/Down Scene labels which were reporting in reverse
- Scene events to user proper button numbers per Zooz docs

## 3.0 / 4.0 - 2020-09-16 (@krlaframboise / Zooz)
https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/zooz/
- Initial Release (for SmartThings)

[Unreleased]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.3.0...HEAD
[1.3.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.2.0...zooz-v1.3.0
[1.2.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.1.0...zooz-v1.2.0
[1.1.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.0.0...zooz-v1.1.0
[1.0.0]: https://github.com/jtp10181/Hubitat/releases/tag/zooz-v1.0.0
