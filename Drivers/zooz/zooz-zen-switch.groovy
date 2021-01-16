/*
 *  Zooz ZEN On/Off Switches Universal
 *    - Model: ZEN21/23 - MINIMUM FIRMWARE 3.04
 *    - Model: ZEN26 - MINIMUM FIRMWARE 2.03
 *    - Model: ZEN71
 *    - Model: ZEN76
 *
 *  Changelog:

## 1.4.1-beta - 2021-01-14 (@jtp10181)
  ###Added
  - Command button to remove invalid parameters
  ###Changed
  - General code cleanup
  - Send events to child as parse so it can handle its own logging (ZEN30)
  - Moved Refresh Params to its own command

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

 *  Copyright 2020 Jeff Page
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

/* ZEN21 (4.01)
CommandClassReport- class:0x25, version:1
CommandClassReport- class:0x55, version:2
CommandClassReport- class:0x59, version:1
CommandClassReport- class:0x5A, version:1
CommandClassReport- class:0x5B, version:3
CommandClassReport- class:0x5E, version:2
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
	0x25: 1,	// Switch Binary (switchbinaryv1)
	0x55: 1,	// Transport Service (transportservicev1) (2)
	0x59: 1,	// Association Grp Info (associationgrpinfov1)
	0x5A: 1,	// Device Reset Locally	(deviceresetlocallyv1)
	0x5B: 3,	// CentralScene (centralscenev3)
	0x5E: 2,	// Zwaveplus Info (zwaveplusinfov2)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 1,	// Configuration (configurationv1)
	0x7A: 4,	// Firmware Update Md (firmwareupdatemdv4)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x73: 1,	// Powerlevel (powerlevelv1)
	0x85: 2,	// Association (associationv2)
	0x86: 3,	// Version (versionv3)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
	0x98: 1,	// Security S0 (securityv1)
	0x9F: 1		// Security S2
]

@Field static final int maxAssocGroups = 3
@Field static final int maxAssocNodes = 5

@Field static Map deviceModelNames = ["B111:1E1C":"ZEN21", "B112:1F1C":"ZEN22", "B111:251C":"ZEN23", "B112:261C":"ZEN24", 
	"A000:A001":"ZEN26", "A000:A002":"ZEN27", "7000:A001":"ZEN71", "7000:A002":"ZEN72", "7000:A006":"ZEN76", "7000:A007":"ZEN77"]

@Field static Map paddleOrientationOptions = [0:"Up for On, Down for Off", 1:"Up for Off, Down for On", 2:"Up or Down for On/Off"]
@Field static Map ledIndicatorOptions = [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"]
@Field static Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]
@Field static Map ledBrightnessOptions = [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"]
@Field static Map disabledEnabledOptions = [0:"Disabled", 1:"Enabled"]
@Field static Map autoOnOffIntervalOptions = [0:"Disabled", 1:"1 Minute", 2:"2 Minutes", 3:"3 Minutes", 4:"4 Minutes", 5:"5 Minutes", 6:"6 Minutes", 7:"7 Minutes", 8:"8 Minutes", 9:"9 Minutes", 10:"10 Minutes", 15:"15 Minutes", 20:"20 Minutes", 25:"25 Minutes", 30:"30 Minutes", 45:"45 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 240:"4 Hours", 300:"5 Hours", 360:"6 Hours", 420:"7 Hours", 480:"8 Hours", 540:"9 Hours", 600:"10 Hours", 720:"12 Hours", 1080:"18 Hours", 1440:"1 Day", 2880:"2 Days", 4320:"3 Days", 5760:"4 Days", 7200:"5 Days", 8640:"6 Days", 10080:"1 Week", 20160:"2 Weeks", 30240:"3 Weeks", 40320:"4 Weeks", 50400:"5 Weeks", 60480:"6 Weeks"]
@Field static Map powerFailureRecoveryOptions = [2:"Restores Last Status", 0:"Forced to Off", 1:"Forced to On"]
@Field static Map relayControlOptions = [1:"Enable Paddle and Z-Wave", 0:"Disable Physical Paddle Control", 2:"Disable Paddle and Z-Wave Control"]
@Field static Map threeWaySwitchTypeOptions = [0:"Toggle On/Off Switch", 1:"Momentary Switch (ZAC99)"]
@Field static Map relayBehaviorOptions = [0:"Reports Status & Changes LED Always", 1:"Doesn't Report Status or Change LED"]
@Field static Map associationReportsOptions = [
	0:"None", 1:"Physical Tap On ZEN Only", 2:"Physical Tap On Connected 3-Way Switch Only", 3:"Physical Tap On ZEN / 3-Way Switch",
	4:"Z-Wave Command From Hub", 5:"Physical Tap On ZEN / Z-Wave Command", 6:"Physical Tap On 3-Way Switch / Z-Wave Command",
	7:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command", 8:"Timer Only", 9:"Physical Tap On ZEN / Timer",
	10:"Physical Tap On 3-Way Switch / Timer", 11:"Physical Tap On ZEN / 3-Way Switch / Timer", 12:"Z-Wave Command From Hub / Timer",
	13:"Physical Tap On ZEN / Z-Wave Command / Timer", 14:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command / Timer",
	15:"All Of The Above" ]

metadata {
	definition (
		name: "Zooz ZEN Switch Advanced",
		namespace: "jtp10181",
		author: "Jeff Page / Kevin LaFramboise (@krlaframboise)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen-switch.groovy"
	) {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		//capability "Light"  //Redundant with Switch
		capability "Configuration"
		capability "Refresh"
		capability "HealthCheck"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"

		command "hideInvalidParams"
		command "refreshParams"

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"B111", deviceId:"1E1C", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN21 Switch"
		fingerprint mfr:"027A", prod:"B111", deviceId:"251C", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN23 Switch"
		fingerprint mfr:"027A", prod:"A000", deviceId:"A001", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN26 S2 Switch"
		fingerprint mfr:"027A", prod:"7000", deviceId:"A001", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN71 Switch"
		fingerprint mfr:"027A", prod:"7000", deviceId:"A006", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN76 S2 Switch"
	}

	preferences {
		configParams.each { param ->
			if (!(param in [autoOffEnabledParam, autoOnEnabledParam, statusReportsParam])) {
				createEnumInput("configParam${param.num}", "${param.name} (#${param.num}):", param.value, param.options)
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

		input "sceneReverse", "bool",
			title: "Scene Up-Down Reversal:",
			description: "If the button numbers and up/down descriptions are backwards in the scene button events change this setting to fix it!",
			defaultValue: true

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

String getAssocDNIsSetting(grp) {
	def val = settings?."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "") // iOS app has no way of clearing string input so workaround is to have users enter 0.
}

def hideInvalidParams() {
	device.removeDataValue("configHide")
	
	List configDisabled = []
	Map configsMap = getParamStoredMap()
	configParams.each { param ->
		if (!(configsMap.find { it.key.toInteger() == param.num } )) {
			configDisabled << param.num
		}
	}
	if (configDisabled) {
		logDebug "Disabled Parameters: ${configDisabled}"
		device.updateDataValue("configHide", configDisabled.inspect())
	}
	else {
		logDebug "Disabled Parameters: NONE"

	}
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

		if (debugEnable) runIn(1800, debugLogsOff, [overwrite: true])

		initialize()

		runIn(2, executeConfigureCmds, [overwrite: true])
	}
}

void initialize() {
	def checkInterval = ((60 * 60 * 3) + (5 * 60))

	if (!device.currentValue("checkInterval")) {
		sendEvent(name: "checkInterval", value:checkInterval, displayed:false, data:[protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	}

	if (!device.currentValue("numberOfButtons")) {
		sendEvent(name:"numberOfButtons", value:10, displayed:false)
	}
}


def configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff, [overwrite: true])

	sendEvent(name:"numberOfButtons", value:10, displayed:false)

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		state.resyncAll = true
	}

	runIn(2, executeRefreshCmds, [overwrite: true])
	runIn(8, executeConfigureCmds, [overwrite: true])
}


void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."
	runIn(6, refreshSyncStatus)

	List<String> cmds = []

	if (!device.currentValue("switch")) {
		cmds << switchBinaryGetCmd()
	}

	if (state.resyncAll || !state.deviceModel || !device.getDataValue("firmwareVersion")) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getAdjustedParamValue(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds << configSetCmd(param, paramVal)
			//ZEN7x models automatically report back and longer delays helps, otherwise request report
			if (state.deviceModel ==~ /ZEN7\d/) cmds << "delay 250"
			else cmds << configGetCmd(param)
		}
	}

	if (state.resyncAll) clearVariables()

	state.resyncAll = false

	if (cmds) sendCommands(cmds)
}

void clearVariables() {
	log.warn "Clearing state variables and data..."

	//Backup
	def devModel = state.deviceModel 

	//Clears State Variables
	state.clear()

	//Clear Data from other Drivers
	device.removeDataValue("configVals")
	device.removeDataValue("configHide")
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

void debugLogsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

private getAdjustedParamValue(Map param) {
	// ZEN7x models do not need this
	if (state.deviceModel ==~ /ZEN7\d/) {
		return param.value
	}

	Integer paramVal
	switch(param.num) {
		case autoOffEnabledParam.num:
			paramVal = autoOffIntervalParam.value == 0 ? 0 : 1
			break
		case autoOffIntervalParam.num:
			paramVal = autoOffIntervalParam.value ?: 60
			break
		case autoOnEnabledParam.num:
			paramVal = autoOnIntervalParam.value == 0 ? 0 : 1
			break
		case autoOnIntervalParam.num:
			paramVal = autoOnIntervalParam.value ?: 60
			break
		default:
			paramVal = param.value
	}
	return paramVal
}


private getConfigureAssocsCmds() {
	def cmds = []

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

		def cmdsEach = []
		def settingNodeIds = getAssocDNIsSettingNodeIds(i)

		//Need to remove first then add in case we are at limit
		def oldNodeIds = state."assocNodes$i"?.findAll { !(it in settingNodeIds) }
		if (oldNodeIds) {
			logDebug "Removing Nodes: Group $i - $oldNodeIds"
			cmdsEach << associationRemoveCmd(i, oldNodeIds)
		}

		def newNodeIds = settingNodeIds?.findAll { !(it in state."assocNodes$i") }
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


def ping() {
	logDebug "ping..."
	return switchBinaryGetCmd()
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
	refreshSyncStatus()

	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << switchBinaryGetCmd()

	sendCommands(cmds)
}

void refreshParams() {
	refreshSyncStatus()

	List<String> cmds = []
	configParams.each { param ->
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}


void sendCommands(List<String> cmds, Long delay=400) {
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

void sendCommands(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}


String associationSetCmd(Integer group, List<Integer> nodes) {
	return secureCmd(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return secureCmd(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV3.versionGet())
}

String switchBinarySetCmd(val) {
	return secureCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: val))
}

String switchBinaryGetCmd() {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet())
}

String configSetCmd(Map param, Integer value) {
	return secureCmd(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param.num))
}

//From: https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/genericZWaveCentralSceneDimmer.groovy
String secureCmd(String cmd){
	return zwaveSecureEncap(cmd)
}

String secureCmd(hubitat.zwave.Command cmd){
	return zwaveSecureEncap(cmd)
}

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


void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)))
}


void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	updateSyncingStatus()

	Map param = configParams.find { it.num == cmd.parameterNumber }
	if (param) {
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

	def grp = cmd.groupingIdentifier

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

		def dnis = convertIntListToHexList(cmd.nodeId)?.join(", ") ?: "none"
		sendEventIfNew("assocDNI$grp", dnis, false)
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}


void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"

	device.updateDataValue("firmwareVersion", fullVersion)

	//Stash the switch model in a state variable
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))])
	def devModel = deviceModelNames[devTypeId.join(":")] ?: false
	state.deviceModel = devModel

	if (!devModel) {
		logDebug "Unknown Device: ${devTypeId}"
	}

	if (state.resyncAll) {
		//Disable sceneReverse setting for known cases otherwise set to true (most need it reversed)
		if ((devModel == "ZEN27" && fullVersion == "3.01") ||
		   (devModel == "ZEN22" && fullVersion == "4.01") ||
		   (devModel ==~ /ZEN7\d/))
		{
			logDebug "Scene Reverse turned off, known Model/Firmware match found."
			device.updateSetting("sceneReverse", false)
		} else {
			logDebug "Scene Reverse turned on, proper settings for most devices."
			device.updateSetting("sceneReverse", true)
		} 
	}
}


void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	logTrace "${cmd}"
	sendSwitchEvents(cmd.value, "physical")
}


void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	logTrace "${cmd}"
	sendSwitchEvents(cmd.value, "digital")
}


void sendSwitchEvents(rawVal, String type) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value}"
	sendEventIfNew("switch", value, true, type, "", desc)
}


void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd){
	if (state.lastSequenceNumber != cmd.sequenceNumber) {
		state.lastSequenceNumber = cmd.sequenceNumber

		logTrace "${cmd}"

		//Flip the sceneNumber if needed (per parameter setting)
		if (settings?.sceneReverse)	{
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


void zwaveEvent(hubitat.zwave.Command cmd) {
	logDebug "Unhandled zwaveEvent: $cmd"
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
	if (!state.deviceModel) return []
	
	def params = [
		paddleOrientationParam,
		ledIndicatorParam,
		ledColorParam,
		ledBrightnessParam,
		autoOffEnabledParam,
		autoOffIntervalParam,
		autoOnEnabledParam,
		autoOnIntervalParam,
		powerFailureRecoveryParam,
		sceneControlParam,
		threeWaySwitchTypeParam,
		relayControlParam,
		relayBehaviorParam,
		statusReportsParam,
		associationReportsParam
	]

	//Remove Hidden Invalid Params
	String configHide = device?.getDataValue("configHide")
	if (configHide != null) {
		List configDisabled = evaluate(configHide)
		params.removeAll { configDisabled.contains(it.num) }
	}	

	// ZEN23/24 does not have a LED at all
	if (state.deviceModel in ["ZEN23", "ZEN24"]) {
		params.removeAll { it == ledIndicatorParam }
	}

	// Remove from all except ZEN21/22/23/24/71/72
	if (!(state.deviceModel ==~ /ZEN\d[1234]/)) {
		params.removeAll { it == threeWaySwitchTypeParam }
	}

	// Remove from all except ZEN7x
	if (!(state.deviceModel ==~ /ZEN7\d/)) {
 		params.removeAll {
 			it == ledColorParam ||
	 		it == ledBrightnessParam ||
	 		it == statusReportsParam ||
	 		it == singleTapParam
	 	}
	}
	//Remove from Only ZEN7x
	else {
		params.removeAll { 
			it == autoOffEnabledParam ||
			it == autoOnEnabledParam ||
			it == doubleTapFunctionParam ||
			it == zwaveRampRateParam
		}
	}

	return params
}

Map getPaddleOrientationParam() {
	return getParam(32, "Paddle Orientation", 1, 0, paddleOrientationOptions)
}

Map getLedIndicatorParam() {
	return getParam(2, "LED Indicator", 1, 0, ledIndicatorOptions)
}

Map getAutoOffEnabledParam() {
	// ZEN7x duplicate param number
	Integer num = (state.deviceModel ==~ /ZEN7\d/) ? null : 3
	return getParam(num, "Auto Turn-Off Timer Enabled", 1, 0, disabledEnabledOptions)
}

Map getAutoOffIntervalParam() {
	// ZEN7x=3, Others=4
	Integer num = (state.deviceModel ==~ /ZEN7\d/) ? 3 : 4
	return getParam(num, "Auto Turn-Off Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getAutoOnEnabledParam() {
	// ZEN7x duplicate param number
	Integer num = (state.deviceModel ==~ /ZEN7\d/) ? null : 5
	return getParam(num, "Auto Turn-On Timer Enabled", 1, 0, disabledEnabledOptions)
}

Map getAutoOnIntervalParam() {
	// ZEN7x=5, Others=6
	Integer num = (state.deviceModel ==~ /ZEN7\d/) ? 5 : 6
	return getParam(num, "Auto Turn-On Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getAssociationReportsParam() {
	return getParam(7, "Send Status Report to Associations on", 1, 15, associationReportsOptions)
}

Map getPowerFailureRecoveryParam() {
	return getParam(8, "Behavior After Power Outage", 1, 2, powerFailureRecoveryOptions)
}

Map getSceneControlParam() {
	// ZEN26/76=10, Other Switches=9, Dimmers=13
	Integer num = (state.deviceModel in ["ZEN26", "ZEN76"]) ? 10 : 9
	return getParam(num, "Scene Control Events", 1, 0, disabledEnabledOptions)
}

Map getRelayControlParam() {
	// ZEN76=12, Other Switches=11, Dimmers=15
	Integer num = (state.deviceModel in ["ZEN76"]) ? 12 : 11
	return getParam(num, "Smart Bulb Mode - Load Control", 1, 1, relayControlOptions)
}

// ZEN21/22/23/24/71/72 Only
Map getThreeWaySwitchTypeParam() {
	// ZEN76 duplicate param number
	Integer num = (state.deviceModel in ["ZEN76"]) ? null : 12
	return getParam(num, "3-Way Switch Type", 1, 0, threeWaySwitchTypeOptions)
}

Map getRelayBehaviorParam() {
	Integer num = 13
	return getParam(num, "Smart Bulb - On/Off when Physical Disabled", 1, 0, relayBehaviorOptions)
}

// ZEN71/72/76/77
Map getLedColorParam() {
	Integer num = 14
	return getParam(num, "LED Indicator Color", 1, 1, ledColorOptions)
}

// ZEN71/72/76/77
Map getLedBrightnessParam() {
	Integer num = 15
	return getParam(num, "LED Indicator Brightness", 1, 1, ledBrightnessOptions)
}

// ZEN71/76 -- HIDDEN, should always be set to 0
Map getStatusReportsParam() {
	return getParam(16, "All Reports Use SwitchBinary", 1, 0, disabledEnabledOptions)
}

Map getParam(Integer num, String name, Integer size, Integer defaultVal, Map options) {
	Integer val = safeToInt(settings?."configParam${num}", defaultVal)
	Map retMap = [num: num, name: name, size: size, value: val, options: options]

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


void sendEventIfNew(String name, value, boolean displayed=true, String type=null, String unit="", String desc=null) {
	if (desc == null) desc = "${name} set to ${value}${unit}"

	if (device.currentValue(name).toString() != value.toString()) {
		Map evt = [name: name, value: value, descriptionText: desc, displayed: displayed]

		if (type) evt.type = type
		if (unit) evt.unit = unit

		if (name != "syncStatus") logTxt(desc)
		sendEvent(evt)
	}
	else if (name != "syncStatus") {
		logDebug "${desc} [NOT CHANGED]"
	}
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
