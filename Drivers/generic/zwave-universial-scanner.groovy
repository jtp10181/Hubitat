/*  
 *  Z-Wave Universal Scanner
 *
 *  For Support, Information and Updates:
 *  https://community.hubitat.com/t/zwave-universal-scanner/97912
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/
 *

Changelog:

## Known Issues
  - Do not try to scan multiple devices at once

## [0.3.0] - 2024-06-28 (@jtp10181)
  - Pushing some older changes up in prep for more updates
  - Added ability to set multichannel lifeline
  - Added Set Wake Interval command
  - Added Set Parameter command (to manually set parameters)
  - Added Command Class Report
  - Added command to remove states and data entries from device
  - Fixed range expansion issue with Hub Mesh (can cause java.lang.OutOfMemoryError)

## [0.2.0] - 2021-08-06 (@jtp10181)
  ### Added
  - Get Info command to fetch device info and restore to data fields
  - Get Info command also prints fingerprint to logs same as the 'Device' driver
  - Set Lifeline Association command, useful after firmware updates if it gets cleared
  - Made scanning for Name and Info optional (some devices hang on these)
  ### Fixed
  - Was unable to update settings if created as different type, fixed by removing before setting
  - Will handle signed or unsigned parameter values based on format specified in report
  - Other minor fixes merged from my other drivers

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

@Field static final String VERSION = "0.3.0" 

metadata {
	definition (
		name: "Z-Wave Universal Scanner",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/generic/zwave-universial-scanner.groovy"
	) {
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"

		command "getInfo"
		command "commandClassReport"

		command "setLifelineAssociation", [[name:"Select Option*", type: "ENUM", constraints: ["Single Channel", "Multi-Channel"]] ]
		command "setWakeInterval", [[name:"Wake Up Interval", description:"Wake Up Interval (in hours)", type: "NUMBER"]]

		command "scanParameters", [[name:"First Parameter", description:"Provide the first valid parameter to start scanning from", type: "NUMBER"]]
		command "syncFromDevice"
		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		command "deleteChild", [[name:"Child DNI*", description:"DNI from Child or ALL to remove all", type: "STRING"]]
		command "removeData",[[name:"dataType*", type: "ENUM", description: "Type of Data to Remove", constraints: ["State", "StateVariable", "DeviceData"]],
							  [name:"dataName*",type:"STRING", description:"Enter exact name of field to delete"]]

		command "setLogLevel", [[name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: debugOpts] ]

		//DEBUGGING
		// command "debugShowVars"
		// command "testCommands"

		attribute "syncStatus", "string"
	}

	preferences {

		//Saved Parameters
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
						range: "${(param.range).toString()}",
						required: false
				}
			}
		}

		//Help Text at very bottom
		input name: "infoText", type: "hidden",
			title: fmtTitle("******************************************<br>" +
				"HIGHLY RECOMMEND after Scan Parameters to Sync from Device and then Refresh the page before saving below for the first time!")

		input "scanName", "bool", title: fmtTitle("Scan for Parameter Name"), defaultValue: false
		input "scanInfo", "bool", title: fmtTitle("Scan for Parameter Info"), defaultValue: false

		//Logging Level Options
		input name: "logLevel", type: "enum", title: fmtTitle("Logging Level (Permanent)"), defaultValue: 3, options: debugOpts


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
	log.warn "paramScan ${paramScan.hashCode()} ${paramScan}"
	//log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	//log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	log.warn "settings ${settings.hashCode()} ${settings}"
}

void testCommands() {
	List<String> cmds = []
	//Request NIF
	// sendHubCommand(new hubitat.device.HubAction("0102", hubitat.device.Protocol.ZWAVE))
	// cmds << zwave.zwaveCmdClassV1.requestNodeInfo() 
	// sendCommands(cmds)
}

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x60: 3,	// Multi Channel
	0x6C: 1,	// Supervision (supervision)
	0x70: 4,	// Configuration (configuration)
	0x72: 2,	// Manufacturer Specific (manufacturerspecific)
	0x86: 2,	// Version (version)
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
	refreshSyncStatus()
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
	configsList["${device.idAsLong}"] = [:]

	List<String> cmds = []
	configParams.each { param ->
		device.removeSetting("configParam${param.num}")
		logDebug "Getting ${param.title} (#${param.num}) from device"
		cmds += configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

void getInfo() {
	List<String> cmds = []
	cmds += getRefreshCmds()
	cmds << mfgSpecificGetCmd()
	cmds << deviceSpecificGetCmd()
	sendCommands(cmds)
}

void setLifelineAssociation(chan) {
	List<String> cmds = []
	logDebug "Setting lifeline association for ${chan}"

	//Remove all group 1 associations
	cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[], multiChannelNodeIds:[]))

	if (chan == "Multi-Channel") {
		cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationSet(groupingIdentifier: 1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress:0, endPointId: 0]]))
		cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: group))
	}
	else {
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	sendCommands(cmds)
}

void commandClassReport() {
	List<String> cmds = []
	List<Integer> ic = getDataValue("inClusters").split(",").collect{ hexStrToUnsignedInt(it) }
	ic += getDataValue("secureInClusters")?.split(",").collect{ hexStrToUnsignedInt(it) }

	ic.each {
		if (it) cmds << secureCmd(zwave.versionV1.versionCommandClassGet(requestedCommandClass:it))
	}

	sendCommands(cmds)
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
	def cmd = secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args))
	//logDebug "Sending: ${cmd.format()} - ${cmd}"

	sendCommands(cmd)
}

String setParameter(paramNum, value, size = null) {
	paramNum = safeToInt(paramNum)
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name}]" : "")
	return configSetCmd([num: paramNum, size: size], value as Integer)
}

void setWakeInterval(wakeInt) {
	wakeInt = safeToInt(wakeInt)

	logDebug "setWakeInterval ( $wakeInt )"
	state.pendingWakeUpInt = wakeInt
}

void removeData(String dataType, String dataName) {
	log.debug "removeData(${dataType}, ${dataName})"

	switch (dataType) {
		case "State":
			device.deleteCurrentState("${dataName}".toString())
			break
		case "StateVariable":
			state.remove("${dataName}".toString())
			break
		case "DeviceData":
			removeDataValue("${dataName}".toString())
			break
		default:
			log.warn "removeSaveData invalid dataType: ${dataType}"
	}
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {

	if (description =~ /command: 700F/) {
		description = description.replace("FF FF FF FF", "7F FF FF FE")
	}

	try { 
		hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)

		if (cmd) {
			logTrace "parse: ${description} --PARSED-- ${cmd}"
			zwaveEvent(cmd)
		} else {
			logWarn "Unable to parse: ${description}"
		}
	}
	catch (Exception err) {
		log.error "parse error: ${err}"
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
	String zwaveVersion = String.format("%d.%02d",cmd.zWaveProtocolVersion,cmd.zWaveProtocolSubVersion)
	device.updateDataValue("firmwareVersion", fullVersion)
	device.updateDataValue("protocolVersion", zwaveVersion)
	device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	//setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Number val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		if (param.format >= 1) {
			Long sizeFactor = Math.pow(256,param.size).round()
			if (val < 0) { val += sizeFactor }
		}

		logDebug "${param.title} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
	}

	if (state.deviceSync) {
		device.updateSetting("configParam${cmd.parameterNumber}", val as Long)
	}
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	logTrace "${cmd}"

	if (cmd.groupingIdentifier == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId}"
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
	logTrace "${cmd}"

	if (cmd.groupingIdentifier == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId} | MC: ${cmd.multiChannelNodeIds}"
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    logInfo "--- CommandClassReport - class:0x${intToHexStr(cmd.requestedCommandClass)}, version:${cmd.commandClassVersion}"
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationPropertiesReport cmd) {
	logTrace "${cmd}"
	List<String> newCmds = []

	//Skip if size=0 (invalid param)
	if (cmd.size) {
		//Save The Properties
		paramScan[cmd.parameterNumber] = [
			num: cmd.parameterNumber, format: cmd.format,
			size: cmd.size, defaultVal: cmd.defaultValue,
			range: ("${cmd.minValue}..${cmd.maxValue}").toString(),
			title: "Parameter #${cmd.parameterNumber}",
			description: ""
		]

		//Request Name and Info
		Map args = [parameterNumber: cmd.parameterNumber]
		if (scanName) newCmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationNameGet(args))
		if (scanInfo) newCmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationInfoGet(args))
	}

	//Request Next Paramater if there is one
	if (cmd.nextParameterNumber && cmd.nextParameterNumber != cmd.parameterNumber) {
		logDebug "Found Param $cmd.parameterNumber and saved, next scanning #$cmd.nextParameterNumber"
		Map args = [parameterNumber: cmd.nextParameterNumber]
		sendEvent(name:"scanStatus", value:"Scanning ($cmd.nextParameterNumber)")
		newCmds << "delay 500" << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args))
	}
	else {
		logDebug "Found Param $cmd.parameterNumber and saved, that was the last one"
		sendEvent(name:"scanStatus", value:"Scanning Final Cleanup...")
		runIn(5, processParamScan)
	}

	//logDebug "Sending: ${newCmds}"
	if (newCmds) sendCommands(newCmds, 500)
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationNameReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber]) {
		if (paramScan[cmd.parameterNumber].titleTmp == null) paramScan[cmd.parameterNumber].titleTmp = []
		paramScan[cmd.parameterNumber].titleTmp[cmd.reportsToFollow] = "$cmd.name"
	} else {
		logWarn "ConfigurationNameReport: Skipping, Unknown Paramater: ${cmd.parameterNumber}"
	}
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationInfoReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber]) {
		if (paramScan[cmd.parameterNumber].descTmp == null) paramScan[cmd.parameterNumber].descTmp = []
		paramScan[cmd.parameterNumber].descTmp[cmd.reportsToFollow] = "$cmd.info"
	} else {
		logWarn "ConfigurationInfoReport: Skipping, Unknown Paramater: ${cmd.parameterNumber}"
	}
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	logTrace "${cmd}"

	device.updateDataValue("manufacturer",cmd.manufacturerId.toString())
	device.updateDataValue("deviceType",cmd.productTypeId.toString())
	device.updateDataValue("deviceId",cmd.productId.toString())

	logInfo "fingerprint  mfr:\"${hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)}\", "+
		"prod:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)}\", "+
		"deviceId:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)}\", "+
		"inClusters:\"${device.getDataValue("inClusters")}\""+
		(device.getDataValue("secureInClusters") ? ", secureInClusters:\"${device.getDataValue("secureInClusters")}\"" : "")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
	logTrace "${cmd}"

	switch (cmd.deviceIdType) {
		case 1: //Serial Number
			String serialNumber = ""
			if (cmd.deviceIdDataFormat == 1) {
				serialNumber = convertIntListToHexList(cmd.deviceIdData).join()
			} else {
				cmd.deviceIdData.each { serialNumber += (char)it }
			}
			logDebug "Device Serial Number: $serialNumber"
			device.updateDataValue("serialNumber", serialNumber)
			break
	}
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
	logTrace "${cmd}"
	BigDecimal wakeHrs = safeToDec(cmd.seconds/3600,0,2)
	logDebug "WakeUp Interval is $cmd.seconds seconds ($wakeHrs hours)"
	device.updateDataValue("zwWakeupInterval", "${cmd.seconds}")
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	logDebug "WakeUp Notification Received"

	List<String> cmds = ["delay 0"]
	cmds << batteryGetCmd()

	Integer newWakeUpInt = (state.pendingWakeUpInt as Integer)
	if (newWakeUpInt != null) {
		Integer wakeSeconds = newWakeUpInt ? newWakeUpInt*3600 : 43200
		if (wakeSeconds != (device.getDataValue("zwWakeupInterval") as Integer)) {
			cmds << wakeUpIntervalSetCmd(wakeSeconds)
			cmds << wakeUpIntervalGetCmd()
		}
		state.remove("pendingWakeUpInt")
		state.remove("wakeInterval")
	}

	//Refresh all if requested
	// if (state.pendingRefresh) { cmds += getRefreshCmds() }
	//Any configuration needed
	// cmds += getConfigureCmds()

	//This needs a longer delay
	// cmds << "delay 1400" << wakeUpNoMoreInfoCmd()

	//Clear pending status
	// state.resyncAll = false
	// state.pendingRefresh = false
	// state.remove("INFO")
	
	sendCommands(cmds, 400)
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
	return secureCmd(zwave.versionV2.versionGet())
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	if (param.format >= 1) {
		Long sizeFactor = Math.pow(256,param.size).round()
		if (value >= sizeFactor/2) { value -= sizeFactor }
	}

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

String mfgSpecificGetCmd() {
	return secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
}

String deviceSpecificGetCmd(type=0) {
	return secureCmd(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType:type))
}

String wakeUpIntervalGetCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalGet())
}

String wakeUpIntervalSetCmd(val) {
	return secureCmd(zwave.wakeUpV2.wakeUpIntervalSet(seconds:val, nodeid:zwaveHubNodeId))
}

String wakeUpNoMoreInfoCmd() {
	return secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation())
}

String batteryGetCmd() {
	return secureCmd(zwave.batteryV1.batteryGet())
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
	configsList["${device.idAsLong}"] = [:]
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
	configsList[device.idAsLong][paramNum] = value
	device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	Map configsMap = configsList[device.idAsLong]
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
		configsList[device.idAsLong] = configsMap
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
		v.titleTmp?.reverseEach { value ->
			if (value!=null) paramScan[k].title += "$value" }
		v.descTmp?.reverseEach { value -> 
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
	//logDebug "Get Config Params"
	if (!device) return []
	List<Map> paramsMap = []
	if (device.getDataValue("parameters")) {
		try {
			paramsMap = evaluate(device.getDataValue("parameters"))
		}
		catch(Exception e) {
			logWarn("Invalid dataValue (parameters): ${e}")
			device.removeDataValue("parameters")
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
Integer getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
Integer getParamValue(Map param, Boolean adjust=false) {
	Integer paramVal = safeToInt(settings."configParam${param.num}", param.defaultVal)
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
