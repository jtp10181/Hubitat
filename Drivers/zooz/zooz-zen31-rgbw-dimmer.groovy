/*
 *  Zooz ZEN31 RGBW Dimmer
 *    - Model: ZEN31
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-zen31-rgbw-dimmer/115212
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [0.1.0] - 2023-03-19 (@jtp10181)
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
import groovy.json.JsonOutput
import hubitat.helper.ColorUtils

@Field static final String VERSION = "0.1.0"
@Field static final Map deviceModelNames = ["0902:2000":"ZEN31"]

metadata {
	definition (
		name: "Zooz ZEN31 RGBW Dimmer Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://github.com/jtp10181/Hubitat/raw/main/Drivers/zooz/zooz-zen31-rgbw-dimmer.groovy"
	) {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "SwitchLevel"
		capability "ChangeLevel"
		capability "Configuration"
		capability "Refresh"
		//capability "Flash"
		capability "PowerMeter"
		capability "ColorMode"
		//capability "ColorControl"
		//capability "ColorTemperature"
		capability "LightEffects"
		capability "PushableButton"
		capability "HoldableButton"
		capability "ReleasableButton"
		capability "DoubleTapableButton"

		//Modified from default to add duration argument
		command "startLevelChange", [
			[name:"Direction*", description:"Direction for level change request", type: "ENUM", constraints: ["up","down"]],
			[name:"Duration", type:"NUMBER", description:"Transition duration in seconds", constraints:["NUMBER"]] ]

		//command "refreshParams"
		command "setScene", [ [name:"Select Scene*", type: "ENUM", constraints: PRESET_SCENES] ]
		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],
			[name:"value*",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]],
			[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"0902", deviceId:"2000", inClusters:"0x5E,0x55,0x98,0x9F,0x56,0x22,0x6C", deviceJoinName:"Zooz ZEN31 RGBW Dimmer"
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

		input "forceBrightness", "bool",
			title: fmtTitle("Force Full Device Brightness"),
			description: fmtDesc("<b>Disabled:</b> Turns on with previous level. <b>Enabled:</b> Forces parent device brightness (level) to maximum, child levels are not affected."),
			defaultValue: false

		input "quickRefresh", "bool",
			title: fmtTitle("Quickly Refresh Status after Changes"),
			description: fmtDesc("<b>Disabled:</b> Waits for device to send status back after making changes. <b>Enabled:</b> Immediately requests status update after making changes. WARNING: This will cause a significant increase in Z-Wave messages."),
			defaultValue: false

		input "preStaging", "bool",
			title: fmtTitle("Allow Pre-Staging"),
			description: fmtDesc("<b>Disabled:</b> Setting level or colors will turn the LED on. <b>Enabled:</b> Allows colors and level to be pre-staged on child devices while LED is off."),
			defaultValue: false

		input "whiteAndRGB", "bool",
			title: fmtTitle("Allow White and RGB Simultaneously"),
			description: fmtDesc("<b>Disabled:</b> turning on white will turn off RGB colors, and turning on RGB will turn off white. <b>Enabled:</b> White and RGB can both be on at the same time."),
			defaultValue: false

		// input "levelCorrection", "bool",
		// 	title: fmtTitle("Brightness Correction"),
		// 	description: fmtDesc("Brightness level set on dimmer is converted to fall within the min/max range but shown with the full range of 1-100%"),
		// 	defaultValue: false

		input "assocEnabled", "bool",
			title: fmtTitle("Show Association Settings"),
			description: fmtDesc("Turn on and Save to show the Association Settings"),
			defaultValue: false
		
		if (assocEnabled) {
			input "assocDNI2", "string",
				title: fmtTitle("Device Associations - Group 2 (ZEN31 Sync)"),
				description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear."),
				required: false

			for(int i in 3..maxAssocGroups) {
				Integer inNum = Math.round((i-2)/2)
				Integer oe = i % 2
				input "assocDNI$i", "string",
					title: fmtTitle("Device Associations - Group $i (IN$inNum " + (oe ? "On/Off" : "Dimming") + ")"),
					description: fmtDesc("Supports up to ${maxAssocNodes} Hex Device IDs separated by commas. Check device documentation for more info. Save as blank or 0 to clear."),
					required: false
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

//Association Settings
@Field static final int maxAssocGroups = 10
@Field static final int maxAssocNodes = 5

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	powerFailure: [ num: 1,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 0,
		options: [1:"Restores Last Status", 0:"Forced to Off", 1:"Forced to On"],
	],
	//Input Types
	swIn1: [ num: 20,
		title: "Input Type (IN1)",
		size: 1, defaultVal: 2,
		options: [0:"Analog with no Pull-up", 1:"Analog with Pull-up", 2:"Momentary Switch", 3:"Toggle Switch", 4:"On/Off Switch"],
	],
	swIn2: [ num: 21,
		title: "Input Type (IN2)",
		size: 1, defaultVal: 2,
		options: [0:"Analog with no Pull-up", 1:"Analog with Pull-up", 2:"Momentary Switch", 3:"Toggle Switch", 4:"On/Off Switch"],
	],
	swIn3: [ num: 22,
		title: "Input Type (IN3)",
		size: 1, defaultVal: 2,
		options: [0:"Analog with no Pull-up", 1:"Analog with Pull-up", 2:"Momentary Switch", 3:"Toggle Switch", 4:"On/Off Switch"],
	],
	swIn4: [ num: 23,
		title: "Input Type (IN4)",
		size: 1, defaultVal: 2,
		options: [0:"Analog with no Pull-up", 1:"Analog with Pull-up", 2:"Momentary Switch", 3:"Toggle Switch", 4:"On/Off Switch"],
	],
	//Scene Control
	sceneIn1: [ num: 40,
		title: "Scene Events (IN1)",
		size: 1, defaultVal: 15,
		options: [0:"Disabled", 15:"All Enabled"],
	],
	sceneIn2: [ num: 41,
		title: "Scene Events (IN2)",
		size: 1, defaultVal: 15,
		options: [0:"Disabled", 15:"All Enabled"],
	],
	sceneIn3: [ num: 42,
		title: "Scene Events (IN3)",
		size: 1, defaultVal: 15,
		options: [0:"Disabled", 15:"All Enabled"],
	],
	sceneIn4: [ num: 43,
		title: "Scene Events (IN4)",
		size: 1, defaultVal: 15,
		options: [0:"Disabled", 15:"All Enabled"],
	],
	//Power Reporting
	powerFrequency: [ num: 62,
		title: "Power (Watts) Reporting Frequency",
		size: 2, defaultVal: 0,
		description: "[0 = Disabled]  Minimum number of seconds between reports",
		range: "0,30..32400",
	],
	voltageThreshold: [ num: 63,
		title: "Sensor Voltage (V) Reporting Threshold",
		size: 2, defaultVal: 0,
		description: "[1 = 0.1V, 100 = 10V]  Report when changed by this amount",
		range: 0..100,
		hidden: true
	],
	voltageFrequency: [ num: 64,
		title: "Sensor Voltage (V) Reporting Frequency",
		size: 2, defaultVal: 0,
		description: "[0 = Disabled]  Minimum number of seconds between reports",
		range: "0,30..32400",
		hidden: true
	],
	energyThreshold: [ num: 65,
		title: "Energy (kWh) Reporting Threshold",
		size: 2, defaultVal: 0,
		description: "[1 = 0.01kWh, 100 = 1kWh]  Report when changed by this amount",
		range: 0..500,
		hidden: true
	],
	energyFrequency: [ num: 66,
		title: "Energy (kWh) Reporting Frequency",
		size: 2, defaultVal: 0,
		description: "[0 = Disabled]  Minimum number of seconds between reports",
		range: "0,30..32400",
		hidden: true
	],
	//Other Settings
	switchMode: [ num: 150,
		title: "RGBW / HSB Wall Switch Mode",
		description: "See Zooz advanced settings docs for more info",
		size: 1, defaultVal: 0,
		options: [0:"RGBW Mode", 1:"HSB Mode"],
	],
	rampRate: [ num: 151,
		title: "Physical Ramp Rate to Full On/Off",
		size: 2, defaultVal: 3,
		options: [0:"Instant On/Off"] //rampRateOptions
	],
	zwaveRampRate: [ num: 152,
		title: "Z-Wave Ramp Rate to Full On/Off",
		size: 2, defaultVal: 3,
		options: [0:"Instant On/Off"] //rampRateOptions
	],

	// Use Command to Change
	// presetPrograms: [ num: 157,
	// 	title: "Preset Special Effects",
	// 	size: 1, defaultVal: 0,
	// 	options: [0:"Disabled", 6:"Fireplace", 7:"Storm", 8:"Rainbow", 9:"Polar Lights", 10:"Police"],
	// ],
]

/* ZEN31
CommandClassReport - class:0x22, version:1    (Application Status)
CommandClassReport - class:0x26, version:4    (Multilevel Switch)
CommandClassReport - class:0x31, version:11    (Multilevel Sensor)
CommandClassReport - class:0x32, version:3    (Meter)
CommandClassReport - class:0x33, version:3    (Color Switch)
CommandClassReport - class:0x55, version:2    (Transport Service)
CommandClassReport - class:0x56, version:1    (CRC-16 Encapsulation)
CommandClassReport - class:0x59, version:2    (Association Group Information (AGI))
CommandClassReport - class:0x5A, version:1    (Device Reset Locally)
CommandClassReport - class:0x5B, version:3    (Central Scene)
CommandClassReport - class:0x5E, version:2    (Z-Wave Plus Info)
CommandClassReport - class:0x60, version:4    (Multi Channel)
CommandClassReport - class:0x6C, version:1    (Supervision)
CommandClassReport - class:0x70, version:1    (Configuration)
CommandClassReport - class:0x71, version:8    (Alarm)
CommandClassReport - class:0x72, version:2    (Manufacturer Specific)
CommandClassReport - class:0x73, version:1    (Powerlevel)
CommandClassReport - class:0x75, version:2    (Protection)
CommandClassReport - class:0x7A, version:4    (Firmware Update Meta Data)
CommandClassReport - class:0x85, version:2    (Association)
CommandClassReport - class:0x86, version:2    (Version)
CommandClassReport - class:0x8E, version:3    (Multi Channel Association)
CommandClassReport - class:0x98, version:1    (Security 0)
CommandClassReport - class:0x9F, version:1    (Security 2)
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x26: 2,	// switchmultilevelv2 (4)
	0x6C: 1,	// supervisionv1
	0x70: 1,	// configurationv1
	0x72: 2,	// manufacturerspecificv2
	0x85: 2,	// associationv2
	0x86: 2,	// versionv2
	0x8E: 3,	// multichannelassociationv3
]

/*** Static Lists and Settings ***/
@Field static final Map COLOR_COMPONENTS = [white:0, red:2, green:3, blue:4]
@Field static final Map PRESET_SCENES = [0:"Disabled", 6:"Fireplace", 7:"Storm", 8:"Rainbow", 9:"Polar Lights", 10:"Police"]

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

	//device.deleteCurrentState("lightEffects")
	sendEvent(name:"lightEffects", value: PRESET_SCENES)
	//sendEvent(name:"lightEffects", value: JsonOutput.toJson(PRESET_SCENES))

	updateSyncingStatus(6)
	runIn(1, executeRefreshCmds)
	runIn(4, executeConfigureCmds)
	runIn(6, executeRefreshCmds)
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
def on() {
	logDebug "on..."
	//flashStop()
	def onVal = forceBrightness ? 99 : 0xFF
	return delayBetween(getSetLevelCmds(onVal), 200)
}

def off() {
	logDebug "off..."
	//flashStop()
	return delayBetween(getSetLevelCmds(0x00), 200)
}

def setLevel(level, duration=null) {
	logDebug "setLevel($level, $duration)..."
	return delayBetween(getSetLevelCmds(level, duration), 200)
}

List<String> startLevelChange(direction, duration=null) {
	Boolean upDown = (direction == "down") ? true : false
	Integer durationVal = validateRange(duration, getParamValue("holdRampRate"), 0, 127)
	logDebug "startLevelChange($direction) for ${durationVal}s"

	List<String> cmds = [switchMultilevelStartLvChCmd(upDown, durationVal)]

	return delayBetween(cmds, 200)
}

String stopLevelChange() {
	logDebug "stopLevelChange()"
	return switchMultilevelStopLvChCmd()
}

//Button commands required with capabilities
void push(buttonId) { sendBasicButtonEvent(buttonId, "pushed") }
void hold(buttonId) { sendBasicButtonEvent(buttonId, "held") }
void release(buttonId) { sendBasicButtonEvent(buttonId, "released") }
void doubleTap(buttonId) { sendBasicButtonEvent(buttonId, "doubleTapped") }

void setEffect(efNum) {
	logDebug "setEffect(${efNum})"
	efNum = safeToInt(efNum)
	String efName = PRESET_SCENES[efNum]
	if (efName != null) {
		//state.effectNumber = efNum
		logDebug "Set Scene [${efNum} : ${efName}]"
		sendCommands(configSetGetCmd([num:157, size:1], efNum))
	}
	else {
		logWarn "setEffect(${efNum}): Invalid Effect Number"
	}
}

void setNextEffect() {
	List keys = PRESET_SCENES.keySet().sort()
	Integer newEfNum = state.effectNumber + 1
	if (!keys.contains(newEfNum)) newEfNum = keys[1]
	sendCommands(configSetGetCmd([num:157, size:1], newEfNum))
}
void setPreviousEffect() {
	List keys = PRESET_SCENES.keySet().sort()
	Integer newEfNum = state.effectNumber - 1
	if (!keys.contains(newEfNum) || newEfNum <= 0) newEfNum = keys.pop()
	sendCommands(configSetGetCmd([num:157, size:1], newEfNum))
}

//Flashing Capability
/*
void flash(Number rateToFlash = 1500) {
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
*/


/*** Custom Commands ***/
void setScene(String efName) {
	Short paramVal = PRESET_SCENES.find{ efName.equalsIgnoreCase(it.value) }.key
	logDebug "Set Scene [${paramVal} : ${efName}]"
	sendCommands(configSetGetCmd([num:157, size:1], paramVal))
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

/*** Child Capabilities ***/
void componentOn(cd) {
	String name = cd.getDataValue("shortName")
	logDebug "componentOn from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	
	List<String> cmds = []
	if (name == "color") {
		cmds += getColorCmds([:], cd)
	}
	else if (name == "white") {
		Integer level = cd.currentValue("level")
		cmds += getWhiteCmds(level)
	}
	if (preStaging) { cmds += on() }

	sendCommands(cmds, 100)
}

void componentOff(cd) {
	String name = cd.getDataValue("shortName")
	logDebug "componentOff from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	
	List<String> cmds = []
	if (name == "color") {
		cmds << switchColorRGBSetCmd([0,0,0])
	}
	else if (name == "white") {
		cmds << switchColorWhiteSetCmd(0)
	}

	if (quickRefresh) {
		COLOR_COMPONENTS.each {	cmds << switchColorGetCmd(it.value) }
	}

	sendCommands(cmds,100)
}

void componentRefresh(cd) {
	String name = cd.getDataValue("shortName")
	logDebug "Refresh from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logWarn "Refresh from child is not supported"
}

//******** Need to implement duration! *****************
void componentSetLevel(cd, level, duration=null) {
	String name = cd.getDataValue("shortName")
	logDebug "setLevel from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"

	List<String> cmds = []
	if (name == "color") {
		cmds += getColorCmds([level: level], cd)
	}
	else if (name == "white") {
		cmds += getWhiteCmds(level)
	}

	sendCommands(cmds,100)
}

void componentSetSaturation(cd, percent) {
	String name = cd.getDataValue("shortName")
	logDebug "setSaturation from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logDebug "setSaturation(${percent})"

	List<String> cmds = []
	if (name == "color") {
		cmds += getColorCmds([saturation: percent], cd)
	}
	sendCommands(cmds,100)
}

void componentSetHue(cd, value) {
	String name = cd.getDataValue("shortName")
	logDebug "setHue from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logDebug "setHue(${value})"

	List<String> cmds = []
	if (name == "color") {
		cmds += getColorCmds([hue: value], cd)
	}
	sendCommands(cmds,100)
}

void componentSetColor(cd, cMap) {
	String name = cd.getDataValue("shortName")
	logDebug "setColor from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logDebug "setColor(${cMap})"

	List<String> cmds = []
	if (name == "color") { 
		cmds += getColorCmds(cMap)
	}
	sendCommands(cmds,100)
}

def componentStartLevelChange(cd, direction) {
	String name = cd.getDataValue("shortName")
	logDebug "StartLevelChange from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logWarn "startLevelChange from child is not supported"
}

def componentStopLevelChange(cd) {
	String name = cd.getDataValue("shortName")
	logDebug "StopLevelChange from ${cd.displayName} (${cd.deviceNetworkId}) [${name}]"
	logWarn "stopLevelChange from child is not supported"
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
	sendEvent(name:"numberOfButtons", value:4)
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
	else if (cmd.parameterNumber == 157) { //Effects Parameter
		sendEffectEvents(val)
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
	//flashStop() //Stop flashing if its running
	sendSwitchEvents(cmd.value, null, ep)
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	// String type = (state.isDigital ? "digital" : "physical")
	// state.remove("isDigital")
	// if (type == "physical") flashStop()

	sendSwitchEvents(cmd.value, null, ep)
}

void zwaveEvent(hubitat.zwave.commands.switchcolorv3.SwitchColorReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (!state.RGBW) state.RGBW = [red:0, green:0, blue:0]
    state.RGBW[cmd.colorComponent] = cmd.targetValue

	//Delayed so we get all updates before sending
	runInMillis(800, sendColorEvents)
}

void zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification cmd, ep=0){
	logTrace "${cmd} (ep ${ep})"

	if (state.csnSequenceNumber == cmd.sequenceNumber) return
	state.csnSequenceNumber = cmd.sequenceNumber

	Map scene = [name: null, value: cmd.sceneNumber, desc: null, type:"physical", isStateChange:true]

	switch (cmd.keyAttributes){
		case 0:
			scene.name = "pushed"
			break
		case 1:
			scene.name = "released"
			break
		case 2:
			scene.name = "held"
			break
		case 3:
			scene.name = "doubleTapped"
			break
		default:
			logDebug "Unhandled keyAttributes: ${cmd}"
	}

	if (scene.name) {
		scene.desc = "button ${scene.value} ${scene.name}"
		sendEventLog(scene, ep)
	}
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep=0) {
	logTrace "${cmd} (meterValue: ${cmd.scaledMeterValue}, previousMeter: ${cmd.scaledPreviousMeterValue}) (ep ${ep})"

	BigDecimal val = safeToDec(cmd.scaledMeterValue, 0, Math.min(cmd.precision,3))
	logDebug "MeterReport: scale:${cmd.scale}, scaledMeterValue:${cmd.scaledMeterValue} (${val}), precision:${cmd.precision}"

	switch (cmd.scale) {
		// case 0: //Energy
		// 	sendEventLog(name:"energy", value:val, unit:"kWh")
		// 	break
		case 2: //Power
			sendEventLog(name:"power", value:val, unit:"W")
			break
		default:
			logDebug "Unhandled Meter Scale: $cmd"
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

String switchColorRGBSetCmd(rgb) {
	Map colors = [red: rgb[0], green: rgb[1], blue: rgb[2]]
	if (!whiteAndRGB && rgb != [0,0,0]) {
		colors += [warmWhite:0]
		logDebug "whiteAndRGB is ${whiteAndRGB}, turning off WHITE ${colors}"
	}
	return secureCmd(zwave.switchColorV3.switchColorSet(colors))
}

String switchColorWhiteSetCmd(value) {
	Map colors = [warmWhite: value]
	if (!whiteAndRGB && value > 0) {
		colors += [red:0, green:0, blue:0]
		logDebug "whiteAndRGB is ${whiteAndRGB}, turning off RGB ${colors}"
	}
	return secureCmd(zwave.switchColorV3.switchColorSet(colors))
}

String switchColorGetCmd(colorId) {	
	return secureCmd(zwave.switchColorV3.switchColorGet(colorComponentId: colorId))
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

	//This needs to be first to check effects mode
    cmds << secureCmd(zwave.configurationV2.configurationGet(parameterNumber: 157))

	//Level, Colors, and Power
	cmds << switchMultilevelGetCmd()
	COLOR_COMPONENTS.each {	cmds << switchColorGetCmd(it.value) }
	cmds << secureCmd(zwave.meterV3.meterGet(scale: 2)) //Power Meter

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
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

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

Integer getPendingChanges() {
	Integer configChanges = configParams.count { param ->
		Integer paramVal = getParamValue(param, true)
		((paramVal != null) && (paramVal != getParamStoredValue(param.num)))
	}
	Integer pendingAssocs = Math.ceil(getConfigureAssocsCmds()?.size()/2) ?: 0
	return (!state.resyncAll ? (configChanges + pendingAssocs) : configChanges)
}

List<String> getSetLevelCmds(Number level, Number duration=null, Integer endPoint=0) {
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

	//state.isDigital = true
	logDebug "getSetLevelCmds output [level:${levelVal}, duration:${durationVal}, endPoint:${endPoint}]"

	List<String> cmds = []
	cmds << switchMultilevelSetCmd(levelVal, durationVal, endPoint)
	if (quickRefresh) cmds << switchMultilevelGetCmd()

	return cmds
}

List<String> getColorCmds(Map hsvMap, child=null) {
	logDebug "getColorCmds(${hsvMap})"

	//Get Current State
	List rgb = [state.RGBW?.red, state.RGBW?.green, state.RGBW?.blue]
	List hsv = ColorUtils.rgbToHSV(rgb)

	//Override with current settings from child device
	if (child) {
		hsv = [
			child.currentValue("hue"),
			child.currentValue("saturation"),
			child.currentValue("level")
		]
	}

	//Figure out desired state from data provided
	def hue = (hsvMap.hue != null ? hsvMap.hue : hsv[0])
	def sat = (hsvMap.saturation != null ? hsvMap.saturation : hsv[1])
	def val = (hsvMap.level != null ? hsvMap.level : hsv[2])

	//Convert back to RGB
	rgb = ColorUtils.hsvToRGB([hue, sat, val])
	logDebug "getColorCmds HSV/RGB: ${[hue,sat,val]} / ${rgb}"

	List<String> cmds = []
	cmds << switchColorRGBSetCmd(rgb)
	if (!preStaging && val > 0) { cmds += on() }
	if (quickRefresh) {
		COLOR_COMPONENTS.each {	cmds << switchColorGetCmd(it.value) }
	}

	return cmds
}

List<String> getWhiteCmds(level) {
	logDebug "getWhiteCmds(${level})"

	//Scale the level from 0-100 to 0-255
	Integer scaledLevel = Math.round((level * 255) / 100)

	List<String> cmds = []
	cmds << switchColorWhiteSetCmd(scaledLevel)
	if (!preStaging && level > 0) { cmds += on() }
	if (quickRefresh) {
		cmds << switchColorGetCmd(COLOR_COMPONENTS.white)
	}

	return cmds
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, ep=null) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Endpoint Events
	if (ep) {
		def childDev = getChildByName(ep)
		String logEp = "(${ep}) "

		if (childDev) {
			if (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
				evt.descriptionText = "${childDev}: ${evt.descriptionText}"
				childDev.parse([evt])
			} else {
				logDebug "${logEp}${evt.descriptionText} [NOT CHANGED]"
				childDev.sendEvent(evt)
			}
		}
		else {
			log.error "No device for endpoint (${ep}). Press Configure to create child devices."
		}
		return
	}

	//Main Device Events
	if (device.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
		logInfo "${evt.descriptionText}"
	} else {
		logDebug "${evt.descriptionText} [NOT CHANGED]"
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSwitchEvents(rawVal, String type, ep=null) {
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

	String whValue = (rawVal && state.whiteEnabled) ? "on" : "off"
	String cValue  = (rawVal && state.colorEnabled) ? "on" : "off"
	sendEventLog(name:"switch", value:whValue, type:type, "white")
	sendEventLog(name:"switch", value:cValue, type:type, "color")
}

void sendBasicButtonEvent(buttonId, String name) {
	String desc = "button ${buttonId} ${name} (digital)"
	sendEventLog(name:name, value:buttonId, type:"digital", desc:desc, isStateChange:true)
}

void sendColorEvents() {
	//Check main switch state
	Boolean mainSwitch = (device.currentValue("switch") == "on")

	//RGB Color Events
	List rgb = [state.RGBW.red, state.RGBW.green, state.RGBW.blue]
	List hsv = ColorUtils.rgbToHSV(rgb)
	Integer cHue = Math.round(hsv[0])
	Integer cSat = Math.round(hsv[1])
	Integer cLevel = Math.round(hsv[2])
	def cdColor = getChildByName("color")

	if (hsv[2] > 0) {
		state.colorEnabled = true
		sendEventLog(name:"hue", value: cHue, "color")
		sendEventLog(name:"saturation", value: cSat, unit:"%", "color")
		sendEventLog(name:"level", value: cLevel, unit:"%", "color")
		sendEventLog(name:"colorName", value: getGenericColor(hsv), "color")
		//sendEventLog(name:"RGB", value: rgb, "color")
		//sendEventLog(name:"color", value: [hue:cHue, saturation:cSat, level:cLevel], "color")
		//Sending to parse doesn't work for RGB on component driver
		cdColor.sendEvent(name:"RGB", value: rgb)
		//If device is on switch on the color child
		if (mainSwitch) {
			sendEventLog(name:"switch", value: "on", "color")
		}
	}
	else {
		state.colorEnabled = false
		sendEventLog(name:"switch", value: "off", "color")
	}
	
	if (state.effectNumber == 0) sendEventLog(name:"colorMode", value:"RGB")

	//White Events
	Integer white = state.RGBW?.warmWhite
	Integer whLevel = Math.round((white*100)/255)
	if (whLevel > 0) {
		state.whiteEnabled = true
		sendEventLog(name:"level", value: whLevel, unit:"%", "white")
		//If device is on switch on the white child
		if (mainSwitch) {
			sendEventLog(name:"switch", value: "on", "white")
		}
	}
	else {
		state.whiteEnabled = false
		sendEventLog(name:"switch", value: "off", "white")
	}

	//If Both off turn off main switch
	if (mainSwitch && !state.colorEnabled && !state.whiteEnabled) {
		sendCommands(getSetLevelCmds(0x00))
	}
}

void sendEffectEvents(effect) {
	logDebug "sendEffectEvents(${effect})"
	String efName = PRESET_SCENES[effect] ?: "Unknown"
	sendEventLog(name:"effectName", value: efName)
	state.effectNumber = effect
	if (effect > 0) {
		sendEventLog(name:"colorMode", value:"EFFECTS", desc:"colorMode set to EFFECTS (${efName})")
		sendEventLog(name:"switch", value:"on")
		sendCommands(["delay 2500",switchMultilevelGetCmd()])
	}
	else {
		sendEventLog(name:"colorMode", value:"RGB", desc:"colorMode set to RGB (Effects Disabled)")
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
	paramsMap.rampRate.options << rampRateOptions
	paramsMap.zwaveRampRate.options << rampRateOptions
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

/*** Parameter Helper Functions ***/
private getRampRateOptions() {
	return getTimeOptionsRange("Second", 1, [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,20,25,30,45,60,75,90])
}

private getTimeOptionsRange(String name, Integer multiplier, List range) {
	return range.collectEntries{ [(it*multiplier): "${it} ${name}${it == 1 ? '' : 's'}"] }
}

/*** Child Helper Functions ***/
private getChildByName(name) {
	def dni = "${device.deviceNetworkId}-${name}"
	return childDevices?.find { dni.equalsIgnoreCase(it.deviceNetworkId) }
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

// iOS app has no way of clearing string input so workaround is to have users enter 0.
String getAssocDNIsSetting(grp) {
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
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

String getGenericColor(List hsv){
    String colorName
    Integer hue = Math.round(hsv[0] * 3.6)
    switch (hue) {
        case 0..15:  colorName = "Red"; break
        case 16..45: colorName = "Orange"; break
        case 46..75: colorName = "Yellow"; break
        case 76..105: colorName = "Chartreuse"; break
        case 106..135: colorName = "Green"; break
        case 136..165: colorName = "Spring"; break
        case 166..195: colorName = "Cyan"; break
        case 196..225: colorName = "Azure"; break
        case 226..255: colorName = "Blue"; break
        case 256..285: colorName = "Violet"; break
        case 286..315: colorName = "Magenta"; break
        case 316..345: colorName = "Rose"; break
        case 346..360: colorName = "Red"; break
    }
    //Check for Low Saturation
    if (hsv[1] <= 10) colorName = "White"

    return colorName
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
