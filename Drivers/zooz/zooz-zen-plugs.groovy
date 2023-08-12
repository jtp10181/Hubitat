/*
 *  Zooz ZEN Plugs Universal
 *    - Model: ZEN04, ZEN05, ZEN14 - All Firmware
 *    - Model: ZEN15 - MINIMUM FIRMWARE 1.06
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-smart-plugs/98333
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.2.1] - 2023-05-24 (@jtp10181)
  - Fix for possible RuntimeException error due to bad cron string

## [1.2.0] - 2023-05-21 (@jtp10181)
  - Refactor code to work with shared library
  - Added proper multi-channel detection
  - Added new settings for ZEN04 FW 1.30
  - Added notes where 0=Disabled everywhere applicable
  - Fixed lifeline setting for multichannel devices
  - Fingerprint adjustments to avoid false matches

## [1.1.2] - 2023-01-14 (@jtp10181)
  ### Added
  - Put useful info into the warnings event description
  - Added lower limits to catch negative reports

## [1.1.0] - 2023-01-08 (@jtp10181)
  ### Added
  - Support for ZEN15
  - energyDuration as an attribute so it can display on dashboard
  - Over limit checking with Warn log and warnings count attribute
  ### Changed
  - Probing device for power metering support instead of hard coded by model
  - Reworked the meter lists to implement the limits
  - Reworked switch event handling to catch BasicReport and SwitchBinaryReport

## [1.0.0] - 2022-12-12 (@jtp10181)
  ### Added
  - Support to track time for energy reporting and reset it on the device
  - Command to set any parameter (can be used in RM)
  - Optional High/Low tracking, must be turned on in Preferences (ZEN04 Only)
  - Accessory attribute to track connected device state (ZEN04 Only)
  ### Changed
  - Increased max precision rounding to 3 decimals

## [0.2.1] - 2022-08-11 (@jtp10181)
  ### Added
  - Flash capability / command

## [0.2.0] - 2021-08-01 (@jtp10181)
  ### Added
  - Support for creating and managing child endpoints
  - Support for ZEN14 Outdoor Double Plug
  ### Fixed
  - More robust model checking when switching from another driver
  - Added switch capability so it shows in rule setup drop downs
  - Various other minor bugs

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

@Field static final String VERSION = "1.2.1"
@Field static final String DRIVER = "Zooz-Plugs"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-smart-plugs/98333"
@Field static final Map deviceModelNames =
	["7000:B002":"ZEN04", "7000:B001":"ZEN05", "7000:B003":"ZEN14", "0101:000D":"ZEN15"]

metadata {
	definition (
		name: "Zooz ZEN Plugs Advanced",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zen-plugs.groovy"
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
		capability "Flash"

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

		fingerprint mfr:"027A", prod:"7000", deviceId:"B002", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x32,0x71,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN04 Plug
		fingerprint mfr:"027A", prod:"7000", deviceId:"B001", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A" //Zooz ZEN05 Outdoor Plug
		fingerprint mfr:"027A", prod:"7000", deviceId:"B003", inClusters:"0x5E,0x25,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x60,0x6C,0x7A,0x70" //Zooz ZEN14 Outdoor Double Plug
		fingerprint mfr:"027A", prod:"0101", deviceId:"000D", inClusters:"0x00,0x00" //Zooz ZEN15 Plug
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

		if (state.powerMetering) {
			input "accThreshold", "number",
				title: fmtTitle("Accessory State Threshold (Watts)"),
				description: fmtDesc("• Sets accessory status when power is above threshold.<br>• 0 = Disabled"),
				defaultValue: 0, range: 0..2000, required: false

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

//Main Parameters Listing
@Field static Map<String, Map> paramsMap =
[
	ledIndicator: [ num: 1,
		title: "LED Indicator",
		size: 1, defaultVal: 0,
		options: [0:"LED On When Switch On", 1:"LED On When Switch Off", 2:"LED Always Off", 3:"LED Always On"],
		changes: [15:[num:27, defaultVal:1, options:[0:"LED On When Device Plugged In", 1:"LED On When Device is On", 2:"LED On 5 Seconds at On/Off", 3:"LED Always Off"]]]
	],
	ledBrightness: [ num: null,
		title: "LED Brightness",
		size: 1, defaultVal: 2,
		options: [2:"Low", 1:"Medium", 0:"High"],
		changes: [04:[num:9], 05:[num:7], 14:[num:7]]
	],
	offTimer: [ num: 2,
		title: "Auto Turn-Off Timer",
		size: 4, defaultVal: 0,
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535,
		changes: [15:[num:34, size:1, range:0..99]]
	],
	offTimer2: [ num: null,
		title: "Auto Turn-Off Timer (Outlet 2)",
		size: 4, defaultVal: 0,
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535,
		changes: [14:[num:3]]
	],
	onTimer: [ num: 4,
		title: "Auto Turn-On Timer",
		size: 4, defaultVal: 0,
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535,
		changes: [04:[num:3], 15:[num:33, size:1, range:0..99]]
	],
	onTimer2: [ num: null,
		title: "Auto Turn-On Timer (Outlet 2)",
		size: 4, defaultVal: 0,
		description: "Time in minutes, 0 = Disabled",
		range: 0..65535,
		changes: [14:[num:5]]
	],
	powerFailure: [ num: 6,
		title: "Behavior After Power Failure",
		size: 1, defaultVal: 2,
		options: [2:"Restores Last Status", 0:"Forced to Off", 1:"Forced to On"],
		changes: [04:[num:4, defaultVal:0, options:[0:"Restores Last Status", 1:"Forced to Off", 2:"Forced to On"]],
			15:[num:21, defaultVal:0, options:[0:"Restores Last Status", 2:"Forced to Off", 1:"Forced to On"]]]
	],
	manualControl: [ num: null,
		title: "Physical Button On/Off Control",
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
		changes: [05:[num:8], 14:[num:8], 15:[num:30]]
	],
	//Undocumented ZEN15 Setting for Z-Wave On/Off
	zwaveOnControl: [ num: null,
		title: "Z-Wave ON Control Allowed",
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
		changes: [15:[num:31]]
	],
	//Undocumented ZEN15 Setting for Z-Wave On/Off
	zwaveOffControl: [ num: null,
		title: "Z-Wave OFF Control Allowed",
		size: 1, defaultVal: 1,
		options: [1:"Enabled", 0:"Disabled"],
		changes: [15:[num:32]]
	],
	powerEnabled: [ num: null,
		title: "Power (Watts) Reporting Enabled",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		changes: [04:[num:10, firmVer:1.30]]
	],
	powerThreshold: [ num: null,
		title: "Power (Watts) Reporting Threshold",
		size: 1, defaultVal: 10,
		description: "Report when changed by this amount",
		range: 5..50,
		changes: [04:[num:5],
			15:[num:151, size:2, defaultValue:50, range:0..32767, description: "Report when changed by this amount, 0 = Disabled"]
		]
	],
	powerPctThreshold: [ num: null,
		title: "Power (Watts) Reporting Percentage Threshold",
		size: 1, defaultVal: 10,
		description: "Report when changed by this percent from previous value, 0 = Disabled",
		range: 0..99,
		changes: [15:[num:152]]
	],
	powerFrequency: [ num: null,
		title: "Power (Watts) Reporting Frequency",
		size: 4, defaultVal: 10,
		description: "Minimum number of minutes between wattage reports",
		range: 1..65535,
		changes: [04:[num:6],
			15:[num:171, range:0..44640, description: "Minimum number of minutes between wattage reports, 0 = Disabled"]
		]
	],
	currentEnabled: [ num: null,
		title: "Current (Amps) Reporting Enabled",
		size: 1, defaultVal: 0,
		options: [0:"Enabled", 1:"Disabled"],
		changes: [04:[num:11, firmVer:1.30]]
	],
	currentThreshold: [ num: null,
		title: "Current (Amps) Reporting Threshold",
		size: 1, defaultVal: 10,
		description: "[1 = 0.1A, 10 = 1A]  Report when changed by this amount",
		range: 1..10,
		changes: [04:[num:7]]
	],
	currentFrequency: [ num: null,
		title: "Current (Amps) Reporting Frequency",
		size: 4, defaultVal: 60,
		description: "Minimum number of minutes between amperage reports",
		range: 1..65535,
		changes: [04:[num:12, firmVer:1.30],
			15:[num:174, range:0..44640, description: "Minimum number of minutes between amperage reports, 0 = Disabled"]
		]
	],
	energyThreshold: [ num: null,
		title: "Energy (kWh) Reporting Threshold",
		size: 1, defaultVal: 10,
		description: "[1 = 0.01kWh, 100 = 1kWh]  Report when changed by this amount",
		range: 1..100,
		changes: [04:[num:8]]
	],
	energyFrequency: [ num: null,
		title: "Energy (kWh) Reporting Frequency",
		size: 4, defaultVal: 60,
		description: "Minimum number of minutes between energy reports",
		range: 0..44640,
		changes: [15:[num:172]]
	],
	voltageFrequency: [ num: null,
		title: "Voltage (V) Reporting Frequency",
		size: 4, defaultVal: 60,
		description: "Minimum number of minutes between voltage reports, 0 = Disabled",
		range: 0..44640,
		changes: [04:[num:13, firmVer:1.30], 15:[num:173]]
	],
	overloadProtection: [ num: null,
		title: "Overload Protection *See Docs*",
		size: 1, defaultVal: 1,
		description: "Zooz DOES NOT recommend disabling this, as it may result in device damage and malfunction!",
		options: [1:"Enabled", 0:"Disabled"],
		changes: [15:[num:20]]
	],
	// Hidden Parameters to Set Defaults
	statusNotifications: [ num: null,
		title: "On/Off Status Change Notifications",
		size: 1, defaultVal: 1,
		options: [0:"Disabled", 1:"Enabled Always", 2:"Enabled Only for Physical"],
		hidden: true,
		changes: [15:[num:24]]
	]
]

/* ZEN04
CommandClassReport - class:0x25, version:2   (Binary Switch)
CommandClassReport - class:0x32, version:5   (Meter)
CommandClassReport - class:0x55, version:2   (Transport Service)
CommandClassReport - class:0x59, version:3   (Association Group Information (AGI))
CommandClassReport - class:0x5A, version:1   (Device Reset Locally)
CommandClassReport - class:0x5E, version:2   (Z-Wave Plus Info)
CommandClassReport - class:0x6C, version:1   (Supervision)
CommandClassReport - class:0x70, version:4   (Configuration)
CommandClassReport - class:0x71, version:8   (Notification)
CommandClassReport - class:0x72, version:2   (Manufacturer Specific)
CommandClassReport - class:0x73, version:1   (Powerlevel)
CommandClassReport - class:0x7A, version:5   (Firmware Update Meta Data)
CommandClassReport - class:0x85, version:3   (Association)
CommandClassReport - class:0x86, version:3   (Version)
CommandClassReport - class:0x87, version:3   (Indicator)
CommandClassReport - class:0x8E, version:4   (Multi Channel Association)
CommandClassReport - class:0x9F, version:1   (Security 2)
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x25: 1,	// Switch Binary
	0x32: 3,	// Meter
	0x60: 3,	// Multi Channel
	0x6C: 1,	// Supervision
	0x70: 2,	// Configuration
	0x71: 8,	// Notification
	0x72: 2,	// ManufacturerSpecific
	0x85: 2,	// Association
	0x86: 2,	// Version
	0x8E: 3,	// Multi Channel Association
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
	runIn(1, executeRefreshCmds)
	runIn(2, executeProbeCmds)
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

void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
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
	logDebug "${cmd}, scaleBinary: ${scales}"

	if (cmd.meterType == 1) {
		logDebug "Power Metering Support Detected and Enabled"
		state.powerMetering = true
		state.highLow = highLowEnable
	}
}

void zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	if (cmd.endPoints > 0) {
		logDebug "${cmd}"
		logDebug "Endpoints (${cmd.endPoints}) Detected and Enabled"
		state.endPoints = cmd.endPoints
		runIn(1,createChildDevices)
	}
}

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep=0) {
	logTrace "${cmd} (meterValue: ${cmd.scaledMeterValue}, previousMeter: ${cmd.scaledPreviousMeterValue}) (ep ${ep})"

	BigDecimal val = safeToDec(cmd.scaledMeterValue, 0, Math.min(cmd.precision,3))
	logDebug "MeterReport: scale:${cmd.scale}, scaledMeterValue:${cmd.scaledMeterValue} (${val}), precision:${cmd.precision}"

	Map meter = metersList.find{ it.scale == cmd.scale }
	if ((meter?.limitHi && val > meter.limitHi) || (meter?.limitLo && val < meter.limitLo)) {
		logWarn "IGNORED MeterReport ${meter.name} (precision: ${cmd.precision}, size: ${cmd.size}, meterValue: ${cmd.scaledMeterValue} ${cmd.meterValue}, deltaTime: ${cmd.deltaTime}, previousMeter: ${cmd.scaledPreviousMeterValue} ${cmd.previousMeterValue}) payLoad: ${cmd.getPayload()}"
		sendEventLog(name:"warnings", value:(device.currentValue("warnings")?:0)+1, isStateChange:true,
			desc:"IGNORED METER REPORT of ${val}${meter.unit} for ${meter.name} " + (val < meter?.limitLo ? "(below limit of ${meter.limitLo}" : "(over limit of ${meter.limitHi}") + "${meter.unit})", ep)
		return
	}

	switch (meter?.name) {
		case "energy":
			sendEnergyEvents(meter, val)
			break
		case "power":
			sendAccessoryEvents(val)
		case "voltage":
		case "amperage":
			sendMeterEvents(meter, val)
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
		String logEp = "(Outlet ${ep}) "

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

	cmds += getConfigureAssocsCmds()

	configParams.each { param ->
		Integer paramVal = getParamValueAdj(param)
		Integer storedVal = getParamStoredValue(param.num)

		if ((paramVal != null) && (state.resyncAll || (storedVal != paramVal))) {
			logDebug "Changing ${param.name} (#${param.num}) from ${storedVal} to ${paramVal}"
			cmds += configSetGetCmd(param, paramVal)
		}
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

List getConfigureAssocsCmds() {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (state.group1Assoc == false) {
			logDebug "Need to reset lifeline association..."
			cmds << associationRemoveCmd(1,[])
			cmds << secureCmd(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId:[], multiChannelNodeIds:[]))
		}
		logTrace "getConfigureAssocsCmds endPoints: ${state.endPoints}"
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

List getChildRefreshCmds(Integer endPoint) {
	List<String> cmds = []
	cmds << switchBinaryGetCmd(endPoint)
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


/*******************************************************************
 ***** Required for Library
********************************************************************/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap['settings'] = [fixed: true]
}

Integer getParamValueAdj(Map param) {
	Integer paramVal = getParamValue(param)

	//Convert minutes to seconds for ZEN15
	if (state.deviceModel == "ZEN15") {
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
/*** Child Creation Functions ***/
void createChildDevices() {
	endPointList.each { endPoint ->
		if (!getChildByEP(endPoint)) {
			addChild(endPoint)
		}
	}
}

void addChild(endPoint) {
	Map deviceType = [namespace:"hubitat", typeName:"Generic Component Switch"]
	Map deviceTypeBak = [:]
	String dni = getChildDNI(endPoint)
	Map properties = [name: "${device.name} - Outlet ${endPoint}", isComponent: false]
	def childDev

	logDebug "Creating 'Outlet ${endPoint}' Child Device"

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
	if (childDev) {
		childDev.updateDataValue("endPoint","$endPoint")
	}
}

/*** Child Common Functions ***/
private getChildByEP(endPoint) {
	def dni = getChildDNI(endPoint)
	return getChildByDNI(dni)
}

private getChildByDNI(dni) {
	return childDevices?.find { it.deviceNetworkId == dni }
}

private getChildEP(childDev) {
	Integer endPoint = safeToInt(childDev.getDataValue("endPoint"))
	if (!endPoint) {
		logDebug "Finding endPoint for $childDev"
		String[] dni = childDev.deviceNetworkId.split('-')
		endPoint = safeToInt(dni[1])
		if (endPoint) {
			childDev.updateDataValue("endPoint","$endPoint")
		} else {
			logWarn "Cannot determine endPoint number for $childDev, defaulting to 0"
		}
	}
	return endPoint
}

String getChildDNI(endPoint) {
	return "${device.deviceId}-${endPoint}"
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
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"

	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd, cmd.sourceEndPoint as Integer)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
	}
}

//Decodes Supervision Encapsulated Commands (and replies to device)
void zwaveSupervision(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd, ep=0) {
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
	setDevModel(new BigDecimal(fullVersion))
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

String batteryGetCmd() {
	return secureCmd(zwave.batteryV1.batteryGet())
}

String notificationGetCmd(notificationType, eventType) {
	return secureCmd(zwave.notificationV3.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType))
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
	String devModel = state.deviceModel ?: "NA"
	String checkUri = "http://jtp10181.gateway.scarf.sh/${DRIVER}/chk-${devModel}-${VERSION}"

	try {
		httpGet(uri:checkUri, timeout:4) { logDebug "Driver ${DRIVER} v${VERSION}" }
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
	def val = settings."assocDNI$grp"
	return ((val && (val.trim() != "0")) ? val : "")
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
	//setDevModel()
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

boolean isDuplicateCommand(Long lastExecuted, Long allowedMil) {
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
	if (settings.logLevel == null) device.updateSetting("logLevel",[value:"3", type:"enum"])
	if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"30", type:"enum"])
	//Schedule turn off and log as needed
	if (levelInfo.level == null) levelInfo = getLogLevelInfo()
	String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]} (${levelInfo.level})"
	if (levelInfo.level >= 3 && levelInfo.time > 0) {
		logMsg += " for ${LOG_TIMES[levelInfo.time]}"
		runIn(60*levelInfo.time, logsOff)
	}
	logInfo(logMsg)
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
	Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
	Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
	device.updateSetting("logLevel",[value:"${level}", type:"enum"])
	checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
	Integer level = settings.logLevel as Integer
	Integer time = settings.logLevelTime as Integer
	return [level: level, time: time]
}

//Legacy Support
void debugLogsOff() {
	logWarn "Debug logging toggle disabled..."
	device.removeSetting("logEnable")
	device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

//Current Support
void logsOff() {
	logWarn "Debug and Trace logging disabled..."
	if (logLevelInfo.level >= 3) {
		device.updateSetting("logLevel",[value:"2", type:"enum"])
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
