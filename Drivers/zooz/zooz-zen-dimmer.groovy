/*
 *  Zooz ZEN Dimmers Universal
 *    - Model: ZEN22, ZEN24 - MINIMUM FIRMWARE 3.07
 *    - Model: ZEN27 - MINIMUM FIRMWARE 2.08
 *    - Model: ZEN72, ZEN74, ZEN77 - All Firmware
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-zen-switches/58649
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.6.4] - 2022-12-13 (@jtp10181)
  ### Added
  - Command to set any parameter (can be used in RM)

## [1.6.3] - 2022-11-22 (@jtp10181)
  ### Changed
  - Enabled parameter 7 for ZEN72 on new firmware
  - Set Level Duration supports up to 254s for 2x and 7,620s (127 mins) for 7x
  ### Fixed
  - Convert signed parameter values to unsigned
  - Fixed error when using buttons from dashboard

## [1.6.2] - 2022-08-11 (@jtp10181)
  ### Added
  - Flash capability / command
  ### Fixed
  - Race condition with configVals (now keeping copy in static var)
  - Reworked multi level commands to use last level and device duration instead of overriding it
  - Various fixes in common functions merged from other drivers
  - Removed some unnecessary code

## [1.6.0] - 2022-07-24 (@jtp10181)
  ### Changed
  - Major refactor to organize code same as sensor drivers
  - Cleaned up some logging
  ### Fixed
  - sceneReverse was using wrong firmware variable name
  - Was possible for hidden parameters to get stuck in the wrong setting
  ### Removed
  - Parameter test/hide code totally removed

## [1.5.3] - 2022-07-13 (@jtp10181)
  ### Changed
  - Extra check to make sure devModel is set
  - Minor refactoring

## [1.5.2] - 2022-07-10 (@jtp10181)
  ### Added
  - Support for multiple hardware/firmware major versions
  - Support for params 27-30 for ZEN72/74/77
  - Support for Association Group 4 (only works on some models)
  - Set deviceModel in device data (press refresh)
  ### Changed
  - Removed getParam.value and replaced with separate function
  - Adding HTML styling to the Preferences
  ### Fixed
  - Some parameters would get multiple [DEFAULT] tags added
  ### Deprecated
  - Parameter test/hide functions, not needed

## [1.5.1] - 2022-04-25 (@jtp10181)
  ### Changed
  - Description text loging enabled by default
  ### Fixed
  - Added inClusters to fingerprint so it will be selected by default
  - threeWaySwitchType options corrected between switches and dimmers
  - Parameter #7 removed from ZEN71/72 which do not have that option
  - Global (Field static) Maps defined explicitly as a ConcurrentHashMap

## [1.5.0] - 2021-11-24 (@jtp10181)
  ### Added
  - ChangeLevel capability support so level can be adjusted from button holds (Dimmers Only)
  - Warning if driver is loaded on unsupported device (including wrong type)
  ### Changed
  - Total overhaul of parameters code to make it easier to maintain
  - Changed switchMultiLevel class down to V2 as no V3 features are needed (Dimmers Only)
  ### Fixed
  - The internal lastActivityDate will only update when the device responds to the hub

## [1.4.4] - 2021-06-08 (@jtp10181)
  ### Added
  - Full supervision support for outgoing Set and Remove commands
  - Toggle to enable/disable outbound supervision encapsulation
  - Associations update with Params Refresh command so you can sync if edited elsewhere
  ### Changed
  - Code cleanup and standardized more code across drivers

## [1.4.3] - 2021-04-21 (@jtp10181)
  ### Added
  - ZEN30 Uses new custom child driver by default, falls back to Hubitat generic
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
  - New method to test the params and find the ones that don't actually work
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
  - Some default designations to match Zooz documentation
  - Up/Down Scene labels which were reporting in reverse
  - Scene events to user proper button numbers per Zooz docs

NOTICE: This file has been modified by *Jeff Page* under compliance with
	the Apache 2.0 License from the original work of *Zooz*.

Below link and changes are for original source (Kevin LaFramboise @krlaframboise)
https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/zooz/

## 3.0 / 4.0 - 2020-09-16 (@krlaframboise / Zooz)
  - Initial Release (for SmartThings)

 *
 *  Copyright 2020-2022 Jeff Page
 *  Copyright 2020 Zooz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
*/

import groovy.transform.Field

@Field static final String VERSION = "1.6.4"
@Field static final Map deviceModelNames =
	["B112:1F1C":"ZEN22", "B112:261C":"ZEN24", "A000:A002":"ZEN27",
	"7000:A002":"ZEN72", "7000:A004":"ZEN74", "7000:A007":"ZEN77"]

metadata {
	definition (
		name: "Zooz ZEN Dimmer Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen-dimmer.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "SwitchLevel"
		capability "ChangeLevel"
		capability "Configuration"
		capability "Refresh"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"
		//capability "DoubleTapableButton"
		capability "Flash"

		//Modified from default to add duration argument
		command "startLevelChange", [
			[name:"Direction*", description:"Direction for level change request", type: "ENUM", constraints: ["up","down"]],
			[name:"Duration", type:"NUMBER", description:"Transition duration in seconds"] ]

		command "refreshParams"
		command "setLED", [
			[name:"Select Color*", description:"Works ONLY on ZEN7x Series!", type: "ENUM", constraints: ledColorOptions] ]
		command "setLEDMode", [
			[name:"Select Mode*", description:"This Sets Preference (#2)*", type: "ENUM", constraints: ledModeCmdOptions] ]
		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "assocDNI4", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"B112", deviceId:"1F1C", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN22 Dimmer
		fingerprint mfr:"027A", prod:"B112", deviceId:"261C", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN24 Dimmer
		fingerprint mfr:"027A", prod:"A000", deviceId:"A002", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN27 S2 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A002", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN72 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A004", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN74 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A007", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN77 S2 Dimmer
	}

	preferences {
		configParams.each { param ->
			if (!param.hidden) {
				Integer paramVal = getParamValue(param)
				if (param.options) {
					input "configParam${param.num}", "enum",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Selected: ${paramVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: paramVal,
						options: param.options,
						required: false
				}
			}
		}

		for(int i in 2..maxAssocGroups) {
			input "assocDNI$i", "string",
				title: fmtTitle("Device Associations - Group $i"),
				description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear."),
				required: false
		}

		input "supervisionGetEncap", "bool",
			title: fmtTitle("Supervision Encapsulation") + "<em> (Experimental)</em>",
			description: fmtDesc("This can increase reliability when the device is paired with security, but may not work correctly on all models."),
			defaultValue: false

		input "levelCorrection", "bool",
			title: fmtTitle("Brightness Correction"),
			description: fmtDesc("Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%"),
			defaultValue: false

		input "sceneReverse", "bool",
			title: fmtTitle("Scene Up-Down Reversal"),
			description: fmtDesc("If the button numbers and up/down descriptions are backwards in the scene button events change this setting to fix it!"),
			defaultValue: false

		//Logging options similar to other Hubitat drivers
		input "txtEnable", "bool", title: fmtTitle("Enable Description Text Logging?"), defaultValue: true
		input "debugEnable", "bool", title: fmtTitle("Enable Debug Logging?"), defaultValue: true
	}
}

//Preference Helpers
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}

void debugShowVars() {
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	log.warn "settings ${settings.hashCode()} ${settings}"
}

//Association Settings
@Field static final int maxAssocGroups = 4
@Field static final int maxAssocNodes = 5

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	paddleControl: [ num: 1,
		title: "Paddle Orientation",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reverse", 2:"Toggle Mode"],
	],
	ledMode: [ num: 2,
		title: "LED Indicator",
		size: 1, defaultVal: 0,
		options: [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"],
		changes: [23:[num:null], 24:[num:null], 73:[defaultVal:2], 74:[defaultVal:2]]
	],
	ledColor: [ num: 23,
		title: "LED Indicator Color",
		size: 1, defaultVal: 1,
		options: [:], //ledColorOptions
		changes: ['2X':[num:null]]
	],
	ledBrightness: [ num: 24,
		title: "LED Indicator Brightness",
		size: 1, defaultVal: 1,
		options: [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"],
		changes: ['2X':[num:null]]
	],
	autoOffEnabled: [ num: 3,
		title: "Auto Turn-Off Timer Enabled",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
		hidden: true,
		changes: ['7X':[num:null]]
	],
	autoOffInterval: [ num: 4,
		title: "Auto Turn-Off Timer",
		size: 4, defaultVal: 0,
		options: [:], //autoOnOffIntervalOptions
		changes: ['7X':[num:3]]
	],
	autoOnEnabled: [ num: 5,
		title: "Auto Turn-On Timer Enabled",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
		hidden: true,
		changes: ['7X':[num:null]]
	],
	autoOnInterval: [ num: 6,
		title: "Auto Turn-On Timer",
		size: 4, defaultVal: 0,
		options: [:], //autoOnOffIntervalOptions
		changes: ['7X':[num:5]]
	],
	powerFailure: [ num: 8,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 2,
		options: [2:"Restores Last Status", 0:"Forced to Off", 1:"Forced to On"],
	],
	rampRate: [ num: 9,
		title: "Ramp Rate to Full ON",
		description: "If no separate OFF setting is shown this controls both",
		size: 1, defaultVal: 1,
		options: [0:"Instant On/Off"], //rampRateOptions
	],
	rampRateOff: [ num: 27,
		title: "Ramp Rate to Full OFF",
		size: 1, defaultVal: 2,
		options: [0:"Instant Off"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: [22:[num: null], 24:[num: null], 27:[num: null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	holdRampRate: [ num: 16,
		title: "Dimming Speed when Paddle is Held",
		size: 1, defaultVal: 5,
		options: [:], //rampRateOptions
	],
	zwaveRampRate: [ num: 17,
		title: "Z-Wave Ramp Rate (Dimming Speed)",
		size: 1, defaultVal: 1,
		options: [ 1:"Z-Wave Can Set Ramp Rate", 0:"Match Physical Ramp Rate"],
		changes: ['7X':[num:null]]
	],
	minimumBrightness: [ num: 10,
		title: "Minimum Brightness",
		size: 1, defaultVal: 1,
		options: [:], //brightnessOptions
	],
	maximumBrightness: [ num: 11,
		title: "Maximum Brightness",
		size: 1, defaultVal: 99,
		options: [:], //brightnessOptions
	],
	doubleTapBrightness: [ num: 12,
		title: "Double Tap Up Brightness",
		size: 1, defaultVal: 0,
		options: [0:"Full Brightness (100%)", 1:"Maximum Brightness Parameter"],
		changes: ['7X':[options:[0:"Full Brightness (100%)", 1:"Custom Brightness Parameter", 2:"Maximum Brightness Parameter", 3:"Disabled"]]],
	],
	doubleTapFunction: [ num: 14,
		title: "Double Tap Up Function",
		size: 1, defaultVal: 0,
		options: [0:"Full/Maximum Brightness", 1:"Disabled, Single Tap Last Brightness (or Custom)", 2:"Disabled, Single Tap Full/Maximum Brightness"],
		changes: ['7X':[num:null]]
	],
	singleTapUp: [ num: null,
		title: "Single Tap Up Brightness",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level", 1:"Custom Brightness Parameter", 2:"Maximum Brightness Parameter", 3:"Full Brightness (100%)"],
		changes: ['7X':[num:25]]
	],
	customBrightness: [ num: 18,
		title: "Custom Brightness when Turned On",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level"], //brightnessOptions
	],
	nightLight: [ num: 22,
		title: "Night Light Mode Brightness",
		size: 1, defaultVal: 20,
		options: [0:"Disabled"], //brightnessOptions
	],
	//sceneControl - Dimmers=13, ZEN26/73/76=10, Other Switches=9
	sceneControl: [ num: 13,
		title: "Scene Control Events",
		description: "Enable to get push and multi-tap events",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
	],
	// sceneControl3w: [ num: null,
	// 	title: "Scene Control Events From 3-Way",
	// 	description: "Enable to get push and multi-tap events from a mechanical switch connected in a direct 3-way",
	// 	size: 1, defaultVal: 0,
	// 	options: [0:"Disabled", 1:"Enabled"],
	// 	changes: [
	// 		72:[num:31, firmVer:2.10, firmVerM:[10:30]],
	// 		77:[num:31, firmVer:3.10, firmVerM:[10:30, 2:20]]
	// 	]
	// ],
	//loadControlParam - Dimmers=15, ZEN73/76=12, Other Switches=11
	loadControl: [ num: 15,
		title: "Smart Bulb Mode - Load Control",
		size: 1, defaultVal: 1,
		options: [1:"Enable Paddle and Z-Wave", 0:"Disable Paddle Control", 2:"Disable Paddle and Z-Wave Control"],
	],
	smartBulbBehavior: [ num: 21, //relayBehaviorParam
		title: "Smart Bulb - On/Off when Paddle Disabled",
		size: 1, defaultVal: 0,
		options: [0:"Reports Status & Changes LED", 1:"Doesn't Report Status or Change LED"],
	],
	smartBulbDimming: [ num: 20, //relayDimmingParam
		title: "Smart Bulb - Dimming when Paddle Disabled",
		size: 1, defaultVal: 0,
		options: [0:"Report Each Brightness Level", 1:"Report Only Final Brightness Level"],
	],
	//threeWaySwitchType - ZEN21/22/23/24/71/72 Only
	threeWaySwitchType: [num: null, // (19)
		title: "3-Way Switch Type",
		size: 1, defaultVal: 0,
		options: [0:"Toggle On/Off Switch", 1:"Toggle On/Off with 2x/3x Shortcuts", 2:"Momentary Switch", 3:"Momentary with 2x/Hold Shortcuts"],
		changes: [22:[num: 19],24:[num: 19],72:[num: 19]]
	],
	paddleProgramming: [ num: null,
		title: "Programming from the Paddle",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		changes: [21:[num: null, firmVer:null], 23:[num: 15, firmVer:4.04], 26:[num: 15, firmVer:3.41],
			22:[num: 24, firmVer:4.04], 24:[num: 24, firmVer:4.04], 27:[num: 24, firmVer:3.04],
			71:[num: 17, firmVer:2.0], 73:[num: 17, firmVer:2.0], 76:[num: 17, firmVer:2.0],
			72:[num: 26, firmVer:2.0], 74:[num: 26, firmVer:2.0], 77:[num: 26, firmVer:2.0]
		],
	],
	zwaveRampRateOn: [ num: 28,
		title: "Z-Wave Ramp Rate to Full ON",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant On"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: [22:[num: null], 24:[num: null], 27:[num: null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	zwaveRampRateOff: [ num: 29,
		title: "Z-Wave Ramp Rate to Full OFF",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant Off"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: [22:[num: null], 24:[num: null], 27:[num: null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	remoteZWaveDim: [ num: 30,
		title: "Remote Z-Wave Dimming Duration",
		description: "Dimming Speed for devices directly associated in Groups 3/4",
		size: 1, defaultVal: 5,
		options: [:], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: [22:[num: null], 24:[num: null], 27:[num: null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	associationReports: [ num: 7,
		title: "Send Status Report to Associations",
		size: 1, defaultVal: 15,
		options: [ 0:"None", 1:"Physical Tap On ZEN Only", 2:"Physical Tap On Connected 3-Way Switch Only", 3:"Physical Tap On ZEN / 3-Way Switch",
			4:"Z-Wave Command From Hub", 5:"Physical Tap On ZEN / Z-Wave Command", 6:"Physical Tap On 3-Way Switch / Z-Wave Command",
			7:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command", 8:"Timer Only", 9:"Physical Tap On ZEN / Timer",
			10:"Physical Tap On 3-Way Switch / Timer", 11:"Physical Tap On ZEN / 3-Way Switch / Timer", 12:"Z-Wave Command From Hub / Timer",
			13:"Physical Tap On ZEN / Z-Wave Command / Timer", 14:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command / Timer",
			15:"All Of The Above"
		],
		changes: [71:[num:null], 72:[firmVer:2.10, firmVerM:[10:30]]]
	],
	sceneMapping: [ num: null,
		title: "Central Scene Mapping",
		size: 1, defaultVal: 0,
		options: [0:"Up is Scene 2, Down is Scene 1", 1:"Up is Scene 1, Down is Scene 2"],
		hidden: true,
		changes: [21:[num: null, firmVer:null], 23:[num: 14, firmVer:4.04], 26:[num: 14, firmVer:3.41],
			22:[num: 23, firmVer:4.04], 24:[num: 23, firmVer:4.04], 27:[num: 23, firmVer:3.04],
		],
	],
]

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x26: 2,	// Switch Multilevel (switchmultilevelv2) (4)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 1,	// Configuration (configurationv1)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x85: 2,	// Association (associationv2)
	0x86: 2,	// Version (versionv2)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
]

/*** Static Lists and Settings ***/
@Field static final Map ledModeCmdOptions = [0:"Default", 1:"Reverse", 2:"Off", 3:"On"]
@Field static final Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
	initialize()
}

void initialize() {
	logWarn "initialize..."
	refresh()
}

void configure() {
	logWarn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		state.resyncAll = true
	}

	updateSyncingStatus(6)
	runIn(1, executeRefreshCmds)
	runIn(4, executeConfigureCmds)
}

void updated() {
	logDebug "updated..."
	logDebug "Debug logging is: ${debugEnable == true}"
	logDebug "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	runIn(1, executeConfigureCmds)
}

void refresh() {
	logDebug "refresh..."
	executeRefreshCmds()
}


/*******************************************************************
 ***** Driver Commands
********************************************************************/
/*** Capabilities ***/
String on() {
	logDebug "on..."
	flashStop()
	return getOnOffCmds(0xFF)
}

String off() {
	logDebug "off..."
	flashStop()
	return getOnOffCmds(0x00)
}

String setLevel(Number level, Number duration=null) {
	logDebug "setLevel($level, $duration)..."
	return getSetLevelCmds(level, duration)
}

List<String> startLevelChange(direction, duration=null) {
	Boolean upDown = (direction == "down") ? true : false
	Integer durationVal = validateRange(duration, getParamValue("holdRampRate"), 0, 127)
	logDebug "startLevelChange($direction) for ${durationVal}s"

	List<String> cmds = [switchMultilevelStartLvChCmd(upDown, durationVal)]

	//Hack for devices that don't implement the duration correctly
	// Map rampRateParam = getParam("rampRate")
	// String devModel = state.deviceModel
	// BigDecimal firmware = firmwareVersion
	// if (firmware <= 2.0) {
	// 	cmds.add( 0, configSetCmd(rampRateParam, durationVal) )
	// 	cmds.add( configSetCmd(rampRateParam, getParamValue(rampRateParam)) )
	// }

	return delayBetween(cmds, 1000)
}

String stopLevelChange() {
	logDebug "stopLevelChange()"
	return switchMultilevelStopLvChCmd()
}

//Button commands required with capabilities
void push(buttonId) { sendBasicButtonEvent(buttonId, "pushed") }
void hold(buttonId) { sendBasicButtonEvent(buttonId, "held") }
void release(buttonId) { sendBasicButtonEvent(buttonId, "released") }
void doubleTap(buttonId) { sendBasicButtonEvent(buttonId, "doubleTapped") }

//Flashing Capability
void flash(Number rateToFlash = 1500) {
	logInfo "Flashing started with rate of ${rateToFlash}ms"

	//Min rate of 1 sec, max of 30, max run time of 5 minutes
	rateToFlash = validateRange(rateToFlash, 1500, 1000, 30000)
	Integer maxRun = validateRange((rateToFlash*30)/1000, 30, 30, 300)
	state.flashNext = device.currentValue("switch")

	//Start the flashing
	runIn(maxRun,flashStop,[data:true])
	flashHandler(rateToFlash)
}

void flashStop(Boolean turnOn = false) {
	if (state.flashNext != null) {
		logInfo "Flashing stopped..."
		unschedule("flashHandler")
		state.remove("flashNext")
		if (turnOn) { runIn(1,on) }
	}
}

void flashHandler(Integer rateToFlash) {
	if (state.flashNext == "on") {
		logDebug "Flash On"
		state.flashNext = "off"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getSetLevelCmds(0xFF, 0))
	}
	else if (state.flashNext == "off") {
		logDebug "Flash Off"
		state.flashNext = "on"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getSetLevelCmds(0x00, 0))
	}
}


/*** Custom Commands ***/
void setLED(String colorName) {
	Map param = getParam("ledColor")

	if (param?.num && state.deviceModel ==~ /ZEN7\d/) {
		Short paramVal = ledColorOptions.find{ colorName.equalsIgnoreCase(it.value) }.key
		logDebug "Indicator Color Value [${colorName} : ${paramVal}]"
		//Set the Preference to match new setting, then send command to device
		device.updateSetting("configParam${param.num}",[value:"${paramVal}", type:"enum"])
		sendCommands(configSetGetCmd(param, paramVal))
	}
	else {
		logWarn "Indicator Color can only be changed on ZEN7x models"
	}
}

void setLEDMode(String modeName) {
	Map param = getParam("ledMode")

	if (param?.num) {
		Short paramVal = ledModeCmdOptions.find{ modeName.equalsIgnoreCase(it.value) }.key
		logDebug "Indicator Value [${modeName} : ${paramVal}]"
		//Set the Preference to match new setting, then send command to device
		device.updateSetting("configParam${param.num}",[value:"${paramVal}", type:"enum"])
		sendCommands(configSetGetCmd(param, paramVal))
	}
	else {
		logWarn "There is No LED Indicator Parameter Found for this model"
	}
}

void refreshParams() {
	List<String> cmds = []
	for (int i = 1; i <= maxAssocGroups; i++) {
		cmds << associationGetCmd(i)
	}

	configParams.each { param ->
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

String setParameter(paramNum, value, size = null) {
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name}]" : "")
	return secureCmd(configSetCmd([num: paramNum, size: size], value as Integer))
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)

	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else {
		logWarn "Unable to parse: ${description}"
	}

	//Update Last Activity
	updateLastCheckIn()
	sendEvent(name:"numberOfButtons", value:10)
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Multichannel Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, ep)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep))
}

//Handles reports back from Supervision Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionReport cmd, ep=0 ) {
	logDebug "Supervision Report - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
	if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }

	switch (cmd.status as Integer) {
		case 0x00: // "No Support"
		case 0x01: // "Working"
		case 0x02: // "Failed"
			logWarn "Supervision NOT Successful - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
			break
		case 0xFF: // "Success"
			supervisedPackets["${device.id}"].remove(cmd.sessionID)
			break
	}
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String fullVersion = String.format("%d.%02d",cmd.firmware0Version,cmd.firmware0SubVersion)
	device.updateDataValue("firmwareVersion", fullVersion)

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		Long sizeFactor = Math.pow(256,param.size).round()
		if (val < 0) { val += sizeFactor }

		logDebug "${param.name} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
	}
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Integer grp = cmd.groupingIdentifier

	if (grp == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId}"
		state.group1Assoc = (cmd.nodeId == [zwaveHubNodeId]) ? true : false
	}
	else if (grp > 1 && grp <= maxAssocGroups) {
		logDebug "Group $grp Association: ${cmd.nodeId}"

		if (cmd.nodeId.size() > 0) {
			state["assocNodes$grp"] = cmd.nodeId
		} else {
			state.remove("assocNodes$grp".toString())
		}

		String dnis = convertIntListToHexList(cmd.nodeId)?.join(", ")
		sendEventLog(name:"assocDNI$grp", value:(dnis ?: "none"))
		device.updateSetting("assocDNI$grp", [value:"${dnis}", type:"string"])
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	flashStop() //Stop flashing if its running
	sendSwitchEvents(cmd.value, "physical", ep)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "digital", ep)
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "digital", ep)
}

void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep=0){
	if (state.lastSequenceNumber != cmd.sequenceNumber) {
		state.lastSequenceNumber = cmd.sequenceNumber

		logTrace "${cmd} (ep ${ep})"

		//Flip the sceneNumber if needed (per parameter setting)
		if (settings.sceneReverse) {
			if (cmd.sceneNumber == 1) cmd.sceneNumber = 2
			else if (cmd.sceneNumber == 2) cmd.sceneNumber = 1
			logTrace "Scene Reversed: ${cmd}"
		}

		Map scene = [name: "pushed", value: cmd.sceneNumber, desc: "", type:"physical", isStateChange:true]
		String actionType
		String btnVal

		switch (cmd.sceneNumber) {
			case 1:
				actionType = "up"
				break
			case 2:
				actionType = "down"
				break
			default:
				logDebug "Unknown sceneNumber: ${cmd}"
		}

		switch (cmd.keyAttributes){
			case 0:
				btnVal = "${actionType} 1x"
				break
			case 1:
				scene.name = "released"
				btnVal = "${actionType} released"
				break
			case 2:
				scene.name = "held"
				btnVal = "${actionType} held"
				break
			case {it >=3 && it <= 6}:
				if      (cmd.sceneNumber == 1) scene.value = (cmd.keyAttributes * 2) - 3
				else if (cmd.sceneNumber == 2) scene.value = (cmd.keyAttributes * 2) - 2
				btnVal = "${actionType} ${cmd.keyAttributes - 1}x"
				break
			default:
				logDebug "Unknown keyAttributes: ${cmd}"
		}

		if (actionType && btnVal) {
			scene.desc = "button ${scene.value} ${scene.name} [${btnVal}]"
			sendEventLog(scene)
		}
	}
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


/*******************************************************************
 ***** Z-Wave Command Shortcuts
********************************************************************/
//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=200) {
	//Calculate supervisionCheck delay based on how many commands
	Integer packetsCount = supervisedPackets?."${device.id}"?.size()
	if (packetsCount > 0) {
		Integer delayTotal = (cmds.size() * delay) + 2000
		logDebug "Setting supervisionCheck to ${delayTotal}ms | ${packetsCount} | ${cmds.size()} | ${delay}"
		runInMillis(delayTotal, supervisionCheck, [data:1])
	}

	//Send the commands
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

//Single Command
void sendCommands(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Consolidated zwave command functions so other code is easier to read
String associationSetCmd(Integer group, List<Integer> nodes) {
	return supervisionEncap(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return supervisionEncap(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return supervisionEncap(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return supervisionEncap(zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return supervisionEncap(zwave.switchMultilevelV2.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return supervisionEncap(zwave.switchMultilevelV2.switchMultilevelStopLevelChange(), ep)
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	Long sizeFactor = Math.pow(256,param.size).round()
	if (value >= sizeFactor/2) { value -= sizeFactor }

	return supervisionEncap(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param.num))
}

List configSetGetCmd(Map param, Integer value) {
	List<String> cmds = []
	cmds << configSetCmd(param, value)
	cmds << configGetCmd(param)
	return cmds
}


/*******************************************************************
 ***** Z-Wave Encapsulation
********************************************************************/
//Secure and MultiChannel Encapsulate
String secureCmd(String cmd) {
	return zwaveSecureEncap(cmd)
}
String secureCmd(hubitat.zwave.Command cmd, ep=0) {
	return zwaveSecureEncap(multiChannelEncap(cmd, ep))
}

//MultiChannel Encapsulate if needed
//This is called from secureCmd or supervisionEncap, do not call directly
String multiChannelEncap(hubitat.zwave.Command cmd, ep) {
	//logTrace "multiChannelEncap: ${cmd} (ep ${ep})"
	if (ep > 0) {
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}

//====== Supervision Encapsulate START ======\\
@Field static Map<String, Map<Short, String>> supervisedPackets = new java.util.concurrent.ConcurrentHashMap()
@Field static Map<String, Short> sessionIDs = new java.util.concurrent.ConcurrentHashMap()

String supervisionEncap(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "supervisionEncap: ${cmd} (ep ${ep})"

	if (settings.supervisionGetEncap) {
		//Encap with SupervisionGet
		Short sessId = getSessionId()
		def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sessId).encapsulate(cmd)

		//Encap with MultiChannel now so it is cached that way below
		cmdEncap = multiChannelEncap(cmdEncap, ep)

		logDebug "New Supervised Packet for Session: ${sessId}"
		if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }
		supervisedPackets["${device.id}"][sessId] = cmdEncap

		//Calculate supervisionCheck delay based on how many cached packets
		Integer packetsCount = supervisedPackets?."${device.id}"?.size()
		Integer delayTotal = (packetsCount * 500) + 2000
		runInMillis(delayTotal, supervisionCheck, [data:1])

		//Send back secured command
		return secureCmd(cmdEncap)
	}
	else {
		//If supervision disabled just multichannel and secure
		return secureCmd(cmd, ep)
	}
}

Short getSessionId() {
	Short sessId = sessionIDs["${device.id}"] ?: state.lastSupervision ?: 0
	sessId = (sessId + 1) % 64  // Will always will return between 0-63
	state.lastSupervision = sessId
	sessionIDs["${device.id}"] = sessId

	return sessId
}

void supervisionCheck(Integer num) {
	Integer packetsCount = supervisedPackets?."${device.id}"?.size()
	logDebug "Supervision Check #${num} - Packet Count: ${packetsCount}"

	if (packetsCount > 0 ) {
		List<String> cmds = []
		supervisedPackets["${device.id}"].each { sid, cmd ->
			logWarn "Re-Sending Supervised Session: ${sid} (Retry #${num})"
			cmds << secureCmd(cmd)
		}
		sendCommands(cmds)

		if (num >= 3) { //Clear after this many attempts
			logWarn "Supervision MAX RETIES (${num}) Reached"
			supervisedPackets["${device.id}"].clear()
		}
		else { //Otherwise keep trying
			Integer delayTotal = (packetsCount * 500) + 2000
			runInMillis(delayTotal, supervisionCheck, [data:num+1])
		}
	}
}
//====== Supervision Encapsulate END ======\\


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	List<String> cmds = []

	if (!firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getParamValue(param, true)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	if (state.resyncAll) clearVariables()
	state.resyncAll = false

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds << switchMultilevelGetCmd()

	sendCommands(cmds)
}

void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel

	//Clears State Variables
	state.clear()

	//Clear Config Data
	configsList["${device.id}"] = [:]
	device.removeDataValue("configVals")
	//Clear Data from other Drivers
	device.removeDataValue("protocolVersion")
	device.removeDataValue("hardwareVersion")
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
}

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
		if (!device.currentValue("assocDNI$i")) {
			sendEventLog(name:"assocDNI$i", value:"none")
		}

		List<String> cmdsEach = []
		List settingNodeIds = getAssocDNIsSettingNodeIds(i)

		//Need to remove first then add in case we are at limit
		List oldNodeIds = state."assocNodes$i"?.findAll { !(it in settingNodeIds) }
		if (oldNodeIds) {
			logDebug "Removing Nodes: Group $i - $oldNodeIds"
			cmdsEach << associationRemoveCmd(i, oldNodeIds)
		}

		List newNodeIds = settingNodeIds.findAll { !(it in state."assocNodes$i") }
		if (newNodeIds) {
			logDebug "Adding Nodes: Group $i - $newNodeIds"
			cmdsEach << associationSetCmd(i, newNodeIds)
		}

		if (cmdsEach || state.resyncAll) {
			cmdsEach << associationGetCmd(i)
			cmds += cmdsEach
		}
	}

	return cmds
}

List getAssocDNIsSettingNodeIds(grp) {
	def dni = getAssocDNIsSetting(grp)
	def nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		logWarn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		logWarn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

String getOnOffCmds(val, Integer endPoint=0) {
	return getSetLevelCmds(val ? 0xFF : 0x00, null, endPoint)
}

String getSetLevelCmds(Number level, Number duration=null, Integer endPoint=0) {
	Short modelSeries = Math.floor(deviceModelShort/10)
	Short levelVal = safeToInt(level, 99)
	// level 0xFF tells device to use last level, 0x00 is off
	if (levelVal != 0xFF && levelVal != 0x00) {
		//Convert level in range of min/max
		levelVal = convertLevel(levelVal, true)
		levelVal = validateRange(levelVal, 99, 1, 99)
	}

	// Duration Encoding:
	// 0x01..0x7F 1 second (0x01) to 127 seconds (0x7F) in 1 second resolution.
	// 0x80..0xFE 1 minute (0x80) to 127 minutes (0xFE) in 1 minute resolution.
	// 0xFF Factory default duration.

	//Convert seconds to minutes for 7x only
	if (modelSeries == 7 && duration > 120) {
		logDebug "getSetLevelCmds converting ${duration}s to ${Math.round(duration/60)}min"
		duration = (duration / 60) + 127
	}

	Short durationVal = validateRange(duration, -1, -1, 254)
	if (duration == null || durationVal == -1) {
		// For model 2x switches the 0xFF default is 0 seconds, lets override it.
		if (modelSeries == 2) {
			durationVal = getParamValue("rampRate")
		} else {
			durationVal = 0xFF
		}
	}

	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"
	return switchMultilevelSetCmd(levelVal, durationVal, endPoint)
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Main Device Events
	if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
		logInfo "${evt.descriptionText}"
	} else {
		logDebug "${evt.descriptionText} [NOT CHANGED]"
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)

	if (rawVal) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		level = convertLevel(level, false)

		desc = "level is set to ${level}%"
		if (type) desc += " (${type})"
		if (levelCorrection) desc += " [actual: ${rawVal}]"
		sendEventLog(name:"level", value:level, type:type, unit:"%", desc:desc, ep)
	}
}

void sendBasicButtonEvent(buttonId, String name) {
	String desc = "button ${buttonId} ${name} (digital)"
	sendEventLog(name:name, value:buttonId, type:"digital", desc:desc, isStateChange:true)
}


/*******************************************************************
 ***** Common Functions
********************************************************************/
/*** Parameter Store Map Functions ***/
@Field static Map<String, Map> configsList = new java.util.concurrent.ConcurrentHashMap()
Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Integer value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	configsList[device.id][paramNum] = value
	device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	Map configsMap = configsList[device.id]
	if (configsMap == null) {
		configsMap = [:]
		if (device.getDataValue("configVals")) {
			try {
				configsMap = evaluate(device.getDataValue("configVals"))
			}
			catch(Exception e) {
				logWarn("Clearing Invalid configVals: ${e}")
				device.removeDataValue("configVals")
			}
		}
		configsList[device.id] = configsMap
	}
	return configsMap
}

//Parameter List Functions
//This will rebuild the list for the current model and firmware only as needed
//paramsList Structure: MODEL:[FIRMWARE:PARAM_MAPS]
//PARAM_MAPS [num, name, title, description, size, defaultVal, options, firmVer]
@Field static Map<String, Map<String, List>> paramsList = new java.util.concurrent.ConcurrentHashMap()
void updateParamsList() {
	logDebug "Update Params List"
	String devModel = state.deviceModel
	Short modelNum = deviceModelShort
	Short modelSeries = Math.floor(modelNum/10)
	BigDecimal firmware = firmwareVersion

	List<Map> tmpList = []
	paramsMap.each { name, pMap ->
		Map tmpMap = pMap.clone()
		tmpMap.options = tmpMap.options?.clone()

		//Save the name
		tmpMap.name = name

		//Apply custom adjustments
		tmpMap.changes.each { m, changes ->
			if (m == devModel || m == modelNum || m ==~ /${modelSeries}X/) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		//Don't need this anymore
		tmpMap.remove("changes")

		//Set DEFAULT tag on the default
		tmpMap.options.each { k, val ->
			if (k == tmpMap.defaultVal) {
				tmpMap.options[(k)] = "${val} [DEFAULT]"
			}
		}

		//Save to the temp list
		tmpList << tmpMap
	}

	//Remove invalid or not supported by firmware
	tmpList.removeAll { it.num == null }
	tmpList.removeAll { firmware < (it.firmVer ?: 0) }
	tmpList.removeAll {
		if (it.firmVerM) {
			(firmware-(int)firmware)*100 < it.firmVerM[(int)firmware]
		}
	}

	//Save it to the static list
	if (paramsList[devModel] == null) paramsList[devModel] = [:]
	paramsList[devModel][firmware] = tmpList
}

//Verify the list and build if its not populated
void verifyParamsList() {
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (!paramsMap.settings?.fixed) fixParamsMap()
	if (paramsList[devModel] == null) updateParamsList()
	if (paramsList[devModel][firmware] == null) updateParamsList()
}
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap.ledColor.options << ledColorOptions
	paramsMap.autoOffInterval.options << autoOnOffIntervalOptions
	paramsMap.autoOnInterval.options << autoOnOffIntervalOptions
	paramsMap.rampRate.options << rampRateOptions
	paramsMap.rampRateOff.options << rampRateOptions
	paramsMap.holdRampRate.options << rampRateOptions
	paramsMap.zwaveRampRateOn.options << rampRateOptions
	paramsMap.zwaveRampRateOff.options << rampRateOptions
	paramsMap.remoteZWaveDim.options << rampRateOptions
	paramsMap.minimumBrightness.options << brightnessOptions
	paramsMap.maximumBrightness.options << brightnessOptions
	paramsMap.customBrightness.options << brightnessOptions
	paramsMap.nightLight.options << brightnessOptions
	paramsMap['settings'] = [fixed: true]
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	if (!device) return []
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion

	//Try to get device model if not set
	if (devModel) { verifyParamsList() }
	else          { runInMillis(200, setDevModel) }
	//Bail out if unknown device
	if (!devModel || devModel == "UNK00") return []

	return paramsList[devModel][firmware]
}

//Get a single param by name or number
Map getParam(def search) {
	//logDebug "Get Param (${search} | ${search.class})"
	Map param = [:]

	verifyParamsList()
	if (search instanceof String) {
		param = configParams.find{ it.name == search }
	} else {
		param = configParams.find{ it.num == search }
	}

	return param
}

//Convert Param Value if Needed
Integer getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
Number getParamValue(Map param, Boolean adjust=false) {
	if (param == null) return
	Number paramVal = safeToInt(settings."configParam${param.num}", param.defaultVal)
	if (!adjust) return paramVal

	//Reset hidden parameters to default
	if (param.hidden && settings."configParam${param.num}" != null) {
		logWarn "Resetting hidden parameter ${param.name} (${param.num}) to default ${param.defaultVal}"
		device.removeSetting("configParam${param.num}")
		paramVal = param.defaultVal
	}

	//Below is not needed for ZEN7X models
	if (state.deviceModel ==~ /ZEN7\d/) return paramVal

	switch(param.name) {
		case "autoOffEnabled":
			paramVal = getParamValue("autoOffInterval") == 0 ? 0 : 1
			break
		case "autoOffInterval":
			paramVal = paramVal ?: 60
			break
		case "autoOnEnabled":
			paramVal = getParamValue("autoOnInterval") == 0 ? 0 : 1
			break
		case "autoOnInterval":
			paramVal = paramVal ?: 60
			break
	}

	return paramVal
}

/*** Parameter Helper Functions ***/
private getBrightnessOptions() {
	Map options = [1:"1%"]
	for(x=5; x<100; x+=5) { options << [(x):"${x}%"] }
	options << [99:"99%"]
	return options
}

private getRampRateOptions() {
	return getTimeOptionsRange("Second", 1, [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,20,25,30,45,60,75,90])
}

private getAutoOnOffIntervalOptions() {
	Map options = [0:"Disabled"]
	options << getTimeOptionsRange("Minute", 1, [1,2,3,4,5,6,7,8,9,10,15,20,25,30,45])
	options << getTimeOptionsRange("Hour", 60, [1,2,3,4,5,6,7,8,9,10,12,18])
	options << getTimeOptionsRange("Day", (60 * 24), [1,2,3,4,5,6])
	options << getTimeOptionsRange("Week", (60 * 24 * 7), [1,2,3,4])
	return options
}

private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
}

//Stash the model in a state variable
String setDevModel(BigDecimal firmware) {
	if (!device) return
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))],4)
	String devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"
	if (!firmware) { firmware = firmwareVersion }

	state.deviceModel = devModel
	device.updateDataValue("deviceModel", devModel)
	logDebug "Set Device Info - Model: ${devModel} | Firmware: ${firmware}"

	if (devModel == "UNK00") {
		logWarn "Unsupported Device USE AT YOUR OWN RISK: ${devTypeId}"
		state.WARNING = "Unsupported Device Model - USE AT YOUR OWN RISK!"
	}
	else state.remove("WARNING")

	//Setup parameters if not set
	verifyParamsList()

	if (state.resyncAll) {
		//Disable sceneReverse setting for known cases otherwise set to true (most need it reversed)
		if ((devModel == "ZEN27" && firmware == 3.01) ||
		  (devModel == "ZEN22" && firmware == 4.01) ||
		  (devModel ==~ /ZEN7\d/)) {
			logDebug "Scene Reverse switched off, known Model/Firmware match found."
			device.updateSetting("sceneReverse", [value:"false",type:"bool"])
		} else if ((devModel ==~ /ZEN2\d/)) {
			logDebug "Scene Reverse switched on, known Model/Firmware match found."
			device.updateSetting("sceneReverse", [value:"true",type:"bool"])
		} else {
			logWarn "Scene Reverse unchanged, no known Model/Firmware match."
		}
	}

	return devModel
}

Integer getDeviceModelShort() {
	return safeToInt(state.deviceModel?.drop(3))
}

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}

String convertToLocalTimeString(dt) {
	def timeZoneId = location?.timeZone?.ID
	if (timeZoneId) {
		return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
	} else {
		return "$dt"
	}
}

List convertIntListToHexList(intList, pad=2) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(pad, "0").toUpperCase())
	}
	return hexList
}

List convertHexListToIntList(String[] hexList) {
	def intList = []

	hexList?.each {
		try {
			it = it.trim()
			intList.add(Integer.parseInt(it, 16))
		}
		catch (e) { }
	}
	return intList
}

Integer convertLevel(level, userLevel=false) {
	if (levelCorrection) {
		Integer brightmax = getParamValue("maximumBrightness")
		Integer brightmin = getParamValue("minimumBrightness")
		brightmax = (brightmax == 99) ? 100 : brightmax
		brightmin = (brightmin == 1) ? 0 : brightmin

		if (userLevel) {
			//This converts what the user selected into a physical level within the min/max range
			level = ((brightmax-brightmin) * (level/100)) + brightmin
			state.levelActual = level
			level = validateRange(Math.round(level), brightmax, brightmin, brightmax)
		}
		else {
			//This takes the true physical level and converts to what we want to show to the user
			if (Math.round(state.levelActual ?: 0) == level) level = state.levelActual
			else state.levelActual = level

			level = ((level - brightmin) / (brightmax - brightmin)) * 100
			level = validateRange(Math.round(level), 100, 1, 100)
		}
	}
	else if (state.levelActual) {
		state.remove("levelActual")
	}

	return level
}

Integer validateRange(val, Integer defaultVal, Integer lowVal, Integer highVal) {
	Integer intVal = safeToInt(val, defaultVal)
	if (intVal > highVal) {
		return highVal
	} else if (intVal < lowVal) {
		return lowVal
	} else {
		return intVal
	}
}

Integer safeToInt(val, defaultVal=0) {
	if ("${val}"?.isInteger())		{ return "${val}".toInteger() }
	else if ("${val}"?.isNumber())	{ return "${val}".toDouble()?.round() }
	else { return defaultVal }
}

boolean isDuplicateCommand(lastExecuted, allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
void logsOff() {}
void debugLogsOff() {
	logWarn "Debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void logWarn(String msg) {
	log.warn "${device.displayName}: ${msg}"
}

void logInfo(String msg) {
	if (txtEnable) log.info "${device.displayName}: ${msg}"
}

void logDebug(String msg) {
	if (debugEnable) log.debug "${device.displayName}: ${msg}"
}

//For Extreme Code Debugging - tracing commands
void logTrace(String msg) {
	//Uncomment to Enable
	//log.trace "${device.displayName}: ${msg}"
}
