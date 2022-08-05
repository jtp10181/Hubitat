/*  
 *  ZWave Universal Scanner
 *
 *  For Support: https://community.hubitat.com/
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/
 *
 *  Changelog:

## Known Issues
  - Scanning multiple devices at once would be bad
  - Size 1 parameters that use unsigned integers and a ranger over 127 will have issues

## [0.1.0] - 2021-07-21 (@jtp10181)
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

@Field static final String VERSION = "0.1.0" 

metadata {
	definition (
		name: "ZWave Universal Scanner",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		//importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/zooz/"
	) {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"

		command "deleteChild", [[name:"Child DNI*", description:"DNI from Child or ALL to remove all", type: "STRING"]]
		command "scanParameters", [[name:"First Parameter", description:"Provide the first valid parameter to start scanning from", type: "NUMBER"]]
		command "syncFromDevice"
		command "setLogLevel", [[name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: debugOpts] ]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"
	}

	preferences {

		//Saved Parameters
		configParams.each { param ->
			//log.debug "${param}"
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
		input name: "infoText", type: "number",
			title: fmtTitle("HIGHLY RECOMMEND to sync from device and refresh the page before saving below!")
	}
}

String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}

String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}

void debugShowVars() {
	log.warn "paramScan ${paramScan.hashCode()} ${paramScan}"
	//log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	//log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	//log.warn "settings ${settings.hashCode()} ${settings}"
}

@Field static final Map commandClassVersions = [
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 3,	// Configuration (configurationv3) (4)
	0x84: 2,	// Wakeup (wakeupv2)
	0x86: 2,	// Version (versionv2) (3)
	0x9F: 1 	// Security 2
]

@Field static final Map debugOpts = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]

/*******************************************************************
 ***** Driver Commands
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

void setLogLevel(String selection) {
	Short level = debugOpts.find{ selection.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	logInfo "Logging Level is: ${level}"
}

void syncFromDevice() {
	sendEvent(name:"syncStatus", value:"About to Sync Settings from Device")
	state.deviceSync = true
	device.removeDataValue("configVals")

	List<String> cmds = []
	configParams.each { param ->
		logDebug "Getting ${param.title} (#${param.num}) from device"
		cmds += configGetCmd(param)
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

void scanParameters(param=1) {
	logDebug "scanParameters: Starting with #${param}"
	paramScan = [:]
	Map args = [parameterNumber: param]
	sendEvent(name:"scanStatus", value:"Scanning ($param)")
	def cmd = new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args)
	//logDebug "Sending: ${cmd.format()} - ${cmd}"

	sendCommands(secureCmd(cmd))
}

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	//logTrace "parse: ${description} -- ${commandClassVersions}"
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
	
	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else {
		logWarn "Unable to parse: ${description}"
	}
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

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
	logTrace "${cmd}"

	String subVersion = String.format("%02d", cmd.firmware0SubVersion)
	String fullVersion = "${cmd.firmware0Version}.${subVersion}"
	device.updateDataValue("firmwareVersion", fullVersion)

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	//setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationReport cmd) {
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
		logDebug "Parameter #${cmd.parameterNumber} = ${val}"
	}

	if (state.deviceSync) {
		device.updateSetting("configParam${cmd.parameterNumber}", [value: val, type:"number"])
	}
}


void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationPropertiesReport cmd) {
	logTrace "${cmd}"
	List<String> cmds = []

	//Skip if size=0 (invalid param)
	if (cmd.size) {
		//Save The Properties
		paramScan[cmd.parameterNumber] = [
			num: cmd.parameterNumber, format: cmd.format,
			size: cmd.size, defaultVal: cmd.defaultValue,
			range: "${cmd.minValue}..${cmd.maxValue}",
			title: "Parameter #${cmd.parameterNumber}",
			description: ""
		]
		//Request Name and Info
		Map args = [parameterNumber: cmd.parameterNumber]
		cmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationNameGet(args))
		cmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationInfoGet(args))
	}

	//Request Next Paramater if there is one
	if (cmd.nextParameterNumber) {
		logDebug "Found Param $cmd.parameterNumber and saved, next scanning #$cmd.nextParameterNumber"
		Map args = [parameterNumber: cmd.nextParameterNumber]
		sendEvent(name:"scanStatus", value:"Scanning ($cmd.nextParameterNumber)")
		cmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args))
	}
	else {
		logDebug "Found Param $cmd.parameterNumber and saved, that was the last one"
		sendEvent(name:"scanStatus", value:"Scanning Final Cleanup...")
		runIn(4, processParamScan)
	}

	//logDebug "Sending: ${cmds}"
	if (cmds) sendCommands(cmds)
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationNameReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber].titleTmp == null) paramScan[cmd.parameterNumber].titleTmp = []
	paramScan[cmd.parameterNumber].titleTmp[cmd.reportsToFollow] = "$cmd.name"
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationInfoReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber].descTmp == null) paramScan[cmd.parameterNumber].descTmp = []
	paramScan[cmd.parameterNumber].descTmp[cmd.reportsToFollow] = "$cmd.info"
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
}


/*******************************************************************
 ***** Z-Wave Commands
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
	return secureCmd(zwave.configurationV3.configurationSet(parameterNumber: param.num, size: param.size, scaledConfigurationValue: value))
}

String configGetCmd(Map param) {
	return secureCmd(zwave.configurationV3.configurationGet(parameterNumber: param.num))
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

	updateSyncingStatus(6)

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()

	return cmds ?: []
}

void clearVariables() {
	device.removeDataValue("configVals")
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
			logWarn("Clearing Invalid configVals: ${e}")
			device.removeDataValue("configVals")
		}
	}
	return configsMap
}

//Parameter List Functions
//paramScan Structure: PARAM_NUM:[PARAM_MAPS]
@Field static Map<String, Map> paramScan = new java.util.concurrent.ConcurrentHashMap()

//Process the scanned parameters and save to data
void processParamScan() {
	List<Map> paramsMap = []
	paramScan.each { k, v ->
		paramScan[k].title = ""
		paramScan[k].description = ""
		v.titleTmp.reverseEach { value ->
			if (value!=null) paramScan[k].title += "$value" }
		v.descTmp.reverseEach { value -> 
			if (value!=null) paramScan[k].description += "$value" }
		//Don't needs this once processed
		paramScan[k].remove("titleTmp")
		paramScan[k].remove("descTmp")
		//Save to List
		paramsMap += paramScan[k]
	}
	//Dump list to device data
	device.updateDataValue("parameters", paramsMap.inspect())
	sendEvent(name:"scanStatus", value:"Scan Complete - REFRESH the Page")
	logDebug "processParamScan Completed "
}

//Gets full list of params
List<Map> getConfigParams() {
	if (!device) return []
	//logDebug "Get Config Params"

	List<Map> paramsMap = []
	String configsStr = device.getDataValue("parameters")

	if (configsStr) {
		try {
			paramsMap = evaluate(configsStr)
		}
		catch(Exception e) {
			logWarn("Invalid dataValue (parameters): ${e}")
			//device.removeDataValue("parameters")
		}
	}
	//log.debug "${paramsMap}"
	return paramsMap
}

//Get a single param by name or number
Map getParam(def search) {
	//logDebug "Get Param (${search} | ${search.class})"
	Map param = [:]

	if (search instanceof String) {
		param = configParams.find{ it.name == search }
	} else {
		param = configParams.find{ it.num == search }
	}

	return param
}

//Convert Param Value if Needed
private getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
private getParamValue(Map param, Boolean adjust=false) {
	Integer paramVal = safeToInt(settings."configParam${param.num}", param.defaultVal)
	return paramVal
}

//Other Helper Functions
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

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}

List convertIntListToHexList(intList) {
	def hexList = []
	intList?.each {
		hexList.add(Integer.toHexString(it).padLeft(2, "0").toUpperCase())
	}
	return hexList
}

private safeToInt(val, defaultVal=0) {
	if ("${val}"?.isInteger())		{ return "${val}".toInteger() } 
	else if ("${val}"?.isDouble())	{ return "${val}".toDouble()?.round() }
	else { return defaultVal }
}

private safeToDec(val, defaultVal=0, roundTo=-1) {
	def decVal = "${val}"?.isBigDecimal() ? "${val}".toBigDecimal() : defaultVal
    if (roundTo == 0)       { decVal = Math.round(decVal) }
    else if (roundTo > 0)   { decVal = decVal.doubleValue().round(roundTo) }
	return decVal
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
void logsOff(){
	// logWarn "${device.displayName}: debug logging disabled..."
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
