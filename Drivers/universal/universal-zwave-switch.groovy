/*  
 *  Universal Z-Wave Switch
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/universal-z-wave-drivers/140325
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/
 *

Changelog:

## Known Issues
  - Do not try to scan multiple devices at once

## [0.1.0] - 2024-07-07 (@jtp10181)
  - Initial Release based from Universal Scanner
  - Added supervised commands support

 *  Copyright 2024 Jeff Page
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@Field static final String VERSION = "0.1.0"
@Field static final String PACKAGE = "Uni-ZW"
@Field static final String DRIVER = "Switch"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/universal-z-wave-drivers/140325"

metadata {
	definition (
		name: "Universal Z-Wave Switch",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		singleThreaded: true,
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/universal/universal-zwave-switch.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Refresh"

		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		command "queryDevice", [[name: "option*", type: "ENUM", constraints: ["Parameters", "Sync Only"]]]

		//DEBUGGING
		// command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"001D", prod:"0042", deviceId:"0002", inClusters:"0x00,0x00", controllerType: "ZWV" //Leviton ZW15S Switch
	}

	preferences {

		//Saved Parameters
		configParams.each { param ->
			if (!param.hidden) {
				if (param.options) {
					Integer paramVal = getParamValue(param)
					input "configParam${param.num}", "enum",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Selected: ${paramVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: param.defaultVal,
						options: param.options,
						required: false
				}
				else if (param.range) {
					input "configParam${param.num}", "number",
						title: fmtTitle("${param.title}"),
						description: fmtDesc("• Parameter #${param.num}, Range: ${(param.range).toString()}, DEFAULT: ${param.defaultVal}" + (param?.description ? "<br>• ${param?.description}" : '')),
						defaultValue: param.defaultVal,
						range: param.range,
						required: false
				}
			}
		}

		if (!configParams) {
			input "instructions", "hidden",
				title: fmtTitle("Instructions / Help"),
				description: fmtDesc("To discover settings for your device, run Query Device (Parameters) and watch the Current States for updates")
		}

		input "supervisedCmds", "bool",
			title: fmtTitle("Supervised Commands") + "<em> (Experimental)</em>",
			description: fmtDesc("This can increase reliability when the device is paired with security, but may not work correctly on all devices."),
			defaultValue: false

		input "scanType", "enum", defaultValue: 3,
			title: fmtTitle("Parameter Scan Type"),
			description: fmtDesc("Try basic scans if the full scan won't complete"),
			options: [3: "Full Name/Details", 2:"Basic/Name Only", 1:"Basic Info Only"]
	}
}

void debugShowVars() {
	log.warn "settings ${settings.hashCode()} ${settings}"
	// log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	// log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	log.warn "paramScan ${paramScan.hashCode()} ${paramScan}"
	log.warn "supervisedPackets ${supervisedPackets.hashCode()} ${supervisedPackets}"
}

//Association Settings
@Field static final int maxAssocGroups = 1
@Field static final int maxAssocNodes = 1

/*** Static Lists and Settings ***/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x25: 2,	// Switch Binary (switchbinary)
	0x60: 3,	// Multi Channel
	0x6C: 1,	// Supervision (supervision)
	0x70: 3,	// Configuration (configuration)
	0x72: 2,	// Manufacturer Specific (manufacturerspecific)
	0x85: 2,	// Association (associationv2)
	0x8E: 3,	// Multi Channel Association (multichannelassociationv3)
	0x86: 2,	// Version (version)
]


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

List<String> configure() {
	logWarn "configure..."

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		clearVariables()
		state.resyncAll = true
	}

	List<String> cmds = ["delay 0"]
	cmds += getRefreshCmds()
	cmds << "delay 2000"
	cmds += getConfigureCmds()

	return cmds ? delayBetween(cmds, 300) : []
}

List<String> updated() {
	logDebug "updated..."
	checkLogLevel()

	state.remove("deviceSync")
	refreshSyncStatus()
	
	List<String> cmds = getConfigureCmds()
	return cmds ? delayBetween(cmds, 300) : []
}

List<String> refresh() {
	logDebug "refresh..."
	List<String> cmds = getRefreshCmds()
	return cmds ? delayBetween(cmds, 200) : []
}


/*******************************************************************
 ***** Driver Commands
********************************************************************/
/*** Capabilities ***/
def on() {
	logDebug "on..."
	return getOnOffCmds(0xFF)
}

def off() {
	logDebug "off..."
	return getOnOffCmds(0x00)
}

/*** Custom Commands ***/
void queryDevice(option) {
	if (option == "Parameters") {
		String cmd = secureCmd(zwave.versionV2.versionCommandClassGet(requestedCommandClass:0x70))
		sendEvent(name:"queryStatus", value:"Probing for Support...")
		sendCommands(cmd)
	}
	else if (option == "Sync Only") {
		syncFromDevice()
	}
	else {
		logWarn "queryDevice unrecognized option: ${option}"
	}
}

void scanParameters(param=1) {
	logDebug "scanParameters: Starting with #${param}"
	paramScan = [:]
	Map args = [parameterNumber: param]
	String cmd = secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args))
	sendEvent(name:"queryStatus", value:"Scanning ($param)")

	sendCommands(cmd)
}

void syncFromDevice() {
	sendEvent(name:"queryStatus", value:"Syncing Settings from Device...")
	state.deviceSync = true
	device.removeDataValue("configVals")
	configsList[device.id] = [:]

	List<String> cmds = []
	for (int i = 1; i <= maxAssocGroups; i++) {
		cmds << associationGetCmd(i)
	}

	configParams.each { param ->
		device.removeSetting("configParam${param.num}")
		logDebug "Getting ${param.title} (#${param.num}) from device"
		cmds += configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

def setParameter(paramNum, value, size = null) {
	paramNum = safeToInt(paramNum)
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name} - ${param.title}]" : "")
	return configSetGetCmd([num: paramNum, size: size], value as Integer)
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	if (description =~ /command: 700F/) {
		description = description.replace("FF FF FF FF", "7F FF FF FE")
	}
	zwaveParse(description)
}
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	zwaveMultiChannel(cmd)
}
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	zwaveSupervision(cmd,ep)
}

void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Long val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		if (param.format >= 1 || param.format == null) {
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

//Associations
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

//Associations (MultiChannel)
void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
	logTrace "${cmd}"

	if (cmd.groupingIdentifier == 1) {
		logDebug "Lifeline Association: ${cmd.nodeId} | MC: ${cmd.multiChannelNodeIds}"
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, digitalCheck(), ep)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	if (cmd.duration == 0) {
		sendSwitchEvents(cmd.value, digitalCheck(), ep)
	}
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	if (cmd.duration == 0) {
		sendSwitchEvents(cmd.value, digitalCheck(), ep)
	}
}

String digitalCheck() {
	String type = (state.isDigital ? "digital" : "physical")
	state.remove("isDigital")
	return type
}

//Handle device that sends Hail instead of status updates
void zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendCommands(switchBinaryGetCmd())
}

//Parameter Scanning
void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationPropertiesReport cmd) {
	logTrace "${cmd}"
	List<String> newCmds = []
	String status = "invalid"

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
		status = "saved"

		//Request Name and Info
		Map args = [parameterNumber: cmd.parameterNumber]
		Integer type = (scanType as Integer) ?: 3
		if (type >= 2) newCmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationNameGet(args))
		if (type >= 3) newCmds << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationInfoGet(args))
	}

	//Request Next Paramater if there is one
	if (cmd.nextParameterNumber && cmd.nextParameterNumber != cmd.parameterNumber) {
		logDebug "Received Param $cmd.parameterNumber (${status}), next is #$cmd.nextParameterNumber"
		Map args = [parameterNumber: cmd.nextParameterNumber]
		sendEvent(name:"queryStatus", value:"Scanning ($cmd.nextParameterNumber)")
		newCmds << "delay 500" << secureCmd(new hubitat.zwave.commands.configurationv3.ConfigurationPropertiesGet(args))
	}
	else {
		logDebug "Received Param $cmd.parameterNumber (${status}), that was the last one"
		sendEvent(name:"queryStatus", value:"Scanning Final Cleanup... please wait")
		runIn(4, processParamScan)
	}

	//logDebug "Sending: ${newCmds}"
	if (newCmds) sendCommands(newCmds, 500)
}

//Parameter Scanning
void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationNameReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber]) {
		if (paramScan[cmd.parameterNumber].titleTmp == null) paramScan[cmd.parameterNumber].titleTmp = []
		paramScan[cmd.parameterNumber].titleTmp[cmd.reportsToFollow] = "$cmd.name"
	} else {
		logWarn "ConfigurationNameReport: Skipping, Unknown Paramater: ${cmd.parameterNumber}"
	}
}

//Parameter Scanning
void zwaveEvent(hubitat.zwave.commands.configurationv3.ConfigurationInfoReport cmd) {
	logTrace "${cmd}"

	if (paramScan[cmd.parameterNumber]) {
		if (paramScan[cmd.parameterNumber].descTmp == null) paramScan[cmd.parameterNumber].descTmp = []
		paramScan[cmd.parameterNumber].descTmp[cmd.reportsToFollow] = "$cmd.info"
	} else {
		logWarn "ConfigurationInfoReport: Skipping, Unknown Paramater: ${cmd.parameterNumber}"
	}
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionCommandClassReport cmd) {
	logTrace "${cmd}"

	//cmd.commandClassVersion
	if ((cmd.commandClassVersion as Integer) >= 3) {
		logDebug "Device reports Configuration CC v${cmd.commandClassVersion}, supports fetching properties"
		runInMillis(500, scanParameters)
	}
	else {
		logWarn "Device reports Configuration CC v${cmd.commandClassVersion}, DOES NOT support fetching properties"
		sendEvent(name:"queryStatus", value:"Parameter Query not supported by device")
	}
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


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()

	//Main Device Events
	if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
		logInfo "${evt.descriptionText}"
	} else {
		logDebug "${evt.descriptionText} [NOT CHANGED]"
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)
}

/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
List<String> getConfigureCmds() {
	logDebug "getConfigureCmds..."

	List<String> cmds = []

	if (!firmwareVersion) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds(true)

	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.title} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	state.resyncAll = false

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion) {
		cmds << mfgSpecificGetCmd()
		cmds << versionGetCmd()
	}

	cmds << switchBinaryGetCmd()

	return cmds ?: []
}

List getConfigureAssocsCmds(Boolean logging=false) {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (logging) logDebug "Setting lifeline association..."
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
		List<String> cmdsEach = []
		List settingNodeIds = getAssocDNIsSettingNodeIds(i)

		//Need to remove first then add in case we are at limit
		List oldNodeIds = state.assocNodes?."$i"?.findAll { !(it in settingNodeIds) }
		if (oldNodeIds) {
			if (logging) logDebug "Removing Group $i Association: ${convertIntListToHexList(oldNodeIds)} // $oldNodeIds"
			cmdsEach << associationRemoveCmd(i, oldNodeIds)
		}

		List newNodeIds = settingNodeIds.findAll { !(it in state.assocNodes?."$i") }
		if (newNodeIds) {
			if (logging) logDebug "Adding Group $i Association: ${convertIntListToHexList(newNodeIds)} // $newNodeIds"
			cmdsEach << associationSetCmd(i, newNodeIds)
		}

		if (cmdsEach || state.resyncAll) {
			cmdsEach << associationGetCmd(i)
			cmds += cmdsEach
		}
	}

	return cmds
}

String getOnOffCmds(val, Integer endPoint=0) {
	state.isDigital = true
	return switchBinarySetCmd(val ? 0xFF : 0x00, endPoint)
}


/*******************************************************************
 ***** Other Functions
********************************************************************/
//paramScan Structure: PARAM_NUM:[PARAM_MAPS]
//PARAM_MAPS [num, name, title, description, size, defaultVal, options, firmVer]
@Field static Map<String, Map> paramScan = new java.util.concurrent.ConcurrentHashMap()

//Process the scanned parameters and save to data
void processParamScan() {
	List<Map> paramsMap = []
	paramScan.each { k, v ->
		if (v.titleTmp) {
			v.title = ""
			v.titleTmp?.reverseEach { if (it!=null) v.title += "$it" }
			v.remove("titleTmp")
		}
		if (v.descTmp) {
			v.description = ""
			v.descTmp?.reverseEach { if (it!=null) v.description += "$it" }
			v.remove("descTmp")
		}
		//Save to List
		paramsMap += paramScan[k]
	}
	//Dump list to device data
	String paramsJson = JsonOutput.toJson(paramsMap) as String
	device.updateDataValue("parameters", paramsJson)
	//sendEvent(name:"queryStatus", value:"Scan Complete - REFRESH the Page")
	logDebug "processParamScan Completed"
	syncFromDevice()
}


/*******************************************************************
 ***** Required for Library
********************************************************************/
Integer getParamValueAdj(Map param) {
	return getParamValue(param)
}


//#include jtp10181.zwaveDriverLibrary
/*******************************************************************
 *******************************************************************
 ***** Z-Wave Driver Library by Jeff Page (@jtp10181)
 *******************************************************************
********************************************************************

Changelog:
2023-05-10 - First version used in drivers
2023-05-12 - Adjustments to community links
2023-05-14 - Updates for power metering
2023-05-18 - Adding requirement for getParamValueAdj in driver
2023-05-24 - Fix for possible RuntimeException error due to bad cron string
2023-10-25 - Less saving to the configVals data, and some new functions
2023-10-26 - Added some battery shortcut functions
2023-11-08 - Added ability to adjust settings on firmware range
2024-01-28 - Adjusted logging settings for new / upgrade installs, added mfgSpecificReport
2024-06-15 - Added isLongRange function, convert range to string to prevent expansion

********************************************************************/

library (
  author: "Jeff Page (@jtp10181)",
  category: "zwave",
  description: "Z-Wave Driver Library",
  name: "zwaveDriverLibrary",
  namespace: "jtp10181",
  documentationLink: ""
)

/*******************************************************************
 ***** Z-Wave Reports (COMMON)
********************************************************************/
//Include these in Driver
//void parse(String description) {zwaveParse(description)}
//void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {zwaveMultiChannel(cmd)}
//void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {zwaveSupervision(cmd,ep)}

void zwaveParse(String description) {
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
void zwaveMultiChannel(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveSupervision(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	hubitat.zwave.Command encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
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

	if (cmd.targetVersions) {
		Map tVersions = [:]
		cmd.targetVersions.each {
			tVersions[it.target] = String.format("%d.%02d",it.version,it.subVersion)
			device.updateDataValue("firmware${it.target}Version", tVersions[it.target])
		}
		logDebug "Received Version Report - Main Firmware: ${fullVersion} | Targets: ${tVersions}"
	}
	else {
		logDebug "Received Version Report - Firmware: ${fullVersion}"
	}
	
	//setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	logTrace "${cmd}"

	device.updateDataValue("manufacturer",cmd.manufacturerId.toString())
	device.updateDataValue("deviceType",cmd.productTypeId.toString())
	device.updateDataValue("deviceId",cmd.productId.toString())

	logDebug "fingerprint  mfr:\"${hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)}\", "+
		"prod:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)}\", "+
		"deviceId:\"${hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)}\", "+
		"inClusters:\"${device.getDataValue("inClusters")}\""+
		(device.getDataValue("secureInClusters") ? ", secureInClusters:\"${device.getDataValue("secureInClusters")}\"" : "")
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
	return superviseCmd(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return superviseCmd(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
}

String associationGetCmd(Integer group) {
	return secureCmd(zwave.associationV2.associationGet(groupingIdentifier: group))
}

String mcAssociationGetCmd(Integer group) {
	return secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: group))
}

String versionGetCmd() {
	return secureCmd(zwave.versionV2.versionGet())
}

String mfgSpecificGetCmd() {
	return secureCmd(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
}

String deviceSpecificGetCmd(type=0) {
	return secureCmd(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType:type))
}

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return superviseCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return superviseCmd(zwave.switchMultilevelV4.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return superviseCmd(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return superviseCmd(zwave.switchMultilevelV4.switchMultilevelStopLevelChange(), ep)
}

String meterGetCmd(meter, Integer ep=0) {
	return secureCmd(zwave.meterV3.meterGet(scale: meter.scale), ep)
}

String meterResetCmd(Integer ep=0) {
	return secureCmd(zwave.meterV3.meterReset(), ep)
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

String sensorMultilevelGetCmd(sensorType) {
	Integer scale = (temperatureScale == "F" ? 1 : 0)
	return secureCmd(zwave.sensorMultilevelV11.sensorMultilevelGet(scale: scale, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType, Integer ep=0) {
	return secureCmd(zwave.notificationV3.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType), ep)
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	if (param.format >= 1 || param.format == null) {
		Long sizeFactor = Math.pow(256,param.size).round()
		if (value >= sizeFactor/2) { value -= sizeFactor }
	}

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
	return zwaveSecureEncap(multiChannelCmd(cmd, ep))
}

//MultiChannel Encapsulate if needed
//This is called from secureCmd or superviseCmd, do not call directly
String multiChannelCmd(hubitat.zwave.Command cmd, ep) {
	//logTrace "multiChannelCmd: ${cmd} (ep ${ep})"
	if (ep > 0) {
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
}

//====== Supervision Encapsulate START ======\\
@Field static Map<String, Map<Short, Map>> supervisedPackets = new java.util.concurrent.ConcurrentHashMap()
@Field static Map<String, Short> sessionIDs = new java.util.concurrent.ConcurrentHashMap()
@Field static final Map supervisedStatus = [0x00:"NO SUPPORT", 0x01:"WORKING", 0x02:"FAILED", 0xFF:"SUCCESS"]
@Field static final Integer SUPERVISED_RETRIES = 2
@Field static final Integer SUPERVISED_DELAY_MS = 500

String superviseCmd(hubitat.zwave.Command cmd, ep=0) {
	//logTrace "superviseCmd: ${cmd} (ep ${ep})"

	if (settings.supervisedCmds) {
		//Encap with SupervisionGet
		Short sID = getSessionId()
		def cmdEncap = zwave.supervisionV1.supervisionGet(sessionID: sID, statusUpdates: false).encapsulate(cmd)

		//Encap with MultiChannel now (if needed) so it is cached that way below
		cmdEncap = multiChannelCmd(cmdEncap, ep)

		logTrace "New Supervised Packet for Session: ${sID}"
		if (supervisedPackets[device.id] == null) { supervisedPackets[device.id] = [:] }
		supervisedPackets[device.id][sID] = [cmd: cmdEncap]

		//Calculate supervisionCheck delay based on how many cached packets
		Integer packetsCount = supervisedPackets[device.id]?.size() ?: 0
		Integer delayTotal = (SUPERVISED_DELAY_MS * packetsCount) + 1000
		runInMillis(delayTotal, supervisionCheck, [data:[sID: sID, num: 1], overwrite:false])

		//Send back secured command
		return secureCmd(cmdEncap)
	}
	else {
		//If supervision disabled just multichannel and secure
		return secureCmd(cmd, ep)
	}
}

Short getSessionId() {
	Short sID = sessionIDs[device.id] ?: (state.supervisionID as Short) ?: 0
	sID = (sID + 1) % 64  // Will always will return between 0-63 (6 bits)
	state.supervisionID = sID
	sessionIDs[device.id] = sID
	return sID
}

//data format: [Short sID, Integer num]
void supervisionCheck(Map data) {
	Short sID = (data.sID as Short)
	Integer num = (data.num as Integer)
	Integer packetsCount = supervisedPackets[device.id]?.size() ?: 0
	logTrace "Supervision Check #${num} Session ${sID}, Packet Count: ${packetsCount}"

	if (supervisedPackets[device.id].containsKey(sID)) {
		if (supervisedPackets[device.id][sID].working) {
			logDebug "Supervision Session ${sID} is WORKING status, will not retry"
			supervisedPackets[device.id].remove(sID)
		}
		else {
			List<String> cmds = []
			if (num <= SUPERVISED_RETRIES) { //Keep trying
				logWarn "Re-Sending Supervised Session: ${sID} (Retry #${num})"
				cmds << secureCmd(supervisedPackets[device.id][sID].cmd)
				Integer delayTotal = SUPERVISED_DELAY_MS
				runInMillis(delayTotal, supervisionCheck, [data:[sID: sID, num: num+1], overwrite:false])
			}
			else { //Clear after too many attempts
				logWarn "Supervision MAX RETRIES Reached - device did not respond"
				supervisedPackets[device.id].remove(sID)
			}
			if (cmds) sendCommands(cmds)
		}
	}
	else {
		logTrace "Supervision Session ${sID} has already been cleared or invalid"
	}
}

//Handles reports back from Supervision Encapsulated Commands
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	if (supervisedPackets[device.id] == null) { supervisedPackets[device.id] = [:] }
	Short sID = (cmd.sessionID as Short)
	Integer status = (cmd.status as Integer)

	switch (status) {
		case 0x00: // "No Support"
		case 0x02: // "Failed"
			logWarn "Supervision ${supervisedStatus[status]} (sessionID: ${sID}, status: ${status})"
			break
		case 0x01: // "Working" - This is as good as success, device got the message
			logDebug "Supervision ${supervisedStatus[status]} (sessionID: ${sID}, status: ${status})"
			if (supervisedPackets[device.id].containsKey(sID)) {
				supervisedPackets[device.id][sID].working = true
			}
			break
		case 0xFF: // "Success"
			logDebug "Supervision ${supervisedStatus[status]} (sessionID: ${sID}, status: ${status})"
			supervisedPackets[device.id].remove(sID)
			break
	}
}
//====== Supervision Encapsulate END ======\\

/*******************************************************************
 ***** Common Functions
********************************************************************/
/*** Parameter Store Map Functions ***/
@Field static Map<String, Map> configsList = new java.util.concurrent.ConcurrentHashMap()
Integer getParamStoredValue(Integer paramNum) {
	//Using Data (Map) instead of State Variables
	Map configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Number value) {
	//Using Data (Map) instead of State Variables
	TreeMap configsMap = getParamStoredMap()
	configsMap[paramNum] = value
	configsList[device.id][paramNum] = value
	//device.updateDataValue("configVals", configsMap.inspect())
}

Map getParamStoredMap() {
	TreeMap configsMap = configsList[device.id]
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

/*** Parameter List Functions ***/

//Verify the list and build if its not populated
void verifyParamsList() {
	//NOT USED
}

//Gets full list of params
List<Map> getConfigParams() {
	//logDebug "Get Config Params"
	if (!device) return []
	List<Map> paramsMap = []
	String paramsJson = device.getDataValue("parameters")
	if (paramsJson) {
		try {
			paramsMap = (new JsonSlurper().parseText(paramsJson)) as Map
		}
		catch(Exception e) {
			logWarn("Invalid dataValue (parameters): ${e}")
			device.removeDataValue("parameters")
		}
	}
	//logDebug "${paramsMap}"
	return paramsMap
}

//Get a single param by name or number
Map getParam(String search) {
	verifyParamsList()
	return configParams.find{ it.name == search }
}
Map getParam(Number search) {
	verifyParamsList()
	return configParams.find{ it.num == search }
}

//Convert Param Value if Needed
BigDecimal getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
BigDecimal getParamValue(Map param) {
	if (param == null) return
	BigDecimal paramVal = safeToDec(settings."configParam${param.num}", param.defaultVal)

	//Reset hidden parameters to default
	if (param.hidden && settings."configParam${param.num}" != null) {
		logWarn "Resetting hidden parameter ${param.name} (${param.num}) to default ${param.defaultVal}"
		device.removeSetting("configParam${param.num}")
		paramVal = param.defaultVal
	}

	return paramVal
}

/*** Preference Helpers ***/
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtHelpInfo(String str) {
	String info = "${PACKAGE} ${DRIVER} v${VERSION}".trim()
	String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
	String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
	String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}

private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
	device.updateDataValue("configVals", getParamStoredMap()?.inspect())
	if (changes==0 && state.deviceSync) {
		sendEvent(name:"queryStatus", value:"Scan Complete -<br> REFRESH the Page, then Save")
		state.remove("deviceSync")
	}
}

void updateLastCheckIn() {
	Date nowDate = new Date()
	state.lastCheckInDate = convertToLocalTimeString(nowDate)

	Long lastExecuted = state.lastCheckInTime ?: 0
	Long allowedMil = 24 * 60 * 60 * 1000   //24 Hours
	if (lastExecuted + allowedMil <= nowDate.time) {
		state.lastCheckInTime = nowDate.time
		if (lastExecuted) runIn(2, doCheckIn)
		scheduleCheckIn()
	}
}

void scheduleCheckIn() {
	unschedule("doCheckIn")
	runIn(86340, doCheckIn)
}

void doCheckIn() {
	scheduleCheckIn()
	String pkg = PACKAGE ?: DRIVER
	String devModel = (state.deviceModel ?: (PACKAGE ? DRIVER : "NA")) + (state.subModel ? ".${state.subModel}" : "")
	String checkUri = "http://jtp10181.gateway.scarf.sh/${pkg}/chk-${devModel}-v${VERSION}"

	try {
		httpGet(uri:checkUri, timeout:4) { logDebug "Driver ${pkg} ${devModel} v${VERSION}" }
		state.lastCheckInTime = now()
	} catch (Exception e) { }
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValueAdj(param)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

//iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	String val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
}

List getAssocDNIsSettingNodeIds(grp) {
	String dni = getAssocDNIsSetting(grp)
	List nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		logWarn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		logWarn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

//Used with configure to reset variables
void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel
	def engTime = state.energyTime

	//Clears State Variables
	state.clear()

	//Clear Config Data
	configsList["${device.id}"] = [:]
	device.removeDataValue("configVals")
	//Clear Data from other Drivers
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
	if (engTime) state.energyTime = engTime
	state.resyncAll = true
}

BigDecimal getFirmwareVersion() {
	String version = device?.getDataValue("firmwareVersion")
	return ((version != null) && version.isNumber()) ? version.toBigDecimal() : 0.0
}

Boolean isLongRange() {
	Integer intDNI = device ? hubitat.helper.HexUtils.hexStringToInt(device.deviceNetworkId) : null
	return (intDNI > 255)
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

Integer convertLevel(level, userLevel=false) {
	// if (levelCorrection) {
	// 	Integer brightmax = getParamValue("maximumBrightness")
	// 	Integer brightmin = getParamValue("minimumBrightness")
	// 	brightmax = (brightmax == 99) ? 100 : brightmax
	// 	brightmin = (brightmin == 1) ? 0 : brightmin

	// 	if (userLevel) {
	// 		//This converts what the user selected into a physical level within the min/max range
	// 		level = ((brightmax-brightmin) * (level/100)) + brightmin
	// 		state.levelActual = level
	// 		level = validateRange(Math.round(level), brightmax, brightmin, brightmax)
	// 	}
	// 	else {
	// 		//This takes the true physical level and converts to what we want to show to the user
	// 		if (Math.round(state.levelActual ?: 0) == level) level = state.levelActual
	// 		else state.levelActual = level

	// 		level = ((level - brightmin) / (brightmax - brightmin)) * 100
	// 		level = validateRange(Math.round(level), 100, 1, 100)
	// 	}
	// }
	// else if (state.levelActual) {
	// 	state.remove("levelActual")
	// }

	return level
}

Integer validateRange(val, Integer defaultVal, Integer lowVal, Integer highVal) {
	Integer intVal = safeToInt(val, defaultVal)
	if (intVal > highVal) {
		return highVal
	} else if (intVal < lowVal) {
		return lowVal
	} else {
		return intVal
	}
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

Boolean isDuplicateCommand(Long lastExecuted, Long allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > now())
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS.values()],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES.values()] ]
*/

//Additional Preferences
preferences {
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 30, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
	unschedule(logsOff)
	//Set Defaults
	if (settings.logLevel == null) {
		device.updateSetting("logLevel",[value:"3", type:"enum"])
		levelInfo.level = 3
	}
	if (settings.logLevelTime == null) {
		device.updateSetting("logLevelTime",[value:"30", type:"enum"])
		levelInfo.time = 30
	}
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
		logMsg += " for ${LOG_TIMES[levelInfo.time]}"
		runIn(60*levelInfo.time, logsOff)
	}
	logInfo(logMsg)

	//Store last level below Debug
	if (levelInfo.level <= 2) state.lastLogLevel = levelInfo.level
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
	Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
	Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
	Integer level = settings.logLevel != null ? settings.logLevel as Integer : 1
	Integer time = settings.logLevelTime != null ? settings.logLevelTime as Integer : 30
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:false, type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		Integer lastLvl = state.lastLogLevel != null ? state.lastLogLevel as Integer : 2
		device.updateSetting("logLevel",[value:lastLvl.toString(), type:"enum"])
		logWarn "Logging Level is: ${LOG_LEVELS[lastLvl]} (${lastLvl})"
	}
}

//Logging Functions
void logErr(String msg) {
	log.error "${device.displayName}: ${msg}"
}
void logWarn(String msg) {
	if (logLevelInfo.level>=1) log.warn "${device.displayName}: ${msg}"
}
void logInfo(String msg) {
	if (logLevelInfo.level>=2) log.info "${device.displayName}: ${msg}"
}
void logDebug(String msg) {
	if (logLevelInfo.level>=3) log.debug "${device.displayName}: ${msg}"
}
void logTrace(String msg) {
	if (logLevelInfo.level>=4) log.trace "${device.displayName}: ${msg}"
}
