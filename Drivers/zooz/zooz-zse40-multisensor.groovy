/*
 *  Zooz ZSE40 4-in-1 Multisensor
 *    - Model: ZSE40 - MINIMUM FIRMWARE 32.02
 *
 *  Changelog:

## [0.1.0] - 2021-09-29 (@jtp10181)
  ### Added
  - Initial Release, supports all known settings and features except associations

NOTICE: This file has been created by *Jeff Page* with some code used 
	from the original work of *Zooz* and *Kevin LaFramboise* under compliance with the Apache 2.0 License.

Below link is for original source (Kevin LaFramboise @krlaframboise)

 *  Copyright 2021 Jeff Page
 *  Copyright ????? Zooz
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

@Field static final String VERSION = "0.1.0" 
@Field static final Map deviceModelNames = ["2021:2101":"ZSE40"]

metadata {
	definition (
		name: "Zooz ZSE40 4-in-1 Multisensor",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: ""
	) {
		capability "Sensor"
		capability "Configuration"
		capability "Motion Sensor"
		capability "Illuminance Measurement"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Battery"
		capability "Tamper Alert"
		//capability "Refresh"

		//command "paramCommands", [[name:"Select Command*", type: "ENUM", constraints: ["Refresh"] ]]

		//DEBUGGING
		//command "debugShowVars"

		//attribute "assocDNI2", "string"
		//attribute "assocDNI3", "string"
		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"2021", deviceId:"2101", deviceJoinName:"Zooz ZSE40 4-in-1 Multisensor"
	}

	preferences {

		configParams.each { param ->
			if (!param.hidden) {
				if (param.options) {
					input "configParam${param.num}", "enum",
						title: "${param.title} (#${param.num}):",
						description: param?.description,
						defaultValue: param.defaultVal,
						options: param.options,
						required: false
				}
				else if (param.range) {
					input "configParam${param.num}", "number",
						title: "${param.title} (#${param.num}):",
						description: "${param?.description ?: ''} (Range: ${param.range}, DEFAULT: ${param.defaultVal})",
						defaultValue: param.defaultVal,
						range: param.range,
						required: false
				}
			}
		}

		// input "assocDNI2", "string",
		// 	title: "Device Associations - Group 2:",
		// 	description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
		// 	required: false

		// input "assocDNI3", "string",
		// 	title: "Device Associations - Group 3:",
		// 	description: "Associations are an advanced feature, only use if you know what you are doing. Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. (Can save as blank or 0 to clear)",
		// 	required: false

		// input "supervisionGetEncap", "bool",
		// 	title: "Supervision Encapsulation Support (BETA):",
		// 	description: "This can increase reliability when the device is paired with security if implemented correctly on the device.",
		// 	defaultValue: false

		//Logging options similar to other Hubitat drivers
		input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: false
		input name: "debugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: true
	}
}

void debugShowVars() {
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
}

@Field static final int maxAssocGroups = 5
@Field static final int maxAssocNodes = 5

@Field static Map<String, Map> paramsMap =
[
	tempUnits: [ num:1, 
		title: "Temperature Units:",
		size: 1, defaultVal: 1, 
		options: [0:"Celsius (°C)", 1:"Fahrenheit (°F)"]
	],
	tempTrigger: [ num:2, 
		title: "Temperature Change Trigger (1 = 0.1° / 10 = 1°)", 
		size: 1, defaultVal: 10, 
		range: "1..50"
	],
	humidityTrigger: [ num:3, 
		title: "Humidity Change Trigger (%)", 
		size: 1, defaultVal: 10, 
		range: "1..50"
	],
	lightTrigger: [ num:4, 
		title: "Light Change Trigger (%)", 
		size: 1, defaultVal: 10, 
		range: "5..50"
	],
	motionClear: [ num:5, 
		title: "Motion Clear Delay / Timeout (seconds)", 
		size: 1, defaultVal: 15, 
		range: "15..255"
	],
	motionSesnitivity: [ num:6, 
		title: "Motion Sensitivity", 
		size: 1, defaultVal: 3, 
		options: [1:"1 - Most Sensitive", 2:"2", 3:"3", 4:"4", 5:"5", 6:"6", 7:"7 - Least Sensitive"]
	],
	ledMode: [ num:7, 
		title: "LED Indicator Mode", 
		size: 1, defaultVal: 4, 
		options: [1:"LED Disabled", 2:"Motion Flash / Temp Pulse", 3:"Motion Flash / Temp Flash (every 3 mins)", 4:"Motion Flash / Temp None"],
		changes: ['ZSE40-700':[defaultVal: 3, options: [1:"LED Disabled", 2:"Motion Flash / Temp Flash (every 3 mins)", 3:"Motion Flash / Temp None"]]],
	],
	group1Report: [ num:8, 
		title: "Group 1 (Hub) Reporting", 
		size: 1, defaultVal: (-1), 
		options: [0:"Notification Reports Only", (-1):"Notification AND Basic Reports"]
	],
]

@Field static final Map commandClassVersions = [
	0x20: 1,	// Basic (basicv1)
	0x31: 11,	// Sensor Multilevel (sensormultilevelv11) ************
	0x59: 1,	// Association Grp Info (associationgrpinfov1)
	0x5A: 1,	// Device Reset Locally	(deviceresetlocallyv1)
	0x5E: 2,	// ZWave Plus Info (zwaveplusinfov2)
	0x70: 2,	// Configuration (configurationv2) ******************
	0x71: 8,	// Notification (notificationv8) (8) ****************
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x73: 1,	// Power Level (powerlevelv1)
	0x7A: 2,	// Firmware Update Md (firmwareupdatemdv2)
	0x80: 1,	// Battery (batteryv1)
	0x84: 2,	// Wakeup *****************
	0x85: 2,	// **** Association (associationv3) *****************
	0x86: 3,	// Version (versionv3) ******************
	0x98: 1,	// Security (securityv1)
	0x9F: 1     // Security 2
]

//Sensor Types
@Field static int sensorTemp = 0x01
@Field static int sensorIlum = 0x03
@Field static int sensorHumid = 0x05
//Notification Types
@Field static int notiHomeSecurity = 0x07
//Notification Events
@Field static int eventIdle = 0x00
@Field static int eventTamper = 0x03
@Field static int eventMotion = 0x08

/*******************************************************************
 ***** Driver Commands
********************************************************************/
void installed() {
	log.warn "installed..."
}

void initialize() {
	log.warn "initialize..."
	executeRefreshCmds()
	runIn(1, executeConfigureCmds)
}

void configure() {
	log.warn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		state.resyncAll = true
	}

	updateSyncingStatus(1)
	//runIn(2, executeRefreshCmds)
	//runIn(6, executeConfigureCmds)
}

List<String> updated() {
	log.info "updated..."
	log.warn "Debug logging is: ${debugEnable == true}"
	log.warn "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	//runIn(1, executeConfigureCmds)
	updateSyncingStatus(1)
	return []
}

def refresh() {
	logDebug "refresh..."
	//executeRefreshCmds()
}

void paramCommands(String str) {
	switch (str) {
		case "Refresh":
			paramsRefresh()
			break
		default:
			log.warn "paramCommands invalid input: ${str}"
	}
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

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
	logTrace "parse: ${description} --PARSED-- ${cmd}"

	if (cmd) {
		zwaveEvent(cmd)
	} else {
		log.warn "Unable to parse: $description"
	}

	updateLastCheckIn()
}

//Decodes Secure Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Multichannel Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
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

//Reports back from Supervision Encapsulated Commands
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

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
	logTrace "${cmd}"

	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"
	device.updateDataValue("firmwareVersion", fullVersion)

	//Stash the model in a state variable
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))]).collect{ it.padLeft(4,'0') }
	def devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"

	//Extra check for ZSE40 (700 Series)
	if (devModel == "ZSE40" && (new BigDecimal(fullVersion)) >= 32.32 && getDataValue("inClusters").contains("0x9F")) {
		devModel = "ZSE40-700"
	}

	logDebug "Received Version Report - Model: ${devModel} | Firmware: ${fullVersion}"
	state.deviceModel = devModel

	if (devModel == "UNK00") {
		log.warn "Unsupported Device USE AT YOUR OWN RISK: ${devTypeId}"
		state.WARNING = "Unsupported Device Model - USE AT YOUR OWN RISK!"
	}
	else state.remove("WARNING")
}

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)

	if (param) {
		Integer val = cmd.scaledConfigurationValue
		String displayVal = cmd.scaledConfigurationValue.toString()
		logDebug "${param.name} (#${param.num}) = ${displayVal}"
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
		//sendEventIfNew("assocDNI$grp", dnis ?: "none", false)
		device.updateSetting("assocDNI$grp", [value:"${dnis}", type:"string"])
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	Integer lvl = cmd.batteryLevel
	if (lvl == 0xFF) {
		lvl = 1
		log.warn "${device.displayName} LOW BATTERY WARNING"
	}

	val = validateRange(val, 100, 1, 100)

	String descText = "battery level is ${lvl}%"
	logTxt(descText)
	sendEvent(name:"battery", value: lvl, unit:"%", descriptionText: descText, isStateChange: true)
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	logDebug "WakeUpNotification"
	
	//List<String> cmds = getPendingConfigureAndRefreshCommands()
	//cmds << zwave.wakeUpV2.wakeUpNoMoreInformation().format()  
	// state.pendingConfigure = false
	// state.initialized = true

	//DEBUGGING
	executeRefreshCmds()
	runIn(1, executeConfigureCmds)
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendEventIfNew("motion", cmd.value ? "active":"inactive")
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"	
	switch (cmd.sensorType) {
		case sensorTemp:
			def temp = convertTemperatureIfNeeded(cmd.scaledSensorValue, (cmd.scale ? "F" : "C"), cmd.precision)			
			sendEventIfNew("temperature", temp, true, "", temperatureScale)
			break
		case sensorIlum:
			sendEventIfNew("illuminance", cmd.scaledSensorValue, true, "", "%")
			break
		case sensorHumid:
			sendEventIfNew("humidity", cmd.scaledSensorValue, true, "", "%")
			break
		default:
			logDebug "Unhandled sensorType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	switch (cmd.notificationType) {
		case notiHomeSecurity:
			sendSecurityEvent(cmd.event, cmd.eventParameter[0])
			break
		default:
			logDebug "Unhandled notificationType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


/*******************************************************************
 ***** Z-Wave Commands
********************************************************************/
//These send commands to the device either a list or a single command
void sendCommands(List<String> cmds, Long delay=400) {
	//Calculate supervisionCheck delay based on how many commands
	Integer packetsCount = supervisedPackets?."${device.id}"?.size()
	if (packetsCount > 0) {
		Integer delayTotal = (cmds.size() * delay) + 2000
		logDebug "Setting supervisionCheck to ${delayTotal}ms | ${packetsCount} | ${cmds.size()} | ${delay}"
		runInMillis(delayTotal, supervisionCheck, [data:1])
	}

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
	return secureCmd(zwave.versionV3.versionGet())
}

String batteryGetCmd() {
	return secureCmd(zwave.batteryV1.batteryGet())
}

String basicGetCmd() {
	return secureCmd(zwave.basicV1.basicGet())
}

String sensorBinaryGetCmd() {
	return secureCmd(zwave.sensorBinaryV1.sensorBinaryGet())
}

String switchBinarySetCmd(Integer value) {
	return supervisionEncap(zwave.switchBinaryV1.switchBinarySet(switchValue: value))
}

String switchBinaryGetCmd() {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet())
}

String sensorMultilevelGetCmd(sensorType) {
	int scale = 0x00
	return secureCmd(zwave.sensorMultilevelV11.sensorMultilevelGet(scale: scale, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType) {
	return secureCmd(zwave.notificationV8.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType))
}

String configSetCmd(Map param, Integer value) {
	return supervisionEncap(zwave.configurationV2.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV2.configurationGet(parameterNumber: param.num))
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
		cmd = zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}

//====== Supervision Encapsulate START ======\\
@Field static Map<String, Map<Short, String>> supervisedPackets = [:]
@Field static Map<String, Short> sessionIDs = [:]

String supervisionEncap(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "supervisionEncap: ${cmd} (ep ${ep})"

	if (settings.supervisionGetEncap) {
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


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
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

	if (cmds) {
		updateSyncingStatus(6)
		sendCommands(cmds)
	}
}

void executeRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << batteryGetCmd()
	cmds << sensorMultilevelGetCmd(sensorTemp)
	cmds << sensorMultilevelGetCmd(sensorIlum)
	cmds << sensorMultilevelGetCmd(sensorHumid)

	//These dont work
	//cmds << notificationGetCmd(notiHomeSecurity, eventTamper)
	//cmds << notificationGetCmd(notiHomeSecurity, eventMotion)

	//Seems like this needs a longer delay
	//cmds << secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation())

	sendCommands(cmds)
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

List getConfigureAssocsCmds() {
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
			//sendEventIfNew("assocDNI$i", "none", false)
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
	String dni = getAssocDNIsSetting(grp)
	List nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		log.warn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		log.warn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
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


/*******************************************************************
 ***** Event Senders
********************************************************************/
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

void sendSecurityEvent(event, parameter) {
	def cleared
	//Idle Event the parameter is the event to clear
	if (event == eventIdle) {
		event = parameter
		cleared = true
	}
	
	switch (event) {
		case eventTamper:
			sendEventIfNew("tamper", cleared ? "clear":"detected")
			break
		case eventMotion:
			sendEventIfNew("motion", cleared ? "inactive":"active")
			break
		default:
			logDebug "Unhandled event: ${event}, ${parameter}"
	}
}


/*******************************************************************
 ***** Common Functions
********************************************************************/
//Parameter Store Map Functions
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

//Parameter List Functions
//This will rebuild the list for the current model and firmware only as needed
//paramsList Structure: MODEL:[FIRMWARE:PARAM_MAPS]
//PARAM_MAPS [num, name, title, description, size, defaultVal, options, firmVer]
@Field static Map<String, Map<String, List>> paramsList = [:]
void updateParamsList() {
	logDebug "Update Params List"
	String devModel = state.deviceModel
	Short modelNum = deviceModelShort
	Short modelSeries = Math.floor(modelNum/10)
	BigDecimal firmware = firmwareVersion
	log.debug "${devModel} | ${modelNum} | ${modelSeries}"

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

	//Save it to the static list
	if (paramsList[devModel] == null) paramsList[devModel] = [:]
	paramsList[devModel][firmware] = tmpList
}

//Verify the list and build if its not populated
void verifyParamsList() {
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (paramsList[devModel] == null) updateParamsList()
	if (paramsList[devModel][firmware] == null) updateParamsList()
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (!devModel || devModel == "UNK00") return []

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

//Other Helper Functions
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEventIfNew("syncStatus", "Syncing...", false)
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventIfNew("syncStatus", (changes ?  "${changes} Pending Changes" : "Synced"), false)
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

List convertIntListToHexList(intList) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(2, "0").toUpperCase())
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


/*******************************************************************
 ***** Logging Functions
********************************************************************/
void debugLogsOff(){
	log.warn "${device.displayName}: debug logging disabled..."
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
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
