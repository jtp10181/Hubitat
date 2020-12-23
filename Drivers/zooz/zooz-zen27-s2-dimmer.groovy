/*
 *  Zooz ZEN27 S2 Dimmer VER. 3.0
 *  	(Model: ZEN27 - MINIMUM FIRMWARE 2.08)
 *
 *  Changelog:
 *
 *    3.0 (09/16/2020) - @krlaframboise / Zooz - https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/zooz/
 *      - Initial Release
 *
 *    1.0.0 (12/10/2020) - @jtp10181
 *      - Ported from ST to HE
 *      - Reset / synced version numbers
 *      - Added SupervisionGet Event
 *      - Fixed a few default designations to match zooz documentation
 *      - Fixed Up/Down Scene labels which were reporting in reverse
 *      - Changed scene events to user proper button numbers per Zooz docs
 *      - Upgraded command classes when possible
 *      - Added parameter 17 ZWave Ramp Rate
 *      - Changed debug and info logging to match Hubitat standards
 *      - Moved storage of config variables to Data (in a Map)
 *      - Added command to flash the light from Hubitat example driver
 *
 *    1.1.0 (12/14/2020) - @jtp10181
 *      - Added Parameter 7 and 20
 *      - Corrected Fingerprint 
 *      - Cleaned up some parameter wording and ordering
 *      - Reverted Up/Down fix per Zooz (except firmware 3.01 due to a bug)
 *
 *    1.2.0 (12/18/2020) - @jtp10181
 *      - Added Group3 Associations
 *
 *    1.3.0 (12/22/2020) - @jtp10181
 *      - Fixed some bugs with the groups associations commands
 *      - Fixed so refresh will update firmware version now
 *      - Saving model number in deviceModel for quick access
 *      - Added code to remove params from list when not available for certain models
 *      - Fixed comparison in SendEventIfNew to handle when value is a number
 *      - Worked towards unifying the switch and dimmer code between models
 *      - Added Brightness Correction - to convert full range to set between min/max
 *
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

/*
CommandClassReport- class:0x26, version:4
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
	0x26: 3,	// Switch Multilevel (switchmultilevelv3) (4)
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

@Field static Map paddlePaddleOrientationOptions = [0:"Up for On, Down for Off [DEFAULT]", 1:"Up for Off, Down for On", 2:"Up or Down for On/Off"]
@Field static Map ledIndicatorOptions = [0:"LED On When Switch Off [DEFAULT]", 1:"LED On When Switch On", 2:"LED Always Off", 3:"LED Always On"]
@Field static Map disabledEnabledOptions = [0:"Disabled [DEFAULT]", 1:"Enabled"]
@Field static Map autoOnOffIntervalOptions = [0:"Disabled [DEFAULT]", 1:"1 Minute", 2:"2 Minutes", 3:"3 Minutes", 4:"4 Minutes", 5:"5 Minutes", 6:"6 Minutes", 7:"7 Minutes", 8:"8 Minutes", 9:"9 Minutes", 10:"10 Minutes", 15:"15 Minutes", 20:"20 Minutes", 25:"25 Minutes", 30:"30 Minutes", 45:"45 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 240:"4 Hours", 300:"5 Hours", 360:"6 Hours", 420:"7 Hours", 480:"8 Hours", 540:"9 Hours", 600:"10 Hours", 720:"12 Hours", 1080:"18 Hours", 1440:"1 Day", 2880:"2 Days", 4320:"3 Days", 5760:"4 Days", 7200:"5 Days", 8640:"6 Days", 10080:"1 Week", 20160:"2 Weeks", 30240:"3 Weeks", 40320:"4 Weeks", 50400:"5 Weeks", 60480:"6 Weeks"]
@Field static Map powerFailureRecoveryOptions = [2:"Restores Last Status [DEFAULT]", 0:"Forced to Off", 1:"Forced to On"]
@Field static Map rampRateOptions = [1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds", 20:"20 Seconds", 25:"25 Seconds", 30:"30 Seconds", 45:"45 Seconds", 60:"60 Seconds", 75:"75 Seconds", 90:"90 Seconds"]
@Field static Map brightnessOptions = [1:"1%", 5:"5%", 10:"10%", 15:"15%", 20:"20%", 25:"25%", 30:"30%", 35:"35%", 40:"40%", 45:"45%", 50:"50%", 55:"55%",60:"60%", 65:"65%", 70:"70%", 75:"75%", 80:"80%", 85:"85%", 90:"90%", 95:"95%", 99:"99%"]
@Field static Map doubleTapUp12Options = [0:"Full Brightness (100%) [DEFAULT]", 1:"Maximum Brightness Parameter"]
@Field static Map doubleTapUp14Options = [0:"Full/Maximum Brightness [DEFAULT]", 1:"Disabled, Single Tap Last Brightness", 2:"Disabled, Single Tap Full/Maximum Brightness"]
@Field static Map relayControlOptions = [1:"Enable Paddle and Z-Wave [DEFAULT]", 0:"Disable Physical Paddle Control", 2:"Disable Paddle and Z-Wave Control"]
@Field static Map threeWaySwitchTypeOptions = [0:"Toggle On/Off Switch [DEFAULT]", 1:"Momentary Switch (ZAC99)"]
@Field static Map relayDimmingOptions = [0:"Report Each Brightness Level for Physical Dimming When Relay Control is Disabled [DEFAULT]", 1:"Report Only Final Brightness Level for Physical Dimming Always"]
@Field static Map relayBehaviorOptions = [0:"Reports Status & Changes LED Always [DEFAULT]", 1:"Doesn't Report Status or Change LED When Relay Control Is Disabled"]
@Field static Map zwaveRampRateOptions = [0:"Match Physical Ramp Rate [DEFAULT]", 1:"Z-Wave Can Set Ramp Rate [RECOMMENDED]"]
@Field static Map associationReportsOptions = [
	0:"None", 1:"Physical Tap On ZEN Only", 2:"Physical Tap On Connected 3-Way Switch Only", 3:"Physical Tap On ZEN / 3-Way Switch", 
	4:"Z-Wave Command From Hub", 5:"Physical Tap On ZEN / Z-Wave Command", 6:"Physical Tap On 3-Way Switch / Z-Wave Command", 
	7:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command", 8:"Timer Only", 9:"Physical Tap On ZEN / Timer", 
	10:"Physical Tap On 3-Way Switch / Timer", 11:"Physical Tap On ZEN / 3-Way Switch / Timer", 12:"Z-Wave Command From Hub / Timer", 
	13:"Physical Tap On ZEN / Z-Wave Command / Timer", 14:"Physical Tap On ZEN / 3-Way Switch / Z-Wave Command / Timer", 
	15:"All Of The Above [DEFAULT]" ]

metadata {
	definition (
		name: "Zooz ZEN27 S2 Dimmer 3.0",
		namespace: "jtp10181",
		author: "Jeff Page / Kevin LaFramboise (@krlaframboise)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/zooz-zen27-s2-dimmer.groovy"
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

		command "flash", [[name:"Flash Rate", type: "NUMBER"]]

		attribute "assocDNI2", "string"
		attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A002", inClusters:"0x5E,0x6C,0x55,0x9F", deviceJoinName:"Zooz ZEN27 S2 Dimmer"
	}

	preferences {
		configParams.each { param ->
			if (!(param in [autoOffEnabledParam, autoOnEnabledParam])) {
				createEnumInput("configParam${param.num}", "${param.name}:", param.value, param.options)
			}
		}

		input "assocDNI2", "string",
			title: "Device Associations - Group 2:",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can enter blank or 0 to clear accosiations)",
			required: false

		input "assocDNI3", "string",
			title: "Device Associations - Group 3:",
			description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can enter blank or 0 to clear accosiations)",
			required: false

		input "levelCorrection", "bool",
			title: "Brightness Correction:",
			description: "Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%",
			required: false

		//Logging options similar to other Hubitat drivers
		input name: "debugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: true
		input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: false
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

def installed() {
	log.warn "installed..."
	configure()
	return []
}


def updated() {
	if (!isDuplicateCommand(state.lastUpdated, 5000)) {
		state.lastUpdated = new Date().time

		log.info "updated..."
		log.warn "debug logging is: ${debugEnable == true}"
		log.warn "description logging is: ${txtEnable == true}"

		if (debugEnable) runIn(1800, debugLogsOff, [overwrite: true])

		initialize()

		runIn(5, executeConfigureCmds, [overwrite: true])
	}
	return []
}

void initialize() {
	def checkInterval = ((60 * 60 * 3) + (5 * 60))

	Map checkIntervalEvt = [name: "checkInterval", value: checkInterval, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"]]

	if (!device.currentValue("checkInterval")) {
		sendEvent(checkIntervalEvt)
	}

	if (!device.currentValue("numberOfButtons")) {
		sendEvent(name:"numberOfButtons", value:10, displayed:false)
	}
}


def configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff, [overwrite: true])

	sendEvent(name:"numberOfButtons", value:10, displayed:false)

	if (state.resyncAll == null) {
		state.resyncAll = true
		runIn(8, executeConfigureCmds, [overwrite: true])
	}
	else {
		if (!pendingChanges) {
			state.resyncAll = true
		}
		executeConfigureCmds()
	}
	return []
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

	if ((state.resyncAll == true) || (state.group1Assoc != true)) {
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getAdjustedParamValue(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds << configSetCmd(param, paramVal)
			cmds << configGetCmd(param)
		}
	}

	if (state.resyncAll) clearVariables()

	state.resyncAll = false
	if (cmds) {
		sendCommands(delayBetween(cmds, 250))
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

private getAdjustedParamValue(Map param) {
	Integer paramVal
	switch(param.num) {
		case autoOffEnabledParam.num:
			paramVal = autoOffIntervalParam.value == 0 ? 0 : 1
			break
		case autoOffIntervalParam.num:
			paramVal = autoOffIntervalParam.value ?: null
			break
		case autoOnEnabledParam.num:
			paramVal = autoOnIntervalParam.value == 0 ? 0 : 1
			break
		case autoOnIntervalParam.num:
			paramVal = autoOnIntervalParam.value ?: null
			break
		default:
			paramVal = param.value
	}
	return paramVal
}


private getConfigureAssocsCmds() {
	def cmds = []

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

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
			cmds << associationSetCmd(1, [zwaveHubNodeId])
		}
		cmds << associationGetCmd(1)
	}

	return cmds
}


private getAssocDNIsSettingNodeIds(grp) {
	def dni = getAssocDNIsSetting(grp)
	def nodeIds = convertHexListToIntList(dni?.split(","))

	if (dni && !nodeIds) {
		log.warn "'${dni}' is not a valid value for the 'Device Associations Network IDs' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds?.size() > maxAssocNodes) {
		log.warn "The 'Device Associations Network IDs' setting contains more than ${maxAssocNodes} IDs so only the first ${maxAssocNodes} will be associated."
	}

	return nodeIds
}


def ping() {
	logDebug "ping..."
	return [ switchMultilevelGetCmd() ]
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

List<String> getSetLevelCmds(level, duration=null) {
	if (level == null) {
		level = device.currentValue("level")
	}
	
	if (level)  level = convertLevel(level, true)

	state.flashing = false
	Integer levelVal = validateRange(level, 99, 0, 99)
	Integer durationVal = validateRange(duration, rampRateParam.value, 0, 99)

	return [ switchMultilevelSetCmd(levelVal, durationVal) ]
}

//Based on https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/genericZWaveCentralSceneDimmer.groovy
String flash(flashRate) {
	if (!state.flashing) { 
		state.flashing = flashRate ?: 750
		logTxt "set to flash with a rate of ${state.flashing} milliseconds"
		return flashOn()
	}
	else {
		state.flashing = false
		logTxt "flashing stopped"
	}
}

String flashOn(){
	if (!state.flashing) return
	runInMillis((state.flashing).toInteger(), flashOff)
	return switchMultilevelSetCmd(null, 0) //Use existing brightness
}

String flashOff(){
	if (!state.flashing) return
	runInMillis((state.flashing).toInteger(), flashOn)
	return switchMultilevelSetCmd(0x00, 0)
}


def refresh() {
	logDebug "refresh..."

	refreshSyncStatus()

	sendCommands([versionGetCmd()])
	sendCommands([switchMultilevelGetCmd()])

	return []
}


void sendCommands(List<String> cmds) {
	if (cmds) {
		sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZWAVE))
	}
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

String switchMultilevelSetCmd(Integer value, Integer duration) {
	return secureCmd(zwave.switchMultilevelV3.switchMultilevelSet(dimmingDuration: duration, value: value))
}

String switchMultilevelGetCmd() {
	return secureCmd(zwave.switchMultilevelV3.switchMultilevelGet())
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

	if (cmd) {
		zwaveEvent(cmd)
	} else {
		log.warn "Unable to parse: $description"
	}

	updateLastCheckIn()
	return []
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
	logTrace "${cmd}"
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}


void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
	logTrace "${cmd}"
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}

	sendHubCommand(new hubitat.device.HubAction(secureCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}


void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	updateSyncingStatus()
	runIn(4, refreshSyncStatus)

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
	runIn(4, refreshSyncStatus)

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

	switch (devTypeId.join(".")) {
		case "B111.1E1C":
			state.deviceModel = "ZEN21"
			break
		case "B112.1F1C":
			state.deviceModel = "ZEN22"
			break
		case "B111.251C":
			state.deviceModel = "ZEN23"
			break
		case "B112.251C":
			state.deviceModel = "ZEN24"
			break
		case "A000.A001":
			state.deviceModel = "ZEN26"
			break
		case "A000.A002":
			state.deviceModel = "ZEN27"
			break
		default:
			state.deviceModel = "Unknown"
			logDebug "Unknown Device: ${devTypeId}"
	}
}


void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	logTrace "${cmd}"
	sendSwitchEvents(cmd.value, "physical")
}


void zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	logTrace "${cmd}"
	sendSwitchEvents(cmd.value, "digital")
}


void sendSwitchEvents(rawVal, String type) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value}"
	sendEventIfNew("switch", value, true, type, "", desc)

	if (rawVal) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		level = convertLevel(level, false)

		desc = "level was set to ${level}%"
		if (levelCorrection) desc += " [actual: ${rawVal}]"
		sendEventIfNew("level", level, true, type, "%", desc)
	}
}


void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd){
	if (state.lastSequenceNumber != cmd.sequenceNumber) {
		state.lastSequenceNumber = cmd.sequenceNumber

		logTrace "${cmd}"
		
		// ZEN22/ZEN27 on certian firmware - need to flip the sceneNumber due to bug
		if ((state.deviceModel == "ZEN27" && device.getDataValue("firmwareVersion") == "3.01") ||
			(state.deviceModel == "ZEN22" && device.getDataValue("firmwareVersion") == "4.01"))
		{
			if (cmd.sceneNumber == 1) cmd.sceneNumber = 2
			else if (cmd.sceneNumber == 2) cmd.sceneNumber = 1
			logTrace "${state.deviceModel} Fix: ${cmd}"
		}

		Map scene = [name: "pushed", value: 1, descriptionText: "", type:"physical", isStateChange:true]
		String actionType
		String btnVal

		switch (cmd.sceneNumber) {
			case 1:
				actionType = "down"	
				scene.value = 2
				break
			case 2:
				actionType = "up"
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
				scene.value = (cmd.keyAttributes * 2) - (cmd.sceneNumber + 1)
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
	Integer pendingAssocs = getConfigureAssocsCmds()?.size() ?: 0
	Integer group1Assoc = (state.group1Assoc != true) ? 1 : 0
	return (configChanges + pendingAssocs + group1Assoc)
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
		paddlePaddleOrientationParam,
		ledIndicatorParam,
		autoOffEnabledParam,
		autoOffIntervalParam,
		autoOnEnabledParam,
		autoOnIntervalParam,
		powerFailureRecoveryParam,
		rampRateParam,
		zwaveRampRateParam,
		minimumBrightnessParam,
		maximumBrightnessParam,
		doubleTapUp12Param,
		doubleTapUp14Param,
		sceneControlParam,
		relayControlParam,
		relayDimmingParam,
		relayBehaviorParam,
		holdRampRateParam,
		customBrightnessParam,
		threeWaySwitchTypeParam,
		nightLightParam,
		associationReportsParam
	]

	if (state.deviceModel in ["ZEN23", "ZEN24"]) {
		params.removeAll { it == ledIndicatorParam }
	}
	if (state.deviceModel in ["ZEN26", "ZEN27"]) {
		params.removeAll { it == threeWaySwitchTypeParam }
	}

	return params
}

Map getPaddlePaddleOrientationParam() {
	return getParam(1, "Paddle Orientation", 1, 0, paddlePaddleOrientationOptions)
}

Map getLedIndicatorParam() {
	return getParam(2, "LED Indicator", 1, 0, ledIndicatorOptions)
}

Map getAutoOffEnabledParam() {
	return getParam(3, "Auto Turn-Off Timer Enabled", 1, 0, disabledEnabledOptions)
}

Map getAutoOffIntervalParam() {
	return getParam(4, "Auto Turn-Off Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getAutoOnEnabledParam() {
	return getParam(5, "Auto Turn-On Timer Enabled", 1, 0, disabledEnabledOptions)
}

Map getAutoOnIntervalParam() {
	return getParam(6, "Auto Turn-On Timer", 4, 0, autoOnOffIntervalOptions)
}

Map getAssociationReportsParam() {
	return getParam(7, "Send Status Repot to Associations on", 1, 15, associationReportsOptions)
}

Map getPowerFailureRecoveryParam() {
	return getParam(8, "Behavior After Power Outage", 1, 2, powerFailureRecoveryOptions)
}

Map getRampRateParam() {
	Map options = [0:"Instant On/Off"]
	options += rampRateOptions
	return getParam(9, "Ramp Rate to Full On/Off", 1, 1, setDefaultOption(options, 1))
}

Map getMinimumBrightnessParam() {
	return getParam(10, "Minimum Brightness", 1, 1, setDefaultOption(brightnessOptions, 1))
}

Map getMaximumBrightnessParam() {
	return getParam(11, "Maximum Brightness", 1, 99, setDefaultOption(brightnessOptions, 99))
}

Map getDoubleTapUp12Param() {
	return getParam(12, "Double Tap Up Brightness", 1, 0, doubleTapUp12Options)
}

Map getSceneControlParam() {
	return getParam(13, "Scene Control Events", 1, 0, disabledEnabledOptions)
}

Map getDoubleTapUp14Param() {
	return getParam(14, "Double Tap Up Function", 1, 0, doubleTapUp14Options)
}

Map getRelayControlParam() {
	return getParam(15, "Smart Bulb Mode - Relay Control", 1, 1, relayControlOptions)
}

Map getHoldRampRateParam() {
	return getParam(16, "Physical Dimming Speed - From 0% to 100%", 1, 4, setDefaultOption(rampRateOptions, 4))
}

Map getZwaveRampRateParam() {
	return getParam(17, "Z-Wave Ramp Rate Control", 1, 0, zwaveRampRateOptions)
}

Map getCustomBrightnessParam() {
	Map options = [0:"Last Brightness Level"]
	options += brightnessOptions
	return getParam(18, "Custom Brightness when Turned On", 1, 0, setDefaultOption(options, 0))
}

// ZEN21/22/23/24 Only
Map getThreeWaySwitchTypeParam() {
	return getParam(19, "3-Way Switch Type", 1, 0, threeWaySwitchTypeOptions)
}

Map getRelayDimmingParam() {
	return getParam(20, "Smart Bulb Mode - Dimming Reporting", 1, 0, relayDimmingOptions)
}

Map getRelayBehaviorParam() {
	return getParam(21, "Smart Bulb Mode - On/Off Reporting", 1, 0, relayBehaviorOptions)
}

Map getNightLightParam() {
	Map options = [0:"Disabled"]
	options += brightnessOptions
	return getParam(22, "Night Light Mode", 1, 20, setDefaultOption(options, 20))
}

Map getParam(Integer num, String name, Integer size, Integer defaultVal, Map options) {
	Integer val = safeToInt((settings ? settings["configParam${num}"] : null), defaultVal)
	return [num: num, name: name, size: size, value: val, options: options]
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

		if (name != "syncStatus") logTxt(desc)

		Map evt = [name: name, value: value, descriptionText: "${desc}", displayed: displayed]

		if (type) evt.type = type
		if (unit) evt.unit = unit
		evt.isStateChange = true

		sendEvent(evt)
	}
	else {
		if (name != "syncStatus") logDebug "${desc} [NOT CHANGED]"
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


Integer convertLevel(level, userLevel=false) {
	if (levelCorrection) {
		Integer brightmax = safeToInt(settings?."configParam${maximumBrightnessParam.num}", 99)
		Integer brightmin = safeToInt(settings?."configParam${minimumBrightnessParam.num}", 1)
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
