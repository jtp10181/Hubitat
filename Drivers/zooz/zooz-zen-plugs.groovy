/*  
 *  Zooz ZEN Plugs Universal
 *    - Model: ZEN04, ZEN05 - All Firmware
 *
 *  For Support: https://community.hubitat.com/t/zooz-smart-plugs/98333
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [0.1.1] - 2021-07-31 (@jtp10181)
  ### Added
  - Support for ZEN05 Outdoor Plug
  ### Fixed
  - Wrong default for powerFailure

## [0.1.0] - 2021-07-28 (@jtp10181)
  - Initial Release, support for ZEN04 Only

 *  Copyright 2022 Jeff Page
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

@Field static final String VERSION = "0.1.1"
@Field static final Map deviceModelNames = ["7000:B002":"ZEN04", "7000:B001":"ZEN05"]

metadata {
	definition (
		name: "Zooz ZEN Plugs Advanced", 
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		//importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/"
	) {
		capability "Actuator"
		// capability "Switch"
		capability "Outlet"
		capability "PowerMeter"
		capability "CurrentMeter"
		capability "VoltageMeasurement"
		capability "EnergyMeter"
		capability "Configuration"
		capability "Refresh"
	
		command "refreshParams"
		//command "resetStats"
		//command "childDevices", [[name:"Select One*", type: "ENUM", constraints: ["Create","Remove"] ]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"7000", deviceId:"B002", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN04 Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B001", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN05 Outdoor Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B002", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x32,0x71,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A", deviceJoinName:"Zooz ZEN04 Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B001", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A", deviceJoinName:"Zooz ZEN05 Outdoor Plug"
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
				else if (param.range) {
					input "configParam${param.num}", "number",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Range: ${(param.range).toString()}, DEFAULT: ${param.defaultVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: paramVal,
						range: param.range,
						required: false
				}
			}
		}
	
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

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[ 
	ledIndicator: [ num: 1,
		title: "LED Indicator", 
		size: 1, defaultVal: 0, 
		options: [0:"LED On When Switch On", 1:"LED On When Switch Off", 2:"LED Always Off", 3:"LED Always On"],
	],
	ledBrightness: [ num: 9,
		title: "LED Brightness", 
		size: 1, defaultVal: 2, 
		options: [2:"Low", 1:"Medium", 0:"High"],
		changes: [05:[num:7]]
	],
	offTimer: [ num: 2,
		title: "Auto Turn-Off Timer", 
		size: 4, defaultVal: 0, 
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535
	],
	onTimer: [ num: 3,
		title: "Auto Turn-On Timer", 
		size: 4, defaultVal: 0, 
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535,
		changes: [05:[num:4]]
	],
	powerFailure: [ num: 4,
		title: "Behavior After Power Failure", 
		size: 1, defaultVal: 0, 
		options: [0:"Restores Last Status", 1:"Forced to Off", 2:"Forced to On"],
		changes: [05:[num:6]]
	],
	manualControl: [ num: null,
		title: "Physical Button On/Off Control", 
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
		changes:[05:[num:8]]
	],
	wattsThreshold: [ num: null,
		title: "Power Wattage (W) Reporting Threshold", 
		size: 1, defaultVal: 10,
		description: "Report when changes by this amount",
		range: 5..50,
		changes:[04:[num:5]]
	],
	wattsFrequency: [ num: null,
		title: "Power Wattage (W) Reporting Frequency", 
		size: 4, defaultVal: 60,
		description: "Minimum number of minutes between wattage reports",
		range: 1..65535,
		changes:[04:[num:6]]
	],
	currentThreshold: [ num: null,
		title: "Electrical Current (A) Reporting Threshold", 
		size: 1, defaultVal: 10,
		description: "[1 = 0.1A, 10 = 1A]  Report when changes by this amount",
		range: 1..10,
		changes:[04:[num:7]]
	],
	energyThreshold: [ num: null,
		title: "Energy (kWh) Reporting Threshold", 
		size: 1, defaultVal: 10,
		description: "[1 = 0.01 kWh, 100 = 1kWh]  Report when changes by this amount",
		range: 1..100,
		changes:[04:[num:8]]
	],
]

/* ZEN04
CommandClassReport - class:0x25, version:2
CommandClassReport - class:0x32, version:5
CommandClassReport - class:0x55, version:2
CommandClassReport - class:0x59, version:3
CommandClassReport - class:0x5A, version:1
CommandClassReport - class:0x5E, version:2
CommandClassReport - class:0x6C, version:1
CommandClassReport - class:0x70, version:4
CommandClassReport - class:0x71, version:8
CommandClassReport - class:0x72, version:2
CommandClassReport - class:0x73, version:1
CommandClassReport - class:0x7A, version:5
CommandClassReport - class:0x85, version:3
CommandClassReport - class:0x86, version:3
CommandClassReport - class:0x87, version:3
CommandClassReport - class:0x8E, version:4
CommandClassReport - class:0x9F, version:1
*/

//Command Classes Supported
@Field static final Map commandClassVersions = [
	0x25: 1,	// Switch Binary (switchbinary)
	0x32: 3,	// Meter
	//0x60: 3,	// Multi Channel
	0x6C: 1,	// Supervision
	0x70: 1,	// Configuration
	0x71: 8,	// Notification
	0x72: 2,	// ManufacturerSpecific
	0x73: 1,	// Powerlevel
	0x85: 2,	// Association
	0x86: 3,	// Version
	0x8E: 3,	// Multi Channel Association
	//0x98: 1,	// Security 0
	0x9F: 1		// Security S2
]

//Static Lists and Settings
@Field static final Map meterEnergy = [name:"energy", scale:0, unit:"kWh", limit:null]
@Field static final Map meterPower = [name:"power", scale:2, unit:"W", limit:2000]
@Field static final Map meterVoltage = [name:"voltage", scale:4, unit:"V", limit:150]
@Field static final Map meterCurrent = [name:"amperage", scale:5, unit:"A", limit:18]

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

	if (!state.deviceModel) { setDevModel(firmwareVersion) }

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
	state.isDigital = true
	return switchBinarySetCmd(0xFF)
}

String off() {
	logDebug "off..."
	state.isDigital = true
	return switchBinarySetCmd(0x00)
}

/*** Custom Commands ***/
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
}

//Decodes Multichannel Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
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

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
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
		logDebug "${param.name} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val}"
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
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	//Some send SwitchBinaryReport AND BasicReport, ignoring BasicReport
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	String type = (state.isDigital ? "digital" : "physical")
	state.remove("isDigital")
	sendSwitchEvents(cmd.value, type, ep)
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep=0) {
	logTrace "${cmd} (scaledMeterValue: ${cmd.scaledMeterValue}) (ep ${ep})"
	
	BigDecimal val = safeToDec(cmd.scaledMeterValue, 0, Math.min(cmd.precision,2))
	
	switch (cmd.scale) {
		case meterEnergy.scale:			
			sendEnergyEvents(val)
			break
		case meterPower.scale:
			sendMeterEvents(meterPower, val)
			break
		case meterVoltage.scale:
			sendMeterEvents(meterVoltage, val)
			break
		case meterCurrent.scale:
			sendMeterEvents(meterCurrent, val)
			break
		default:
			logDebug "Unknown Meter Scale: $cmd"
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
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

//Single Command
void sendCommands(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.ZWAVE))
}

//Consolidated zwave command functions so other code is easier to read
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

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String meterGetCmd(meter, Integer ep=0) {
	return secureCmd(zwave.meterV3.meterGet(scale: meter.scale), ep)
}

String meterResetCmd(Integer ep=0) {
	return secureCmd(zwave.meterV3.meterReset(), ep)
}

String configSetCmd(Map param, Integer value) {
	return secureCmd(zwave.configurationV1.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
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
		cmd = zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
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

	if (state.resyncAll) clearVariables()
	state.resyncAll = false

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	//Refresh Switch
	cmds << switchBinaryGetCmd()

	//Refresh Meters
	if (state.deviceModel == 'ZEN04') {
		cmds += [
			meterGetCmd(meterEnergy, endPoint),
			meterGetCmd(meterPower, endPoint),
			meterGetCmd(meterVoltage, endPoint),
			meterGetCmd(meterCurrent, endPoint)
		]
	}
	
	sendCommands(cmds,300)
}

void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel 

	//Clears State Variables
	state.clear()

	//Clear Data from other Drivers
	device.removeDataValue("configVals")
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
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	return cmds
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description is not passed in
	evt.descriptionText = desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Main Device Events
	if (evt.name != "syncStatus") {
		if (device.currentValue(evt.name).toString() != evt.value.toString()) {
			logInfo "${evt.descriptionText}"
		} else {
			logDebug "${evt.descriptionText} [NOT CHANGED]"
		}
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch was turned ${value} (${type})"
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)
}

void sendMeterEvents(meter, value, Integer ep=0) {
	sendEventLog(name:meter.name, value:value, unit:meter.unit, ep)
}

void sendEnergyEvents(value, Integer ep=0) {
	sendEventLog(name:meterEnergy.name, value:value, unit:meterEnergy.unit, ep)
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
	paramsMap['settings'] = [fixed: true]
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	String devModel = state.deviceModel
	BigDecimal firmware = firmwareVersion
	if (!devModel || devModel == "UNK00") return []

	verifyParamsList()

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


/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEventLog(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventLog(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

String setDevModel(BigDecimal firmware) {
	//Stash the model in a state variable
	def devTypeId = convertIntListToHexList([safeToInt(device.getDataValue("deviceType")),safeToInt(device.getDataValue("deviceId"))],4)
	String devModel = deviceModelNames[devTypeId.join(":")] ?: "UNK00"

	logDebug "Set Device Info - Model: ${devModel} | Firmware: ${firmware}"
	state.deviceModel = devModel
	device.updateDataValue("deviceModel", devModel)

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

Integer safeToInt(val, defaultVal=0) {
	if ("${val}"?.isInteger())		{ return "${val}".toInteger() } 
	else if ("${val}"?.isDouble())	{ return "${val}".toDouble()?.round() }
	else { return defaultVal }
}

BigDecimal safeToDec(val, defaultVal=0, roundTo=-1) {
	BigDecimal decVal = "${val}"?.isBigDecimal() ? "${val}".toBigDecimal() : defaultVal
	if (roundTo == 0)		{ decVal = Math.round(decVal) }
	else if (roundTo > 0)	{ decVal = decVal.setScale(roundTo, BigDecimal.ROUND_HALF_UP).stripTrailingZeros() }
	if (decVal.scale()<0)	{ decVal = decVal.setScale(0) }
	return decVal
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
