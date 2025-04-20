/*
 *  Zooz ZEN Dimmers Universal
 *    - Model: ZEN22, ZEN24 - MINIMUM FIRMWARE 3.07 (to EOL)
 *    - Model: ZEN27 - MINIMUM FIRMWARE 2.08 (to EOL)
 *    - Model: ZEN72, ZEN74, ZEN77 - All Firmware (up to 3.50 / 2.30 / 4.60)
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-zen-switches/58649
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [2.0.3] - 2025-04-20 (@jtp10181)
  - Added singleThreaded flag
  - Updated Library and common code
  - Updated support info link for 2.4.x platform
  - Added option to set level to 0 when turned off
  - Added parameters 33 and 34 for ZEN7x models
  - Added descriptions to all settings from Zooz docs
  - Fixed Set Level command to work with mobile app v2
  - Fixed Set Parameter command to work with new UI

## [2.0.2] - 2023-12-10 (@jtp10181)
  - Set fallback log level to Info when not set yet
  - Fixed issue where not actually disabling brightness correction

## [2.0.0] - 2023-12-09 (@jtp10181)
  - Rearranged functions and merged with library code
  - Removed unnecessary association attrbiutes
  - Deprecated the childDevices and refreshParams commands
  - Added doubleTapped events for better Button Controller support
  - Flash command now remembers last rate as default
  - Put in proper multichannel lifeline association for ZEN30
  - Fix for ZEN30 v4 firmware using different endpoint numbers
  - Added new settings for ZEN7x 10.20/10.40 base and ZEN30 800LR v4
  - Set Level Duration supports up to 7,620s (127 mins) for ZEN30 v4
  - Brightness correction disabled when included in device firmware
  - Added possible workaround for Homekit integration issues (dimmers only)
  - Added paramters 16 and 17 for ZEN21 v4.05

## [1.6.4] - 2022-12-13 (@jtp10181)
  - Added Command to set any parameter (can be used in RM)

## [1.6.3] - 2022-11-22 (@jtp10181)
  ### Changed
  - Enabled parameter 7 for ZEN72 on new firmware
  - Set Level Duration supports up to 254s for 2x/ZEN30 and 7,620s (127 mins) for 7x
  ### Fixed
  - Fixed lifeline association checking on ZEN30
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

## [1.3.2] - 2021-01-09 (@jtp10181) ZEN30 ONLY
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

Below link is for original source (Kevin LaFramboise @krlaframboise)
https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/zooz/

 *
 *  Copyright 2020-2025 Jeff Page
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

@Field static final String VERSION = "2.0.3"
@Field static final String DRIVER = "Zooz-Dimmers"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-zen-switches-dimmers-advanced/58649"
@Field static final Map deviceModelNames =
	["B112:1F1C":"ZEN22", "B112:261C":"ZEN24", "A000:A002":"ZEN27",
	"7000:A002":"ZEN72", "7000:A004":"ZEN74", "7000:A007":"ZEN77"]

metadata {
	definition (
		name: "Zooz ZEN Dimmer Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		singleThreaded: true,
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
		capability "DoubleTapableButton"
		capability "Flash"

		//Modified from default to add duration argument
		command "startLevelChange", [
			[name:"Direction*", description:"Direction for level change request", type: "ENUM", constraints: ["up","down"]],
			[name:"Duration", type:"NUMBER", description:"Transition duration in seconds"] ]

		//command "refreshParams"
		command "setLED", [
			[name:"Select Color*", description:"ONLY for ZEN7x Series!", type: "ENUM", constraints: ledColorOptions] ]
		command "setLEDMode", [
			[name:"Select Mode*", description:"This Sets Parameter (#2)", type: "ENUM", constraints: ledModeCmdOptions] ]
		command "setParameter",[[name:"Parameter Number *", type:"NUMBER"],
			[name:"Parameter Value *", type:"NUMBER"], [name:"Parameter Size", type:"NUMBER"]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"B112", deviceId:"1F1C", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN22 Dimmer
		fingerprint mfr:"027A", prod:"B112", deviceId:"261C", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN24 Dimmer
		fingerprint mfr:"027A", prod:"A000", deviceId:"A002", inClusters:"0x5E,0x26,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x9F,0x6C,0x7A" //Zooz ZEN27 S2 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A002", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN72 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A004", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN74 Dimmer
		fingerprint mfr:"027A", prod:"7000", deviceId:"A007", inClusters:"0x5E,0x26,0x70,0x5B,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN77 S2 Dimmer
	}

	preferences {
		input(helpInfoInput)
		configParams.each { param ->
			if (!param.hidden) {
				if (param.options) {
					Integer paramVal = getParamValue(param)
					input "configParam${param.num}", "enum",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Selected: ${paramVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: param.defaultVal,
						options: param.options,
						required: false
				}
				else if (param.range) {
					input "configParam${param.num}", "number",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Range: ${(param.range).toString()}, DEFAULT: ${param.defaultVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: param.defaultVal,
						range: param.range,
						required: false
				}
			}
		}

		if (!isLongRange()) {
			for(int i in 2..maxAssocGroups) {
				input "assocDNI$i", "string", required: false,
					title: fmtTitle("Device Associations - Group $i"),
					description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear.")
			}
		} else {
			input "assocEnabled", "hidden", title: fmtTitle("Associations Not Available"),
				description: fmtDesc("Associations are not available when device is paired in Long Range mode")
		}

		input "supervisionGetEncap", "bool",
			title: fmtTitle("Supervision Encapsulation") + "<em> (Experimental)</em>",
			description: fmtDesc("This can increase reliability when the device is paired with security, but may not work correctly on all models."),
			defaultValue: false

		if (hardwareLevelCorrection()) {
			input "levelCorrection", "hidden", 
				title: fmtTitle("Brightness Correction"),
				description: fmtDesc("This feature is implimented within the hardware and cannot be changed."),
				defaultValue: false
		}
		else {
			input "levelCorrection", "bool",
				title: fmtTitle("Brightness Correction"),
				description: fmtDesc("Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%"),
				defaultValue: false
		}

		input "levelOff", "bool",
			title: fmtTitle("Set Level when Turned Off"),
			description: fmtDesc("When disabled the level will stay at the prior value when turned off [DEFAULT], Enable to have the level set to 0 when turned off."),
			defaultValue: false

		input "sceneReverse", "bool",
			title: fmtTitle("Scene Up-Down Reversal"),
			description: fmtDesc("If the button numbers and up/down descriptions are backwards in the scene button events change this setting to fix it"),
			defaultValue: false
	}
}

void debugShowVars() {
	log.warn "settings ${settings.hashCode()} ${settings}"
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
}

//Association Settings
@Field static final int maxAssocGroups = 4
@Field static final int maxAssocNodes = 5

/*** Static Lists and Settings ***/
@Field static final Map ledModeCmdOptions = [0:"Default", 1:"Reverse", 2:"Off", 3:"On"]
@Field static final Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]
@Field static Map<String, Long> turningOn = new java.util.concurrent.ConcurrentHashMap()

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	paddleControl: [ num: 1,
		title: "Paddle Orientation",
		description: "Choose if you want the upper paddle to turn the light on or turn the light off when tapped.",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reverse", 2:"Toggle Mode"]
	],
	ledMode: [ num: 2,
		title: "LED Indicator",
		description: "Choose if you want the LED indicator to turn on when the dimmer (light) is on or off, or if you want it to remain on or off at all times.",
		size: 1, defaultVal: 0,
		options: [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"],
		changes: [23:[num:null], 24:[num:null], 73:[defaultVal:2], 74:[defaultVal:2]]
	],
	ledColor: [ num: 23,
		title: "LED Indicator Color",
		description: "Choose the color of the LED indicator.",
		size: 1, defaultVal: 1,
		options: [:], //ledColorOptions
		changes: ['2X':[num:null]]
	],
	ledBrightness: [ num: 24,
		title: "LED Indicator Brightness",
		description: "Choose the LED indicator's brightness level.",
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
		description: "Auto-off timer will automatically turn the dimmer off after x minutes once it has been turned on.",
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
		description: "Auto-on timer will automatically turn the dimmer on after x minutes once it has been turned off.",
		size: 4, defaultVal: 0,
		options: [:], //autoOnOffIntervalOptions
		changes: ['7X':[num:5]]
	],
	powerFailure: [ num: 8,
		title: "Status After Power Failure",
		description: "Set the on off status for the switch after power failure.",
		size: 1, defaultVal: 2,
		options: [2:"Restores Prior Status", 0:"Always Off once restored", 1:"Always On once restored"]
	],
	rampRate: [ num: 9,
		title: "Physical Ramp Rate ON",
		description: "Adjust the ramp rate ON for your dimmer when the top paddle is pressed. Values correspond to the number of seconds it takes for the dimmer to reach full brightness when operated manually. *If no separate OFF setting is shown this controls both*",
		size: 1, defaultVal: 1,
		options: [0:"Instant On/Off"], //rampRateOptions
		changes: ['7X':[defaultVal:0]]
	],
	rampRateOff: [ num: 27,
		title: "Physical Ramp Rate OFF",
		description: "Adjust the ramp rate OFF for your dimmer when the bottom paddle is pressed. Values correspond to the number of seconds it takes for the dimmer to completely turn off when operated manually.",
		size: 1, defaultVal: 2,
		options: [0:"Instant Off"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: ['2X':[num:null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		]
	],
	holdRampRate: [ num: 16,
		title: "Dimming Speed",
		description: "Set the number of seconds it takes to get from 0% to 100% brightness when pressing and holding the paddle (physical dimming).",
		size: 1, defaultVal: 5,
		options: [:], //rampRateOptions
	],
	zwaveRampRate: [ num: 17,
		title: "Z-Wave Ramp Rate (Dimming Speed)",
		size: 1, defaultVal: 1,
		options: [ 1:"Z-Wave Can Set Ramp Rate", 0:"Match Physical Ramp Rate"],
		changes: ['7X':[num:null]]
	],
	switchMode: [ num: 33,
		title: "On/Off Switch Mode",
		description: "Convert the dimmer to an on off switch. When enabled, the dimmer will behave as a switch without the ability to dim. All ramp rates will be set to instant ON/OFF and the brightness level will locked at 99%.",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
		changes: ['2X':[num:null],
			72:[firmVer:3.40, firmVerM:[10:99]],
			74:[firmVer:2.20, firmVerM:[10:99]], 
			77:[firmVer:4.50, firmVerM:[10:99]]
		]
	],
	minimumBrightness: [ num: 10,
		title: "Minimum Brightness",
		description: "Set the minimum brightness level (in %) for your dimmer. You won’t be able to dim the light below the set value.",
		size: 1, defaultVal: 1,
		options: [:], //brightnessOptions
	],
	maximumBrightness: [ num: 11,
		title: "Maximum Brightness",
		description: "Set the maximum brightness level (in %) for your dimmer. You won’t be able to add brightness to the light beyond the set value.",
		size: 1, defaultVal: 99,
		options: [:], //brightnessOptions
	],
	doubleTapBrightness: [ num: 12,
		title: "Double Tap Up",
		description: "Choose what you'd like the dimmer to do when you double-tap the upper paddle.",
		size: 1, defaultVal: 0,
		options: [0:"Full Brightness (100%)", 1:"Maximum Brightness Parameter"],
		changes: ['7X':[options:[0:"Full Brightness (100%)", 1:"Custom Brightness Parameter", 2:"Maximum Brightness Parameter", 3:"Disabled"]]]
	],
	doubleTapFunction: [ num: 14,
		title: "Double Tap Up Function",
		size: 1, defaultVal: 0,
		options: [0:"Full/Maximum Brightness", 1:"Disabled, Single Tap Last Brightness (or Custom)", 2:"Disabled, Single Tap Full/Maximum Brightness"],
		changes: ['7X':[num:null]]
	],
	singleTapUp: [ num: 25,
		title: "Single Tap Up",
		description: "Choose what you'd like the dimmer to do when you tap the upper paddle once.",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level", 1:"Custom Brightness Parameter", 2:"Maximum Brightness Parameter", 3:"Full Brightness (100%)"],
		changes: ['2X':[num:null]]
	],
	customBrightness: [ num: 18,
		title: "Physical Custom Brightness On",
		description: "Set the custom brightness level (or leave the last brightness level) for single tap and double tap (see params 12 and 25).",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level"], //brightnessOptions
	],
	basicCustomBrightness: [ num: 34,
		title: "Basic Set Custom Brightness On",
    	description: "Set custom brightness for Basic Set ON commands when triggered by another device by direct association.",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level"], //brightnessOptions
		changes: ['2X':[num:null],
			72:[firmVer:3.50, firmVerM:[10:99]],
			74:[firmVer:2.30, firmVerM:[10:99]], 
			77:[firmVer:4.60, firmVerM:[10:99]]
		]
	],
	nightLight: [ num: 22,
		title: "Night Light Brightness",
		description: "Set the brightness level the dimmer will turn on to, when off and the lower paddle is held DOWN for 3 seconds.",
		size: 1, defaultVal: 20,
		options: [0:"Disabled"], //brightnessOptions
	],
	//sceneControl - Dimmers=13, ZEN26/73/76=10, Other Switches=9
	sceneControl: [ num: 13,
		title: "Scene Control Events",
		description: "Enable or disable scene control functionality for quick push and multi-tap triggers.",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
	],
	sceneControl3w: [ num: null,
		title: "Scene Control Events From 3-Way",
		description: "Enable scene control functionality from a mechanical switch connected in a direct 3-way",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [72:[num:31, firmVer:2.20, firmVerM:[10:40]]]
	],
	//loadControlParam - Dimmers=15, ZEN73/76=12, Other Switches=11
	loadControl: [ num: 15,
		title: "Load Control (Smart Bulb Mode)",
		description: "Enable or disable physical and Z-Wave on/off control. Disable both physical paddle and Z-Wave control for smart bulbs (use central scene triggers). Scene control and other functionality will still be available from paddles.",
		size: 1, defaultVal: 1,
		options: [1:"Enable Paddle and Z-Wave", 0:"Disable Paddle Control", 2:"Disable Paddle and Z-Wave Control"]
	],
	smartBulbBehavior: [ num: 21, //relayBehaviorParam
		title: "Disabled Load Behavior",
		description: "Set reporting behavior for disabled physical control of the load connected to the dimmer (smart bulb mode).",
		size: 1, defaultVal: 0,
		options: [0:"Reports Status & Changes LED", 1:"Doesn't Report Status or Change LED"],
	],
	smartBulbDimming: [ num: 20, //relayDimmingParam
		title: "Multilevel Dimming Reports",
		description: "Choose how you'd like the dimmer to report when paddles are tapped and held and physical / Z-Wave control is enabled or disabled. **See Documentation for more details**",
		size: 1, defaultVal: 0,
		options: [0:"Each Brightness Level", 1:"Final Brightness Only"]
	],
	//threeWaySwitchType - ZEN21/22/23/24/71/72 Only
	threeWaySwitchType: [num: null, // (19)
		title: "3-Way Switch Type",
		description: "Choose the type of 3-way switch you want to use with this dimmer.",
		size: 1, defaultVal: 0,
		options: [0:"Toggle On/Off Switch", 1:"Toggle On/Off with 2x/3x Shortcuts", 2:"Momentary Switch", 3:"Momentary with 2x/Hold Shortcuts"],
		changes: [22:[num: 19],24:[num: 19],72:[num: 19]]
	],
	paddleProgramming: [ num: null,
		title: "Paddle Programming",
	    description: "Enable or disable programming functionality on the switch paddles. If this setting is disabled, then inclusion, exclusion, smart bulb mode no longer work when switch paddles are activated (factory reset and scene control will still work). This allows 3x tap to work better.",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		changes: [
			22:[num: 24, firmVer:4.04], 24:[num: 24, firmVer:4.04], 27:[num: 24, firmVer:3.04],
			72:[num: 26, firmVer:2.0], 74:[num: 26, firmVer:2.0], 77:[num: 26, firmVer:2.0]
		],
	],
	zwaveRampRateOn: [ num: 28,
		title: "Z-Wave Ramp Rate ON",
		description: "Adjust the ramp rate ON for your dimmer when controlled with Z-Wave for a smooth fade-in effect (in seconds).",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant On"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: ['2X':[num:null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	zwaveRampRateOff: [ num: 29,
		title: "Z-Wave Ramp Rate OFF",
		description: "Adjust the ramp rate OFF for your dimmer when controlled with Z-Wave for a smooth fade-out effect (in seconds).",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant Off"], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: ['2X':[num:null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		],
	],
	remoteZWaveDim: [ num: 30,
		title: "Remote Z-Wave Dimming Duration",
		description: "Number of seconds it takes to get from 0% to 100% brightness on dimmers and smart bulbs directly associated in Groups 3 and 4.",
		size: 1, defaultVal: 5,
		options: [:], //rampRateOptions
		firmVer: 2.0, firmVerM:[10:10],
		changes: ['2X':[num:null],
			72:[], 74:[], 77:[firmVer:2.10, firmVerM:[10:20]]
		]
	],
	ledFlash: [ num: 32,
		title: "LED Indicator Flash On Changes",
    	description: "Choose if the LED should flash whenever a parameter is adjusted on the device to confirm the change.",
		size: 1, defaultVal: 0,
		options: [0:"Flash Enabled", 1:"Flash Disabled"],
		changes: ['2X':[num:null],
			72:[firmVer:2.20, firmVerM:[10:40]],
			74:[firmVer:2.00, firmVerM:[10:99]],
			77:[firmVer:3.20, firmVerM:[10:40, 2:30]]
		]
	],
	associationReports: [ num: 7,
		title: "Association Reports",
		description: "Choose physical and Z-Wave triggers for the dimmer to send a status change report to the associated devices. See manual for details.",
		size: 1, defaultVal: 15,
		options: [ 0:"0: None", 1:"1: Physical Tap On ZEN Only", 2:"2: Physical Tap On 3-Way Switch Only", 3:"3: Physical On ZEN / 3-Way Switch",
			4:"4: Z-Wave Command From Hub", 5:"5: Physical On ZEN / Z-Wave Command", 6:"6: Physical On 3-Way Switch / Z-Wave Command",
			7:"7: Physical On ZEN / 3-Way Switch / Z-Wave Command", 8:"8: Timer Only", 9:"9: Physical On ZEN / Timer",
			10:"10: Physical On 3-Way Switch / Timer", 11:"11: Physical On ZEN / 3-Way Switch / Timer", 12:"12: Z-Wave Command From Hub / Timer",
			13:"13: Physical On ZEN / Z-Wave Command / Timer", 14:"14: Physical On ZEN / 3-Way Switch / Z-Wave Command / Timer",
			15:"15: All Of The Above"
		],
		changes: [72:[firmVer:2.10, firmVerM:[10:30]]]
	],
	//Hidden Settings to set Defaults
	sceneMapping: [ num: null,
		title: "Central Scene Mapping",
		size: 1, defaultVal: 0,
		options: [0:"Up is Scene 2, Down is Scene 1", 1:"Up is Scene 1, Down is Scene 2"],
		hidden: true,
		changes: [21:[num: 16, firmVer:4.05], 23:[num: 14, firmVer:4.04], 26:[num: 14, firmVer:3.41],
			22:[num: 23, firmVer:4.04], 24:[num: 23, firmVer:4.04], 27:[num: 23, firmVer:3.04],
		]
	],
]

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x26: 2,	// Switch Multilevel (switchmultilevelv2) (4)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 1,	// Configuration (configurationv1)
	0x85: 2,	// Association (associationv2)
	0x86: 2,	// Version (versionv2)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
]


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

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		clearVariables()
		state.resyncAll = true
	}

	updateSyncingStatus(6)
	runIn(1, executeRefreshCmds)
	runIn(4, executeConfigureCmds)
}

void updated() {
	logDebug "updated..."
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
def on() {
	logDebug "on..."
	flashStop()
	if (turningOn[device.id] > now()) {
		logWarn "on() blocked, already adjusting"
		return
	}
	return getOnOffCmds(0xFF)
}

def off() {
	logDebug "off..."
	flashStop()
	return getOnOffCmds(0x00)
}

String setLevel(level, duration=null) {
	logDebug "setLevel($level, $duration)..."
	turningOn[device.id] = now() + 1000
	return getSetLevelCmds(level, duration)
}

List<String> startLevelChange(direction, duration=null) {
	Boolean upDown = (direction == "down") ? true : false
	Integer durationVal = validateRange(duration, getParamValue("holdRampRate") as Integer, 0, 127)
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
void flash(rateToFlash = null) {
	if (!rateToFlash) rateToFlash = state.flashRate
	//Min rate of 750ms sec, max of 30s
	rateToFlash = validateRange(rateToFlash, 1500, 750, 30000)
	Integer maxRun = 30 * 60 //30 Minutes
	state.flashNext = (device.currentValue("switch")=="on" ? "off" : "on")
	state.flashRate = rateToFlash

	logInfo "Flashing started with rate of ${rateToFlash}ms"

	//Start the flashing
	runIn(maxRun,flashStop,[data:true])
	flashHandler(rateToFlash)
}

void flashStop(Boolean turnOn = false) {
	if (state.flashNext != null) {
		logInfo "Flashing stopped..."
		unschedule("flashHandler")
		unschedule("flashStop")
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

def setParameter(paramNum, value, size = null) {
	paramNum = safeToInt(paramNum)
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name} - ${param.title}]" : "")
	return configSetGetCmd([num: paramNum, size: size], value as Integer)
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	zwaveParse(description)
	sendEvent(name:"numberOfButtons", value:10)
}
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	zwaveMultiChannel(cmd)
}
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	zwaveSupervision(cmd,ep)
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

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		Long sizeFactor = Math.pow(256,param.size).round()
		if (val < 0) { val += sizeFactor }

		logDebug "${param.name} - ${param.title} (#${param.num}) = ${val.toString()}"
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
		String dnis = convertIntListToHexList(cmd.nodeId)?.join(", ")
		logDebug "Confirmed Group $grp Association: " + (cmd.nodeId.size()>0 ? "${dnis} // ${cmd.nodeId}" : "None")

		if (cmd.nodeId.size() > 0) {
			if (!state.assocNodes) state.assocNodes = [:]
			state.assocNodes["$grp"] = cmd.nodeId
		} else {
			state.assocNodes?.remove("$grp" as String)
		}
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
	logTrace "${cmd} (ep ${ep})"

	//Flip the sceneNumber if needed (per parameter setting)
	if (settings.sceneReverse) {
		if (cmd.sceneNumber == 1) cmd.sceneNumber = 2
		else if (cmd.sceneNumber == 2) cmd.sceneNumber = 1
		logTrace "Scene Reversed: ${cmd}"
	}

	Map sceneEvt = [name: "", value: cmd.sceneNumber, desc: "", type:"physical", isStateChange:true]
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

	//DoubleTapped
	if (actionType && cmd.keyAttributes == 3) {
		sceneEvt.name = "doubleTapped"
		sceneEvt.desc = "button ${sceneEvt.value} ${sceneEvt.name} [${actionType}]"
		sendEventLog(sceneEvt, ep)
	}

	switch (cmd.keyAttributes) {
		case 0:
			sceneEvt.name = "pushed"
			btnVal = "${actionType} 1x"
			break
		case 1:
			sceneEvt.name = "released"
			btnVal = "${actionType} released"
			break
		case 2:
			sceneEvt.name = "held"
			btnVal = "${actionType} held"
			break
		case {it >=3 && it <= 6}:
			sceneEvt.name = "pushed"
			sceneEvt.value = cmd.sceneNumber + (2 * (cmd.keyAttributes - 2))
			btnVal = "${actionType} ${cmd.keyAttributes - 1}x"
			break
		default:
			logDebug "Unknown keyAttributes: ${cmd}"
	}

	if (actionType && btnVal) {
		sceneEvt.desc = "button ${sceneEvt.value} ${sceneEvt.name} [${btnVal}]"
		sendEventLog(sceneEvt, ep)
	}
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()

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
	turningOn[device.id] = 0

	if (rawVal || levelOff) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		if (level) level = convertLevel(level, false)

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
 ***** Execute / Build Commands
********************************************************************/
void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	//Checks and sets scheduled turn off
	checkLogLevel()

	List<String> cmds = []

	if (!firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds(true)

	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} - ${param.title} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	state.resyncAll = false

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << mfgSpecificGetCmd()
		cmds << versionGetCmd()
		runIn(3, checkSceneReverse)
	}

	cmds << switchMultilevelGetCmd()

	sendCommands(cmds)
}

List getConfigureAssocsCmds(Boolean logging=false) {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (logging) logDebug "Setting lifeline association..."
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
		List<String> cmdsEach = []
		List settingNodeIds = getAssocDNIsSettingNodeIds(i)

		//Need to remove first then add in case we are at limit
		List oldNodeIds = state.assocNodes?."$i"?.findAll { !(it in settingNodeIds) }
		if (oldNodeIds) {
			if (logging) logDebug "Removing Group $i Association: ${convertIntListToHexList(oldNodeIds)} // $oldNodeIds"
			cmdsEach << associationRemoveCmd(i, oldNodeIds)
		}

		List newNodeIds = settingNodeIds.findAll { !(it in state.assocNodes?."$i") }
		if (newNodeIds) {
			if (logging) logDebug "Adding Group $i Association: ${convertIntListToHexList(newNodeIds)} // $newNodeIds"
			cmdsEach << associationSetCmd(i, newNodeIds)
		}

		if (cmdsEach || state.resyncAll) {
			cmdsEach << associationGetCmd(i)
			cmds += cmdsEach
		}
	}

	return cmds
}

String getOnOffCmds(val, Integer endPoint=0) {
	return getSetLevelCmds(val ? 0xFF : 0x00, null, endPoint)
}

String getSetLevelCmds(level, duration=null, Integer endPoint=0) {
	Short modelSeries = Math.floor(deviceModelShort/10)
	Short levelVal = safeToInt(level, 99)
	duration = (duration as Integer)

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
	if (duration > 120 && modelSeries == 7) {
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
 ***** Required for Library
********************************************************************/
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
	paramsMap.basicCustomBrightness.options << brightnessOptions
	paramsMap.nightLight.options << brightnessOptions
	paramsMap['settings'] = [fixed: true]
}

Integer getParamValueAdj(Map param) {
	Integer paramVal = getParamValue(param)

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


/*******************************************************************
 ***** Child/Other Functions
********************************************************************/
/*** Other Functions ***/
Boolean hardwareLevelCorrection() {
	Short modelNum = deviceModelShort
	BigDecimal firmVer = firmwareVersion

	if ((modelNum == 77) && ((firmVer >= 2.10 && firmVer < 10.0) || firmVer >= 10.20)) {
		if (levelCorrection) { device.removeSetting("levelCorrection") }
		return true
	}
	return false
}

void checkSceneReverse() {
	String devModel = state.deviceModel

	//Set the sceneReverse setting for known cases otherwise leave alone
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


//#include jtp10181.zwaveDriverLibrary
/*******************************************************************
 *******************************************************************
 ***** Z-Wave Driver Library by Jeff Page (@jtp10181)
 *******************************************************************
********************************************************************

Changelog:
2023-05-10 - First version used in drivers
2023-05-12 - Adjustments to community links
2023-05-14 - Updates for power metering
2023-05-18 - Adding requirement for getParamValueAdj in driver
2023-05-24 - Fix for possible RuntimeException error due to bad cron string
2023-10-25 - Less saving to the configVals data, and some new functions
2023-10-26 - Added some battery shortcut functions
2023-11-08 - Added ability to adjust settings on firmware range
2024-01-28 - Adjusted logging settings for new / upgrade installs; added mfgSpecificReport
2024-06-15 - Added isLongRange function; convert range to string to prevent expansion
2024-07-16 - Support for multi-target version reports; adjust checkIn logic
2025-02-14 - Clearing all scheduled jobs during clearVariables / configure
           - Reworked saving/restoring of important states during clearVariables
           - Updated formatting and help info for 2.4.x platform

********************************************************************/

library (
  author: "Jeff Page (@jtp10181)",
  category: "zwave",
  description: "Z-Wave Driver Library",
  name: "zwaveDriverLibrary",
  namespace: "jtp10181",
  documentationLink: ""
)

/*******************************************************************
 ***** Z-Wave Reports (COMMON)
********************************************************************/
//Include these in Driver
//void parse(String description) {zwaveParse(description)}
//void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {zwaveMultiChannel(cmd)}
//void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {zwaveSupervision(cmd,ep)}

void zwaveParse(String description) {
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)

	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else {
		logWarn "Unable to parse: ${description}"
	}

	//Update Last Activity
	updateLastCheckIn()
}

//Decodes Multichannel Encapsulated Commands
void zwaveMultiChannel(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveSupervision(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, ep)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep))
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String fullVersion = String.format("%d.%02d",cmd.firmware0Version,cmd.firmware0SubVersion)
	String zwaveVersion = String.format("%d.%02d",cmd.zWaveProtocolVersion,cmd.zWaveProtocolSubVersion)
	device.updateDataValue("firmwareVersion", fullVersion)
	device.updateDataValue("protocolVersion", zwaveVersion)
	device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")

	if (cmd.targetVersions) {
		Map tVersions = [:]
		cmd.targetVersions.each {
			tVersions[it.target] = String.format("%d.%02d",it.version,it.subVersion)
			device.updateDataValue("firmware${it.target}Version", tVersions[it.target])
		}
		logDebug "Received Version Report - Main Firmware: ${fullVersion} | Targets: ${tVersions}"
	}
	else {
		logDebug "Received Version Report - Firmware: ${fullVersion}"
	}
	
	setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	logTrace "${cmd}"

	device.updateDataValue("manufacturer",cmd.manufacturerId.toString())
	device.updateDataValue("deviceType",cmd.productTypeId.toString())
	device.updateDataValue("deviceId",cmd.productId.toString())

	logDebug "fingerprint  mfr:\"${hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)}\", "+
		"prod:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)}\", "+
		"deviceId:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)}\", "+
		"inClusters:\"${device.getDataValue("inClusters")}\""+
		(device.getDataValue("secureInClusters") ? ", secureInClusters:\"${device.getDataValue("secureInClusters")}\"" : "")
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep}) [${getObjectClassName(cmd)}]"
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
	return superviseCmd(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return superviseCmd(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String mcAssociationGetCmd(Integer group) {
	return secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String mfgSpecificGetCmd() {
	return secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
}

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return superviseCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return superviseCmd(zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return superviseCmd(zwave.switchMultilevelV2.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return superviseCmd(zwave.switchMultilevelV2.switchMultilevelStopLevelChange(), ep)
}

String meterGetCmd(meter, Integer ep=0) {
	return secureCmd(zwave.meterV3.meterGet(scale: meter.scale), ep)
}

String meterResetCmd(Integer ep=0) {
	return secureCmd(zwave.meterV3.meterReset(), ep)
}

String wakeUpIntervalGetCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalGet())
}

String wakeUpIntervalSetCmd(val) {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalSet(seconds:val, nodeid:zwaveHubNodeId))
}

String wakeUpNoMoreInfoCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation())
}

String batteryGetCmd() {
	return secureCmd(zwave.batteryV1.batteryGet())
}

String sensorMultilevelGetCmd(sensorType) {
	Integer scale = (temperatureScale == "F" ? 1 : 0)
	return secureCmd(zwave.sensorMultilevelV11.sensorMultilevelGet(scale: scale, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType, Integer ep=0) {
	return secureCmd(zwave.notificationV3.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType), ep)
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	Long sizeFactor = Math.pow(256,param.size).round()
	if (value >= sizeFactor/2) { value -= sizeFactor }

	return superviseCmd(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
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
	return zwaveSecureEncap(multiChannelCmd(cmd, ep))
}

//MultiChannel Encapsulate if needed
//This is called from secureCmd or superviseCmd, do not call directly
String multiChannelCmd(hubitat.zwave.Command cmd, ep) {
	//logTrace "multiChannelCmd: ${cmd} (ep ${ep})"
	if (ep > 0) {
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}

//====== Supervision Encapsulate START ======\\
@Field static Map<String, Map<Short, String>> supervisedPackets = new java.util.concurrent.ConcurrentHashMap()
@Field static Map<String, Short> sessionIDs = new java.util.concurrent.ConcurrentHashMap()

String superviseCmd(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "superviseCmd: ${cmd} (ep ${ep})"

	if (settings.supervisionGetEncap) {
		//Encap with SupervisionGet
		Short sessId = getSessionId()
		def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sessId).encapsulate(cmd)

		//Encap with MultiChannel now so it is cached that way below
		cmdEncap = multiChannelCmd(cmdEncap, ep)

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
 ***** Common Functions
********************************************************************/
/*** Parameter Store Map Functions ***/
@Field static Map<String, Map> configsList = new java.util.concurrent.ConcurrentHashMap()
Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	Map configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Number value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	configsList[device.id][paramNum] = value
	//device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	TreeMap configsMap = configsList[device.id]
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

/*** Parameter List Functions ***/
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
		if (tmpMap.options) tmpMap.options = tmpMap.options?.clone()
		if (tmpMap.range) tmpMap.range = (tmpMap.range).toString()

		//Save the name
		tmpMap.name = name

		//Apply custom adjustments
		tmpMap.changes.each { m, changes ->
			if (m == devModel || m == modelNum || m ==~ /${modelSeries}X/) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		tmpMap.changesFR.each { m, changes ->
			if (firmware >= m.getFrom() && firmware <= m.getTo()) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		//Don't need this anymore
		tmpMap.remove("changes")
		tmpMap.remove("changesFR")

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
	if (devModel) {
		if (paramsList[devModel] == null) updateParamsList()
		else if (paramsList[devModel][firmware] == null) updateParamsList()
	}
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
Map getParam(String search) {
	verifyParamsList()
	return configParams.find{ it.name == search }
}
Map getParam(Number search) {
	verifyParamsList()
	return configParams.find{ it.num == search }
}

//Convert Param Value if Needed
BigDecimal getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
BigDecimal getParamValue(Map param) {
	if (param == null) return
	BigDecimal paramVal = safeToDec(settings."configParam${param.num}", param.defaultVal)

	//Reset hidden parameters to default
	if (param.hidden && settings."configParam${param.num}" != null) {
		logWarn "Resetting hidden parameter ${param.name} (${param.num}) to default ${param.defaultVal}"
		device.removeSetting("configParam${param.num}")
		paramVal = param.defaultVal
	}

	return paramVal
}

/*** Preference Helpers ***/
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}
String fmtDesc(String str) {
	if (location.hub.firmwareVersionString >= "2.4.0.0") {
		return "<div style='font-style: italic; padding: 0px 0px 4px 6px; line-height:1.4;'>${str}</div>"
	} else {
		return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
	}
}

String getInfoLink() {
	String str = "Community Support"
	String info = ((PACKAGE ?: '') + " ${DRIVER} v${VERSION}").trim()
	String hrefStyle = "style='font-size: 140%; padding: 2px 16px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
	String htmlTag = "<a ${hrefStyle} href='${COMM_LINK}' target='_blank'><div style='font-size: 70%;'>${info}</div>${str}</a>"
	String finalLink = "<div style='text-align:center; position:relative; display:flex; justify-content:center; align-items:center;'><ul class='nav'><li>${htmlTag}</ul></li></div>"
	return finalLink
}

String getFloatingLink() {
	String info = ((PACKAGE ?: '') + " ${DRIVER} v${VERSION}").trim()
	String topStyle = "style='font-size: 100%; padding: 2px 12px; border: 2px solid SlateGray; border-radius: 6px;'" //SlateGray
	String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${info}</a>"
	String finalLink = "<div style='text-align: center; position: absolute; top: 8px; right: 60px; padding: 0px; background-color: white;'><ul class='nav'><li>${topLink}</ul></li></div>"
	return finalLink
}

//Use this at top of preferences, example: input(helpInfoInput)
Map getHelpInfoInput () {
	return [name: "helpInfo", type: "hidden", title: "Support Information:", description: "${infoLink}"]
}

//Adds fake command with support info link
command "!SupportInfo:", [[name:"${infoLink}"]]
void "!SupportInfo:"() { log.info "${infoLink}" }


private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}

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

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
	device.updateDataValue("configVals", getParamStoredMap()?.inspect())
	if (changes==0 && state.deviceSync) { state.remove("deviceSync") }
}

void updateLastCheckIn() {
	Date nowDate = new Date()
	state.lastCheckInDate = convertToLocalTimeString(nowDate)

	Long lastExecuted = state.lastCheckInTime ?: 0
	Long allowedMil = 24 * 60 * 60 * 1000   //24 Hours
	if (lastExecuted + allowedMil <= nowDate.time) {
		state.lastCheckInTime = nowDate.time
		if (lastExecuted) runIn(2, doCheckIn)
		scheduleCheckIn()
	}
}

void scheduleCheckIn() {
	unschedule("doCheckIn")
	runIn(86340, doCheckIn)
}

void doCheckIn() {
	scheduleCheckIn()
	String pkg = PACKAGE ?: DRIVER
	String devModel = (state.deviceModel ?: (PACKAGE ? DRIVER : "NA")) + (state.subModel ? ".${state.subModel}" : "")
	String checkUri = "http://jtp10181.gateway.scarf.sh/${pkg}/chk-${devModel}-v${VERSION}"

	try {
		httpGet(uri:checkUri, timeout:4) { logDebug "Driver ${pkg} ${devModel} v${VERSION}" }
		state.lastCheckInTime = now()
	} catch (Exception e) { }
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValueAdj(param)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

//iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	String val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
}

List getAssocDNIsSettingNodeIds(grp) {
	String dni = getAssocDNIsSetting(grp)
	List nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		logWarn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		logWarn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

//Used with configure to reset variables
void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	List saveList = ["deviceModel","resyncAll","deviceSync","energyTime","group1Assoc"]
	Map saveMap = state.findAll { saveList.contains(it.key) && it.value != null }

	//Clears State Variables
	state.clear()

	//Clear Config Data
	configsList["${device.id}"] = [:]
	device.removeDataValue("configVals")
	//Clear Data from other Drivers
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Clear Schedules
	unschedule()

	//Restore Saved States
	state.putAll(saveMap)
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

	return devModel
}

Integer getDeviceModelShort() {
	return safeToInt(state.deviceModel?.drop(3))
}

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}

Boolean isLongRange() {
	Integer intDNI = device ? hubitat.helper.HexUtils.hexStringToInt(device.deviceNetworkId) : null
	return (intDNI > 255)
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

BigDecimal safeToDec(val, defaultVal=0, roundTo=-1) {
	BigDecimal decVal = "${val}"?.isNumber() ? "${val}".toBigDecimal() : defaultVal
	if (roundTo == 0)		{ decVal = Math.round(decVal) }
	else if (roundTo > 0)	{ decVal = decVal.setScale(roundTo, BigDecimal.ROUND_HALF_UP).stripTrailingZeros() }
	if (decVal.scale()<0)	{ decVal = decVal.setScale(0) }
	return decVal
}

Boolean isDuplicateCommand(Long lastExecuted, Long allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > now())
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
*/

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule("logsOff")
	//Set Defaults
	if (settings.logLevel == null) {
		device.updateSetting("logLevel",[value:"3", type:"enum"])
		levelInfo.level = 3
	}
	if (settings.logLevelTime == null) {
		device.updateSetting("logLevelTime",[value:"30", type:"enum"])
		levelInfo.time = 30
	}
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
		logMsg += " for ${LOG_TIMES[levelInfo.time]}"
		runIn(60*levelInfo.time, logsOff)
	}
	logInfo(logMsg)

	//Store last level below Debug
	if (levelInfo.level <= 2) state.lastLogLevel = levelInfo.level
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
	Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
	Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
	Integer level = settings.logLevel != null ? settings.logLevel as Integer : 1
	Integer time = settings.logLevelTime != null ? settings.logLevelTime as Integer : 30
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:false, type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		Integer lastLvl = state.lastLogLevel != null ? state.lastLogLevel as Integer : 2
		device.updateSetting("logLevel",[value:lastLvl.toString(), type:"enum"])
		logWarn "Logging Level is: ${LOG_LEVELS[lastLvl]} (${lastLvl})"
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${device.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=1) log.warn "${device.displayName}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=2) log.info "${device.displayName}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=3) log.debug "${device.displayName}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=4) log.trace "${device.displayName}: ${msg}"
}
