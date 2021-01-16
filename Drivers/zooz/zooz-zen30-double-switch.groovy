/*
 *  Zooz ZEN30 Double Switch
 *  	(Model: ZEN30 - All Firmware)
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
 *  Copyright 2020 Jeff Page
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
	0x5E: 2,	// Zwaveplus Info (zwaveplusinfov2)
	0x60: 3,	// MultiChannel (multichannelv3) (4)
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
@Field static Map endpoints = ["dimmer": 0, "switch": 1]

@Field static Map ledModeOptions = [0:"LED On When Switch Off", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"]
@Field static Map ledColorOptions = [0:"White", 1:"Blue", 2:"Green", 3:"Red"]
@Field static Map ledBrightnessOptions = [0:"Bright (100%)", 1:"Medium (60%)", 2:"Low (30%)"]
@Field static Map ledSceneControlOptions = [0:"LED Enabled", 1:"LED Disabled"]
@Field static Map autoOnOffIntervalOptions = [0:"Disabled", 1:"1 Minute", 2:"2 Minutes", 3:"3 Minutes", 4:"4 Minutes", 5:"5 Minutes", 6:"6 Minutes", 7:"7 Minutes", 8:"8 Minutes", 9:"9 Minutes", 10:"10 Minutes", 15:"15 Minutes", 20:"20 Minutes", 25:"25 Minutes", 30:"30 Minutes", 45:"45 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 240:"4 Hours", 300:"5 Hours", 360:"6 Hours", 420:"7 Hours", 480:"8 Hours", 540:"9 Hours", 600:"10 Hours", 720:"12 Hours", 1080:"18 Hours", 1440:"1 Day", 2880:"2 Days", 4320:"3 Days", 5760:"4 Days", 7200:"5 Days", 8640:"6 Days", 10080:"1 Week", 20160:"2 Weeks", 30240:"3 Weeks", 40320:"4 Weeks", 50400:"5 Weeks", 60480:"6 Weeks"]
@Field static Map powerFailureOptions = [0:"Dimmer Off / Relay Off", 1:"Dimmer Off / Relay On", 2:"Dimmer On / Relay Off", 3:"Dimmer Restored / Relay Restored", 4:"Dimmer Restored / Relay On", 5:"Dimmer Restored / Relay Off", 6:"Dimmer On / Relay Restored", 7:"Dimmer Off / Relay Restored", 8:"Dimmer On / Relay On"]
@Field static Map rampRateOptions = [1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds", 20:"20 Seconds", 25:"25 Seconds", 30:"30 Seconds", 35:"35 Seconds", 40:"40 Seconds", 45:"45 Seconds", 50:"50 Seconds", 55:"55 Seconds", 60:"60 Seconds", 65:"65 Seconds", 70:"70 Seconds", 75:"75 Seconds", 80:"80 Seconds", 85:"85 Seconds", 90:"90 Seconds", 95:"95 Seconds", 99:"99 Seconds"]
@Field static Map brightnessOptions = [1:"1%", 5:"5%", 10:"10%", 15:"15%", 20:"20%", 25:"25%", 30:"30%", 35:"35%", 40:"40%", 45:"45%", 50:"50%", 55:"55%",60:"60%", 65:"65%", 70:"70%", 75:"75%", 80:"80%", 85:"85%", 90:"90%", 95:"95%", 99:"99%"]
@Field static Map doubleTapBrightnessOptions = [0:"Full Brightness (100%)", 1:"Maximum Brightness Parameter"]
@Field static Map doubleTapFunctionOptions = [0:"Full/Maximum Brightness", 1:"Disabled, Single Tap Last Brightness (or Custom)", 2:"Disabled, Single Tap Full/Maximum Brightness"]
@Field static Map dimmerDigitalRampRateBehaviorOptions = [0:"Match Physical Ramp Rate", 1:"Z-Wave Can Set Ramp Rate [RECOMMENDED]"]
@Field static Map loadControlOptions = [1:"Enable Paddle and Z-Wave", 0:"Disable Physical Paddle Control", 2:"Disable Paddle and Z-Wave Control"]
@Field static Map paddleControlOptions = [0:"Normal", 1:"Reverse", 2:"Toggle Mode"]
@Field static Map physicalDisabledBehaviorOptions = [0:"Reports Status & Changes LED Always", 1:"Doesn't Report Status or Change LED"]

metadata {
	definition (
		name: "Zooz ZEN30 Double Switch",
		namespace: "jtp10181",
		author: "Jeff Page / Kevin LaFramboise (@krlaframboise)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen30-double-switch.groovy"
	) {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "SwitchLevel"
		//capability "Light"  //Redundant with Switch
		capability "Configuration"
		capability "Refresh"
		capability "HealthCheck"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"
		capability "DoubleTapableButton"

		command "childDevices", [[name:"Select One*", type: "ENUM", constraints: ["Create","Remove"] ]]
		command "refreshParams"
		//command "debugAssociationReports"
		//command "debugFixLifeline"

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A008", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN30 Double Switch"
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

		input "levelCorrection", "bool",
			title: "Brightness Correction:",
			description: "Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%",
			defaultValue: false

		//Logging options similar to other Hubitat drivers
		input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: false
		input name: "debugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: true
	}
}

/*
TEMPORARY DEBUGGING CODE
*/

List<String> debugAssociationReports(){
	List<String> cmds = []
	1.upto(5, {
		cmds.add(secureCmd(zwave.associationV2.associationGet(groupingIdentifier: it)))
		cmds.add(secureCmd(zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: it)))
		cmds.add(secureCmd(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: it)))
	})
	return delayBetween(cmds,500)
}

List<String> debugFixLifeline(){
	List<String> cmds = []

	cmds << secureCmd(zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[1], ))
	cmds << secureCmd(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId:[1], multiChannelNodeIds:[] ))
	cmds << secureCmd(zwave.associationV2.associationGet(groupingIdentifier: 1))
	cmds << secureCmd(zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 1))

	return delayBetween(cmds,500)
}

/*
AssociationReport(groupingIdentifier: 1, maxNodesSupported: 1, reportsToFollow: 0, nodeId: [])
MultiChannelAssociationReport(groupingIdentifier:1, maxNodesSupported:1, reportsToFollow:0, nodeId:[], multiChannelNodeIds:[[nodeId:1, bitAddress:0, endPointId:0]])
END OF DEBUGGING CODE
*/


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

	childDevicesCreate()
}


def configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff, [overwrite: true])

	sendEvent(name:"numberOfButtons", value:10, displayed:false)
	childDevices[0]?.sendEvent(name:"numberOfButtons", value:5, displayed:false)

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
		cmds << switchMultilevelGetCmd()
	}

	if (state.resyncAll || !device.getDataValue("firmwareVersion")) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds()
	
	configParams.each { param ->
		Integer paramVal = param.value
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds << configSetCmd(param, paramVal)
			cmds << configGetCmd(param)
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
	def child = addChildDevice(
		"hubitat",
		"Generic Component Central Scene Switch",
		"${device.deviceNetworkId}-1",
		[
			isComponent: true,
			name: "${device.name} RELAY"
		]
	)
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
	def cmds = []

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
	return switchMultilevelGetCmd()
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
	Integer durationVal = validateRange(duration, dimmerRampRateParam.value, 0, 99)

	return switchMultilevelSetCmd(levelVal, durationVal)
}


def refresh() {
	logDebug "refresh..."
	executeRefreshCmds()
}

void executeRefreshCmds() {
	refreshSyncStatus()
	
	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << switchMultilevelGetCmd()
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

String switchBinarySetCmd(Integer val) {
	return multiChannelCmdEncapCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: val), endpoints.switch)
}

String switchBinaryGetCmd() {
	return multiChannelCmdEncapCmd(zwave.switchBinaryV1.switchBinaryGet(), endpoints.switch)
}

String switchMultilevelSetCmd(Integer value, Integer duration) {
	return multiChannelCmdEncapCmd(zwave.switchMultilevelV3.switchMultilevelSet(dimmingDuration: duration, value: value), endpoints.dimmer)
}

String switchMultilevelGetCmd() {
	return multiChannelCmdEncapCmd(zwave.switchMultilevelV3.switchMultilevelGet(), endpoints.dimmer)
}

String configSetCmd(Map param, Integer value) {
	return secureCmd(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param.num))
}

String multiChannelCmdEncapCmd(cmd, endpoint) {
	logTrace "multiChannelCmdEncapCmd: ${cmd} (ep ${endpoint})"
	if (endpoint) {
		return secureCmd(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:safeToInt(endpoint)).encapsulate(cmd))
	} else {
		return secureCmd(cmd)
	}
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

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, endpoint=0) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, endpoint)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}

	sendCommands(multiChannelCmdEncapCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0),endpoint))
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
}


void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, endpoint=0) {
	logTrace "${cmd} (ep ${endpoint})"
	sendSwitchEvents(cmd.value, "physical", endpoint)
}


void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, endpoint=0) {
	logTrace "${cmd} (ep ${endpoint})"
	
	String type = (state.pendingRelay ? "digital" : "physical")
	state.remove("pendingRelay")
	
	sendSwitchEvents(cmd.value, type, endpoint)
}


void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, endpoint=0) {
	logTrace "${cmd} (ep ${endpoint})"
	sendSwitchEvents(cmd.value, "digital", endpoint)
}


void sendSwitchEvents(rawVal, String type, Integer endpoint) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value}"
	sendEventIfNew("switch", value, true, type, "", desc, endpoint)

	if (rawVal && endpoint == endpoints.dimmer) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		level = convertLevel(level, false)

		desc = "level was set to ${level}%"
		if (levelCorrection) desc += " [actual: ${rawVal}]"
		sendEventIfNew("level", level, true, type, "%", desc, endpoint)
	}
}


void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, endpoint=0){
	if (state.lastSequenceNumber != cmd.sequenceNumber) {
		state.lastSequenceNumber = cmd.sequenceNumber

		logTrace "${cmd} (ep ${endpoint})"

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
				scene.descriptionText = "${child.displayName}: ${scene.descriptionText}"
				child.parse([scene])
			}
		}
	}
}


void zwaveEvent(hubitat.zwave.Command cmd, endpoint=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${endpoint})"
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

		dimmerRampRateParam,
		dimmerDigitalRampRateBehaviorParam,

		dimmerMinimumBrightnessParam,
		dimmerMaximumBrightnessParam,
		dimmerDoubleTapBrightnessParam,
		dimmerDoubleTapFunctionParam,

		dimmerLoadControlParam,
		relayLoadControlParam,
		dimmerPhysicalDisabledBehaviorParam,
		relayPhysicalDisabledBehaviorParam,

		dimmerPaddleHeldRampRateParam,
		dimmerCustomBrightnessParam,
		dimmerNightModeBrightnessParam
	]

	//Remove params not supported with this firmware
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

Map getDimmerRampRateParam() {
	Map options = [0:"Instant On/Off"]
	options << rampRateOptions
	return getParam(13, "Dimmer Ramp Rate to Full On/Off", 1, 1, options)
}

Map getDimmerMinimumBrightnessParam() {
	return getParam(14, "Dimmer Minimum Brightness", 1, 1, brightnessOptions)
}

Map getDimmerMaximumBrightnessParam() {
	return getParam(15, "Dimmer Maximum Brightness", 1, 99, brightnessOptions)
}

Map getDimmerDoubleTapBrightnessParam() {
	return getParam(17, "Dimmer Double Tap Up Brightness", 1, 0, doubleTapBrightnessOptions)
}

Map getDimmerDoubleTapFunctionParam() {
	return getParam(18, "Dimmer Double Tap Up Function", 1, 0, doubleTapFunctionOptions)
}

Map getDimmerLoadControlParam() {
	return getParam(19, "Smart Bulb Mode - Dimmer Load", 1, 1, loadControlOptions)
}

Map getRelayLoadControlParam() {
	return getParam(20, "Smart Bulb Mode - Relay Load", 1, 1, loadControlOptions)
}

Map getDimmerPaddleHeldRampRateParam() {
	return getParam(21, "Dimming Speed when Paddle is Held", 1, 4, rampRateOptions)
}

// Removed in firmware v1.05
Map getDimmerDigitalRampRateBehaviorParam() {
	return getParam(22, "Dimmer Z-Wave Ramp Rate", 1, 0, dimmerDigitalRampRateBehaviorOptions, 0, 1.04)
}

Map getDimmerCustomBrightnessParam() {	
	Map options = [0:"Last Brightness Level"]
	options << brightnessOptions
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
Map getDimmerNightModeBrightnessParam() {
	Map options = [0:"Disabled"]
	options << brightnessOptions
	return getParam(26, "Night Light Brightness", 1, 20, options, 1.05)
}

// Added in firmware v1.05
Map getDimmerPaddleControlParam() {
	return getParam(27, "Paddle Orientation for Dimmer", 1, 0, paddleControlOptions, 1.05)
}


Map getParam(Integer num, String name, Integer size, Integer defaultVal, Map options, BigDecimal minVer=null, BigDecimal maxVer=null) {
	Integer val = safeToInt(settings?."configParam${num}", defaultVal)
	Map retMap = [num: num, name: name, size: size, value: val, options: options, minVer: minVer, maxVer: maxVer]

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


void sendEventIfNew(String name, value, boolean displayed=true, String type=null, String unit="", String desc=null, Integer endpoint=0) {
	if (desc == null) desc = "${name} set to ${value}${unit}"
	String logEp = (endpoint ? "(RELAY) " : "")
	def eventDev = (endpoint ? childDevices[0] : device)

	if (!eventDev) {
		log.error "No device for endpoint (${endpoint}). Use command button to create child devices."
	}
	else if (eventDev.currentValue(name).toString() != value.toString()) {
		Map evt = [name: name, value: value, descriptionText: desc, displayed: displayed]

		if (type) evt.type = type
		if (unit) evt.unit = unit

		if (endpoint) {
			evt.descriptionText = "${eventDev.displayName}: ${evt.descriptionText}"
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
	return (((param.minVer == null) || (firmware >= param.minVer)) && ((param.maxVer == null) || (firmware <= param.maxVer)))
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
		Integer brightmax = safeToInt(dimmerMaximumBrightnessParam.value, 99)
		Integer brightmin = safeToInt(dimmerMinimumBrightnessParam.value, 1)
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
