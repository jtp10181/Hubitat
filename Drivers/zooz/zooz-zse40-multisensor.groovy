/*  
 *  Zooz ZSE40 4-in-1 Multisensor
 *    - Model: ZSE40 - MINIMUM FIRMWARE 32.02
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/zooz-sensors/81074
 *  https://github.com/jtp10181/Hubitat/tree/main/Drivers/zooz
 *

Changelog:

## [1.2.2] - 2024-06-28 (@jtp10181)
  - Added ZSE70 Outdoor Sensor to package
  - Fixed install sequence so device will fully configure at initial pairing
  - Update library and common code

## [1.2.0] - 2024-03-30 (@jtp10181)
  - Added ZSE43, ZSE18, and ZSE11 to HPM package
  - Updated Library code
  - Added Wake-up Interval Setting
  - Added singleThreaded flag
  - Changed some command class versions
  - Fixed decimal bug with hardware offsets on ZSE44

## [1.1.0] - 2023-11-08 (@jtp10181)
  - Rearranged functions to get ready for library code
  - Merged new code base and library
  - Added ZSE41 and ZSE42 to package

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
  - Refresh command to force full refresh next wake-up
  - Log messages with instructions when you need to wake up the device
  - Properly sending wakeUpNoMoreInfoCmd to save battery
  ### Fixed
  - Added min firmware to parameter 8 setting
  - Wake-up only gets battery level by default
  - parse() logTrace would fail if command could not be parsed
  
## [0.1.0] - 2021-09-29 (@jtp10181)
  ### Added
  - Initial Release, supports all known settings and features except associations

NOTICE: This file has been created by *Jeff Page* with some code used 
	from the original work of *Zooz* and *Kevin LaFramboise* under compliance with the Apache 2.0 License.

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

@Field static final String VERSION = "1.2.2"
@Field static final String DRIVER = "Zooz-Sensors"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/zooz-sensors/81074"
@Field static final Map deviceModelNames = ["2021:2101":"ZSE40"]

metadata {
	definition (
		name: "Zooz ZSE40 4-in-1 Multisensor",
		namespace: "jtp10181",
		author: "Jeff Page (@jtp10181)",
		singleThreaded: true,
		importUrl: "https://raw.githubusercontent.com/jtp10181/Hubitat/main/Drivers/zooz/zooz-zse40-multisensor.groovy"
	) {
		capability "Sensor"
		capability "MotionSensor"
		capability "IlluminanceMeasurement"
		capability "RelativeHumidityMeasurement"
		capability "TemperatureMeasurement"
		capability "Battery"
		capability "TamperAlert"

		command "fullConfigure"
		command "forceRefresh"

		//DEBUGGING
		//command "debugShowVars"

		attribute "syncStatus", "string"

		fingerprint mfr:"027A", prod:"2021", deviceId:"2101", inClusters:"0x5E,0x86,0x72,0x5A,0x85,0x59,0x73,0x80,0x71,0x31,0x70,0x84,0x7A,0x98", controllerType: "ZWV" //Zooz ZSE40 4-in-1 Multisensor
	}

	preferences {
		configParams.each { param ->
			if (!param.hidden) {
				if (param.options) {
					BigDecimal paramVal = getParamValue(param)
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

		input "tempOffset", "decimal",
			title: fmtTitle("Temperature Offset"),
			description: fmtDesc("Range: -25.0..25.0, DEFAULT: 0"),
			defaultValue: 0, range: "-25..25", required: false

		input "humidityOffset", "decimal",
			title: fmtTitle("Humidity Offset"),
			description: fmtDesc("Range: -25.0..25.0, DEFAULT: 0"),
			defaultValue: 0, range: "-25..25", required: false

		input "lightOffset", "decimal",
			title: fmtTitle("Light % Offset"),
			description: fmtDesc("Range: -25.0..25.0, DEFAULT: 0"),
			defaultValue: 0, range: "-25..25", required: false

		input "wakeUpInt", "number", defaultValue: 12, range: "1..24",
			title: fmtTitle("Wake-up Interval (hours)"),
			description: fmtDesc("How often the device will wake up to receive commands from the hub")
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
		size: 1, defaultVal: 3, 
		options: [1:"LED Disabled", 2:"Motion Flash / Temp Flash (every 3 mins)", 3:"Motion Flash / Temp None"],
		changesFR: [(16..32.30):[defaultVal:4, options:[1:"LED Disabled", 2:"Motion Flash / Temp Pulse", 3:"Motion Flash / Temp Flash (every 3 mins)", 4:"Motion Flash / Temp None"]]],
	],
	group1Report: [ num:8, 
		title: "Group 1 (Hub) Reporting", 
		size: 1, defaultVal: 255, 
		options: [0:"Notification Reports Only", 255:"Notification AND Basic Reports"],
		changesFR: [(16..30):[num:null]],
	],
	tempUnits: [ num:1,
		title: "Temperature Units:",
		size: 1, defaultVal: 1,
		options: [0:"Celsius (°C)", 1:"Fahrenheit (°F)"]
	],
]

/* ZSE40
CommandClassReport
*/

//Set Command Class Versions
@Field static final Map commandClassVersions = [
	0x31: 5,	// Sensor Multilevel (sensormultilevelv5)
	0x70: 1,	// Configuration (configurationv1)
	0x71: 3,	// Notification (notificationv3) (8)
	0x80: 1,	// Battery (batteryv1)
	0x84: 2,	// Wakeup (wakeupv2)
	0x85: 2,	// Association (associationv2)
	0x86: 2,	// Version (versionv2) (3)
]


/*******************************************************************
 ***** Core Functions
********************************************************************/
void installed() {
	logWarn "installed..."
	state.resyncAll = true
	runIn(2, runWakeupCmds)
	sendCommands(getRefreshCmds(),400)
}

void fullConfigure() {
	logWarn "fullConfigure..."

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
	checkLogLevel()

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

	setSubModel()

	updateSyncingStatus(1)
}

void forceRefresh() {
	logDebug "forceRefresh..."
	state.pendingRefresh = true
	logForceWakeupMessage "Sensor Info Refresh"
}


/*******************************************************************
 ***** Driver Commands
********************************************************************/
/*** Capabilities ***/

/*** Custom Commands ***/

/*******************************************************************
 ***** Z-Wave Reports
********************************************************************/
void parse(String description) {
	zwaveParse(description)
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
	runWakeupCmds()
}

void runWakeupCmds() {
	List<String> cmds = ["delay 0"]
	cmds << batteryGetCmd()

	//Refresh all if requested
	if (state.pendingRefresh) { cmds += getRefreshCmds() }
	//Any configuration needed
	cmds += getConfigureCmds()

	//This needs a longer delay
	cmds << "delay 1400" << wakeUpNoMoreInfoCmd()

	//Clear pending status
	state.resyncAll = false
	state.pendingRefresh = false
	state.remove("INFO")
	setSubModel()
	
	sendCommands(cmds,300)
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	sendEventLog(name:"motion", value:(cmd.value ? "active":"inactive"))
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"	
	switch (cmd.sensorType) {
		case SENSOR_TYPE_TEMPERATURE: //0x01
			String temp = convertTemperatureIfNeeded(cmd.scaledSensorValue, (cmd.scale ? "F" : "C"), cmd.precision)
			BigDecimal offset = safeToDec(settings?.tempOffset,0)
			BigDecimal tempOS = safeToDec(temp,0) + offset
			logDebug "Temperature Offset by ${offset} from ${temp} to ${tempOS}"
			sendEventLog(name:"temperature", value:(safeToDec(tempOS,0,Math.min(cmd.precision,1))), unit:"°${temperatureScale}")
			break
		case SENSOR_TYPE_LUMINANCE: //0x03
			BigDecimal offset = safeToDec(settings?.lightOffset,0)
			BigDecimal lightOS = safeToDec(cmd.scaledSensorValue,0) + offset
			logDebug "Light % Offset by ${offset} from ${cmd.scaledSensorValue} to ${lightOS}"
			sendEventLog(name:"illuminance", value:(Math.round(lightOS)), unit:"%")
			break
		case SENSOR_TYPE_HUMIDITY:  //0x05
			BigDecimal offset = safeToDec(settings?.humidityOffset,0)
			BigDecimal humidOS = safeToDec(cmd.scaledSensorValue,0) + offset
			logDebug "Humidity Offset by ${offset} from ${cmd.scaledSensorValue} to ${humidOS}"
			sendEventLog(name:"humidity", value:(safeToDec(humidOS,0,Math.min(cmd.precision,1))), unit:"%")
			break
		default:
			logDebug "Unhandled sensorType: ${cmd}"
	}
}

void zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd, ep=0) {
	logTrace "${cmd} (ep ${ep})"
	switch (cmd.notificationType) {
		case NOTIFICATION_TYPE_SECURITY:
			sendSecurityEvent(cmd.event, cmd.eventParameter[0])
			break
		default:
			logDebug "Unhandled notificationType: ${cmd}"
	}
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

void sendSecurityEvent(event, parameter) {
	Boolean cleared
	Integer eventAdj = event
	//Idle Event the parameter is the event to clear
	if (event == EVENT_PARAM_IDLE) {
		eventAdj = parameter
		cleared = true
	}
	
	switch (eventAdj) {
		case EVENT_PARAM_TAMPER:
			sendEventLog(name:"tamper", value:(cleared ? "clear":"detected"))
			break
		case EVENT_PARAM_MOTION:
			sendEventLog(name:"motion", value:(cleared ? "inactive":"active"))
			break
		default:
			logDebug "Unhandled Security Event: ${event}, ${parameter}"
	}
}


/*******************************************************************
 ***** Execute / Build Commands
********************************************************************/
List<String> getConfigureCmds() {
	logDebug "getConfigureCmds..."

	List<String> cmds = []

	Integer wakeSeconds = wakeUpInt ? wakeUpInt*3600 : 43200
	if (state.resyncAll || wakeSeconds != (device.getDataValue("zwWakeupInterval") as Integer)) {
		logDebug "Settting WakeUp Interval to $wakeSeconds seconds"
		cmds << wakeUpIntervalSetCmd(wakeSeconds)
		cmds << wakeUpIntervalGetCmd()
	}
	if (state.resyncAll || !firmwareVersion || !state.deviceModel) {
		cmds << mfgSpecificGetCmd()
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

	if (state.resyncAll) {
		clearVariables()
	}
	state.resyncAll = false

	if (cmds) updateSyncingStatus(6)

	return cmds ?: []
}

List<String> getRefreshCmds() {
	List<String> cmds = []

	cmds << wakeUpIntervalGetCmd()
	cmds << versionGetCmd()

	//Sensors
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_TEMPERATURE)
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_LUMINANCE)
	cmds << sensorMultilevelGetCmd(SENSOR_TYPE_HUMIDITY)
	//These don't work
	//cmds << notificationGetCmd(NOTIFICATION_TYPE_SECURITY, EVENT_PARAM_TAMPER)
	//cmds << notificationGetCmd(NOTIFICATION_TYPE_SECURITY, EVENT_PARAM_MOTION)

	return cmds ?: []
}

List getConfigureAssocsCmds(Boolean logging=false) {
	List<String> cmds = []

	if (!state.group1Assoc || state.resyncAll) {
		if (logging) logDebug "Setting lifeline association..."
		cmds << associationSetCmd(1, [zwaveHubNodeId])
		cmds << associationGetCmd(1)
	}

	return cmds
}

private logForceWakeupMessage(msg) {
	String helpText = "You can force a wake up by using a paper clip to push the Z-Wave button on the device."
	logWarn "${msg} will execute the next time the device wakes up.  ${helpText}"
	state.INFO = "*** ${msg} *** Waiting for device to wake up.  ${helpText}"
}

private setSubModel() {
	String devModel = state.deviceModel
	if (devModel == "ZSE40-700") { devModel = setDevModel() }
	if (!state.subModel) {
		if (devModel == "ZSE40" && getDataValue("inClusters").contains("0x9F")) {
			state.subModel = "v700"
		}
	}
}


/*******************************************************************
 ***** Required for Library
********************************************************************/
//These have to be added in after the fact or groovy complains
void fixParamsMap() {
	paramsMap['settings'] = [fixed: true]
}

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
	return secureCmd(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: scale, sensorType: sensorType))
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
