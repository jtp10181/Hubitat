/*
 *  Zooz ZEN17 PARAMETER COMPANION DRIVER
 *    - Model: ZEN17 - MINIMUM FIRMWARE 1.04
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-relays-advanced/98194
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.0.2] - 2022-02-28 (@jtp10181)
  - Minor cleanup and code sync with shared functions

## [1.0.0] - 2021-07-26 (@jtp10181)
  ### Added
  - Drop down menus for parameters where feasible
  - Automatically figure out reverse setting based on trigger
  ### Fixed
  - Race condition with configVals (now keeping copy in static var)

## [0.1.0] - 2021-07-18 (@jtp10181)
  - Initial Release

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

@Field static final String VERSION = "1.0.2"
@Field static final Map deviceModelNames = ["7000:A00A":"ZEN17"]

metadata {
	definition (
		name: "Zooz ZEN17 Companion Driver",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zen17-companion-driver.groovy"
	) {
		capability "Actuator"
		//capability "Configuration"
		capability "Refresh"

		command "syncFromDevice"
		command "deleteChild", [[name:"Child DNI*", description:"DNI from Child or ALL to remove all", type: "STRING"]]
		command "setLogLevel", [[name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: debugOpts] ]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"
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

		//Logging Level Options
		input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"), defaultValue: 3, options: debugOpts

		//Help Text at very bottom
		input name: "infoText", type: "hidden", title: fmtTitle("HIGHLY RECOMMENDED"),
			description: fmtDesc("Sync from device and refresh the page before saving below!")
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
	log.warn "configsList ${configsList.hashCode()} ${configsList}"
	log.warn "settings ${settings.hashCode()} ${settings}"
}

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	p1: [ num:1,
		title: "On / Off Status After Power Failure",
		size: 1, defaultVal: 1,
		//range: "0..4"
		options: [
			0:"All relays forced OFF",
			1:"All relays restores last state",
			2:"All relays forced ON",
			3:"Relay 1 restores last, Relay 2 forced ON",
			4:"Relay 2 restores last, Relay 1 forced ON",
		]

	],
	p2: [ num:2,
		title: "Input Type for S1 C terminals",
		size: 1, defaultVal: 2,
		//range: "0..11"
		options: [
			0:"Momentary (for lights only)",
			1:"Toggle Switch On/Off",
			2:"Toggle Switch State Change",
			3:"Garage Door Momentary (for Z-Wave control)",
			4:"Water Sensor",
			5:"Heat Sensot",
			6:"Motion Sensor",
			7:"Contact Sensor",
			8:"Carbon Monoxide (CO) Sensor",
			9:"Carbon Dioxide (CO₂) Sensor",
			10:"Dry Contact Switch/Sensor",
			11:"Relay- Garage Door / Input- Contact Sensor"
		]
	],
	p3: [ num:3,
		title: "Input Type for S2 C terminals",
		size: 1, defaultVal: 2,
		//range: "0..11"
		options: [
			0:"Momentary (for lights only)",
			1:"Toggle Switch On/Off",
			2:"Toggle Switch State Change",
			3:"Garage Door Momentary (for Z-Wave control)",
			4:"Water Sensor",
			5:"Heat Sensot",
			6:"Motion Sensor",
			7:"Contact Sensor",
			8:"Carbon Monoxide (CO) Sensor",
			9:"Carbon Dioxide (CO₂) Sensor",
			10:"Dry Contact Switch/Sensor",
			11:"Relay- Garage Door / Input- Contact Sensor"
		]
	],
	p10: [ num:10,
		title: "Input Trigger for Relay 1",
		description: "Should relay input automatically trigger the load?",
		size: 1, defaultVal: 1,
		options: [0:"Disabled",1:"Enabled"]
	],
	p11: [ num:11,
		title: "Input Trigger for Relay 2",
		description: "Should relay input automatically trigger the load?",
		size: 1, defaultVal: 1,
		options: [0:"Disabled",1:"Enabled"]
	],
	p24: [ num:24,
		title: "DC Motor Mode",
		description: "Sync R1 and R2 together to prevent them from being activated at the same time",
		size: 1, defaultVal: 0,
		options: [0:"Disabled",1:"Enabled"]
	],
	p19: [ num:19,
		title: "Reverse reported values on S1",
		description: "See online device docs for which triggers allow this",
		size: 1, defaultVal: 0,
		//range: "0,4..10",
		firmVer: 1.10,
		options: [0:"Disabled",1:"Enabled"],
		// options: [
		// 	0:"Disabled (report normally)",
		// 	4:"Water Sensor",
		// 	5:"Heat Sensot",
		// 	6:"Motion Sensor",
		// 	7:"Contact Sensor",
		// 	8:"Carbon Monoxide (CO) Sensor",
		// 	9:"Carbon Dioxide (CO₂) Sensor",
		// 	10:"Dry Contact Switch/Sensor",
		// ]
	],
	p20: [ num:20,
		title: "Reverse reported values on S2",
		description: "See online device docs for which triggers allow this",
		size: 1, defaultVal: 0,
		//range: "0,4..10",
		firmVer: 1.10,
		options: [0:"Disabled",1:"Enabled"],
		// options: [
		// 	0:"Disabled (report normally)",
		// 	4:"Water Sensor",
		// 	5:"Heat Sensot",
		// 	6:"Motion Sensor",
		// 	7:"Contact Sensor",
		// 	8:"Carbon Monoxide (CO) Sensor",
		// 	9:"Carbon Dioxide (CO₂) Sensor",
		// 	10:"Dry Contact Switch/Sensor",
		// ]
	],
	p5: [ num:5,
		title: "LED Indicator Control",
		size: 1, defaultVal: 0,
		//range: "0..3"
		options: [
			0:"LED on when ALL relays off",
			1:"LED on when ANY relays on",
			2:"LED Indicator always off",
			3:"LED Indicator always on",
		]
	],
	p6: [ num:6,
		title: "Auto Turn-Off Relay 1: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	p15: [ num:15,
		title: "Auto Turn-Off Relay 1: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	p7: [ num:7,
		title: "Auto Turn-On Relay 1: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	p16: [ num:16,
		title: "Auto Turn-On Relay 1: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	p8: [ num:8,
		title: "Auto Turn-Off Relay 2: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	p17: [ num:17,
		title: "Auto Turn-Off Relay 2: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
	p9: [ num:9,
		title: "Auto Turn-On Relay 2: TIME",
		size: 4, defaultVal: 0,
		range: "0..65535"
	],
	p18: [ num:18,
		title: "Auto Turn-On Relay 2: UNITS",
		size: 1, defaultVal: 0,
		options: [0:"minutes",1:"seconds",2:"hours"]
	],
]

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x60: 3,	// Multi Channel
	0x6C: 1,	// Supervision
	0x70: 2,	// Configuration
	0x86: 2,	// Version
]

/*** Static Lists and Settings ***/
@Field static final Map debugOpts = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
}

List<String> configure() {
	logWarn "configure..."

	sendEvent(name:"syncStatus", value:"Save Preferences to finish Configure")
	state.resyncAll = true
	return []
}

List<String> updated() {
	logDebug "updated..."
	logInfo "Logging Level is: ${logLevel}"
	state.remove("deviceSync")
	List<String> cmds = getConfigureCmds()
	return cmds ? delayBetween(cmds,200) : []
}

List<String> refresh() {
	logDebug "refresh..."
	List<String> cmds = getRefreshCmds()
	return cmds ? delayBetween(cmds,200) : []
}


/*******************************************************************
 ***** Driver Commands
********************************************************************/
void setLogLevel(String selection) {
	Short level = debugOpts.find{ selection.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	logInfo "Logging Level is: ${level}"
}

void syncFromDevice() {
	sendEvent(name:"syncStatus", value:"About to Sync Settings from Device")
	state.deviceSync = true
	device.removeDataValue("configVals")
	configsList["${device.id}"] = [:]

	List<String> cmds = []
	configParams.each { param ->
		logDebug "Getting ${param.title} (#${param.num}) from device"
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

void deleteChild(String dni) {
	logDebug "deleteChild: ${dni}"
	if (dni == "ALL") {
		childDevices.each { child ->
			deleteChildDevice(child.deviceNetworkId)
		}
	}
	else {
		deleteChildDevice(dni)
	}
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

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		//if (val < 0 && param.size == 1) { val += 256 } //Convert scaled signed integer to unsigned
		logDebug "${param.title} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
	}

	if (state.deviceSync) {
		device.updateSetting("configParam${cmd.parameterNumber}", [value: val, type:"number"])
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

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String configSetCmd(Map param, Integer value) {
	//if (value > 127 && param.size == 1) { value -= 256 }
	return secureCmd(zwave.configurationV2.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
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
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
List<String> getConfigureCmds() {
	logDebug "getConfigureCmds..."

	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion) {
		cmds << versionGetCmd()
	}

	configParams.each { param ->
		Integer paramVal = getParamValue(param, true)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.title} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	if (state.resyncAll) clearVariables()
	state.remove("resyncAll")

	if (cmds) updateSyncingStatus(6)

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()

	return cmds ?: []
}

void clearVariables() {
	device.removeDataValue("configVals")
	configsList["${device.id}"] = [:]
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}

	return (configChanges)
}


/*******************************************************************
 ***** Event Senders
********************************************************************/


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
Map getParam(search) {
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

	switch(param.num) {
		case 19..20: //Reverse Params
			if (paramVal > 0) {
				Map trigParam = getParam(param.num-17)
				Integer trigVal = getParamValue(trigParam)
				if (trigVal >=4 && trigVal <=10) {
					paramVal = trigVal
				}
				else if (trigVal > 0) { //Cannot Enable
					logWarn "Cannot Reverse when ${trigParam.title} = ${trigVal}"
					device.updateSetting("configParam${param.num}", [value:"0",type:"enum"])
					paramVal = 0
				}
			}
			break
	}

	return paramVal
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
	if (changes==0 && state.deviceSync) {
		sendEvent(name:"syncStatus", value:"REFRESH the Page, then Save Preferences")
		state.remove("deviceSync")
	}
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

List convertIntListToHexList(intList, pad=2) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(pad, "0").toUpperCase())
	}
	return hexList
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


/*******************************************************************
 ***** Logging Functions
********************************************************************/
void logsOff() {
	// logWarn "Debug logging disabled..."
	// device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void logWarn(String msg) {
	if (safeToInt(logLevel,3)>=1) log.warn "${device.displayName}: ${msg}"
}

void logInfo(String msg) {
	if (safeToInt(logLevel,3)>=2) log.info "${device.displayName}: ${msg}"
}

void logDebug(String msg) {
	if (safeToInt(logLevel,3)>=3) log.debug "${device.displayName}: ${msg}"
}

void logTrace(String msg) {
	if (safeToInt(logLevel,3)>=4) log.trace "${device.displayName}: ${msg}"
}
