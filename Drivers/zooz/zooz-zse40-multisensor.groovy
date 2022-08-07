/*  
 *  Zooz ZSE40 4-in-1 Multisensor
 *    - Model: ZSE40 - MINIMUM FIRMWARE 32.02
 *
 *  For Support, Information and Updates:
 *  https://community.hubitat.com/t/zooz-sensors/81074
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.0.5] - 2022-08-06 (@jtp10181)
  ### Fixed
  - Forgot to change param 8 setting from -1 to 255 when I added the signed/unsigned conversion
  - Put in proper scalable signed/unsigned parameter value conversion

## [1.0.4] - 2022-08-02 (@jtp10181)
  ### Fixed
  - Race condition with configVals (now keeping copy in static var)
  - Various fixes in common functions merged from other drivers
  - The deviceModel checking should be even better now, with page refresh
  - Handling of ZSE40-700 with 1.10 firmware fixed
  ### Removed
  - Supervision encapsulation code, not being used
  
## [1.0.2] - 2022-07-25 (@jtp10181)
  ### Fixed
  - Fixed issue handling decimal parameters introduced in 1.0.1
  
## [1.0.1] - 2022-07-25 (@jtp10181)
  ### Added
  - Set deviceModel in device data (press refresh)
  ### Changed
  - Description text loging enabled by default
  - Removed getParam.value and replaced with separate function
  - Adding HTML styling to the Preferences
  - Cleaned up some logging functions
  - Other minor function updates synced from other drivers
  ### Fixed
  - Motion Clear Delay upper limit changed back to 255 and properly fixed
  
## [1.0.0] - 2022-04-25 (@jtp10181)
  ### Added
  - More robust checking for missing firmware/model data to help new users
  - INFO state message about anything pending that needs the device to wake up, more visible than logging
  ### Changed
  - Renamed Configure and Refresh commands since they do not work instantly like a mains device
  - Downgraded some command class versions to hopefully better support older devices
  - Removed some unused code carried over from copying another driver
  ### Fixed
  - Added inClusters to fingerprint so it will be selected by default
  - Global (Field static) Maps defined explicitly as a ConcurrentHashMap
  - Corrected upper limit of Motion Clear delay (thanks @conrad4 for finding it)
  
## [0.3.0] - 2022-01-23 (@jtp10181)
  ### Added
  - Basic WakeUpInterval support (configure will force it to 12 hours)
  ### Fixed
  - Removed Initialize function, was causing issues with pairing
  
## [0.2.0] - 2022-01-17 (@jtp10181)
  ### Added
  - Temperature, Humidity, and Light offsets
  - Refresh command to force full refresh next wakeup
  - Log messages with instructions when you need to wake up the device
  - Properly sending wakeUpNoMoreInfoCmd to save battery
  ### Fixed
  - Added min firmware to parameter 8 setting
  - Wakeup only gets battery level by default
  - parse() logTrace would fail if command could not be parsed
  
## [0.1.0] - 2021-09-29 (@jtp10181)
  ### Added
  - Initial Release, supports all known settings and features except associations

NOTICE: This file has been created by *Jeff Page* with some code used 
	from the original work of *Zooz* and *Kevin LaFramboise* under compliance with the Apache 2.0 License.

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

@Field static final String VERSION = "1.0.5" 
@Field static final Map deviceModelNames = ["2021:2101":"ZSE40"]

metadata {
	definition (
		name: "Zooz ZSE40 4-in-1 Multisensor",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zse40-multisensor.groovy"
	) {
		capability "Sensor"
		capability "Motion Sensor"
		capability "Illuminance Measurement"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Battery"
		capability "Tamper Alert"

		command "fullConfigure"
		command "forceRefresh"

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"2021", deviceId:"2101", inClusters:"0x5E,0x22,0x98,0x55", deviceJoinName:"Zooz ZSE40 4-in-1 Multisensor"
		fingerprint mfr:"027A", prod:"2021", deviceId:"2101", inClusters:"0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x80,0x71,0x31,0x70,0x84,0x7A", deviceJoinName:"Zooz ZSE40 4-in-1 Multisensor"
		fingerprint mfr:"027A", prod:"2021", deviceId:"2101", inClusters:"0x5E,0x22,0x98,0x9F,0x6C,0x55", deviceJoinName:"Zooz ZSE40-700 4-in-1 Multisensor"
	}

	preferences {

		configParams.each { param ->
			if (!param.hidden) {
				BigDecimal paramVal = getParamValue(param)
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

		input "tempOffset", "decimal",
			title: fmtTitle("Temperature Offset [-25.0 to 25.0]:"),
			defaultValue: 0, range: "-25..25", required: false

		input "humidityOffset", "decimal",
			title: fmtTitle("Humidity Offset [-25.0 to 25.0]:"),
			defaultValue: 0, range: "-25..25", required: false

		input "lightOffset", "decimal",
			title: fmtTitle("Light % Offset [-25.0 to 25.0]:"),
			defaultValue: 0, range: "-25..25", required: false

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
	tempTrigger: [ num:2, 
		title: "Temperature Change Report Trigger (1 = 0.1° / 10 = 1°)", 
		size: 1, defaultVal: 10, 
		range: "1..50"
	],
	humidityTrigger: [ num:3, 
		title: "Humidity Change Report Trigger (%)", 
		size: 1, defaultVal: 10, 
		range: "1..50"
	],
	lightTrigger: [ num:4, 
		title: "Light Change Report Trigger (%)", 
		size: 1, defaultVal: 10, 
		range: "5..50"
	],
	motionClear: [ num:5, 
		title: "Motion Clear Delay / Timeout (seconds)", 
		size: 1, defaultVal: 15, 
		range: "15..255"
	],
	motionSensitivity: [ num:6, 
		title: "Motion Sensitivity", 
		size: 1, defaultVal: 3, 
		options: [1:"1 - Most Sensitive", 2:"2", 3:"3", 4:"4", 5:"5", 6:"6", 7:"7 - Least Sensitive"]
	],
	ledMode: [ num:7, 
		title: "LED Indicator Mode", 
		size: 1, defaultVal: 4, 
		options: [1:"LED Disabled", 2:"Motion Flash / Temp Pulse", 3:"Motion Flash / Temp Flash (every 3 mins)", 4:"Motion Flash / Temp None"],
		changes: ['ZSE40-700':[defaultVal: 3, options: [1:"LED Disabled", 2:"Motion Flash / Temp Flash (every 3 mins)", 3:"Motion Flash / Temp None"]]],
	],
	group1Report: [ num:8, 
		title: "Group 1 (Hub) Reporting", 
		size: 1, defaultVal: 255, 
		options: [0:"Notification Reports Only", 255:"Notification AND Basic Reports"],
		changes: ['ZSE40':[firmVer: 32.00]]
	],
	tempUnits: [ num:1,
		title: "Temperature Units:",
		size: 1, defaultVal: 1,
		options: [0:"Celsius (°C)", 1:"Fahrenheit (°F)"]
	],
]

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x31: 5,	// Sensor Multilevel (sensormultilevelv5)
	0x6C: 1,	// Supervision (supervisionv1)
	0x70: 2,	// Configuration (configurationv2)
	0x71: 3,	// Notification (notificationv3) (8)
	0x72: 2,	// Manufacturer Specific (manufacturerspecificv2)
	0x80: 1,	// Battery (batteryv1)
	0x84: 2,	// Wakeup (wakeupv2)
	0x85: 2,	// Association (associationv2) (3)
	0x86: 2,	// Version (versionv2) (3)
	0x98: 1,	// Security (securityv1)
]

/*** Static Lists and Settings ***/
//Sensor Types
@Field static Short SENSOR_TYPE_TEMPERATURE = 0x01
@Field static Short SENSOR_TYPE_LUMINANCE = 0x03
@Field static Short SENSOR_TYPE_HUMIDITY = 0x05
//Notification Types
@Field static Short NOTIFICATION_TYPE_SECURITY = 0x07
//Notification Events
@Field static Short EVENT_PARAM_IDLE = 0x00
@Field static Short EVENT_PARAM_TAMPER = 0x03
@Field static Short EVENT_PARAM_MOTION = 0x08

/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
}

void fullConfigure() {
	logWarn "configure..."
	if (debugEnable) runIn(1800, debugLogsOff)

	if (!pendingChanges || state.resyncAll == null) {
		logForceWakeupMessage "Full Re-Configure"
		state.resyncAll = true
	} else {
		logForceWakeupMessage "Pending Configuration Changes"
	}

	updateSyncingStatus(1)
}

void updated() {
	logDebug "updated..."
	logDebug "Debug logging is: ${debugEnable == true}"
	logDebug "Description logging is: ${txtEnable == true}"

	if (debugEnable) runIn(1800, debugLogsOff)

	if (!firmwareVersion || !state.deviceModel) {
		state.resyncAll = true
		state.pendingRefresh = true
		logForceWakeupMessage "Full Re-Configure and Refresh"
	}

	if (pendingChanges) {
		logForceWakeupMessage "Pending Configuration Changes"
	}
	else if (!state.resyncAll && !state.pendingRefresh) {
		state.remove("INFO")
	}

	updateSyncingStatus(1)
}

void forceRefresh() {
	logDebug "refresh..."
	state.pendingRefresh = true
	logForceWakeupMessage "Sensor Info Refresh"
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

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCmd = cmd.encapsulatedCommand(commandClassVersions)
	logTrace "${cmd} --ENCAP-- ${encapsulatedCmd}"
	
	if (encapsulatedCmd) {
		zwaveEvent(encapsulatedCmd)
	} else {
		logWarn "Unable to extract encapsulated cmd from $cmd"
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
	Number val = cmd.scaledConfigurationValue

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

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"

	Integer batLvl = cmd.batteryLevel
	if (batLvl == 0xFF) {
		batLvl = 1
		logWarn "LOW BATTERY WARNING"
	}

	batLvl = validateRange(batLvl, 100, 1, 100)
	String descText = "battery level is ${batLvl}%"
	sendEventLog(name:"battery", value:batLvl, unit:"%", desc:descText, isStateChange:true)
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

	List<String> cmds = []
	cmds << batteryGetCmd()

	//Refresh all if requested
	if (state.pendingRefresh) { cmds += getRefreshCmds() }
	//Any configuration needed
	cmds += getConfigureCmds()

	//This needs a longer delay
	cmds << "delay 1000" << wakeUpNoMoreInfoCmd()

	//Clear pending status
	state.resyncAll = false
	state.pendingRefresh = false
	state.remove("INFO")
	
	sendCommands(cmds, 400)
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendEventLog(name:"motion", value:(cmd.value ? "active":"inactive"))
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"	
	switch (cmd.sensorType) {
		case SENSOR_TYPE_TEMPERATURE: //0x01
			def temp = convertTemperatureIfNeeded(cmd.scaledSensorValue, (cmd.scale ? "F" : "C"), cmd.precision)
			def offset = safeToDec(settings?.tempOffset,0)
			def tempOS = safeToDec(temp,0) + offset
			logDebug "Temperature Offset by ${offset} from ${temp} to ${tempOS}"
			sendEventLog(name:"temperature", value:(safeToDec(tempOS,0,Math.min(cmd.precision,1))), unit:"°${temperatureScale}")
			break
		case SENSOR_TYPE_LUMINANCE: //0x03
			def offset = safeToDec(settings?.lightOffset,0)
			def lightOS = safeToDec(cmd.scaledSensorValue,0) + offset
			logDebug "Light % Offset by ${offset} from ${cmd.scaledSensorValue} to ${lightOS}"
			sendEventLog(name:"illuminance", value:(Math.round(lightOS)), unit:"%")
			break
		case SENSOR_TYPE_HUMIDITY:  //0x05
			def offset = safeToDec(settings?.humidityOffset,0)
			def humidOS = safeToDec(cmd.scaledSensorValue,0) + offset
			logDebug "Humidity Offset by ${offset} from ${cmd.scaledSensorValue} to ${humidOS}"
			sendEventLog(name:"humidity", value:(safeToDec(humidOS,0,Math.min(cmd.precision,1))), unit:"%")
			break
		default:
			logDebug "Unhandled SensorMultilevelReport sensorType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	switch (cmd.notificationType) {
		case NOTIFICATION_TYPE_SECURITY:
			sendSecurityEvent(cmd.event, cmd.eventParameter[0])
			break
		default:
			logDebug "Unhandled NotificationReport notificationType: ${cmd}"
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

String sensorBinaryGetCmd() {
	return secureCmd(zwave.sensorBinaryV1.sensorBinaryGet())
}

String sensorMultilevelGetCmd(sensorType) {
	return secureCmd(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 0x00, sensorType: sensorType))
}

String notificationGetCmd(notificationType, eventType) {
	return secureCmd(zwave.notificationV3.notificationGet(notificationType: notificationType, v1AlarmType:0, event: eventType))
}

String configSetCmd(Map param, Number value) {
	//Convert from unsigned to signed for scaledConfigurationValue
	Long sizeFactor = Math.pow(256,param.size).round()
	if (value >= sizeFactor/2) { value -= sizeFactor }

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

	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << versionGetCmd()
		cmds << wakeUpIntervalSetCmd(43200)
		cmds << wakeUpIntervalGetCmd()
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

	if (cmds) updateSyncingStatus(6)

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []
	cmds << versionGetCmd()
	cmds << wakeUpIntervalGetCmd()
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_TEMPERATURE)
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_LUMINANCE)
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_HUMIDITY)

	//These don't work
	//cmds << notificationGetCmd(NOTIFICATION_TYPE_SECURITY, EVENT_PARAM_TAMPER)
	//cmds << notificationGetCmd(NOTIFICATION_TYPE_SECURITY, EVENT_PARAM_MOTION)

	return cmds ?: []
}

void clearVariables() {
	logWarn "Clearing state variables and data..."

	//Backup
	String devModel = state.deviceModel 

	//Clears State Variables
	state.clear()

	//Clear Data from other Drivers
	device.removeDataValue("configVals")
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
		if (state.group1Assoc == false) {
			logDebug "Adding missing lifeline association..."
		}
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
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


/*******************************************************************
 ***** Event Senders
********************************************************************/
//evt = [name, value, type, unit, desc, isStateChange]
void sendEventLog(Map evt, Integer ep=0) {
	//Set description if not passed in
	evt.descriptionText = evt.desc ?: "${evt.name} set to ${evt.value}${evt.unit ?: ''}"

	//Main Device Events
	if (evt.name != "syncStatus") {
		if (device.currentValue(evt.name).toString() != evt.value.toString()) {
			logInfo "${evt.descriptionText}"
		} else {
			logDebug "${evt.descriptionText} [NOT CHANGED]"
		}
	}
	//Always send event to update last activity
	sendEvent(evt)
}

void sendSecurityEvent(event, parameter) {
	Boolean cleared
	//Idle Event the parameter is the event to clear
	if (event == EVENT_PARAM_IDLE) {
		event = parameter
		cleared = true
	}
	
	switch (event) {
		case EVENT_PARAM_TAMPER:
			sendEventLog(name:"tamper", value:(cleared ? "clear":"detected"))
			break
		case EVENT_PARAM_MOTION:
			sendEventLog(name:"motion", value:(cleared ? "inactive":"active"))
			break
		default:
			logDebug "Unhandled event: ${event}, ${parameter}"
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
BigDecimal getParamValue(String paramName) {
	return getParamValue(getParam(paramName))
}
BigDecimal getParamValue(Map param, Boolean adjust=false) {
	if (param == null) return
	BigDecimal paramVal = safeToDec(settings."configParam${param.num}", param.defaultVal)
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
	sendEventLog(name:"syncStatus", value:"Syncing...")
}

void refreshSyncStatus() {
	Integer changes = pendingChanges
	sendEventLog(name:"syncStatus", value:(changes ? "${changes} Pending Changes" : "Synced"))
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

	//Extra check for ZSE40 (700 Series)
	if (devModel == "ZSE40" && getDataValue("inClusters").contains("0x9F")) {
		devModel = "ZSE40-700"
	}

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
private logForceWakeupMessage(msg) {
	String helpText = "You can force a wake up by using a paper clip to push the Z-Wave button on the device."
	logWarn "${msg} will execute the next time the device wakes up.  ${helpText}"
	state.INFO = "*** ${msg} *** Waiting for device to wake up.  ${helpText}"
}

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
