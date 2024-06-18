/*
 *  Zooz ZEN Plugs Universal
 *    - Model: ZEN20 - MINIMUM FIRMWARE 2.0
 *    - Model: ZEN25 - MINIMUM FIRMWARE 2.0
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-smart-plugs/98333
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.2.6] - 2024-06-15 (@jtp10181)
  - Fix for range expansion and sharing with Hub Mesh

## [1.2.2] - 2024-04-06 (@jtp10181)
  - Added singleThreaded flag
  - Updated Library and common code
  - Other minor fixes

## [0.1.0] - 2023-10-21 (@jtp10181)
  - Initial public release to HPM
  - Enabled, tested, and adjusted addChild code
  - Fixed bug with lower limits not being enforced

## [0.0.6] - 2023-10-20 (@jtp10181)
  - Hard coded list for metering endpoints to avoid probing failures
  - Adjusted paramater code to save to data less often
  - Force reporting on for all channels when configure is run

## [0.0.5] - 2023-10-15 (@jtp10181)
  - Better child detection for ZEN20 using predefined list
  - Probe each endpoint for metering support

## [0.0.4] - 2023-10-11 (@jtp10181)
  - Initial Beta Release based on v1.2.1 of my Zooz Plugs driver

 *  Copyright 2022-2024 Jeff Page
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

import groovy.transform.Field

@Field static final String VERSION = "1.2.6"
@Field static final String DRIVER = "Zooz-Plugs-MCP"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-smart-plugs/98333"
@Field static final Map deviceModelNames =
	["A000:A003":"ZEN25", "A000:A004":"ZEN20"]

metadata {
	definition (
		name: "Zooz ZEN Plugs MCP Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		singleThreaded: true,
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zen-plugs-mcp.groovy"
	) {
		capability "Actuator"
		capability "Switch"
		capability "Outlet"
		capability "PowerMeter"
		capability "CurrentMeter"
		capability "VoltageMeasurement"
		capability "EnergyMeter"
		capability "Configuration"
		capability "Refresh"
		// capability "Flash"

		//command "refreshParams"
		command "resetStats"

		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number"],
			[name:"value*",type:"NUMBER", description:"Parameter Value"],
			[name:"size",type:"NUMBER", description:"Parameter Size"]]

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"
		attribute "accessory", "string"
		attribute "energyDuration", "number"
		attribute "amperageHigh", "number"
		attribute "amperageLow", "number"
		attribute "powerHigh", "number"
		attribute "powerLow", "number"
		attribute "voltageHigh", "number"
		attribute "voltageLow", "number"
		attribute "warnings", "number"

		fingerprint mfr:"027A", prod:"A000", deviceId:"A003", inClusters:"0x5E,0x25,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x70,0x71,0x32,0x9F,0x60,0x6C,0x7A", controllerType: "ZWV" //Zooz ZEN25 Double Plug
		fingerprint mfr:"027A", prod:"A000", deviceId:"A004", inClusters:"0x00,0x00", controllerType: "ZWV" //Zooz ZEN20 Power Strip
	}

	preferences {
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

		if (state.powerMetering) {
			// input "accThreshold", "number",
			// 	title: fmtTitle("Accessory State Threshold (Watts)"),
			// 	description: fmtDesc("• Sets accessory status when power is above threshold.<br>• 0 = Disabled"),
			// 	defaultValue: 0, range: "0..2000", required: false

			input "highLowEnable", "bool",
				title: fmtTitle("Enable High/Low Attributes"),
				description: fmtDesc("• Track high and low values in separate attributes"),
				defaultValue: false
		}
	}
}

void debugShowVars() {
	log.warn "settings ${settings.hashCode()} ${settings}"
	log.warn "paramsList ${paramsList.hashCode()} ${paramsList}"
	log.warn "paramsMap ${paramsMap.hashCode()} ${paramsMap}"
}

//Association Settings
@Field static final int maxAssocGroups = 1
@Field static final int maxAssocNodes = 1

/*** Static Lists and Settings ***/
@Field static List metersList = [
	[name:"energy", scale:0, unit:"kWh", limitLo:0, limitHi:null],
	[name:"power", scale:2, unit:"W", limitLo:0, limitHi:2400],
	[name:"voltage", scale:4, unit:"V", limitLo:0, limitHi:140],
	[name:"amperage", scale:5, unit:"A", limitLo:0, limitHi:18]
]
@Field static Map epNames = [
	ZEN20:[1:"CH1", 2:"CH2", 3:"CH3", 4:"CH4", 5:"CH5", 6:"USB1", 7:"USB2"],
	ZEN25:[1:"Left", 2:"Right", 3:"USB"]
]
@Field static Map epMeters = [ZEN20:(0..5), ZEN25:(0..2)]

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	powerFailure: [ num: 1,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 0,
		options: [0:"Restores Last Status", 1:"All Turned On", 2:"All Turned Off"]
	],
	powerThreshold: [ num: 2,
		title: "Power (Watts) Reporting Threshold",
		size: 4, defaultVal: 10,
		description: "Report when changed by this amount, 0 = Disabled",
		range: "0..65535"
	],
	powerFrequency: [ num: 3,
		title: "Power (Watts) Reporting Frequency",
		size: 4, defaultVal: 30,
		description: "Minimum number of MINUTES between wattage reports",
		//range: "30..2678400" //Seconds
		range: "1..44640" //Minutes
	],
	energyFrequency: [ num: 4,
		title: "Energy (kWh) Reporting Frequency",
		size: 4, defaultVal: 60,
		description: "Minimum number of MINUTES between energy reports",
		//range: "30..2678400" //Seconds
		range: "1..44640" //Minutes
	],
	voltageFrequency: [ num: 5,
		title: "Voltage (V) Reporting Frequency",
		size: 4, defaultVal: 120,
		description: "Minimum number of MINUTES between voltage reports",
		//range: "30..2678400" //Seconds
		range: "1..44640", //Minutes
		changes: [20:[num:36, firmVer:2.03, range: "0..44640"]]
	],
	currentFrequency: [ num: 6,
		title: "Current (Amps) Reporting Frequency",
		size: 4, defaultVal: 60,
		description: "Minimum number of MINUTES between amperage reports",
		//range: "30..2678400" //Seconds
		range: "1..44640", //Minutes
		changes: [20:[num:25, firmVer:2.03, range: "0..44640"]]
	],
	overloadProtection: [ num: 7,
		title: "Overload Protection *See Docs*",
		size: 1, defaultVal: 10,
		description: "Zooz DOES NOT recommend disabling this, as it may result in device damage and malfunction!",
		range: "1..10",
		changes: [20:[num:5, size:2, defaultVal:1500, range:"0..1500"]]
	],
	//---Auto On/Off Timers--- CH1
	offTimer1Enabled: [ num: 8,
		title: "Auto Turn-Off Timer Enabled (CH1)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:6]]
	],
	offTimer1: [ num: 9,
		title: "Auto Turn-Off Timer (CH1)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:7]]
	],
	onTimer1Enabled: [ num: 10,
		title: "Auto Turn-On Timer Enabled (CH1)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:8]]
	],
	onTimer1: [ num: 11,
		title: "Auto Turn-On Timer (CH1)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:9]]
	],
	//---Auto On/Off Timers--- CH2
	offTimer2Enabled: [ num: 12,
		title: "Auto Turn-Off Timer Enabled (CH2)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:10]]
	],
	offTimer2: [ num: 13,
		title: "Auto Turn-Off Timer (CH2)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:11]]
	],
	onTimer2Enabled: [ num: 14,
		title: "Auto Turn-On Timer Enabled (CH2)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:12]]
	],
	onTimer2: [ num: 15,
		title: "Auto Turn-On Timer (CH2)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:13]]
	],
	//---Auto On/Off Timers--- CH3
	offTimer3Enabled: [ num: null,
		title: "Auto Turn-Off Timer Enabled (CH3)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:14]]
	],
	offTimer3: [ num: null,
		title: "Auto Turn-Off Timer (CH3)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:15]]
	],
	onTimer3Enabled: [ num: null,
		title: "Auto Turn-On Timer Enabled (CH3)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:16]]
	],
	onTimer3: [ num: null,
		title: "Auto Turn-On Timer (CH3)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:17]]
	],
	//---Auto On/Off Timers--- CH4
	offTimer4Enabled: [ num: null,
		title: "Auto Turn-Off Timer Enabled (CH4)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:14]]
	],
	offTimer4: [ num: null,
		title: "Auto Turn-Off Timer (CH4)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:15]]
	],
	onTimer4Enabled: [ num: null,
		title: "Auto Turn-On Timer Enabled (CH4)",
		size: 1, defaultVal: 0, hidden: true,
		options: [0:"Disabled", 1:"Enabled"],
		changes: [20:[num:16]]
	],
	onTimer4: [ num: null,
		title: "Auto Turn-On Timer (CH4)",
		size: 4, defaultVal: 0, range: "0..65535",
		description: "Time in minutes, 0 = Disabled",
		changes: [20:[num:17]]
	],
	//-----------------------------
	manualControl: [ num: 16,
		title: "Physical Button On/Off Control",
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
		changes: [20:[num:26]]
	],
	ledIndicator: [ num: 17,
		title: "LED Indicator Mode",
		size: 1, defaultVal: 1,
		options: [0:"Shows Power Consumption, Always On", 1:"LED On When Plug On", 2:"Shows Power Consumption for 5s", 3:"LED Always Off"],
		changes: [20:[num:27, options: [0:"LED On when Off", 1:"LED On When On", 2:"LED Always Off"]]]
	],
	reportingEnabled: [ num: 18,
		title: "Energy Reporting Settings",
		size: 1, defaultVal: 0,
		options: [0:"Enable Reporting", 1:"Disable Energy and USB Reports",
			2:"Disable LEFT Energy Reports", 3:"Disable RIGHT Energy Reports", 4:"Disable USB Status Reports"],
		changes: [20:[num:28, defaultVal:1, options: [1:"Enable Reporting", 0:"Disable All Reporting"]]]
	]
]

/* ZEN25
CommandClassReport - class:0x25, version:1   (Binary Switch)
CommandClassReport - class:0x32, version:4   (Meter)
CommandClassReport - class:0x55, version:2   (Transport Service)
CommandClassReport - class:0x59, version:1   (Association Group Information (AGI))
CommandClassReport - class:0x5A, version:1   (Device Reset Locally)
CommandClassReport - class:0x5E, version:2   (Z-Wave Plus Info)
CommandClassReport - class:0x60, version:4   (Multi Channel)
CommandClassReport - class:0x6C, version:1   (Supervision)
CommandClassReport - class:0x70, version:1   (Configuration)
CommandClassReport - class:0x71, version:8   (Notification)
CommandClassReport - class:0x72, version:2   (Manufacturer Specific)
CommandClassReport - class:0x73, version:1   (Powerlevel)
CommandClassReport - class:0x7A, version:4   (Firmware Update Meta Data)
CommandClassReport - class:0x85, version:2   (Association)
CommandClassReport - class:0x86, version:3   (Version)
CommandClassReport - class:0x8E, version:3   (Multi Channel Association)
CommandClassReport - class:0x9F, version:1   (Security 2)
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x25: 1,	// SwitchBinary
	0x32: 3,	// Meter
	0x60: 3,	// MultiChannel
	0x6C: 1,	// Supervision
	0x70: 1,	// Configuration
	0x71: 8,	// Notification
	0x72: 2,	// ManufacturerSpecific
	0x85: 2,	// Association
	0x86: 2,	// Version
	0x8E: 3,	// MultiChannelAssociation
]


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
	state.energyTime = new Date().time
	initialize()
}

void initialize() {
	logWarn "initialize..."
	refresh()
}

void configure() {
	logWarn "configure..."

	if (!pendingChanges || state.resyncAll == null) {
		logDebug "Enabling Full Re-Sync"
		clearVariables()
		state.resyncAll = true
	}

	updateSyncingStatus(6)
	runIn(1, executeProbeCmds)
	runIn(2, executeRefreshCmds)
	runIn(5, executeConfigureCmds)
}

void updated() {
	logDebug "updated..."

	if (!accThreshold) {
		device.deleteCurrentState("accessory")
	}
	if (highLowEnable != state.highLow) {
		state.highLow = highLowEnable
		resetStats(false)
	}

	executeProbeCmds()
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
	flashStop()
	return getOnOffCmds(0xFF)
}

def off() {
	logDebug "off..."
	flashStop()
	return getOnOffCmds(0x00)
}

//Flashing Capability
void flash(rateToFlash = null) {
	if (!rateToFlash) rateToFlash = state.flashRate
	//Min rate of 750ms sec, max of 30s
	rateToFlash = validateRange(rateToFlash, 1500, 750, 30000)
	Integer maxRun = 30 * 60 //30 Minutes
	state.flashNext = (device.currentValue("switch")=="on" ? "off" : "on")
	state.flashRate = rateToFlash

	logInfo "Flashing started with rate of ${rateToFlash}ms"

	//Start the flashing
	runIn(maxRun,flashStop,[data:true])
	flashHandler(rateToFlash)
}

void flashStop(Boolean turnOn = false) {
	if (state.flashNext != null) {
		logInfo "Flashing stopped..."
		unschedule("flashHandler")
		unschedule("flashStop")
		state.remove("flashNext")
		if (turnOn) { runIn(1,on) }
	}
}

void flashHandler(Integer rateToFlash) {
	if (state.flashNext == "on") {
		logDebug "Flash On"
		state.flashNext = "off"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getOnOffCmds(0xFF))
	}
	else if (state.flashNext == "off") {
		logDebug "Flash Off"
		state.flashNext = "on"
		runInMillis(rateToFlash, flashHandler, [data:rateToFlash])
		sendCommands(getOnOffCmds(0x00))
	}
}


/*** Custom Commands ***/
void refreshParams() {
	List<String> cmds = []
	cmds << mcAssociationGetCmd(1)
	for (int i = 1; i <= maxAssocGroups; i++) {
		cmds << associationGetCmd(i)
	}

	configParams.each { param ->
		cmds << configGetCmd(param)
	}

	if (cmds) sendCommands(cmds)
}

void resetStats(fullReset = true) {
	logDebug "reset..."

	runIn(2, refresh)

	device.deleteCurrentState("amperageHigh")
	device.deleteCurrentState("amperageLow")
	device.deleteCurrentState("powerHigh")
	device.deleteCurrentState("powerLow")
	device.deleteCurrentState("voltageHigh")
	device.deleteCurrentState("voltageLow")

	if (fullReset) {
		state.energyTime = new Date().time
		state.remove("energyDuration")
		device.deleteCurrentState("energyDuration")
		device.deleteCurrentState("warnings")
		if (state.powerMetering) {
			sendEventLog(name:"warnings", value:0, desc:"Reset Warnings and Energy Stats")
			sendCommands(meterResetCmd(0))
		}
	}
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

/*** Child Capabilities ***/
def componentOn(cd) {
	logDebug "componentOn from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getOnOffCmds(0xFF, getChildEP(cd)))
}

def componentOff(cd) {
	logDebug "componentOff from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getOnOffCmds(0x00, getChildEP(cd)))
}

def componentRefresh(cd) {
	logDebug "componentRefresh from ${cd.displayName} (${cd.deviceNetworkId})"
	sendCommands(getChildRefreshCmds(getChildEP(cd)))
}

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	zwaveParse(description)
}
void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	zwaveMultiChannel(cmd)
}
void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
	zwaveSupervision(cmd,ep)
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

		logDebug "${param.name} - ${param.title} (#${param.num}) = ${val.toString()}"
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
		if (!state.endPoints) {
			logDebug "Lifeline Association: ${cmd.nodeId}"
			state.group1Assoc = (cmd.nodeId == [zwaveHubNodeId]) ? true : false
		}
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
	logTrace "${cmd}"
	updateSyncingStatus()

	List mcNodes = []
	cmd.multiChannelNodeIds.each {mcNodes += "${it.nodeId}:${it.endPointId}"}

	if (cmd.groupingIdentifier == 1) {
		if (state.endPoints) {
			logDebug "Lifeline Association: ${cmd.nodeId} | MC: ${mcNodes}"
			state.group1Assoc = (mcNodes == ["${zwaveHubNodeId}:0"] ? true : false)
		}
	}
	else {
		logDebug "Unhandled Group: $cmd"
	}
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, null, ep)
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendSwitchEvents(cmd.value, null, ep)
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterSupportedReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	String scales = Integer.toBinaryString(cmd.scaleSupported)
	logTrace "${cmd}, scaleBinary: ${scales}"

	if (cmd.meterType == 1) {
		logDebug "Power Metering Support Detected and Enabled"
		state.powerMetering = true
		state.highLow = highLowEnable
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (cmd.endPoints > 0) {
		logDebug "Endpoints (${cmd.endPoints}) Detected and Enabled"
		state.endPoints = cmd.endPoints
		runInMillis(500,probeEndPoints)
		runIn(4,createChildDevices)
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (state.meterEPs == null) state.meterEPs = [0]
	if (cmd.commandClass.find { it == 0x32 }) {
		state.meterEPs[(cmd.endPoint as Integer)] = cmd.endPoint
		logDebug "Power Metering Detected for Endpoint ${cmd.endPoint}"
	}
	else { logDebug "Power Metering NOT FOUND for Endpoint ${cmd.endPoint}" }
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep=0) {
	logTrace "${cmd} (meterValue: ${cmd.scaledMeterValue}, previousMeter: ${cmd.scaledPreviousMeterValue}) (ep ${ep})"

	BigDecimal val = safeToDec(cmd.scaledMeterValue, 0, Math.min(cmd.precision,3))
	logDebug "MeterReport: scale:${cmd.scale}, scaledMeterValue:${cmd.scaledMeterValue} (${val}), precision:${cmd.precision} (ep ${ep})"

	Map meter = metersList.find{ it.scale == cmd.scale }
	if ((meter?.limitHi != null && val > safeToDec(meter.limitHi)) || (meter?.limitLo != null && val < safeToDec(meter.limitLo))) {
		logWarn "IGNORED MeterReport ${meter.name} (precision: ${cmd.precision}, size: ${cmd.size}, meterValue: ${cmd.scaledMeterValue} ${cmd.meterValue}, deltaTime: ${cmd.deltaTime}, previousMeter: ${cmd.scaledPreviousMeterValue} ${cmd.previousMeterValue}) payLoad: ${cmd.getPayload()}"
		sendEventLog(name:"warnings", value:(device.currentValue("warnings")?:0)+1, isStateChange:true,
			desc:"IGNORED METER REPORT of ${val}${meter.unit} for ${meter.name} " + (val < meter.limitLo ? "(below limit of ${meter.limitLo}" : "(over limit of ${meter.limitHi}") + "${meter.unit})", ep)
		return
	}

	switch (meter?.name) {
		case "energy":
			sendEnergyEvents(meter, val, ep)
			break
		case "power":
			sendAccessoryEvents(val)
		case "voltage":
		case "amperage":
			sendMeterEvents(meter, val, ep)
			break
		default:
			logDebug "Unknown Meter Scale: $cmd"
	}
}


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value} ${evt.unit ?: ''}".trim()

	//Endpoint Events
	if (ep) {
		def childDev = getChildByEP(ep)
	
		if (childDev) {
			if (childDev.currentValue(evt.name).toString() != evt.value.toString() || evt.isStateChange) {
				evt.descriptionText = "${childDev}: ${evt.descriptionText}"
				childDev.parse([evt])
			} else {
				String epName = epNames[state.deviceModel] ? epNames[state.deviceModel][ep] : "Outlet ${ep}"
				logDebug "(${epName}) ${evt.descriptionText} [NOT CHANGED]"
				childDev.sendEvent(evt)
			}
		}
		else {
			logErr "No device for endpoint (${ep}). Press Configure to create child devices."
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

void sendSwitchEvents(rawVal, String type, Integer ep=0) {
	String value = (rawVal ? "on" : "off")
	String epStr = (ep ? "$ep" : "")
	def isDigital = state["isDigital$epStr"]

	if (!type) {
		if (isDigital == true || isDigital == value) {
			logTrace "sendSwitchEvents: isDigital$epStr = ${isDigital}"
			type = "digital"
			state["isDigital$epStr"] = value
			runInMillis(500, switchDigitalRemove)
		}
		else { type = "physical" }
	}
	if (type == "physical") flashStop()

	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)
}
void switchDigitalRemove() {
	logTrace "switchDigitalRemove: Removing isDigital (${state.isDigital})"
	state.remove("isDigital")
	endPointList.each { ep ->
		logTrace "switchDigitalRemove: Removing isDigital$ep (${state["isDigital$ep"]})"
		state.remove("isDigital$ep" as String)
	}
}

void sendMeterEvents(meter, value, Integer ep=0) {
	sendEventLog(name:meter.name, value:value, unit:meter.unit, ep)

	if (highLowEnable && value != 0 && ep == 0) {
		Map low = [name: "${meter.name}Low", value: device.currentValue("${meter.name}Low")]
		Map high = [name: "${meter.name}High", value: device.currentValue("${meter.name}High")]

		if (value > high.value) {
			sendEventLog(name:high.name, value:value, unit:meter.unit, ep)
		}
		if (value < low.value || low.value == null) {
			sendEventLog(name:low.name, value:value, unit:meter.unit, ep)
		}
	}
}

void sendEnergyEvents(meter, value, Integer ep=0) {
	sendEventLog(name:meter.name, value:value, unit:meter.unit, ep)

	//Calculate and send the energyDuration
	if (!state.energyTime) { state.energyTime = new Date().time }

	BigDecimal duration = ((new Date().time) - state.energyTime)/60000
	BigDecimal enDurDays = safeToDec(duration/(24*60), 0, 2)
	BigDecimal enDurHours = safeToDec(duration/60, 0, 2)

	if (enDurDays > 1.0) {
		state.energyDuration = enDurDays + " Days"
	} else {
		state.energyDuration = enDurHours + " Hours"
	}
	sendEventLog(name:"energyDuration", value:enDurDays, unit:"days", ep)
}

void sendAccessoryEvents(powerVal) {
	if (accThreshold) {
		String value = (powerVal > accThreshold) ? "on" : "off"
		String desc = "accessory is turned ${value}"
		if (value != device.currentValue("accessory")) {
			sendEventLog(name:"accessory", value:value, desc:desc, ep)
		}
	}
}


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
void executeConfigureCmds() {
	logDebug "executeConfigureCmds..."

	//Checks and sets scheduled turn off
	checkLogLevel()

	List<String> cmds = []

	if (!firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
	}

	cmds += getConfigureAssocsCmds(true)

	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} - ${param.title} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
	}

	//Enabled reporting on each channel individually
	if (state.deviceModel == "ZEN20") {
		// (29..33).each { cmds += configSetGetCmd([num:it, size:1], 1) }
		cmds += configSetGetCmd([num:29, size:1], 1)
		cmds += configSetGetCmd([num:30, size:1], 1)
		cmds += configSetGetCmd([num:31, size:1], 1)
		cmds += configSetGetCmd([num:32, size:1], 1)
		cmds += configSetGetCmd([num:33, size:1], 1)
	}

	state.resyncAll = false

	if (cmds) sendCommands(cmds)
}

void executeProbeCmds() {
	logTrace "executeProbeCmds..."

	List<String> cmds = []

	//End Points Check
	if (state.endPoints == null) {
		logDebug "Probing for Multiple End Points"
		cmds << secureCmd(zwave.multiChannelV3.multiChannelEndPointGet())
		state.endPoints = 0
	}

	//Power Metering Check
	if (state.powerMetering == null) {
		logDebug "Probing for Power Metering Support"
		cmds << secureCmd(zwave.meterV3.meterSupportedGet())
		state.powerMetering = false
	}

	if (cmds) sendCommands(cmds)
}

void executeRefreshCmds() {
	List<String> cmds = []

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << mfgSpecificGetCmd()
		cmds << versionGetCmd()
	}

	//Refresh Switch
	cmds << switchBinaryGetCmd()

	//Refresh Meters
	if (state.powerMetering) {
		metersList.each { meter ->
			cmds << meterGetCmd(meter)
		}
	}

	//Refresh Children
	endPointList.each { endPoint ->
		cmds += getChildRefreshCmds(endPoint)
	}

	sendCommands(cmds,300)
}

List getConfigureAssocsCmds(Boolean logging=false) {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			if (logging) logDebug "Clearing incorrect lifeline association..."
			cmds << associationRemoveCmd(1,[])
			cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[], multiChannelNodeIds:[]))
		}
		if (logging) logDebug "Setting ${state.endPoints ? 'multi-channel' : 'standard'} lifeline association..."
		if (state.endPoints > 0) {
			cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationSet(groupingIdentifier: 1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress:0, endPointId: 0]]))
			cmds << mcAssociationGetCmd(1)
		}
		else {
			cmds << associationSetCmd(1, [zwaveHubNodeId])
			cmds << associationGetCmd(1)
		}
	}

	return cmds
}

List getOnOffCmds(val, Integer endPoint=0) {
	List<String> cmds = []

	state.isDigital = true
	cmds << switchBinarySetCmd(val ? 0xFF : 0x00, endPoint)

	if (state.endPoints > 0) {
		List epList = [endPoint]
		if (endPoint==0) epList = endPointList
		epList.each { ep ->
			state["isDigital$ep"] = true
		}
	}

	return cmds
}

List getChildRefreshCmds(Integer endPoint) {
	List<String> cmds = []
	cmds << switchBinaryGetCmd(endPoint)
	if (state.powerMetering && state.meterEPs?.getAt(endPoint)) {
		logDebug "Checking Meters on EndPoint ${endPoint}"
		//Meters energy and power only
		metersList.findAll{ it.scale <= 2 }.each { meter ->
			cmds << meterGetCmd(meter, endPoint)
		}
	}
	return cmds
}


/*******************************************************************
 ***** Required for Library
********************************************************************/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap['settings'] = [fixed: true]
}

Integer getParamValueAdj(Map param) {
	Integer paramVal = getParamValue(param)

	//Check if timer and find number
	def tNum = 0;
	if (param.name[0..7] == "offTimer") tNum = param.name[8]
	if (param.name[0..6] == "onTimer") tNum = param.name[7]

	if (tNum) {
		switch(param.name) {
			case "offTimer${tNum}Enabled":
				paramVal = !getParamValue("offTimer${tNum}") ? 0 : 1 ; break
			case "offTimer${tNum}":
				paramVal = paramVal ?: 60 ; break
			case "onTimer${tNum}Enabled":
				paramVal = !getParamValue("onTimer${tNum}") ? 0 : 1 ; break
			case "onTimer${tNum}":
				paramVal = paramVal ?: 60 ; break
		}
	}
	else {
		//Convert minutes to seconds
		switch(param.name) {
			case "powerFrequency":
			case "currentFrequency":
			case "energyFrequency":
			case "voltageFrequency":
				paramVal = paramVal*60
		}
	}

	return paramVal
}


/*******************************************************************
 ***** Child/Other Functions
********************************************************************/
void probeEndPoints() {
	if (epMeters[state.deviceModel]) {
		logDebug "Loaded meterEPs from preconfigured list"
		state.meterEPs = epMeters[state.deviceModel]
	}
	else {
		List<String> cmds = []
		endPointList.each { ep ->
			cmds << secureCmd(zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: ep))
		}
		sendCommands(cmds,300)
	}
}

/*** Child Creation Functions ***/
void createChildDevices() {
	endPointList.each { endPoint ->
		if (!getChildByEP(endPoint)) {
			logDebug "Creating new child device for endPoint ${endPoint}, did not find existing"
			addChild(endPoint)
		}
	}
}

void addChild(endPoint) {
	//Driver Settings
	Map deviceType = [namespace:"hubitat", typeName:"Generic Component Switch"]
	Map deviceTypeBak = [:]
	//Set metering driver if supported
	if (state.meterEPs[endPoint]) deviceType.typeName = "Generic Component Metering Switch"

	String devModel = state.deviceModel
	String epName = epNames[devModel] ? epNames[devModel][endPoint] : "Outlet ${endPoint}"
	String dni = getChildDNI(epNames[devModel] ? epName : endPoint)
	Map properties = [name:"${device.name} - ${epName}", isComponent:true, endPoint:"${endPoint}"]
	logDebug "Creating '${epName}' Child Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		logWarn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

/*** Child Common Functions ***/
private getChildByEP(endPoint) {
	String devModel = state.deviceModel
	//Searching using endPoint data value
	def childDev = childDevices?.find { safeToInt(it.getDataValue("endPoint")) == endPoint }
	if (childDev) logTrace "Found Child for endPoint ${endPoint} using data.endPoint: ${childDev.displayName} (${childDev.deviceNetworkId})"
	//If not found try deeper search using the child DNIs
	else {
		childDev = childDevices?.find  { ch ->
			List<String> dni = ch.deviceNetworkId.split('-')
			if (dni.size() <= 1) return false
			String dniEp = dni[1]

			//Search using defined EP List
			Integer ep = epNames[devModel]?.find{ dniEp.equalsIgnoreCase(it.value) }?.key ?: 0
			if (!ep) ep = safeToInt(dniEp) //Format DNI-<EP>

			//Return true if match found to save child device
			return (ep == endPoint)
		}
		if (childDev) {
			logDebug "Found Child for endPoint ${endPoint} parsing DNI: ${childDev.displayName} (${childDev.deviceNetworkId})"
			//Save the EP on the device so we can find it easily next time
			childDev.updateDataValue("endPoint","$endPoint")
		}
	}
	return childDev
}

private getChildEP(childDev) {
	Integer endPoint = safeToInt(childDev.getDataValue("endPoint"))
	if (!endPoint) logWarn "Cannot determine endPoint number for $childDev (defaulting to 0), run Configure to detect existing endPoints"
	return endPoint
}

String getChildDNI(epName) {
	return "${device.deviceId}-${epName}".toUpperCase()
}

List getEndPointList() {
	return (state.endPoints>0 ? 1..(state.endPoints) : [])
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

	logDebug "Received Version Report - Firmware: ${fullVersion}"
	setDevModel(new BigDecimal(fullVersion))
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
	return secureCmd(zwave.associationV2.associationSet(groupingIdentifier: group, nodeId: nodes))
}

String associationRemoveCmd(Integer group, List<Integer> nodes) {
	return secureCmd(zwave.associationV2.associationRemove(groupingIdentifier: group, nodeId: nodes))
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
	Map configsMap = getParamStoredMap()
	return safeToInt(configsMap[paramNum], null)
}

void setParamStoredValue(Integer paramNum, Integer value) {
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
		if (tmpMap.options) tmpMap.options = tmpMap.options?.clone()
		if (tmpMap.range) tmpMap.range = (tmpMap.range).toString()

		//Save the name
		tmpMap.name = name

		//Apply custom adjustments
		tmpMap.changes.each { m, changes ->
			if (m == devModel || m == modelNum || m ==~ /${modelSeries}X/) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		tmpMap.changesFR.each { m, changes ->
			if (firmware >= m.getFrom() && firmware <= m.getTo()) {
				tmpMap.putAll(changes)
				if (changes.options) { tmpMap.options = changes.options.clone() }
			}
		}
		//Don't need this anymore
		tmpMap.remove("changes")
		tmpMap.remove("changesFR")

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
	String info = "${DRIVER} v${VERSION}"
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
}

void updateLastCheckIn() {
	def nowDate = new Date()
	state.lastCheckInDate = convertToLocalTimeString(nowDate)

	Long lastExecuted = state.lastCheckInTime ?: 0
	Long allowedMil = 24 * 60 * 60 * 1000   //24 Hours
	if (lastExecuted + allowedMil <= nowDate.time) {
		state.lastCheckInTime = nowDate.time
		if (lastExecuted) runIn(4, doCheckIn)
		scheduleCheckIn()
	}
}

void scheduleCheckIn() {
	def cal = Calendar.getInstance()
	cal.add(Calendar.MINUTE, -1)
	Integer hour = cal[Calendar.HOUR_OF_DAY]
	Integer minute = cal[Calendar.MINUTE]
	schedule( "0 ${minute} ${hour} * * ?", doCheckIn)
}

void doCheckIn() {
	String devModel = (state.deviceModel ?: "NA") + (state.subModel ? ".${state.subModel}" : "")
	String checkUri = "http://jtp10181.gateway.scarf.sh/${DRIVER}/chk-${devModel}-v${VERSION}"

	try {
		httpGet(uri:checkUri, timeout:4) { logDebug "Driver ${DRIVER} ${devModel} v${VERSION}" }
		state.lastCheckInTime = (new Date()).time
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

BigDecimal safeToDec(val, defaultVal=0, roundTo=-1) {
	BigDecimal decVal = "${val}"?.isNumber() ? "${val}".toBigDecimal() : defaultVal
	if (roundTo == 0)		{ decVal = Math.round(decVal) }
	else if (roundTo > 0)	{ decVal = decVal.setScale(roundTo, BigDecimal.ROUND_HALF_UP).stripTrailingZeros() }
	if (decVal.scale()<0)	{ decVal = decVal.setScale(0) }
	return decVal
}

Boolean isDuplicateCommand(Long lastExecuted, Long allowedMil) {
	!lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}


/*******************************************************************
 ***** Logging Functions
********************************************************************/
//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Error", 1:"Warn", 2:"Info", 3:"Debug", 4:"Trace"]
@Field static final Map LOG_TIMES = [0:"Indefinitely", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]

/*//Command to set log level, OPTIONAL. Can be copied to driver or uncommented here
command "setLogLevel", [ [name:"Select Level*", description:"Log this type of message and above", type: "ENUM", constraints: LOG_LEVELS],
	[name:"Debug/Trace Time", description:"Timer for Debug/Trace logging", type: "ENUM", constraints: LOG_TIMES] ]
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
