# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.4] - 2021-06-08 (@jtp10181)
  ### Added
  - Full supervision support for outgoing Set and Remove commands
  - Toggle to enable/disable outbound supervision encapsulation
  - Associations update with Params Refresh command so you can sync if edited elsewhere
  ### Changed
  - Code cleanup and standardized more code across drivers

## [1.4.3] - 2021-04-21 (@jtp10181)
  ### Added
  - ZEN30 Uses new custom child driver by default, falls back to hubitat generic
  - Command to change indicator on/off settings
  - Support for ZEN73 and ZEN74
  - Support for Push, Hold, and Release commands
  ### Changed
  - Removed unnecessary capabilities
  - Renamed indicatorColor to setLED to match other Zooz drivers
  ### Fixed
  - Status Syncing... was not always updating properly

## [1.4.2] - 2021-01-31 (@jtp10181)
  ### Added
  - Command to change indicator color (can be used from Rule Machine!)
  - New method to test the params and find the ones that dont actually work
  - Command button to remove invalid parameters
  ### Changed
  - More cleanup and adding some comments
  - Consolidated parameters related commands
  - Changed ZEN30 from Multi Channel V3 to V4
  - Send events to child as parse so it can handle its own logging (ZEN30)
  - Moved Refresh Params to its own command
  ### Fixed
  - Scene reverse setting was reset to default after running configure

## [1.4.0] - 2021-01-12 (@jtp10181)
  ### Added
  - Merged some enhancements from ZEN30 back to other drivers
  - Added support for new ZEN 71/72/76/77
  - Refresh will get a full parameter report
  ### Changed
  - Scene Reverse is setting instead of hard coding into driver
  ### Fixed
  - Was running configure twice at install
  - Added initialize to the install function

## 1.3.2 - 2021-01-09 (@jtp10181) ZEN30 ONLY
  ### Added
  - Merged changes into ZEN30 ST driver and ported
  - Param number to title for easy match up to manufacturer docs
  ### Changed
  - Minor text fixes
  ### Removed
  - Flash feature was broken, use the community app

## [1.3.1] - 2020-12-29 (@jtp10181)
  ### Fixed
  - Spelling mistakes
  - Force version refresh if deviceModel is blank

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
*New release of ZEN21/22/26 drivers, all 1.0.0 changes included*
  ### Added
  - Parameter 7 for associations
  - Parameter 20 for Smart Bulb Dimming (dimmers only)
  ### Fixed
  - Corrected Fingerprints for Hubitat
  - Cleaned up some parameter wording and ordering
  - Reverted Up/Down fix per Zooz (except firmware 3.01 due to a bug)

## [1.0.0] - 2020-12-10 (@jtp10181)
*ZEN27 Only, all changes rolled into other models as added*
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

[Unreleased]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.4.4...HEAD
[1.4.4]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.4.3...zooz-v1.4.4
[1.4.3]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.4.2...zooz-v1.4.3
[1.4.2]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.4.0...zooz-v1.4.2
[1.4.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.3.1...zooz-v1.4.0
[1.3.1]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.3.0...zooz-v1.3.1
[1.3.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.2.0...zooz-v1.3.0
[1.2.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.1.0...zooz-v1.2.0
[1.1.0]: https://github.com/jtp10181/Hubitat/compare/zooz-v1.0.0...zooz-v1.1.0
[1.0.0]: https://github.com/jtp10181/Hubitat/releases/tag/zooz-v1.0.0
