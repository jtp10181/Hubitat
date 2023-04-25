/*
 *  Zooz ZEN54 0-10V Dimmer
 *    - Model: ZEN54
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-zen53-dc-motor-controller/117790
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [0.1.0] - 2023-04-24 (@jtp10181)
  - Initial Release

 *  Copyright 2023 Jeff Page
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
@Field static final Map deviceModelNames = ["0904:0219":"ZEN53"]

metadata {
	definition (
		name: "Zooz ZEN53 DC Motor Controller",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zen53-dc-motor.groovy"
	) {
		capability "Actuator"
		capability "WindowShade"
		capability "Configuration"
		capability "Refresh"

    	command "startCalibration", [ [name:"Calibration Mode*", type: "ENUM", constraints: CALIBRATION_MODES] ]

		//command "refreshParams"

		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],
			[name:"value*",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]],
			[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"0904", deviceId:"0219", inClusters:"0x5E,0x55,0x9F,0x22,0x6C", deviceJoinName:"Zooz ZEN53 DC Motor Controller"
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

		for(int i in 2..maxAssocGroups) {
			input "assocDNI$i", "string",
				title: fmtTitle("Device Associations - Group $i"),
				description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear."),
				required: false
		}

		//Logging options similar to other Hubitat drivers
		input "txtEnable", "bool", title: fmtTitle("Enable Description Text Logging?"), defaultValue: true
		input "debugEnable", "bool", title: fmtTitle("Enable Debug Logging?"), defaultValue: true
	}
}

void debugShowVars() {
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
	log.warn "settings ${settings.hashCode()} ${settings}"
}

//Association Settings
@Field static final int maxAssocGroups = 4
@Field static final int maxAssocNodes = 5

/*** Static Lists and Settings ***/
@Field static final Map CALIBRATION_MODES = ["0":"Exit Calibration", "5:1":"Shutter Mode", "5:2":"Ventian Mode", "17:1":"Repositioning Mode"]

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	powerFailure: [ num: 1,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 100,
		options: [100:"Restores Last Status", 0:"Forced to Closed", 99:"Forced to Full Open"],
	],
	ledMode: [ num: 2,
		title: "LED Indicator",
		size: 1, defaultVal: 1,
		options: [1:"LED On When Load On", 0:"LED Always Off"]
	],
	motorMode: [ num: 6,
		title: "Motor Mode",
		size: 1, defaultVal: 1,
		options: [0:"Shutter without positioning", 1:"Shutter with positioning", 2:"Venetian"]
	],
	motorReverse: [ num: 9,
		title: "Reverse Motor Operation",
		size: 1, defaultVal: 0,
		options: [0:"Normal", 1:"Reversed", 2:"Venetian"]
	],
	cycleShutter: [ num: 3,
		title: "Motor Cycle Duration for Shutter Mode",
        description: "[0 = Use Limit Switches, 100 = 1 second]",
		size: 2, defaultVal: 6000,
		range: 0..65535
	],
	cycleVenetian: [ num: 4,
		title: "Motor Cycle Duration for Venetian Mode",
        description: "[0 = Use Limit Switches, 100 = 1 second]",
		size: 2, defaultVal: 1000,
		range: 0..65535
	],
	paddleProgramming: [ num: 8,
		title: "External Switch Pairing",
		description: "Allow activating Z-Wave inclusion from the wall switch",
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
	],
	switchType: [ num: 7,
		title: "External Switch Type",
		size: 1, defaultVal: 0,
		options: [0:"Single 2-Button Switch (SPDT)", 1:"Two 1-Button Switches (SPST)"],
	],
	switchTypeS1: [ num: 14,
		title: "Switch Type (S1)",
		size: 1, defaultVal: 0,
		options: [0:"Toggle Switch", 1:"Momentary Switch"],
	],
	switchTypeS2: [ num: 15,
		title: "Switch Type (S2)",
		size: 1, defaultVal: 0,
		options: [0:"Toggle Switch", 1:"Momentary Switch"],
	],
	offTimer: [ num: 11,
		title: "Auto Close Timer",
		size: 4, defaultVal: 0,
		description: "0 - Disabled",
		range: 0..86400,
	],
	onTimer: [ num: 10,
		title: "Auto Open Timer",
		size: 4, defaultVal: 0,
		description: "0 - Disabled",
		range: 0..86400,
	],
	timerUnits: [ num: 12,
		title: "Auto Timer Units",
		size: 1, defaultVal: 0,
		options: [0:"seconds", 1:"minutes"],
	],
	loadControl: [ num: 13,
		title: "Load Control Mode",
		size: 1, defaultVal: 1,
		options: [1:"Enable Switch and Z-Wave", 0:"Disable Switch Control", 2:"Disable Switch and Z-Wave Control"],
	],
	// Hidden Parameters to Set Defaults
	assocReports: [ num: 16,
		title: "Association Reports",
		size: 1, defaultVal: 0,
		options: [0:"MultiLevel for Z-Wave, Basic for Physical", 1:"Always MultiLevel Reports"],
		hidden: true
	],
	calibrationMode: [ num: 5,
		title: "Auto Calibration Mode",
		size: 1, defaultVal: 0,
		options: [0:"Exit Calibration", 1:"Shutter Mode", 2:"Ventian Mode"],
		hidden: true
	],
	calibrationManual: [ num: 17,
		title: "Manual Calibration",
		size: 1, defaultVal: 0,
		options: [0:"Exit Calibration", 1:"Start Repositioning"],
		hidden: true
	],

]

/* ZEN53
CommandClassReport - class:0x22, version:1    (Application Status)
CommandClassReport - class:0x26, version:4    (Multilevel Switch)
CommandClassReport - class:0x55, version:2    (Transport Service)
CommandClassReport - class:0x59, version:3    (Association Group Information (AGI))
CommandClassReport - class:0x5A, version:1    (Device Reset Locally)
CommandClassReport - class:0x5E, version:2    (Z-Wave Plus Info)
CommandClassReport - class:0x6A, version:1    (Window Covering)
CommandClassReport - class:0x6C, version:1    (Supervision)
CommandClassReport - class:0x70, version:4    (Configuration)
CommandClassReport - class:0x72, version:2    (Manufacturer Specific)
CommandClassReport - class:0x73, version:1    (Powerlevel)
CommandClassReport - class:0x7A, version:5    (Firmware Update Meta Data)
CommandClassReport - class:0x85, version:2    (Association)
CommandClassReport - class:0x86, version:3    (Version)
CommandClassReport - class:0x87, version:3    (Indicator)
CommandClassReport - class:0x8E, version:3    (Multi Channel Association)
CommandClassReport - class:0x9F, version:1    (Security 2)
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x26: 4,	// switchmultilevelv4
	0x6A: 1,	// (Window Covering)
	0x6C: 1,	// supervisionv1
	0x70: 1,	// configurationv1
	0x72: 2,	// manufacturerspecificv2
	0x85: 2,	// associationv2
	0x86: 2,	// versionv2
	0x8E: 3,	// multichannelassociationv3
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
def open() {
	logDebug "open..."
	return getOnOffCmds(0xFF)
}

def close() {
	logDebug "close..."
	return getOnOffCmds(0x00)
}

def setPosition(Number level) {
	logDebug "setLevel($level)..."
	return getSetLevelCmds(level)
}

List<String> startPositionChange(direction) {
	Boolean upDown = (direction == "close") ? true : false
	Integer durationVal = validateRange(duration, getParamValue("holdRampRate"), 0, 127)
	logDebug "startPositionChange($direction)"

	List<String> cmds = [
		switchMultilevelStartLvChCmd(upDown, 255),
		switchMultilevelGetCmd()
	]

	return delayBetween(cmds, 200)
}

String stopPositionChange() {
	logDebug "stopPositionChange()"
	return switchMultilevelStopLvChCmd()
}


/*** Custom Commands ***/
void startCalibration(String efName) {
	String keyVal = CALIBRATION_MODES.find{ efName.equalsIgnoreCase(it.value) }.key
    List param = keyVal.split(":")
	logDebug "Start Calibration [${keyVal} : ${efName}] | ${param}"

	List<String> cmds = []
    if (param[0] == "0") {
        cmds += configSetGetCmd([num:5, size:1], 0)
        cmds += configSetGetCmd([num:17, size:1], 0)
		cmds += configGetCmd(getParam("cycleShutter"))
        cmds += configGetCmd(getParam("cycleVenetian"))
    }
    else {
        cmds += configSetGetCmd([num:(param[0] as Integer), size:1], (param[1] as Integer))
    }

	sendCommands(cmds, 500)
}

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

String setParameter(paramNum, value, size = null) {
	Map param = getParam(paramNum)
	if (param && !size) { size = param.size	}

	if (paramNum == null || value == null || size == null) {
		logWarn "Incomplete parameter list supplied..."
		logWarn "Syntax: setParameter(paramNum, value, size)"
		return
	}
	logDebug "setParameter ( number: $paramNum, value: $value, size: $size )" + (param ? " [${param.name}]" : "")
	return secureCmd(configSetCmd([num: paramNum, size: size], value as Integer))
}


/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)

	if (cmd) {
		logTrace "parse: ${description} --PARSED-- ${cmd}"
		zwaveEvent(cmd)
	} else if (description =~ /command: 6A([0-9A-F]{2})/) {
		logDebug "Unhandled zwaveEvent (Windowshade): $description"
	} else {
		logWarn "Unable to parse: ${description}"
	}

	//Update Last Activity
	updateLastCheckIn()
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
	} else if (cmd.commandClassIdentifier == 0x6A) { //Unsupported Windowshade
		logDebug "Unhandled zwaveEvent (Windowshade): $cmd (ep ${ep})"
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
	setDevModel(new BigDecimal(fullVersion))
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	Map param = getParam(cmd.parameterNumber)
	Integer val = cmd.scaledConfigurationValue

	if (param) {
		//Convert scaled signed integer to unsigned
		Long sizeFactor = Math.pow(256,param.size).round()
		if (val < 0) { val += sizeFactor }

		logDebug "${param.name} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
	}

	if (param.name == "cycleShutter" || param.name == "cycleVenetian") {
		logDebug "${param.name} (#${param.num}) Updating Settings"
		device.updateSetting(param.name, [type: "number", value: param.num])
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
		device.updateSetting("assocDNI$grp", [value:"${dnis}", type:"string"])
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents([value:cmd.value, duration:0], "physical", ep)
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents([value:cmd.value, targetValue:cmd.targetValue, duration:cmd.duration], "digital", ep)
}

void zwaveEvent(hubitat.zwave.Command cmd, ep=0) {
	logDebug "Unhandled zwaveEvent: $cmd (ep ${ep})"
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

	cmds << switchMultilevelGetCmd()

	sendCommands(cmds)
}

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
	}

	for (int i = 2; i <= maxAssocGroups; i++) {
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
	def dni = getAssocDNIsSetting(grp)
	def nodeIds = convertHexListToIntList(dni.split(","))

	if (dni && !nodeIds) {
		logWarn "'${dni}' is not a valid value for the 'Device Associations - Group ${grp}' setting.  All z-wave devices have a 2 character Device Network ID and if you're entering more than 1, use commas to separate them."
	}
	else if (nodeIds.size() > maxAssocNodes) {
		logWarn "The 'Device Associations - Group ${grp}' setting contains more than ${maxAssocNodes} IDs so some (or all) may not get associated."
	}

	return nodeIds
}

List getOnOffCmds(val, Integer endPoint=0) {
	return getSetLevelCmds(val ? 0xFF : 0x00, null, endPoint)
}

List getSetLevelCmds(Number level, Number duration=null, Integer endPoint=0) {
	Short levelVal = safeToInt(level, 99)
	// level 0xFF tells device to use last level, 0x00 is off
	if (levelVal != 0xFF && levelVal != 0x00) {
		levelVal = validateRange(levelVal, 99, 1, 99)
	}

	// Duration Encoding:
	// 0x01..0x7F 1 second (0x01) to 127 seconds (0x7F) in 1 second resolution.
	// 0x80..0xFE 1 minute (0x80) to 127 minutes (0xFE) in 1 minute resolution.
	// 0xFF Factory default duration.

	//Convert seconds to minutes above 120s
	if (duration > 120) {
		logDebug "getSetLevelCmds converting ${duration}s to ${Math.round(duration/60)}min"
		duration = (duration / 60) + 127
	}

	Short durationVal = validateRange(duration, -1, -1, 254)
	if (duration == null || durationVal == -1) {
		durationVal = 0xFF
	}

	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"
	return delayBetween( [
		switchMultilevelSetCmd(levelVal, durationVal, endPoint),
		switchMultilevelGetCmd()], 200)
}

/*** Parameter Helper Functions ***/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	//paramsMap.rampRate.options << rampRateOptions
	paramsMap['settings'] = [fixed: true]
}

private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Main Device Events
	if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
		logInfo "${evt.descriptionText}"
	} else {
		logDebug "${evt.descriptionText} [NOT CHANGED]"
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(Map cmd, String type, Integer ep=0) {
	Integer pos = cmd.value
	String wShade = "unknown"
	if (cmd.duration == null || cmd.duration == 0) {
		//Done Moving
		switch (cmd.value as Integer) {
			case 0:
				wShade = "closed"
				break
			case 1..98:
				wShade = "partially open"
				break
			case 99:
				wShade = "open"
				break
		}
	}
	else {
		pos = 0 //Ignore position
		if (cmd.targetValue > cmd.value) {
			wShade = "opening"
		} else {
			wShade = "closing"
		}
	}

	String desc = "windowShade is ${wShade}" + (type ? " (${type})" : "")
	sendEventLog(name:"windowShade", value:wShade, type:type, desc:desc, ep)

	if (pos) {
		Integer level = (pos == 99 ? 100 : pos)
		desc = "position is set to ${level}%"
		if (type) desc += " (${type})"
		sendEventLog(name:"position", value:level, type:type, unit:"%", desc:desc, ep)
	}
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

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV4.switchMultilevelStopLevelChange(), ep)
}

String configSetCmd(Map param, Integer value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	Long sizeFactor = Math.pow(256,param.size).round()
	if (value >= sizeFactor/2) { value -= sizeFactor }

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
		cmd = zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
	}
	return cmd.format()
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
	if (param == null) return
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

/*** Preference Helpers ***/
String fmtDesc(String str) {
	return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}
String fmtTitle(String str) {
	return "<strong>${str}</strong>"
}

/*** Other Helper Functions ***/
void updateSyncingStatus(Integer delay=2) {
	runIn(delay, refreshSyncStatus)
	sendEvent(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEvent(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
}

void updateLastCheckIn() {
	if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
		state.lastCheckInTime = new Date().time
		state.lastCheckInDate = convertToLocalTimeString(new Date())
	}
}

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

//iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
}

//Used with configure to reset variables
void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel

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
	setDevModel()
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
