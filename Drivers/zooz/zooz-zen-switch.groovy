/*
 *  Zooz ZEN On/Off Switches Universal
 *    - Model: ZEN21, ZEN23 - MINIMUM FIRMWARE 3.04
 *    - Model: ZEN26 - MINIMUM FIRMWARE 2.03
 *    - Model: ZEN71, ZEN73, ZEN76 - All Firmware
 *
 *  Changelog:

## [1.5.1] - 2022-03-22 (@jtp10181)
  ### Changed
  - Description text loging enabled by default
  ### Fixed
  - Added inClusters to fingerprint so it will be selected by default
  - threeWaySwitchType options corrected between switches and dimmers
  - Parameter #7 removed from ZEN71/72 which do not ave that option
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

 *  Copyright 2020-2021 Jeff Page
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

@Field static Map commandClassVersions = [
	0x20: 1,	// Basic (basicv1)
	0x25: 1,	// Switch Binary (switchbinaryv1)
	0x55: 1,	// Transport Service (transportservicev1) (2)
	0x59: 1,	// Association Grp Info (associationgrpinfov1)
	0x5A: 1,	// Device Reset Locally	(deviceresetlocallyv1)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x5E: 2,	// ZWave Plus Info (zwaveplusinfov2)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 1,	// Configuration (configurationv1)
	0x7A: 4,	// Firmware Update Md (firmwareupdatemdv4)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x73: 1,	// Power Level (powerlevelv1)
	0x85: 2,	// Association (associationv2)
	0x86: 3,	// Version (versionv3)
	0x87: 3,	// Indicator (indicatorv3)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
	0x9F: 1		// Security S2
]

@Field static final int maxAssocGroups = 3
@Field static final int maxAssocNodes = 5

@Field static final String VERSION = "1.5.0" 
@Field static Map deviceModelNames =
	["B111:1E1C":"ZEN21", "B111:251C":"ZEN23", "A000:A001":"ZEN26", 
	"7000:A001":"ZEN71", "7000:A003":"ZEN73", "7000:A006":"ZEN76"]

@Field static Map ledModeCmdOptions = [0:"Default", 1:"Reverse", 2:"Off", 3:"On"]
@Field static Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]

metadata {
	definition (
		name: "Zooz ZEN Switch Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen-switch.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Refresh"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"
		//capability "DoubleTapableButton"

		command "paramCommands", [[name:"Select Command*", type: "ENUM", constraints: ["Refresh","Test All","Hide Invalid","Clear Hidden"] ]]
		command "setLED", [
			[name:"Select Color*", description:"Works ONLY on ZEN7x Series!", type: "ENUM", constraints: ledColorOptions] ]
		command "setLEDMode", [
			[name:"Select Mode*", description:"This Sets Preference (#2)*", type: "ENUM", constraints: ledModeCmdOptions] ]

		//DEBUGGING
		//command "debugShowVars"

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"B111", deviceId:"1E1C", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN21 Switch"
		fingerprint mfr:"027A", prod:"B111", deviceId:"251C", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN23 Switch"
		fingerprint mfr:"027A", prod:"A000", deviceId:"A001", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN26 S2 Switch"
		fingerprint mfr:"027A", prod:"7000", deviceId:"A001", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN71 Switch"
		fingerprint mfr:"027A", prod:"7000", deviceId:"A003", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN73 Switch"
		fingerprint mfr:"027A", prod:"7000", deviceId:"A006", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN76 S2 Switch"
	}

	preferences {
		configParams.each { param ->
			if (!param.hidden) {
				input "configParam${param.num}", "enum",
					title: "${param.title} (#${param.num}):",
					description: param?.description,
					defaultValue: param.value,
					options: param.options,
					required: false
			}
		}

		input "assocDNI2", "string",
			title: "Device Associations - Group 2:",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
			required: false

		input "assocDNI3", "string",
			title: "Device Associations - Group 3:",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
			required: false

		input "supervisionGetEncap", "bool",
			title: "Supervision Encapsulation (Experimental):",
			description: "This can increase reliability when the device is paired with security, but may not work correctly on all models.",
			defaultValue: false

		input "sceneReverse", "bool",
			title: "Scene Up-Down Reversal:",
			description: "If the button numbers and up/down descriptions are backwards in the scene button events change this setting to fix it!",
			defaultValue: true

		//Logging options similar to other Hubitat drivers
		input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: true
		input name: "debugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: true
	}
}

void debugShowVars() {
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
}


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
	ledColor: [ num: 14,
		title: "LED Indicator Color",
		size: 1, defaultVal: 1,
		options: [:], //ledColorOptions,
		changes: ['2X':[num:null]]
	],
	ledBrightness: [ num: 15,
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
		options: [:], //autoOnOffIntervalOptions,
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
		options: [:], //autoOnOffIntervalOptions,
		changes: ['7X':[num:5]]
	],
	powerFailure: [ num: 8,
		title: "Behavior After Power Failure", 
		size: 1, defaultVal: 2, 
		options: [2:"Restores Last Status", 0:"Forced to Off", 1:"Forced to On"],
	],
	//sceneControl - Dimmers=13, ZEN26/73/76=10, Other Switches=9
	sceneControl: [ num: 9,
		title: "Scene Control Events",
		description: "Enable to report pushed and multi-tap events",
		size: 1, defaultVal: 0,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [26:[num: 10], 73:[num: 10], 76:[num: 10]]
	],
	//loadControlParam - Dimmers=15, ZEN73/76=12, Other Switches=11
	loadControl: [ num: 11,
		title: "Smart Bulb Mode - Load Control",
		size: 1, defaultVal: 1,
		options: [1:"Enable Paddle and Z-Wave", 0:"Disable Paddle Control", 2:"Disable Paddle and Z-Wave Control"],
		changes: [73:[num: 12], 76:[num: 12]]
	],
	smartBulbBehavior: [ num: 13, // relayBehaviorParam
		title: "Smart Bulb - On/Off when Paddle Disabled",
		size: 1, defaultVal: 0, 
		options: [0:"Reports Status & Changes LED", 1:"Doesn't Report Status or Change LED"],
	],
	//threeWaySwitchType - ZEN21/22/23/24/71/72 Only
	threeWaySwitchType: [num: null, // (12)
		title: "3-Way Switch Type",
		size: 1, defaultVal: 0, 
		options: [0:"Toggle On/Off Switch", 1:"Momentary Switch"],
		changes: [21:[num: 12],23:[num: 12],71:[num: 12]]
	],
	paddleProgramming: [ num: null,
		title: "Programming from the Paddle",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		changes: [21:[num: null, firmVer:null], 23:[num: 15, firmVer:4.04], 26:[num: 15, firmVer:3.41],
			22:[num: 24, firmVer:4.04], 24:[num: 24, firmVer:4.04], 27:[num: 24, firmVer:3.04],
			71:[num: 17, firmVer:10.0], 73:[num: 17, firmVer:10.0], 76:[num: 17, firmVer:10.0],
			72:[num: 26, firmVer:10.0], 74:[num: 26, firmVer:10.0], 77:[num: 26, firmVer:10.0]
		],
	],
	associationReports: [ num: 7,
		title: "Send Status Report to Associations on", 
		size: 1, defaultVal: 15, 
		options: [ 0:"None", 1:"Physical Tap On ZEN Only", 2:"Physical Tap On Connected 3-Way Switch Only", 3:"Physical Tap On ZEN / 3-Way Switch",
			4:"Z-Wave Command From Hub", 5:"Physical Tap On ZEN / Z-Wave Command", 6:"Physical Tap On 3-Way Switch / Z-Wave Command",
			7:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command", 8:"Timer Only", 9:"Physical Tap On ZEN / Timer",
			10:"Physical Tap On 3-Way Switch / Timer", 11:"Physical Tap On ZEN / 3-Way Switch / Timer", 12:"Z-Wave Command From Hub / Timer",
			13:"Physical Tap On ZEN / Z-Wave Command / Timer", 14:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command / Timer",
			15:"All Of The Above" 
		],
		changes: [71:[num:null], 72:[num:null]]
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
	statusReports: [ num: null,
		title: "All Reports Use SwitchBinary", 
		size: 1, defaultVal: 0, 
		options: [0:"Disabled", 1:"Enabled"],
		hidden: true,
		changes: [71:[num: 16], 73:[num: 16], 76:[num: 16]]
	],
]

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "") 
}

void push(buttonId) { sendBasicButtonEvent(buttonId, "pushed") }
void hold(buttonId) { sendBasicButtonEvent(buttonId, "held") }
void release(buttonId) { sendBasicButtonEvent(buttonId, "released") }
void doubleTap(buttonId) { sendBasicButtonEvent(buttonId, "doubleTapped") }

void sendBasicButtonEvent(BigDecimal buttonId, String name) {
	Map event = [name: name, value: buttonId, type:"digital", isStateChange:true]
	event.descriptionText="button ${buttonId} ${name}"
	logTxt "${event.descriptionText} (${event.type})"
	sendEvent(event)
}

void paramCommands(String str) {
	switch (str) {
		case "Refresh":
			paramsRefresh()
			break
		case "Test All":
			state.tmpFailedTest = []
			paramsTestAll()
			break
		case "Hide Invalid":
			paramsHideInvalid()
			break
		case "Clear Hidden":
			paramsClearHidden()
			break
		default:
			log.warn "paramCommands invalid input: ${str}"
	}
}

void paramsTestAll() {
	Map configsMap = getParamStoredMap()
	List lastTest = state.tmpLastTest.collect()
	Integer key = configsMap.find{ !lastTest || it.key > lastTest[0] }?.key
	if (!key) {
		logDebug "Finished Testing All Params"
		runInMillis(1400, paramsHideInvalid)
		return
	}

	Map param = getParam(key)
	Integer val = configsMap.get(key)
	Integer testVal = param.value ?: 1
	state.tmpLastTest = [key, val, testVal, "T"]

	if (!param) {
		state.tmpFailedTest << key
		logDebug "Testing #${key} NOT FOUND in visible Params list"
		runInMillis(400,paramsTestAll)
		return
	}
	else {
		logDebug "Testing Param: [num:${key}, currentVal:${val}, testVal:${testVal}]"
		//Test by setting param and then check response
		sendCommands(configSetGetCmd(param, testVal))
	}
}

void paramsHideInvalid() {
	List configDisabled = state.tmpFailedTest.collect() ?: []
	TreeMap configsMap = getParamStoredMap()

	configParams.each { param ->
		if (!(configsMap.find { it.key.toInteger() == param.num } )) {
			configDisabled << param.num
		}
	}

	if (configDisabled) {
		configDisabled.unique()
		configDisabled.sort()
		logDebug "Disabled Parameters: ${configDisabled}"
		device.updateDataValue("configHide", configDisabled.inspect())

		//Clean up configVals, remove hidden params
		configDisabled.each { configsMap.remove(it) }
		device.updateDataValue("configVals", configsMap.inspect())
		updateSyncingStatus()
	}
	else {
		logDebug "Disabled Parameters: NONE"
	}

	state.remove("tmpLastTest")
	state.remove("tmpFailedTest")
	updateParamsList()

	sendEvent(name: "WARNING", value: "COMPLETE - RELOAD THE PAGE!", isStateChange: true)
}

void paramsClearHidden() {
	logDebug "Clearing Hidden Parameters"
	state.remove("tmpLastTest")
	state.remove("tmpFailedTest")
	device.removeDataValue("configHide")
	updateSyncingStatus()
	updateParamsList()

	sendEvent(name: "WARNING", value: "COMPLETE - RELOAD THE PAGE!", isStateChange: true)
}

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
		log.warn "Indicator Color can only be changed on ZEN7x models"
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
			log.warn "There is No LED Indicator Parameter Found for this model"
	}
}


void installed() {
	log.warn "installed..."
}


def updated() {
	log.info "updated..."
	log.warn "Debug logging is: ${debugEnable == true}"
	log.warn "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	runIn(1, executeConfigureCmds)
}

void initialize() {
	log.warn "initialize..."
	refresh()
}


void configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		state.resyncAll = true
	}

	updateSyncingStatus(8)
	runIn(2, executeRefreshCmds)
	runIn(6, executeConfigureCmds)
}


void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getAdjustedParamValue(param)
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

void clearVariables() {
	log.warn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel 

	//Clears State Variables
	state.clear()

	//Clear Data from other Drivers
	device.removeDataValue("configVals")
	device.removeDataValue("firmwareVersion")
	device.removeDataValue("protocolVersion")
	device.removeDataValue("hardwareVersion")
	device.removeDataValue("serialNumber")
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
}

void debugLogsOff() {
	log.warn "debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
void logsOff() {
	//Do nothing, this is commonly scheduled by other drivers and this prevents an error message.
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

private getAdjustedParamValue(Map param) {
	//Not needed for ZEN7X models
	if (state.deviceModel ==~ /ZEN7\d/) return param.value

	Integer paramVal = param.value
	switch(param.name) {
		case "autoOffEnabled":
			paramVal = getParam("autoOffInterval").value == 0 ? 0 : 1
			break
		case "autoOffInterval":
			paramVal = param.value ?: 60
			break
		case "autoOnEnabled":
			paramVal = getParam("autoOnInterval").value == 0 ? 0 : 1
			break
		case "autoOnInterval":
			paramVal = param.value ?: 60
			break
	}

	return paramVal
}

private getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
		if (!device.currentValue("assocDNI$i")) {
			sendEventIfNew("assocDNI$i", "none", false)
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


private getAssocDNIsSettingNodeIds(grp) {
	def dni = getAssocDNIsSetting(grp)
	def nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		log.warn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		log.warn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

private getDeviceModelShort() {
	return safeToInt(state.deviceModel?.drop(3))
}


def on() {
	logDebug "on..."
	return switchBinarySetCmd(0xFF)
}

def off() {
	logDebug "off..."
	return switchBinarySetCmd(0x00)
}


def refresh() {
	logDebug "refresh..."
	executeRefreshCmds()
}

void executeRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << switchBinaryGetCmd()

	sendCommands(cmds)
}

void paramsRefresh() {
	List<String> cmds = []
	for (int i = 1; i <= maxAssocGroups; i++) {
		cmds << associationGetCmd(i)
	}
	
	configParams.each { param ->
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}


//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=400) {
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
	return secureCmd(zwave.versionV3.versionGet())
}

String switchBinarySetCmd(Integer value) {
	return supervisionEncap(zwave.switchBinaryV1.switchBinarySet(switchValue: value))
}

String switchBinaryGetCmd() {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet())
}

String switchMultilevelSetCmd(Integer value, Integer duration) {
	return supervisionEncap(zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration: duration, value: value))
}

String switchMultilevelGetCmd() {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelGet())
}

String configSetCmd(Map param, Integer value) {
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
		cmd = zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
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

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionReport cmd, ep=0 ) {
	logDebug "Supervision Report - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
	if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }

	switch (cmd.status as Integer) {
		case 0x00: // "No Support"
		case 0x01: // "Working"
		case 0x02: // "Failed"
			log.warn "Supervision NOT Successful - SessionID: ${cmd.sessionID}, Status: ${cmd.status}"
			break
		case 0xFF: // "Success"
			supervisedPackets["${device.id}"].remove(cmd.sessionID)
			break
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
			log.warn "Re-Sending Supervised Session: ${sid} (Retry #${num})"
			cmds << secureCmd(cmd)
		}
		sendCommands(cmds)

		if (num >= 2) { //Clear after this many attempts
			log.warn "Supervision MAX RETIES (${num}) Reached"
			supervisedPackets["${device.id}"].clear()
		}
		else { //Otherwise keep trying
			Integer delayTotal = (packetsCount * 500) + 2000
			runInMillis(delayTotal, supervisionCheck, [data:num+1])
		}
	}
}
//====== Supervision Encapsulate END ======\\


def parse(String description) {
	def cmd = zwave.parse(description, commandClassVersions)

	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else {
		log.warn "Unable to parse: $description"
	}

	//Update Last Activity
	updateLastCheckIn()
	sendEvent(name:"numberOfButtons", value:10, displayed:false)
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

String convertToLocalTimeString(dt) {
	def timeZoneId = location?.timeZone?.ID
	if (timeZoneId) {
		return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
	} else {
		return "$dt"
	}
}


void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, ep)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0), ep))
}


void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)

	//When running Param Test
	List lastTest = state.tmpLastTest
	if (param && lastTest && lastTest[3] == "T") {
		if (param.num == lastTest[0] && cmd.scaledConfigurationValue == lastTest[2]) {
			lastTest[3] = "P"
			logDebug "Testing #${lastTest[0]} PASSED"
		}
		else {
			lastTest[3] = "F"
			state.tmpFailedTest << lastTest[0]
			logDebug "Testing #${lastTest[0]} FAILED - Returned: ${cmd.parameterNumber}:${cmd.scaledConfigurationValue}"
		}
		//Set the param back how it was
		sendCommands(configSetGetCmd(param, lastTest[1]))

		runInMillis(1400, paramsTestAll)
		return
	}

	//Handle normal Param changes
	else if (param) {
		Integer val = cmd.scaledConfigurationValue
		logDebug "${param.name} (#${param.num}) = ${val}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${cmd.scaledConfigurationValue}"
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
		sendEventIfNew("assocDNI$grp", dnis ?: "none", false)
		device.updateSetting("assocDNI$grp", [value:"${dnis}", type:"string"])
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}


void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
	logTrace "${cmd}"

	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"
	device.updateDataValue("firmwareVersion", fullVersion)

	//Stash the model in a state variable
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))],4)
	def devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"
	logDebug "Received Version Report - Model: ${devModel} | Firmware: ${fullVersion}"
	state.deviceModel = devModel

	if (devModel == "UNK00") {
		log.warn "Unsupported Device USE AT YOUR OWN RISK: ${devTypeId}"
		state.WARNING = "Unsupported Device Model - USE AT YOUR OWN RISK!"
	}
	else state.remove("WARNING")

	//Setup parameters if not set
	verifyParamsList()

	if (state.resyncAll) {
		//Disable sceneReverse setting for known cases otherwise set to true (most need it reversed)
		if ((devModel == "ZEN27" && fullVersion == "3.01") ||
		   (devModel == "ZEN22" && fullVersion == "4.01") ||
		   (devModel ==~ /ZEN7\d/))
		{
			logDebug "Scene Reverse switched off, known Model/Firmware match found."
			device.updateSetting("sceneReverse", [value:"false",type:"bool"])
		} else if (settings.sceneReverse == false) {
			log.warn "Scene Reverse is off, not a known Model/Firmware match but leaving how it is set."
		} else {
			logDebug "Scene Reverse is already on, this is the default setting"
		}
	}
}


void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
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


void sendSwitchEvents(rawVal, String type, Integer ep) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value} (${type})"
	sendEventIfNew("switch", value, true, type, "", desc, ep)
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

		Map scene = [name: "pushed", value: cmd.sceneNumber, descriptionText: "", type:"physical", isStateChange:true]
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
			scene.descriptionText="button ${scene.value} ${scene.name} [${btnVal}]"
			logTxt "${scene.descriptionText}"
			sendEvent(scene)
		}
	}
}


void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEventIfNew("syncStatus", "Syncing...", false)
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventIfNew("syncStatus", (changes ?  "${changes} Pending Changes" : "Synced"), false)
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getAdjustedParamValue(param)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	//Integer group1Assoc = (state.group1Assoc != true) ? 1 : 0
	return (configChanges + pendingAssocs)
}

Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Integer value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	Map configsMap = [:]
	String configsStr = device.getDataValue("configVals")

	if (configsStr) {
		try {
			configsMap = evaluate(configsStr)
		}
		catch(Exception e) {
			log.warn("Clearing Invalid configVals: ${e}")
			device.removeDataValue("configVals")
		}
	}
	return configsMap
}

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
			if (m == modelNum || m ==~ /${modelSeries}X/) {
				tmpMap.putAll(changes)
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

	//Remove Hidden Invalid Params
	String configHide = device.getDataValue("configHide")
	if (configHide != null) {
		List configDisabled = evaluate(configHide)
		tmpList.removeAll { configDisabled.contains(it.num) }
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

void fixParamsMap() {
	paramsMap.ledColor.options << ledColorOptions
	paramsMap.autoOffInterval.options << autoOnOffIntervalOptions
	paramsMap.autoOnInterval.options << autoOnOffIntervalOptions
	paramsMap['settings'] = [fixed: true]
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (!devModel) return []

	verifyParamsList()
	List<Map> params = []
	paramsList[devModel][firmware].each { params << it.clone() }

	//Get current values
	params.each {
		it.put("value", safeToInt(settings."configParam${it.num}", it.defaultVal))
	}

	return params
}

//Get a single param by name or number
Map getParam(def search) {
	//logDebug "Get Param (${search} | ${search.class})"
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	Map param = [:]

	verifyParamsList()
	if (search instanceof String) {
		param = paramsList[devModel][firmware].find{ it.name == search }
	} else {
		param = paramsList[devModel][firmware].find{ it.num == search }
	}

	//Update current value
	if (param && param?.num) {
		param = param.clone()
		param.put("value", safeToInt(settings."configParam${param.num}", param.defaultVal))
	}

	return param
}


void sendEventIfNew(String name, value, boolean displayed=true, String type=null, String unit="", String desc=null, Integer ep=0) {
	if (desc == null) desc = "${name} set to ${value}${unit}"

	Map evt = [name: name, value: value, descriptionText: desc, displayed: displayed]
	if (type) evt.type = type
	if (unit) evt.unit = unit

	if (name != "syncStatus") {
		if (device.currentValue(name).toString() != value.toString()) {
			logTxt(desc)
		} else {
			logDebug "${desc} [NOT CHANGED]"
		}
	}

	//Always send event to update last activity
	sendEvent(evt)
}


BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}


private convertIntListToHexList(intList, pad=2) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(pad, "0").toUpperCase())
	}
	return hexList
}

private convertHexListToIntList(String[] hexList) {
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


Integer safeToInt(val, Integer defaultVal=0) {
	return "${val}"?.isInteger() ? "${val}".toInteger() : defaultVal
}


boolean isDuplicateCommand(lastExecuted, allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}


void logDebug(String msg) {
	if (debugEnable) log.debug "${device.displayName}: ${msg}"
}

void logTxt(String msg) {
	if (txtEnable) log.info "${device.displayName}: ${msg}"
}

//For Extreme Code Debugging - tracing commands
void logTrace(String msg) {
	//Uncomment to Enable
	//log.trace "${device.displayName}: ${msg}"
}
