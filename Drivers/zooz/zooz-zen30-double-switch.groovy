/*
 *  Zooz ZEN30 Double Switch
 *    - Model: ZEN30 - All Firmware
 *
 *  Changelog:

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

NOTICE: This file has been modified by *Jeff Page* under compliance with
	the Apache 2.0 License from the original work of *Kevin LaFramboise*.

Below link and changes are for original source (Kevin LaFramboise @krlaframboise)
https://github.com/krlaframboise/SmartThings/blob/master/devicetypes/krlaframboise/zooz-double-switch.src/zooz-double-switch.groovy

 *    1.0.2 (10/15/2020)
 *      - Changed icon from dimmer to light.
 *
 *    1.0.1 (08/10/2020)
 *      - Added ST workaround for S2 Supervision bug with MultiChannel Devices.
 *
 *    1.0 (06/23/2020)
 *      - Initial Release
 *
 *  Copyright 2020-2021 Jeff Page
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

/* ZEN30 (1.05)
CommandClassReport- class:0x25, version:1
CommandClassReport- class:0x26, version:4
CommandClassReport- class:0x55, version:2
CommandClassReport- class:0x59, version:1
CommandClassReport- class:0x5A, version:1
CommandClassReport- class:0x5B, version:3
CommandClassReport- class:0x5E, version:2
CommandClassReport- class:0x60, version:4
CommandClassReport- class:0x6C, version:1
CommandClassReport- class:0x70, version:1
CommandClassReport- class:0x72, version:2
CommandClassReport- class:0x73, version:1
CommandClassReport- class:0x7A, version:4
CommandClassReport- class:0x85, version:2
CommandClassReport- class:0x86, version:3
CommandClassReport- class:0x8E, version:3
CommandClassReport- class:0x9F, version:1
*/

@Field static Map commandClassVersions = [
	0x20: 1,	// Basic (basicv1)
	0x25: 1,	// SwitchBinary (switchbinaryv1)
	0x26: 3,	// Switch Multilevel (switchmultilevelv3) (4)
	0x55: 1,	// Transport Service (transportservicev1) (2)
	0x59: 1,	// Association Grp Info (associationgrpinfov1)
	0x5A: 1,	// Device Reset Locally	(deviceresetlocallyv1)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x5E: 2,	// ZWave Plus Info (zwaveplusinfov2)
	0x60: 4,	// MultiChannel (multichannelv4)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 1,	// Configuration (configurationv1)
	0x7A: 4,	// Firmware Update Md (firmwareupdatemdv4)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x73: 1,	// Power Level (powerlevelv1)
	0x85: 2,	// Association (associationv2)
	0x86: 3,	// Version (versionv3)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
	0x98: 1,	// Security S0 (securityv1)
	0x9F: 1		// Security S2
]

@Field static final int maxAssocGroups = 3
@Field static final int maxAssocNodes = 5

@Field static Map endpoints = ["dimmer":0, "switch":1]

@Field static Map paddleControlOptions = [0:"Normal", 1:"Reverse", 2:"Toggle Mode"]
@Field static Map ledModeOptions = [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"]
@Field static Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]
@Field static Map ledBrightnessOptions = [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"]
@Field static Map disabledEnabledOptions = [0:"Disabled", 1:"Enabled"]
@Field static Map ledSceneControlOptions = [0:"LED Enabled", 1:"LED Disabled"]
@Field static Map autoOnOffIntervalOptions = [0:"Disabled", 1:"1 Minute", 2:"2 Minutes", 3:"3 Minutes", 4:"4 Minutes", 5:"5 Minutes", 6:"6 Minutes", 7:"7 Minutes", 8:"8 Minutes", 9:"9 Minutes", 10:"10 Minutes", 15:"15 Minutes", 20:"20 Minutes", 25:"25 Minutes", 30:"30 Minutes", 45:"45 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 240:"4 Hours", 300:"5 Hours", 360:"6 Hours", 420:"7 Hours", 480:"8 Hours", 540:"9 Hours", 600:"10 Hours", 720:"12 Hours", 1080:"18 Hours", 1440:"1 Day", 2880:"2 Days", 4320:"3 Days", 5760:"4 Days", 7200:"5 Days", 8640:"6 Days", 10080:"1 Week", 20160:"2 Weeks", 30240:"3 Weeks", 40320:"4 Weeks", 50400:"5 Weeks", 60480:"6 Weeks"]
@Field static Map powerFailureOptions = [0:"Dimmer Off / Relay Off", 1:"Dimmer Off / Relay On", 2:"Dimmer On / Relay Off", 3:"Dimmer Restored / Relay Restored", 4:"Dimmer Restored / Relay On", 5:"Dimmer Restored / Relay Off", 6:"Dimmer On / Relay Restored", 7:"Dimmer Off / Relay Restored", 8:"Dimmer On / Relay On"]
@Field static Map rampRateOptions = [1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds", 20:"20 Seconds", 25:"25 Seconds", 30:"30 Seconds", 45:"45 Seconds", 60:"60 Seconds", 75:"75 Seconds", 90:"90 Seconds"]
@Field static Map brightnessOptions = [1:"1%", 5:"5%", 10:"10%", 15:"15%", 20:"20%", 25:"25%", 30:"30%", 35:"35%", 40:"40%", 45:"45%", 50:"50%", 55:"55%",60:"60%", 65:"65%", 70:"70%", 75:"75%", 80:"80%", 85:"85%", 90:"90%", 95:"95%", 99:"99%"]
@Field static Map doubleTapBrightnessOptions = [0:"Full Brightness (100%)", 1:"Maximum Brightness Parameter"]
@Field static Map doubleTapFunctionOptions = [0:"Full/Maximum Brightness", 1:"Disabled, Single Tap Last Brightness (or Custom)", 2:"Disabled, Single Tap Full/Maximum Brightness"]
@Field static Map loadControlOptions = [1:"Enable Paddle and Z-Wave", 0:"Disable Physical Paddle Control", 2:"Disable Paddle and Z-Wave Control"]
@Field static Map physicalDisabledBehaviorOptions = [0:"Reports Status & Changes LED Always", 1:"Doesn't Report Status or Change LED"]
@Field static Map zwaveRampRateOptions = [0:"Match Physical Ramp Rate", 1:"Z-Wave Can Set Ramp Rate [RECOMMENDED]"]

metadata {
	definition (
		name: "Zooz ZEN30 Double Switch",
		namespace: "jtp10181",
		author: "Jeff Page / Kevin LaFramboise (@krlaframboise)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen30-double-switch.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "SwitchLevel"
		capability "Configuration"
		capability "Refresh"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"
		//capability "DoubleTapableButton"

		command "childDevices", [[name:"Select One*", type: "ENUM", constraints: ["Create","Remove"] ]]
		command "paramCommands", [[name:"Select Command*", type: "ENUM", constraints: ["Refresh","Test All","Hide Invalid","Clear Hidden"] ]]
		command "setLED", [
			[name:"Select LED*", type: "ENUM", constraints: ["Dimmer","Relay"] ],
			[name:"Select Color*", type: "ENUM", constraints: ledColorOptions] ]
		command "setLEDMode", [
			[name:"Select LED*", type: "ENUM", constraints: ["Dimmer","Relay"] ],
			[name:"Select Mode*", description:"This Sets Preference (#2)*", type: "ENUM", constraints: ["Default","Reverse","Off","On"]] ]

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A008", deviceJoinName:"Zooz ZEN30 Double Switch"
	}

	preferences {
		configParams.each { param ->
			createEnumInput("configParam${param.num}", "${param.name} (#${param.num}):", param.value, param.options)
		}

		input "assocDNI2", "string",
			title: "Device Associations - Group 2 (Dimmer):",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
			required: false

		input "assocDNI3", "string",
			title: "Device Associations - Group 3 (Relay):",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
			required: false

		input "supervisionGetEncap", "bool",
			title: "Supervision Encapsulation Support:",
			description: "This can increase reliability when the device is paired with security. If the device is not operating normally with this on, turn it back off.",
			defaultValue: false

		input "levelCorrection", "bool",
			title: "Brightness Correction:",
			description: "Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%",
			defaultValue: false

		//Logging options similar to other Hubitat drivers
		input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: false
		input name: "debugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: true
	}
}

void createEnumInput(String name, String title, Integer defaultVal, Map options) {
	input name, "enum",
		title: title,
		required: false,
		defaultValue: defaultVal.toString(),
		options: options
}

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings?."assocDNI$grp"
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

void paramCommands(str) {
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

	Map param = configParams.find { it.num == key }
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

	sendEvent(name: "WARNING", value: "COMPLETE - RELOAD THE PAGE!", isStateChange: true)
}

void paramsClearHidden() {
	logDebug "Clearing Hidden Parameters"
	state.remove("tmpLastTest")
	state.remove("tmpFailedTest")
	device.removeDataValue("configHide")
	updateSyncingStatus()

	sendEvent(name: "WARNING", value: "COMPLETE - RELOAD THE PAGE!", isStateChange: true)
}

void setLED(String which, String colorName) {
	def param = (which =="Dimmer") ? dimmerLedColorParam : relayLedColorParam
	Integer paramVal = ledColorOptions.find{ it.value?.toUpperCase() == colorName?.toUpperCase() }?.key
	logDebug "Indicator Color Value (${which}) [${colorName} : ${paramVal}]"
	//Set the Preference to match new setting, then send command to device
	device.updateSetting("configParam${param.num}",[value:"${paramVal}", type:"enum"])
	sendCommands(configSetGetCmd(param, paramVal))
}

void setLEDMode(String which, String modeName) {
	def param = (which =="Dimmer") ? dimmerLedModeParam : relayLedModeParam
	Map modeMap = ["default":0,"reverse":1,"off":2,"on":3]
	Integer paramVal = modeMap[modeName?.toLowerCase()] ?: 0
	logDebug "Indicator Value (${which}) [${modeName} : ${paramVal}]"
	//Set the Preference to match new setting, then send command to device
	device.updateSetting("configParam${param.num}",[value:"${paramVal}", type:"enum"])
	sendCommands(configSetGetCmd(param, paramVal))
}


def installed() {
	log.warn "installed..."
	initialize()
}


def updated() {
	if (!isDuplicateCommand(state.lastUpdated, 5000)) {
		state.lastUpdated = new Date().time

		log.info "updated..."
		log.warn "Debug logging is: ${debugEnable == true}"
		log.warn "Description logging is: ${txtEnable == true}"

		if (debugEnable) runIn(1800, debugLogsOff)

		initialize()

		runIn(1, executeConfigureCmds)
	}
}

void initialize() {
	if (!device.currentValue("numberOfButtons")) {
		sendEvent(name:"numberOfButtons", value:10, displayed:false)
	}

	childDevicesCreate()
}


def configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	sendEvent(name:"numberOfButtons", value:10, displayed:false)
	childDevices[0]?.sendEvent(name:"numberOfButtons", value:5, displayed:false)

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		state.resyncAll = true
	}

	updateSyncingStatus()
	runIn(2, executeRefreshCmds)
	runIn(5, updateSyncingStatus)
	runIn(8, executeConfigureCmds)
}


void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."
	runIn(6, refreshSyncStatus)

	List<String> cmds = []

	if (!device.currentValue("switch")) {
		cmds << switchMultilevelGetCmd()
	}

	if (state.resyncAll || !firmwareVersion) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = param.value
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

void childDevices(str) {
	switch (str) {
		case "Create":
			childDevicesCreate()
			break
		case "Remove":
			childDevicesRemove()
			break
		default:
			log.warn "childDevices invalid input: ${str}"
	}
}

void childDevicesCreate() {
	if (childDevices) return

	logDebug "Creating Child Device for RELAY"

	String deviceType = "Child Central Scene Switch"
	String deviceTypeBak = "Generic Component Central Scene Switch"
	String dni = "${device.deviceNetworkId}-1"
	Map properties = [isComponent: true, name: "${device.name} RELAY"]

	def child
	try {
		child = addChildDevice(deviceType, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed, using '${deviceTypeBak}' instead"
		child = addChildDevice("hubitat", deviceTypeBak, dni, properties)
	}

	child.sendEvent(name:"numberOfButtons", value:5, displayed:false)
}

void childDevicesRemove() {
	logDebug "childDevicesRemove..."
	childDevices.each { child ->
		deleteChildDevice(child.deviceNetworkId)
	}
}

void clearVariables() {
	log.warn "Clearing state variables and data..."

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
}

void debugLogsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}


private getConfigureAssocsCmds() {
	List<String> cmds = []

	//This is BROKEN in ZEN30 due to factory set MultiChannel Association
	// if (!state.group1Assoc || state.resyncAll) {
	// 	if (state.group1Assoc == false) {
	// 		logDebug "Adding missing lifeline association..."
	// 	}
	// 	cmds << associationSetCmd(1, [zwaveHubNodeId])
	// 	cmds << associationGetCmd(1)
	// }

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

		List newNodeIds = settingNodeIds?.findAll { !(it in state."assocNodes$i") }
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
	def nodeIds = convertHexListToIntList(dni?.split(","))

	if (dni && !nodeIds) {
		log.warn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds?.size() > maxAssocNodes) {
		log.warn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}


def on() {
	logDebug "on..."
	return getSetLevelCmds(null)
}

def off() {
	logDebug "off..."
	return getSetLevelCmds(0x00)
}


def setLevel(level) {
	logDebug "setLevel($level)..."
	return getSetLevelCmds(level)
}


def setLevel(level, duration) {
	logDebug "setLevel($level, $duration)..."
	return getSetLevelCmds(level, duration)
}

String getSetLevelCmds(level, duration=null) {
	if (level == null) {
		level = device.currentValue("level")
	}
	
	if (level)  level = convertLevel(level, true)

	Integer levelVal = validateRange(level, 99, 0, 99)
	Integer durationVal = validateRange(duration, rampRateParam.value, 0, 99)

	return switchMultilevelSetCmd(levelVal, durationVal)
}


def refresh() {
	logDebug "refresh..."
	executeRefreshCmds()
}

void executeRefreshCmds() {
	updateSyncingStatus()

	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << switchMultilevelGetCmd()
	cmds << switchBinaryGetCmd()

	sendCommands(cmds)
}

void paramsRefresh() {
	updateSyncingStatus()

	List<String> cmds = []
	for (int i = 1; i <= maxAssocGroups; i++) {
		cmds << associationGetCmd(i)
	}
	
	configParams.each { param ->
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

// Child Device Methods
def componentOn(cd) {
	logDebug "componentOn from ${cd.displayName}"
	state.pendingRelay = true
	sendCommands(switchBinarySetCmd(0xFF))
}

def componentOff(cd) {
	logDebug "componentOff from ${cd.displayName}"
	state.pendingRelay = true
	sendCommands(switchBinarySetCmd(0x00))
}

def componentRefresh(cd) {
	logDebug "componentRefresh from ${cd.displayName}"
	executeRefreshCmds()
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
	return supervisionEncap(zwave.switchBinaryV1.switchBinarySet(switchValue: value), endpoints.switch)
}

String switchBinaryGetCmd() {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), endpoints.switch)
}

String switchMultilevelSetCmd(Integer value, Integer duration) {
	return supervisionEncap(zwave.switchMultilevelV3.switchMultilevelSet(dimmingDuration: duration, value: value), endpoints.dimmer)
}

String switchMultilevelGetCmd() {
	return secureCmd(zwave.switchMultilevelV3.switchMultilevelGet(), endpoints.dimmer)
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
@Field static Map<String, Map<Short, String>> supervisedPackets = [:]
@Field static Map<String, Short> sessionIDs = [:]

String supervisionEncap(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "supervisionEncap: ${cmd} (ep ${ep})"

	if (settings?.supervisionGetEncap) {
		//Encap with SupervisionGet
		Short sessId = getSessionId()
		def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sessId).encapsulate(cmd)

		//Encap that with MultiChannel now so it is cached that way below
		cmdEncap = multiChannelEncap(cmdEncap, ep)

		logDebug "New Supervised Packet for Session: ${sessId}"
		if (supervisedPackets["${device.id}"] == null) { supervisedPackets["${device.id}"] = [:] }
		supervisedPackets["${device.id}"][sessId] = cmdEncap

		//Calculate supervisionCheck delay based on how many cached packets
		Integer packetsCount = supervisedPackets?."${device.id}"?.size()
		Integer delayTotal = (packetsCount * 500) + 2000
		runInMillis(delayTotal, supervisionCheck, [data:1])

		//Already handled MC so don't send endpoint here
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
		supervisedPackets["${device.id}"].each { k, v ->
			log.warn "Re-Sending Supervised Session: ${k} (Retry #${num})"
			sendCommands(secureCmd(v))
		}

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
	logTrace "parse: ${description} --PARSED-- ${cmd}"

	if (cmd) {
		zwaveEvent(cmd)
	} else {
		log.warn "Unable to parse: $description"
	}

	updateLastCheckIn()
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

String convertToLocalTimeString(dt) {
	try {
		def timeZoneId = location?.timeZone?.ID
		if (timeZoneId) {
			return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
		}
		else {
			return "$dt"
		}
	}
	catch (ex) {
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

	Map param = configParams.find { it.num == cmd.parameterNumber }

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
	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"

	device.updateDataValue("firmwareVersion", fullVersion)
}


void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "physical", ep)
}


void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	
	String type = (state.pendingRelay ? "digital" : "physical")
	state.remove("pendingRelay")
	
	sendSwitchEvents(cmd.value, type, ep)
}


void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, "digital", ep)
}


void sendSwitchEvents(rawVal, String type, Integer ep) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value} (${type})"
	sendEventIfNew("switch", value, true, type, "", desc, ep)

	if (rawVal && ep == endpoints.dimmer) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		level = convertLevel(level, false)

		desc = "level was set to ${level}% (${type})"
		if (levelCorrection) desc += " [actual: ${rawVal}]"
		sendEventIfNew("level", level, true, type, "%", desc, ep)
	}
}


void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep=0){
	if (state.lastSequenceNumber != cmd.sequenceNumber) {
		state.lastSequenceNumber = cmd.sequenceNumber

		logTrace "${cmd} (ep ${ep})"

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
			case 3:
				actionType = "relay"
				scene.value = 1
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
				else if (cmd.sceneNumber == 3) scene.value = cmd.keyAttributes - 1
				btnVal = "${actionType} ${cmd.keyAttributes - 1}x"
				break
			default:
				logDebug "Unknown keyAttributes: ${cmd}"
		}

		if (actionType && btnVal) {
			scene.descriptionText="button ${scene.value} ${scene.name} [${btnVal}]"
			if (cmd.sceneNumber < 3) {
				logTxt "${scene.descriptionText}"
				sendEvent(scene)
			}
			else if (childDevices) {
				def child = childDevices[0]
				child.parse([scene])
			}
		}
	}
}


void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


void updateSyncingStatus() {
	runIn(4, refreshSyncStatus)
	sendEventIfNew("syncStatus", "Syncing...", false)
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventIfNew("syncStatus", (changes ?  "${changes} Pending Changes" : "Synced"), false)
}


Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = param.value
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
	String configsStr = device?.getDataValue("configVals")

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

List<Map> getConfigParams() {
	def params = [
		dimmerLedModeParam,
		relayLedModeParam,
		dimmerLedColorParam,
		relayLedColorParam,
		dimmerLedBrightnessParam,
		relayLedBrightnessParam,
		ledSceneControlParam,
		dimmerPaddleControlParam,

		dimmerAutoOffParam,
		relayAutoOffParam,
		dimmerAutoOnParam,
		relayAutoOnParam,

		powerFailureParam,
		rampRateParam,
		//zwaveRampRateParam, //Removed in firmware 1.05
		minimumBrightnessParam,
		maximumBrightnessParam,
		doubleTapBrightnessParam,
		doubleTapFunctionParam,

		dimmerLoadControlParam,
		relayLoadControlParam,
		dimmerPhysicalDisabledBehaviorParam,
		relayPhysicalDisabledBehaviorParam,

		holdRampRateParam,
		customBrightnessParam,
		nightLightParam
	]

	//Remove Hidden Invalid Params
	String configHide = device?.getDataValue("configHide")
	if (configHide != null) {
		List configDisabled = evaluate(configHide)
		params.removeAll { configDisabled.contains(it.num) }
	}

	//Remove Params not supported with this firmware
	BigDecimal firmware = firmwareVersion
	params.removeAll { !firmwareSupportsParam(firmware, it) }

	return params
}

Map getDimmerLedModeParam() {
	return getParam(1, "Dimmer LED Indicator Mode", 1, 0, ledModeOptions)
}

Map getRelayLedModeParam() {
	return getParam(2, "Relay LED Indicator Mode", 1, 0, ledModeOptions)
}

Map getDimmerLedColorParam() {
	return getParam(3, "Dimmer LED Indicator Color", 1, 0, ledColorOptions)
}

Map getRelayLedColorParam() {
	return getParam(4, "Relay LED Indicator Color", 1, 0, ledColorOptions)
}

Map getDimmerLedBrightnessParam() {
	return getParam(5, "Dimmer LED Indicator Brightness", 1, 1, ledBrightnessOptions)
}

Map getRelayLedBrightnessParam() {
	return getParam(6, "Relay LED Indicator Brightness", 1, 1, ledBrightnessOptions)
}

Map getLedSceneControlParam() {
	return getParam(7, "LED Indicator for Scene Selections", 1, 1, ledSceneControlOptions)
}

Map getDimmerAutoOffParam() {
	return getParam(8, "Dimmer Auto Turn-Off Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getDimmerAutoOnParam() {
	return getParam(9, "Dimmer Auto Turn-On Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getRelayAutoOffParam() {
	return getParam(10, "Relay Auto Turn-Off Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getRelayAutoOnParam() {
	return getParam(11, "Relay Auto Turn-On Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getPowerFailureParam() {
	return getParam(12, "Behavior After Power Failure", 1, 3, powerFailureOptions)
}

Map getRampRateParam() {
	Map options = [0:"Instant On/Off"]
	options << rampRateOptions
	return getParam(13, "Dimmer Ramp Rate to Full On/Off", 1, 1, options)
}

Map getMinimumBrightnessParam() {
	return getParam(14, "Dimmer Minimum Brightness", 1, 1, brightnessOptions)
}

Map getMaximumBrightnessParam() {
	return getParam(15, "Dimmer Maximum Brightness", 1, 99, brightnessOptions)
}

Map getDoubleTapBrightnessParam() {
	return getParam(17, "Dimmer Double Tap Up Brightness", 1, 0, doubleTapBrightnessOptions)
}

Map getDoubleTapFunctionParam() {
	return getParam(18, "Dimmer Double Tap Up Function", 1, 0, doubleTapFunctionOptions)
}

Map getDimmerLoadControlParam() {
	return getParam(19, "Smart Bulb Mode - Dimmer Load", 1, 1, loadControlOptions)
}

Map getRelayLoadControlParam() {
	return getParam(20, "Smart Bulb Mode - Relay Load", 1, 1, loadControlOptions)
}

Map getHoldRampRateParam() {
	return getParam(21, "Dimming Speed when Paddle is Held", 1, 5, rampRateOptions)
}

// Removed in firmware v1.05
// Map getZwaveRampRateParam() {
// 	return getParam(22, "Dimmer Z-Wave Ramp Rate", 1, 0, zwaveRampRateOptions)
// }

Map getCustomBrightnessParam() {
	Map options = [0:"Last Brightness Level"]
	options += brightnessOptions
	return getParam(23, "Custom Brightness when Turned On", 1, 0, options)
}

// Added in firmware v1.05
Map getDimmerPhysicalDisabledBehaviorParam() {
	return getParam(24, "Smart Bulb - Dimmer when Physical Disabled", 1, 0, physicalDisabledBehaviorOptions, 1.05)
}

// Added in firmware v1.05
Map getRelayPhysicalDisabledBehaviorParam() {
	return getParam(25, "Smart Bulb - Relay when Physical Disabled", 1, 0, physicalDisabledBehaviorOptions, 1.05)
}

// Added in firmware v1.05
Map getNightLightParam() {
	Map options = [0:"Disabled"]
	options << brightnessOptions
	return getParam(26, "Night Light Brightness", 1, 20, options, 1.05)
}

// Added in firmware v1.05
Map getDimmerPaddleControlParam() {
	return getParam(27, "Paddle Orientation for Dimmer", 1, 0, paddleControlOptions, 1.05)
}


Map getParam(Integer num, String name, Integer size, Integer defaultVal, Map options, BigDecimal minVer=null) {
	Integer val = safeToInt(settings?."configParam${num}", defaultVal)
	Map retMap = [num: num, name: name, size: size, value: val, options: options, minVer: minVer]

	if (options) {
		retMap.valueName = options?.find { k, v -> "${k}" == "${val}" }?.value
		retMap.options = setDefaultOption(options, defaultVal)
	}

	return retMap
}

Map setDefaultOption(Map options, Integer defaultVal) {
	return options?.collectEntries { k, v ->
		if ("${k}" == "${defaultVal}") {
			v = "${v} [DEFAULT]"
		}
		["$k": "$v"]
	}
}


void sendEventIfNew(String name, value, boolean displayed=true, String type=null, String unit="", String desc=null, Integer ep=0) {
	if (desc == null) desc = "${name} set to ${value}${unit}"
	String logEp = (ep ? "(RELAY) " : "")
	def eventDev = (ep ? childDevices[0] : device)

	if (!eventDev) {
		log.error "No device for endpoint (${ep}). Use command button to create child devices."
	}
	else if (eventDev.currentValue(name).toString() != value.toString()) {
		Map evt = [name: name, value: value, descriptionText: desc, displayed: displayed]

		if (type) evt.type = type
		if (unit) evt.unit = unit

		if (ep) {
			eventDev.parse([evt])
		}
		else {
			if (name != "syncStatus") logTxt(desc)
			eventDev.sendEvent(evt)
		}
	}
	else if (name != "syncStatus") {
		logDebug "${logEp}${desc} [NOT CHANGED]"
	}
}


boolean firmwareSupportsParam(BigDecimal firmware, Map param) {
	return (firmware >= param.minVer ?: 0)
}

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}


private convertIntListToHexList(intList) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(2, "0").toUpperCase())
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


Integer convertLevel(level, userLevel=false) {
	if (levelCorrection) {
		Integer brightmax = safeToInt(maximumBrightnessParam.value, 99)
		Integer brightmin = safeToInt(minimumBrightnessParam.value, 1)
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
	}
	else if (intVal < lowVal) {
		return lowVal
	}
	else {
		return intVal
	}
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
