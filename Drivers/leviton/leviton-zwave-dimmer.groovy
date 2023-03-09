/*  
 *  Leviton Z-Wave Plus Dimmer
 *    - Model: DZPD3 Plug-In Dimmer
 *    - Model: DZ6HD In Wall Dimmer
 *
 *  For Support, Information and Updates:
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/
 *

Changelog:

## [1.0.0] - 2023-03-08 (@jtp10181)
  - Added support for DZ6HD
  - Testing completed and no issues found

## [0.1.0] - 2023-03-01 (@jtp10181)
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

@Field static final String VERSION = "1.0.0"
@Field static final Map deviceModelNames = ["3501:0001":"DZPD3", "3201:0001":"DZ6HD"]

metadata {
	definition (
		name: "Leviton Z-Wave Plus Dimmer",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/leviton/leviton-zwave-dimmer.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "SwitchLevel"
		capability "ChangeLevel"
		capability "Configuration"
		capability "Refresh"
		capability "Flash"

		//Modified from default to add duration argument
		command "startLevelChange", [
			[name:"Direction*", description:"Direction for level change request", type: "ENUM", constraints: ["up","down"]],
			[name:"Duration", type:"NUMBER", description:"Transition duration in seconds", constraints:["NUMBER"]] ]

		// command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],
		// 	[name:"value*",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]],
		// 	[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"001D", prod:"3501", deviceId:"0001", inClusters:"0x5E,0x85,0x59,0x86,0x72,0x70,0x5A,0x73,0x26,0x20,0x27,0x2C,0x2B,0x7A", outClusters:"0x82", deviceJoinName:"Leviton DZPD3 Plug Dimmer"
		fingerprint mfr:"001D", prod:"3201", deviceId:"0001", inClusters:"0x5E,0x85,0x59,0x86,0x72,0x70,0x5A,0x73,0x26,0x20,0x27,0x2C,0x2B,0x7A", outClusters:"0x82", deviceJoinName:"Leviton DZ6HD Dimmer"
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

		input "levelCorrection", "bool",
			title: fmtTitle("Brightness Correction"),
			description: fmtDesc("Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%"),
			defaultValue: false

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
	fadeOn: [ num: 1,
		title: "Fade On Time (seconds)",
		size: 1, defaultVal: 2,
		range: "0..127"
	],
	fadeOff: [ num: 2,
		title: "Fade Off Time (seconds)",
		size: 1, defaultVal: 2,
		range: "0..127"
	],
	minimumBrightness: [ num: 3,
		title: "Minimum Brightness (level)",
		size: 1, defaultVal: 10,
		range: "0..100"
	],
	maximumBrightness: [ num: 4,
		title: "Maximum Brightness (level)",
		size: 1, defaultVal: 100,
		range: "0..100"
	],
	presetLevel: [ num: 5,
		title: "Preset Light Level",
		description: "0 = Restore last level when turned on",
		size: 1, defaultVal: 0,
		range: "0..100",
		changes: ['DZPD3':[num:null]]
	],
	ledTimeout: [ num: 6,
		title: "LED Dim Indicator Timeout (seconds)",
		description: "0 = Off / 255 = Always On",
		size: 1, defaultVal: 3,
		range: "0..255",
		changes: ['DZPD3':[num:null]]
	],
	ledMode: [ num: 7,
		title: "LED Indicator",
		size: 1, defaultVal: 255,
		options: [255:"LED On When Switched Off", 254:"LED On When Switched On", 0:"LED Always Off"]
	],
	loadType: [ num: 8,
		title: "Load Type",
		size: 1, defaultVal: 0,
		options: [0:"Incandescent", 1:"LED", 2:"CFL"]
	]
]

/* Leviton DZPD3
CommandClassReport - class:0x20, version:2
CommandClassReport - class:0x26, version:4
CommandClassReport - class:0x27, version:1
CommandClassReport - class:0x2B, version:1
CommandClassReport - class:0x2C, version:1
CommandClassReport - class:0x59, version:1
CommandClassReport - class:0x5A, version:1
CommandClassReport - class:0x5E, version:2
CommandClassReport - class:0x70, version:1
CommandClassReport - class:0x72, version:2
CommandClassReport - class:0x73, version:1
CommandClassReport - class:0x7A, version:4
CommandClassReport - class:0x85, version:2
CommandClassReport - class:0x86, version:2
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x26: 2,	// Switch Multilevel (switchmultilevelv2) (4)
	0x70: 1,	// Configuration (configurationv1)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x85: 2,	// Association (associationv2)
	0x86: 2,	// Version (versionv2)
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
String on() {
	logDebug "on..."
	flashStop()
	return getOnOffCmds(0xFF)
}

String off() {
	logDebug "off..."
	flashStop()
	return getOnOffCmds(0x00)
}

String setLevel(level, duration=null) {
	logDebug "setLevel($level, $duration)..."
	return getSetLevelCmds(level, duration)
}

String startLevelChange(direction, duration=null) {
	Boolean upDown = (direction == "down") ? true : false
	Integer durationVal = validateRange(duration, 0, 0, 127)
	logDebug "startLevelChange($direction) for ${durationVal}s"

	//Required after change to send updated report
	runIn((durationVal+1),stopLevelChange)

	return switchMultilevelStartLvChCmd(upDown, durationVal)
}

String stopLevelChange() {
	logDebug "stopLevelChange()"
	return switchMultilevelStopLvChCmd()
}

//Flashing Capability
void flash(rateToFlash = 1500) {
	logInfo "Flashing started with rate of ${rateToFlash}ms"

	//Min rate of 1 sec, max of 30, max run time of 5 minutes
	rateToFlash = validateRange(rateToFlash, 1500, 1000, 30000)
	Integer maxRun = validateRange((rateToFlash*30)/1000, 30, 30, 300)
	state.flashNext = device.currentValue("switch")

	//Start the flashing
	runIn(maxRun,flashStop,[data:true])
	flashHandler(rateToFlash)
}

void flashStop(Boolean turnOn = false) {
	if (state.flashNext != null) {
		logInfo "Flashing stopped..."
		unschedule("flashHandler")
		state.remove("flashNext")
		if (turnOn) { runIn(1,on) }
	}
}

void flashHandler(Integer rateToFlash) {
	if (state.flashNext == "on") {
		logDebug "Flash On"
		state.flashNext = "off"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getSetLevelCmds(0xFF, 0))
	}
	else if (state.flashNext == "off") {
		logDebug "Flash Off"
		state.flashNext = "on"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getSetLevelCmds(0x00, 0))
	}
}


/*** Custom Commands ***/
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
	} else {
		logWarn "Unable to parse: ${description}"
	}

	//Update Last Activity
	updateLastCheckIn()
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
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
		//Convert scaled signed integer to unsigned
		Long sizeFactor = Math.pow(256,param.size).round()
		if (val < 0) { val += sizeFactor }

		logDebug "${param.name} (#${param.num}) = ${val.toString()}"
		setParamStoredValue(param.num, val)
	}
	else {
		logDebug "Parameter #${cmd.parameterNumber} = ${val.toString()}"
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
	sendSwitchEvents(cmd.value, digitalCheck(), ep)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, digitalCheck(), ep)
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, digitalCheck(), ep)
}

String digitalCheck() {
	String type = (state.isDigital ? "digital" : "physical")
	state.remove("isDigital")
	if (type == "physical") flashStop()
	return type
}

//Handle device that sends Hail instead of status updates
void zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendCommands(switchMultilevelGetCmd())
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

String switchBinarySetCmd(Integer value, Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinarySet(switchValue: value), ep)
}

String switchBinaryGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchBinaryV1.switchBinaryGet(), ep)
}

String switchMultilevelSetCmd(Integer value, Integer duration, Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelSet(dimmingDuration: duration, value: value), ep)
}

String switchMultilevelGetCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelGet(), ep)
}

String switchMultilevelStartLvChCmd(Boolean upDown, Integer duration, Integer ep=0) {
	//upDown: false=up, true=down
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelStartLevelChange(upDown: upDown, ignoreStartLevel:1, dimmingDuration: duration), ep)
}

String switchMultilevelStopLvChCmd(Integer ep=0) {
	return secureCmd(zwave.switchMultilevelV2.switchMultilevelStopLevelChange(), ep)
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
	device.removeDataValue("protocolVersion")
	device.removeDataValue("hardwareVersion")

	//Restore
	if (devModel) state.deviceModel = devModel
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

String getOnOffCmds(val, Integer endPoint=0) {
	return getSetLevelCmds(val ? 0xFF : 0x00, null, endPoint)
}

String getSetLevelCmds(level, duration=null, Integer endPoint=0) {
	Short levelVal = safeToInt(level, 99)
	// level 0xFF tells device to use last level, 0x00 is off
	if (levelVal != 0xFF && levelVal != 0x00) {
		//Convert level in range of min/max
		levelVal = convertLevel(levelVal, true)
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

	state.isDigital = true
	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"
	return switchMultilevelSetCmd(levelVal, durationVal, endPoint)
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

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)

	if (rawVal) {
		Integer level = (rawVal == 99 ? 100 : rawVal)
		level = convertLevel(level, false)

		desc = "level is set to ${level}%"
		if (type) desc += " (${type})"
		if (levelCorrection) desc += " [actual: ${rawVal}]"
		sendEventLog(name:"level", value:level, type:type, unit:"%", desc:desc, ep)
	}
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
	BigDecimal firmware = firmwareVersion

	List<Map> tmpList = []
	paramsMap.each { name, pMap ->
		Map tmpMap = pMap.clone()
		tmpMap.options = tmpMap.options?.clone()

		//Save the name
		tmpMap.name = name

		//Apply custom adjustments
		tmpMap.changes.each { m, changes ->
			if (m == devModel) {
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

Integer convertLevel(level, userLevel=false) {
	if (levelCorrection) {
		Integer brightmax = getParamValue("maximumBrightness")
		Integer brightmin = getParamValue("minimumBrightness")
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
