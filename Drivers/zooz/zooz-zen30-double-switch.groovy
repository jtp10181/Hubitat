/*
 *  Zooz ZEN30 Double Switch
 *    - Model: ZEN30 - All Firmware
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-zen-switches/58649
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [2.0.0.b2] - 2023-XX-XX (@jtp10181)
  - Rearranged functions to get ready for library code
  - Removed unnecessary association attrbiutes
  - Put in proper multichannel lifeline association
  - Depreciated the childDevices and refreshParams commands
  - Fix for new firmware using different endpoint numbers
  - Added new settings for 800LR (v4) devices

## [1.6.4] - 2022-12-13 (@jtp10181)
  - Added Command to set any parameter (can be used in RM)

## [1.6.3] - 2022-11-22 (@jtp10181)
  ### Changed
  - Set Level Duration supports up to 254s
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
  - Was possible for hidden parameters to get stuck in the wrong setting
  ### Removed
  - Parameter test/hide code totally removed

## [1.5.3] - 2022-07-13 (@jtp10181)
  ### Added
  - Better support ZEN30 v2/v3 hardware/firmware
  ### Changed
  - Extra check to make sure devModel is set
  - Minor refactoring

  ## Merged from Dimmer v1.5.2
  ### Added
  - Support for multiple hardware/firmware major versions
  - Support for Association Group 4 (only works on some models)
  - Set deviceModel in device data (press refresh)
  ### Changed
  - Removed getParam.value and replaced with separate function
  - Adding HTML styling to the Preferences
  ### Fixed
  - Some parameters would get multiple [DEFAULT] tags added
  ### Deprecated
  - Parameter test/hide functions, not needed

  ## Merged from Dimmer v1.5.1
  ### Changed
  - Description text loging enabled by default
  ### Fixed
  - Added inClusters to fingerprint so it will be selected by default
  - Global (Field static) Maps defined explicitly as a ConcurrentHashMap

  ## Merged from Dimmer v1.5.0
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

NOTICE: This file has been modified by *Jeff Page* under compliance with
	the Apache 2.0 License from the original work of *Kevin LaFramboise*.

Below link is for original source (Kevin LaFramboise @krlaframboise)
https://github.com/krlaframboise/SmartThings/blob/master/devicetypes/krlaframboise/zooz-double-switch.src/zooz-double-switch.groovy

 *
 *  Copyright 2020-2023 Jeff Page
 *  Copyright 2020 Kevin LaFramboise
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

@Field static final String VERSION = "2.0.0.b2"
@Field static final String DRIVER = "Zooz-ZEN30"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-zen-switches-dimmers-advanced/58649"
@Field static final Map deviceModelNames = ["A000:A008":"ZEN30"]

metadata {
	definition (
		name: "Zooz ZEN30 Double Switch",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen30-double-switch.groovy"
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
		//command "childDevices", [[name:"Select One*", type: "ENUM", constraints: ["Create","Remove"] ]]
		command "setLED", [
			[name:"Select LED*", type: "ENUM", constraints: ["Dimmer","Relay"] ],
			[name:"Select Color*", type: "ENUM", constraints: ledColorOptions] ]
		command "setLEDMode", [
			[name:"Select LED*", type: "ENUM", constraints: ["Dimmer","Relay"] ],
			[name:"Select Mode*", description:"This Sets Preference (#2)*", type: "ENUM", constraints: ledModeCmdOptions] ]
		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A008", inClusters:"0x5E,0x26,0x25,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x70,0x5B,0x60,0x9F,0x6C,0x7A" //Zooz ZEN30 Double Switch
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

		//Logging options similar to other Hubitat drivers
		input "txtEnable", "bool", title: fmtTitle("Enable Description Text Logging?"), defaultValue: true
		input "debugEnable", "bool", title: fmtTitle("Enable Debug Logging?"), defaultValue: true
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
//@Field static final Map endPoints = ["dimmer":0, "relay":1]
@Field static final Map ledModeCmdOptions = [0:"Default", 1:"Reverse", 2:"Off", 3:"On"]
@Field static final Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	ledModeDimmer: [ num: 1,
		title: "Dimmer LED Indicator Mode",
		size: 1, defaultVal: 0,
		options: [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"],
	],
	ledColorDimmer: [ num: 3,
		title: "Dimmer LED Indicator Color",
		size: 1, defaultVal: 0,
		options: [:] //ledColorOptions
	],
	ledBrightnessDimmer: [ num: 5,
		title: "Dimmer LED Indicator Brightness",
		size: 1, defaultVal: 1,
		options: [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"],
	],
	ledModeRelay: [ num: 2,
		title: "Relay LED Indicator Mode",
		size: 1, defaultVal: 0,
		options: [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"]
	],
	ledColorRelay: [ num: 4,
		title: "Relay LED Indicator Color",
		size: 1, defaultVal: 0,
		options: [:] //ledColorOptions
	],
	ledBrightnessRelay: [ num: 6,
		title: "Relay LED Indicator Brightness",
		size: 1, defaultVal: 1,
		options: [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"],
	],
	ledSceneDisplay: [ num: 7,
		title: "LED Indicator Displays Scene Selections",
		size: 1, defaultVal: 1,
		options: [0:"LED Enabled", 1:"LED Disabled"]
	],
	autoOffIntervalDimmer: [ num: 8,
		title: "Dimmer Auto Turn-Off Timer",
		size: 4, defaultVal: 0,
		options: [:] //autoOnOffIntervalOptions
	],
	autoOnIntervalDimmer: [ num: 9,
		title: "Dimmer Auto Turn-On Timer",
		size: 4, defaultVal: 0,
		options: [:] //autoOnOffIntervalOptions
	],
	autoOffIntervalRelay: [ num: 10,
		title: "Relay Auto Turn-Off Timer",
		size: 4, defaultVal: 0,
		options: [:] //autoOnOffIntervalOptions
	],
	autoOnIntervalRelay: [ num: 11,
		title: "Relay Auto Turn-On Timer",
		size: 4, defaultVal: 0,
		options: [:] //autoOnOffIntervalOptions
	],
	powerFailure: [ num: 12,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 3,
		options: [0:"Dimmer Off / Relay Off", 1:"Dimmer Off / Relay On", 2:"Dimmer On / Relay Off",
			3:"Dimmer Restored / Relay Restored", 4:"Dimmer Restored / Relay On", 5:"Dimmer Restored / Relay Off",
			6:"Dimmer On / Relay Restored", 7:"Dimmer Off / Relay Restored", 8:"Dimmer On / Relay On"],
	],
	rampRate: [ num: 13,
		title: "Dimmer Ramp Rate to Full On/Off",
		size: 1, defaultVal: 1,
		options: [0:"Instant On/Off"], //rampRateOptions
		changesFR: [(3.20..99):[title:"Dimmer Ramp Rate - Physical ON", defaultVal:0]]
	],
	rampRateOff: [ num: 31,
		title: "Dimmer Ramp Rate - Physical OFF",
		size: 1, defaultVal: 2,
		options: [0:"Instant Off"], //rampRateOptions
		firmVer: 3.20
	],
	holdRampRate: [ num: 21,
		title: "Dimming Speed when Paddle is Held",
		size: 1, defaultVal: 4,
		options: [:], //rampRateOptions
	],
	// zwaveRampRate: [ num: 22, // Removed in firmware v1.05
	// 	title: "Z-Wave Ramp Rate (Dimming Speed)",
	// 	size: 1, defaultVal: 1,
	// 	options: [ 1:"Z-Wave Can Set Ramp Rate", 0:"Match Physical Ramp Rate"],
	// ],
	minimumBrightness: [ num: 14,
		title: "Dimmer Minimum Brightness",
		size: 1, defaultVal: 1,
		options: [:], //brightnessOptions
	],
	maximumBrightness: [ num: 15,
		title: "Dimmer Maximum Brightness",
		size: 1, defaultVal: 99,
		options: [:], //brightnessOptions
	],
	doubleTapBrightness: [ num: 17,
		title: "Double Tap Up Brightness",
		size: 1, defaultVal: 0,
		options: [0:"Full Brightness (100%)", 1:"Maximum Brightness Setting"],
		changesFR: [(3.20..99):[options: [0:"Full Brightness (100%)", 1:"Custom Brightness Setting", 2:"Maximum Brightness Setting", 3:"Disabled"]]],
	],
	dimmerTapFunction: [ num: 18,
		title: "Dimmer Tap Up Functions",
		size: 1, defaultVal: 0,
		options: [0:"Single: Last/Custom | Double: Full/Max", 1:"Single: Last/Custom | Double: Disabled", 2:"Single: Full/Max | Double: Disabled"],
		changesFR: [(3.20..99):[
			title: "Single Tap Turn On Brightness",
			options: [0:"Last Brightness", 1:"Custom Brightness Setting", 2:"Maximum Brightness Setting", 3:"Full Brightness (100%)"]
		]],
	],
	customBrightness: [ num: 23,
		title: "Custom Brightness Setting",
		size: 1, defaultVal: 0,
		options: [0:"Last Brightness Level"], //brightnessOptions
	],
	nightLight: [ num: 26,
		title: "Night Light Mode Brightness",
		size: 1, defaultVal: 20,
		options: [0:"Disabled"], //brightnessOptions
		firmVer: 1.05
	],
	sceneControlDimmer: [ num: 28,
		title: "Dimmer Scene Control Events",
		description: "Enable to get push and multi-tap events for the dimmer",
		size: 1, defaultVal: 1,
		options: [0:"Disabled", 1:"Enabled"],
		firmVerM: [1:11,2:11,3:10]
	],
	sceneControlRelay: [ num: 29,
		title: "Relay Scene Control Events",
		description: "Enable to get push and multi-tap events for the relay button",
		size: 1, defaultVal: 1,
		options: [0:"Disabled", 1:"Enabled"],
		firmVerM: [1:11,2:11,3:10]
	],
	loadControlDimmer: [ num: 19,
		title: "Dimmer Smart Bulb - Load Control",
		size: 1, defaultVal: 1,
		options: [1:"Enable Paddle and Z-Wave", 0:"Disable Paddle Control", 2:"Disable Paddle and Z-Wave Control"],
	],
	smartBulbBehaviorDimmer: [ num: 24,
		title: "Dimmer Smart Bulb - When Paddle Disabled",
		size: 1, defaultVal: 0,
		options: [0:"Reports Status & Changes LED", 1:"Doesn't Report Status or Change LED"],
		firmVer: 1.05
	],
	loadControlRelay: [ num: 20,
		title: "Relay Smart Bulb - Load Control",
		size: 1, defaultVal: 1,
		options: [1:"Enable Paddle and Z-Wave", 0:"Disable Paddle Control", 2:"Disable Paddle and Z-Wave Control"],
	],
	smartBulbBehaviorRelay: [ num: 25,
		title: "Relay Smart Bulb - When Paddle Disabled",
		size: 1, defaultVal: 0,
		options: [0:"Reports Status & Changes LED", 1:"Doesn't Report Status or Change LED"],
		firmVer: 1.05
	],
	paddleControl: [ num: 27,
		title: "Paddle Orientation",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reverse", 2:"Toggle Mode"],
		firmVer: 1.05
	],
	paddleProgramming: [ num: 30,
		title: "Programming from the Paddle",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		firmVerM: [1:11,2:11,3:10]
	],
	zwaveRampRateOn: [ num: 32,
		title: "Dimmer Ramp Rate - Z-Wave ON",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant On"], //rampRateOptions
		firmVer: 3.20
	],
	zwaveRampRateOff: [ num: 33,
		title: "Dimmer Ramp Rate - Z-Wave OFF",
		size: 1, defaultVal: 255,
		options: [255:"Match Physical",0:"Instant Off"], //rampRateOptions
		firmVer: 3.20
	]
]

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x25: 1,	// SwitchBinary (switchbinaryv1)
	0x26: 2,	// Switch Multilevel (switchmultilevelv2) (4)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x60: 3,	// MultiChannel (multichannelv3)
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
	childDevicesCreate()
	initialize()
}

void initialize() {
	logWarn "initialize..."
	refresh()
}

void configure() {
	logWarn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	childDevicesCreate()

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		clearVariables()
		state.resyncAll = true
	}

	updateSyncingStatus(6)
	runIn(1, executeProbeCmds)
	runIn(2, executeRefreshCmds)
	runIn(5, executeConfigureCmds)
}

void updated() {
	logDebug "updated..."
	logDebug "Debug logging is: ${debugEnable == true}"
	logDebug "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	executeProbeCmds()
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
	return getOnOffCmds(0xFF, endPoints.dimmer)
}

String off() {
	logDebug "off..."
	flashStop()
	return getOnOffCmds(0x00, endPoints.dimmer)
}

String setLevel(Number level, Number duration=null) {
	logDebug "setLevel($level, $duration)..."
	return getSetLevelCmds(level, duration, endPoints.dimmer)
}

List<String> startLevelChange(direction, duration=null) {
	Boolean upDown = (direction == "down") ? true : false
	Integer durationVal = validateRange(duration, getParamValue("holdRampRate") as Integer, 0, 127)
	logDebug "startLevelChange($direction) for ${durationVal}s"

	List<String> cmds = [switchMultilevelStartLvChCmd(upDown, durationVal, endPoints.dimmer)]

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
	return switchMultilevelStopLvChCmd(endPoints.dimmer)
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
		sendCommands(getSetLevelCmds(0xFF, 0, endPoints.dimmer))
	}
	else if (state.flashNext == "off") {
		logDebug "Flash Off"
		state.flashNext = "on"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getSetLevelCmds(0x00, 0, endPoints.dimmer))
	}
}


/*** Custom Commands ***/
void setLED(String which, String colorName) {
	Map param = (which == "Dimmer") ? getParam("ledColorDimmer") : getParam("ledColorRelay")

	if (param?.num) {
		Short paramVal = ledColorOptions.find{ colorName.equalsIgnoreCase(it.value) }.key
		logDebug "Indicator Color Value (${which}) [${colorName} : ${paramVal}]"
		//Set the Preference to match new setting, then send command to device
		device.updateSetting("configParam${param.num}",[value:"${paramVal}", type:"enum"])
		sendCommands(configSetGetCmd(param, paramVal))
	}
	else {
		logWarn  "There is No LED Color Parameter Found for this model"
	}
}

void setLEDMode(String which, String modeName) {
	Map param = (which == "Dimmer") ? getParam("ledModeDimmer") : getParam("ledModeRelay")

	if (param?.num) {
		Short paramVal = ledModeCmdOptions.find{ modeName.equalsIgnoreCase(it.value) }.key
		logDebug "Indicator Value (${which}) [${modeName} : ${paramVal}]"
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
	cmds << mcAssociationGetCmd(1)
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


/*** Child Capabilities ***/
def componentOn(cd) {
	logDebug "componentOn from ${cd.displayName}"
	state.relayDigital = true
	sendCommands(switchBinarySetCmd(0xFF, endPoints.relay))
}

def componentOff(cd) {
	logDebug "componentOff from ${cd.displayName}"
	state.relayDigital = true
	sendCommands(switchBinarySetCmd(0x00, endPoints.relay))
}

def componentRefresh(cd) {
	logDebug "componentRefresh from ${cd.displayName}"
	executeRefreshCmds()
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
// void parse(String description) {
// 	zwaveParse(description)
// }
// void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
// 	zwaveMultiChannel(cmd)
// }
// void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
// 	zwaveSupervision(cmd,ep)
// }

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
		//state.group1Assoc = (cmd.nodeId == [zwaveHubNodeId]) ? true : false
	}
	else if (grp > 1 && grp <= maxAssocGroups) {
		logDebug "Group $grp Association: ${cmd.nodeId}"

		if (cmd.nodeId.size() > 0) {
			state["assocNodes$grp"] = cmd.nodeId
		} else {
			state.remove("assocNodes$grp".toString())
		}

		String dnis = convertIntListToHexList(cmd.nodeId)?.join(", ")
		//sendEventLog(name:"assocDNI$grp", value:(dnis ?: "none"))
		device.updateSetting("assocDNI$grp", [value:"${dnis}", type:"string"])
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	if (cmd.groupingIdentifier == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId} | MC: ${cmd.multiChannelNodeIds}"
		state.group1Assoc = (cmd.multiChannelNodeIds == [[nodeId:zwaveHubNodeId, bitAddress:0, endPointId:0]] ? true : false)
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

	if (ep == endPoints.relay) {  //Should only get this for the relay
		String type = (state.relayDigital ? "digital" : "physical")
		state.remove("relayDigital")
		sendSwitchEvents(cmd.value, type, ep)
	}
	else {
		logDebug "Unexpected (Ignored): ${cmd} (endPoint ${ep})"

		//Check on Relay Status
		sendCommands(switchBinaryGetCmd(endPoints.relay))
	}

}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "digital", ep)
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (cmd.endPoints > 0) {
		logDebug "Endpoints (${cmd.endPoints}) Detected and Enabled"
		state.endPoints = cmd.endPoints
		// if (cmd.endPoints == 2) {
		// 	endPoints = ["dimmer":1, "relay":2]
		// }
		runIn(1,childDevicesCreate)
	}
}

void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep=0){
	logTrace "${cmd} (ep ${ep})"

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
		case 3:
			actionType = "relay"
			scene.value = 1
			ep = endPoints.relay
			break
		default:
			logDebug "Unknown sceneNumber: ${cmd}"
	}

	switch (cmd.keyAttributes) {
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
			else if (cmd.sceneNumber == 3) scene.value = cmd.keyAttributes - 1
			btnVal = "${actionType} ${cmd.keyAttributes - 1}x"
			break
		default:
			logDebug "Unknown keyAttributes: ${cmd}"
	}

	if (actionType && btnVal) {
		scene.desc = "button ${scene.value} ${scene.name} [${btnVal}]"
		sendEventLog(scene, ep)
	}
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Endpoint Events
	if (ep == endPoints.relay) {
		def childDev = childDevices[0]
		String logEp = "(RELAY) "

		if (childDev) {
			if (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
				childDev.parse([evt])
			} else {
				logDebug "${logEp}${evt.descriptionText} [NOT CHANGED]"
				childDev.sendEvent(evt)
			}
		}
		else {
			log.error "No device for endpoint (${ep}). Run configure to create child devices."
		}
		return
	}

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

	if (rawVal && ep == endPoints.dimmer) {
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

	state.resyncAll = false

	if (cmds) sendCommands(cmds,300)
}

void executeProbeCmds() {
	logTrace "executeProbeCmds..."

	List<String> cmds = []

	//End Points Check
	if (state.endPoints == null) {
		logDebug "Probing for Multiple End Points"
		cmds << secureCmd(zwave.multiChannelV3.multiChannelEndPointGet())
		state.endPoints = 0
	}

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds << switchMultilevelGetCmd(endPoints.dimmer)
	cmds << switchBinaryGetCmd(endPoints.relay)
	state.relayDigital = true

	sendCommands(cmds)
}

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		logDebug "Need to set lifeline association..."
		cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[], multiChannelNodeIds:[]))
		cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationSet(groupingIdentifier: 1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress:0, endPointId: 0]]))
		cmds << mcAssociationGetCmd(1)
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
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

	Short durationVal = validateRange(duration, -1, -1, 254)
	if (duration == null || durationVal == -1) {
		durationVal = 0xFF
	}

	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"
	return switchMultilevelSetCmd(levelVal, durationVal, endPoint)
}


/*******************************************************************
 ***** Required for Library
********************************************************************/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap.ledColorDimmer.options << ledColorOptions
	paramsMap.ledColorRelay.options << ledColorOptions
	paramsMap.autoOffIntervalDimmer.options << autoOnOffIntervalOptions
	paramsMap.autoOnIntervalDimmer.options << autoOnOffIntervalOptions
	paramsMap.autoOffIntervalRelay.options << autoOnOffIntervalOptions
	paramsMap.autoOnIntervalRelay.options << autoOnOffIntervalOptions
	paramsMap.rampRate.options << rampRateOptions
	paramsMap.rampRateOff.options << rampRateOptions
	paramsMap.holdRampRate.options << rampRateOptions
	paramsMap.zwaveRampRateOn.options << rampRateOptions
	paramsMap.zwaveRampRateOff.options << rampRateOptions
	paramsMap.minimumBrightness.options << brightnessOptions
	paramsMap.maximumBrightness.options << brightnessOptions
	paramsMap.customBrightness.options << brightnessOptions
	paramsMap.nightLight.options << brightnessOptions
	paramsMap['settings'] = [fixed: true]
}


/*******************************************************************
 ***** Child/Other Functions
********************************************************************/
/*** Child Device Methods ***/
// void childDevices(str) {
// 	switch (str) {
// 		case "Create":
// 			childDevicesCreate()
// 			break
// 		case "Remove":
// 			childDevicesRemove()
// 			break
// 		default:
// 			logWarn "childDevices invalid input: ${str}"
// 	}
// }

/*** Child Creation Functions ***/
void childDevicesCreate() {
	if (childDevices) return

	logDebug "Creating Child Device for RELAY"

	String deviceType = "Child Central Scene Switch"
	String deviceTypeBak = "Generic Component Central Scene Switch"
	String dni = "${device.deviceNetworkId}-1"
	Map properties = [isComponent: false, name: "${device.name} RELAY"]

	def child
	try {
		child = addChildDevice(deviceType, dni, properties)
	}
	catch (e) {
		logWarn "The '${deviceType}' driver failed, using '${deviceTypeBak}' instead"
		child = addChildDevice("hubitat", deviceTypeBak, dni, properties)
	}

	child.sendEvent(name:"numberOfButtons", value:5, displayed:false)
}

// void childDevicesRemove() {
// 	logDebug "childDevicesRemove..."
// 	childDevices.each { child ->
// 		deleteChildDevice(child.deviceNetworkId)
// 	}
// }

/*** Child Common Functions ***/
Map getEndPoints() {
	return (state.endPoints == 2 ? ["dimmer":1, "relay":2] : ["dimmer":0, "relay":1])
}


/*******************************************************************
 *******************************************************************

/*******************************************************************
 ***** Z-Wave Reports (COMMON)
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
	childDevices[0]?.sendEvent(name:"numberOfButtons", value:5)
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

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String fullVersion = String.format("%d.%02d",cmd.firmware0Version,cmd.firmware0SubVersion)
	device.updateDataValue("firmwareVersion", fullVersion)

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	setDevModel(new BigDecimal(fullVersion))
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

String mcAssociationGetCmd(Integer group) {
	return secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: group))
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
	if (paramsList[devModel] == null) updateParamsList()
	if (paramsList[devModel][firmware] == null) updateParamsList()
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

	return paramVal
}

/*** Parameter Helper Functions ***/
//Preference Helpers
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}

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
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
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
