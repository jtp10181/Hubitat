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

@Field static final String VERSION = "1.1.0"
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

		command "refreshParams"
		command "resetStats"

		command "setParameter",[[name:"parameterNumber*",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],
			[name:"value*",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]],
			[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]]]

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

		fingerprint mfr:"027A", prod:"7000", deviceId:"B002", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN04 Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B001", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN05 Outdoor Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B003", inClusters:"0x5E,0x55,0x9F,0x6C", deviceJoinName:"Zooz ZEN14 Outdoor Double Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B002", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x32,0x71,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A", deviceJoinName:"Zooz ZEN04 Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B001", inClusters:"0x5E,0x25,0x70,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A", deviceJoinName:"Zooz ZEN05 Outdoor Plug"
		fingerprint mfr:"027A", prod:"7000", deviceId:"B003", inClusters:"0x5E,0x25,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x98,0x9F,0x60,0x6C,0x7A,0x70", deviceJoinName:"Zooz ZEN14 Outdoor Double Plug"
		fingerprint mfr:"027A", prod:"0101", deviceId:"000D", inClusters:"0x5E,0x25,0x32,0x27,0x2C,0x2B,0x70,0x85,0x59,0x72,0x86,0x7A,0x73,0x5A", deviceJoinName: "Zooz ZEN15 Plug"
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
@Field static final int maxAssocGroups = 1
@Field static final int maxAssocNodes = 1

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
	powerThreshold: [ num: null,
		title: "Power (Watts) Reporting Threshold",
		size: 1, defaultVal: 10,
		description: "Report when changed by this amount",
		range: 5..50,
		changes: [04:[num:5], 15:[num:151, size:2, defaultValue:50, range:0..32767]]
	],
	powerPctThreshold: [ num: null,
		title: "Power (Watts) Reporting Percentage Threshold",
		size: 1, defaultVal: 10,
		description: "Report when changed by this percent from previous value",
		range: 0..99,
		changes: [15:[num:152]]
	],
	powerFrequency: [ num: null,
		title: "Power (Watts) Reporting Frequency",
		size: 4, defaultVal: 10,
		description: "Minimum number of minutes between wattage reports",
		range: 1..65535,
		changes: [04:[num:6], 15:[num:171, range:0..44640]]
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
		range: 0..44640,
		changes: [15:[num:174]]
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
		description: "Minimum number of minutes between voltage reports",
		range: 0..44640,
		changes: [15:[num:173]]
	],
	overloadProtection: [ num: null,
		title: "Overload Protection *See Docs*",
		size: 1, defaultVal: 1,
		description: "Zooz DOES NOT recommend disabling this, as it may result in device damage and malfunction!",
		options: [1:"Enabled", 0:"Disabled"],
		changes: [15:[num:20]]
	],
	statusNotifications: [ num: null,
		title: "On/Off Status Change Notifications",
		size: 1, defaultVal: 1,
		options: [0:"Disabled", 1:"Enabled Always", 2:"Enabled Only for Physical"],
		hidden: true,
		changes: [15:[num:24]]
	]
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

/*** Static Lists and Settings ***/
@Field static List metersList = [
	[name:"energy", scale:0, unit:"kWh", limit:null],
	[name:"power", scale:2, unit:"W", limit:2400],
	[name:"voltage", scale:4, unit:"V", limit:140],
	[name:"amperage", scale:5, unit:"A", limit:18]
]
@Field static final Map multiChan = [ZEN14:[endpoints:1..2]]

/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
	state.energyTime = new Date().time
	createChildDevices()
	initialize()
}

void initialize() {
	logWarn "initialize..."
	refresh()
}

void configure() {
	logWarn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	createChildDevices()

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

	if (!accThreshold) { 
		device.deleteCurrentState("accessory") 
	}
	if (highLowEnable != state.highLow) {
		state.highLow = highLowEnable
		resetStats(false)
	}

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

//Flashing Capability
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
	
	runIn(5, refresh)
		
	device.deleteCurrentState("amperageHigh")
	device.deleteCurrentState("amperageLow")
	device.deleteCurrentState("powerHigh")
	device.deleteCurrentState("powerLow")
	device.deleteCurrentState("voltageHigh")
	device.deleteCurrentState("voltageLow")

	if (fullReset) {
		state.energyTime = new Date().time
		state.remove("energyDuration")
		sendEventLog(name:"energyDuration", value:0)
		sendEventLog(name:"warnings", value:0)
		sendCommands(meterResetCmd(0))
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

void zwaveEvent(hubitat.zwave.commands.meterv3.MeterReport cmd, ep=0) {
	logTrace "${cmd} (scaledMeterValue: ${cmd.scaledMeterValue}) (ep ${ep})"

	BigDecimal val = safeToDec(cmd.scaledMeterValue, 0, Math.min(cmd.precision,3))
	logDebug "MeterReport: scale:${cmd.scale}, scaledMeterValue:${cmd.scaledMeterValue} (${val}), precision:${cmd.precision}"

	Map meter = metersList.find{ it.scale == cmd.scale }
	if (meter?.limit && val > meter.limit) {
		logWarn "IGNORING OVER LIMIT METER REPORT: ${val}${meter.unit} reported, ${meter.name} limit is ${meter.limit}${meter.unit}"
		logWarn "${cmd} (scaledMeterValue: ${cmd.scaledMeterValue}) (ep ${ep}) ${cmd.getPayload()}"
		sendEvent(name:"warnings", value:(device.currentValue("warnings")?:0)+1)
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

String meterGetCmd(meter, Integer ep=0) {
	return secureCmd(zwave.meterV3.meterGet(scale: meter.scale), ep)
}

String meterResetCmd(Integer ep=0) {
	return secureCmd(zwave.meterV3.meterReset(), ep)
}

String configSetCmd(Map param, Integer value) {
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

	if (state.powerMetering == null) {
		logDebug "Probing for Power Metering Support"
		cmds.add(0, secureCmd(zwave.meterV3.meterSupportedGet()))
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

	//Refresh Childs
	multiChan[state.deviceModel]?.endpoints.each { endPoint ->
		cmds += getChildRefreshCmds(endPoint)
	}

	sendCommands(cmds,300)
}

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
	device.removeDataValue("protocolVersion")
	device.removeDataValue("hardwareVersion")
	device.removeDataValue("zwaveAssociationG1")
	device.removeDataValue("zwaveAssociationG2")
	device.removeDataValue("zwaveAssociationG3")

	//Restore
	if (devModel) state.deviceModel = devModel
	if (engTime) state.energyTime = engTime
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

List getChildRefreshCmds(Integer endPoint) {
	List<String> cmds = []
	cmds << switchBinaryGetCmd(endPoint)
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
	state.isDigital = true
	return switchBinarySetCmd(val ? 0xFF : 0x00, endPoint)
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

	if (ep == 0 && !type) {
		if (state.isDigital == true || state.isDigital == value) {
			logTrace "sendSwitchEvents: isDigital = ${state.isDigital}"
			type = "digital"
			state.isDigital = value
			runInMillis(500, switchDigitalRemove)
		}
		else { type = "physical" }
	}
	if (type == "physical") flashStop()

	String desc = "switch is turned ${value}" + (type ? " (${type})" : "")
	sendEventLog(name:"switch", value:value, type:type, desc:desc, ep)
}
void switchDigitalRemove() {
	logTrace "switchDigitalRemove: Removing isDigital ${state.isDigital}"
	state.remove("isDigital")
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

void sendAccessoryEvents(powerVal, Integer ep=0) {
	if (accThreshold) {
		String value = (powerVal > accThreshold) ? "on" : "off"
		String desc = "accessory is turned ${value}"
		sendEventLog(name:"accessory", value:value, desc:desc, ep)
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


/*** Child Helper Functions ***/
void createChildDevices() {
	multiChan[state.deviceModel]?.endpoints.each { endPoint ->
		if (!getChildByEP(endPoint)) {
			addChildOutlet(endPoint)
		}
	}
}

void addChildOutlet(endPoint) {
	Map deviceType = [namespace:"hubitat", typeName:"Generic Component Switch"]
	Map deviceTypeBak = [:]
	String dni = getChildDNI(endPoint)
	Map properties = [name: "${device.name} (Outlet ${endPoint})", isComponent: false]
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
	if (childDev) childDev.updateDataValue("endPoint","$endPoint")
}

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
