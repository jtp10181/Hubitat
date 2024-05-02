// hubitat start
// hub: 192.168.1.99  <- this is hub's IP address
// type: app          <- valid values here are "app" and "device"
// id: 204           <- this is app or driver's id
// hubitat end

/**
 * ====================================================
 *  App Name: SmartStart Manager
 *  Platform: Hubitat Elevation
 *
 *  For Support, Information, and Updates:
 *  https://github.com/jtp10181/Hubitat/tree/main/Apps/
 * ====================================================
 *
 *  Copyright 2023 Jeff Page
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * Changelog:
 * 0.3.0 (2024-05-01) - Added feature to disable entry but keep record within the app
 *                      Added setting to show the full DSK (default to masked)
 *                      Fixed bugs when SmartStart list is blank (@csteele)
 * 0.2.0 (2024-04-28) - Add delete button to edit page
 * 0.1.0 (2024-04-28) - Initial beta release
 *
 */

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

definition(
    name: "SmartStart Manager",
    namespace: "jtp10181",
    author: "Jeff Page (@jtp10181)",
    description: "Manage your SmartStart Devices List",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "",
    documentationLink: "",
    singleInstance: true,
    singleThreaded: true,
    installOnOpen: true
)

preferences {
    page name: "pageMain"
    page name: "pageViewList"
    page name: "pageEditEntry"
}

@Field static final LinkedHashMap<Integer,String> bootModes = [0:"S2 (Manual)", 1:"SmartStart Mesh", 2:"SmartStart LR"]
@Field static final LinkedHashMap<Integer,String> gKeys = [0:"No Security", 128:"S0 Unauthenticated", 1:"S2 Unauthenticated", 2:"S2 Authenticated", 4:"S2 Access Control"]
@Field static final LinkedHashMap<Integer,String> gKeysShort = [0:"None", 128:"S0", 1:"S2Ua", 2:"S2A", 4:"S2AC"]
@Field static final String disabledBackupFile = "smartstartManager_disabledBackup.json"

void installed() {
   log.info("Installed with settings: ${settings}")
   initialize()
}

void updated() {
    log.info("Updated with settings: ${settings}")
    initialize()
}

void initialize() {
    logDebug("initialize...")
    //Subscribe to stuff here???
}

void appButtonHandler(String btn) {
    if (btn == "btnEditSave") {
        Map ssNode = [
                dsk: settings.editDSK, nodeName: settings.editName, nodeLocation: settings.editLocation,
                grantKeys: (settings.editGrants)?.sum { it as Integer } as String,
                bootMode: settings.editBootMode as String
        ]
        smartEditPost(ssNode)
        if (state.editSelection == -1) { state.editSelection = -2 }
    }
    else if (btn == "btnEditDelete") {
        smartDelPost(settings.editDSK)
        resetEditSettings(-1)
    }
    else if (btn == "btnLoadBackupD") {
        disabledLoadBackup()
    }
    else if ((btn.tokenize(":"))[0] == "btnEnabled") {
        String dsk = (btn.tokenize(":"))[1]
        smartListToggle(dsk)
    }
    else {
        log.warn "Unhandled button press: $btn"
    }
}

def pageMain() {
    logDebug "Loading pageMain()..."
    smartListUpdate()
    zwDetailsUpdate()
    state.editSelection = -2
    if (!state.ssEnabled) state.ssEnabled = [:]
    if (!state.smartListDisabled) state.smartListDisabled = []

    dynamicPage(name: "pageMain", title:styleSection("Smart Start Manager"), uninstall: true, install: true) {
        section() {
            href(name: "pageViewListHref", page: "pageViewList",
                    title: "View SmartStart List", description: "", width: 4, newLine: true
            )
            href(name: "pageEditEntryHref", page: "pageEditEntry",
                    title: "Edit / Add SmartStart Entries", description: "", width: 4, newLine: true
            )
        }

        section("Settings", hideable: true, hidden: true) {
            input name: "debugLevel", type: "enum", title: "Debug Logging Level:", submitOnChange: true,
                    options: [0:"None", 1:"Debug", 2:"Trace"], defaultValue: 0, width: 4, newLineAfter: true
            input name: "maskDSK", type: "bool", title: "Partially Mask DSK (for screenshots)", defaultValue: true, width:6, submitOnChange: true, newLineAfter: true
            input name: "btnLoadBackupD", type: "button", title: "Load Disabled List Backup from File Manager (Automatically Created)", width: 6, newLine: true
        }
    }
}

def pageViewList() {
    logDebug "Loading pageViewList()..."

    dynamicPage(name: "pageViewList", title:styleSection("Smart Start Manager: View List"), uninstall: false, install: false) {
        section() {
            paragraph listTable()
        }
    }
}

String listTable() {
    ArrayList<String> tHead = ["Boot Mode","Grants","Status","Node","Device Name","Security","Mode"]
    String X = "<i class='he-checkbox-checked'></i>"
    String O = "<i class='he-checkbox-unchecked'></i>"
    List ssList = getSmartList()

    if (!ssList) { return "SmartStart List is Empty: Add to SmartStart using the Edit Section or Mobile App"}

    String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
    str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
            "</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
            "<thead><tr style='border-bottom:2px solid black'>" +
            "<th style='border-right:2px solid black' colspan='2'><strong>Name</strong></th>"
    tHead.each{str += "<th><strong>${it}</strong></th>"}
    str += "</tr></thead>"

    ssList.sort{it.nodeName.toLowerCase()}.eachWithIndex { ss, index ->
        Map nodeInfo = state.zwNodes.find { it.nodeId == ss.nodeDec }
        Map devInfo = state.zwDevices["${ss.nodeDec}"]
        str += "<tr style='color:black'>"
        str += "<td>${buttonLink("btnEnabled:$ss.dsk", state.ssEnabled["$ss.dsk"] == "off" ? O : X, "#1A77C9")}</td>"
        str += "<td style='border-right:2px solid black'>${ss.nodeName}</td>"
        str += "<td>${bootModes[ss.bootMode as Integer]}</td>" +
                "<td>${ss.gkList.collect{ gKeysShort[it] }.join(', ')}</td>" +
                "<td>${ss.status?.capitalize() ?: '-'}</td>" +
                "<td>${ss.node ?: '-'}" + (ss.nodeDec ? " (${ss.nodeDec})" : "") + "</td>" +
                //"<td>${ss.nodeLocation ?: ''}</td>"
                "<td style='border-left:2px solid black'>"+ (devInfo?.displayName ? "<a href='/device/edit/${devInfo.id}'>${devInfo.displayName}</a>" : "-") +"</td>" +
                "<td>"+ (nodeInfo?.security ?: "-") +"</td>" +
                "<td>"+ (ss.nodeDec>=256 ? "Long Range" : (ss.nodeDec ? "Mesh" : "-")) +"</td>"
    }
    str += "</tr></table></div>"
    return str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
    return "<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div>" +
            "<div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div>" +
            "<input type='hidden' name='settings[$btnName]' value=''>" as String
}

def pageEditEntry() {
    logDebug "Loading pageEditEntry()..."
    Map ssOptions = getSmartIndexes()
    Integer sel = ("${settings.selectedDev}".isNumber() ? settings.selectedDev as Integer : -1)
    Map ssEditing = resetEditSettings(sel)
    Boolean addNew = (ssEditing.dsk ? false : true)
    String dsk = (maskDSK == false ? ssEditing.dsk : ssEditing.dskMasked)

    dynamicPage(name: "pageEditEntry", title:styleSection("Smart Start Manager: Edit"), uninstall: false, install: false) {
        section() {
            //Display inputs
            input name: "selectedDev", type: "enum", title: styleInputTitle("Select Entry to Edit:"), options: ssOptions,
                    defaultValue: (-1), width:8, submitOnChange: true, newLine: true, newLineAfter: true, showFilter: true
            if (!addNew) {
                if (ssEditing.nodeDec) {
                    paragraph styleInputTitle("Included Node Info: ") + "<br/>" +
                            state.zwDevices["${ssEditing.nodeDec}"]['displayName'] + "<br/>" +
                            (ssEditing.nodeDec >= 256 ? "Long Range" : "Mesh Mode") + " - " +
                            state.zwNodes.find { it.nodeId == ssEditing.nodeDec }.security + " - " +
                            "Node: ${ssEditing.node} (${ssEditing.nodeDec})"
                }
                paragraph styleInputTitle("DSK:") + "<br/>${dsk}"
            } else {
                input name: "editDSK", type: "string", title: styleInputTitle("DSK:"), width:8, newLine: true, submitOnChange: true
            }
            input name: "editName", type: "string", title: styleInputTitle("SmartStart Name:"), width:8, newLine: true, submitOnChange: true
            input name: "editLocation", type: "string", title: styleInputTitle("SmartStart Location:"), width:8, newLine: true, submitOnChange: true
            input name: "editGrants", type: "enum", title: styleInputTitle("Grant Keys:"), multiple: true, options: gKeys, width:4, newLine: true, submitOnChange: true
            input name: "editBootMode", type: "enum", title: styleInputTitle("Boot Mode:"), options: bootModes, width:4, submitOnChange: true
            //input name: "editEnabled", type: "bool", title: styleInputTitle("SmartStart Status: " + (editEnabled ? "Enabled" : "Disabled")), width:6, submitOnChange: true, newLine: true
            if (!addNew) paragraph "This SmartStart Entry is <strong><span style='color:" + (ssEditing.enabled ? "DarkGreen" : "DarkRed") + "'>" +
                    (ssEditing.enabled ? "Enabled" : "Disabled") + "</span></strong> (adjust in list view)"
        }

        section() {
            if (ssEditing.enabled) {
                input name: "btnEditSave", type: "button", title: "Save to SmartStart", width: 3, textColor: "white", backgroundColor: "green"
                if (!addNew) {
                    input name: "btnEditDelete", type: "button", title: "Delete from SmartStart", width: 3, textColor: "white", backgroundColor: "#cc2d3b"
                }
            } else {
                paragraph styleInputTitle("Sorry, editing or deleting disabled entries has not been implemented yet!")
            }
        }
    }
}

Map resetEditSettings(Integer selection) {
    logDebug "resetEditSettings(${selection})"
    List ssList = getSmartList()
    Map ssEditing = [dsk:"", nodeName:"", nodeLocation:"", gkList:[1, 2], bootMode:1, enabled:true]
    //logDebug "${settings.selectedDev} | ${selection} | ${state.editSelection}"

    //Get things ready
    if (selection >= ssList?.size() || state.editSelection < -1) { selection = -1 }
    if (selection >= 0) { ssEditing = ssList.getAt(selection) }
    logDebug("selectedDev: ${selection} -- ${ssEditing?.nodeName}")
    if (state.editSelection != selection) {
        state.editSelection = selection
        app.updateSetting("selectedDev", [type: "enum", value: "$selection"])
        app.updateSetting("editDSK", [type: "string", value: ssEditing.dsk])
        app.updateSetting("editName", [type: "string", value: ssEditing.nodeName])
        app.updateSetting("editLocation", [type: "string", value: ssEditing.nodeLocation])
        app.updateSetting("editGrants", [type: "enum", value: ssEditing.gkList])
        app.updateSetting("editBootMode", [type: "enum", value: "$ssEditing.bootMode"])
        app.updateSetting("editEnabled", [type: "bool", value: ssEditing.enabled])
    }
    return ssEditing
}

//Download SS List From Endpoint
void smartListUpdate() {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/mobileapi/zwave/smartstart/list",
    ]
    logDebug "smartListUpdate ${params}"
    asynchttpGet("smartListHandler", params)
}

void smartListHandler(resp, data){
    logDebug "smartListHandler"
    Map respData = [:]
    try{
        def jSlurp = new JsonSlurper()
        respData = (Map)jSlurp.parseText((String)resp.data)
    } catch (EX) {
        log.error "$EX"
    }
    saveSmartList(respData?.items ?: [] as List)
}

//Download Z-Wave Details from Endpoint
void zwDetailsUpdate(){
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveDetails/json",
    ]
    logDebug "zwDetailsUpdate ${params}"
    asynchttpGet("zwDetailsHandler", params)
}

void zwDetailsHandler(resp, data){
    logDebug "zwDetailsHandler"
    Map respData = [:]
    try{
        def jSlurp = new JsonSlurper()
        respData = (Map)jSlurp.parseText((String)resp.data)
    } catch (EX) {
        log.error "$EX"
    }
    state.zwDevices = respData.zwDevices as Map
    state.zwNodes = respData.nodes as List
}

//Save New/Edited SmartStart Entry back to hub
//node = [dsk, nodeName, nodeLocation, grantKeys, bootMode]
void smartEditPost(Map ssNode) {
    params = [
            uri        : "http://127.0.0.1:8080",
            path       : "/mobileapi/zwave/smartstart/edit",
            contentType: "application/json",
            body       : [ nodeDSK: ssNode.dsk, nodeName:ssNode.nodeName, nodeLocation:ssNode.nodeLocation,
                    grantKeys: ssNode.grantKeys as String, bootMode: ssNode.bootMode as String ]
    ]
    logDebug "smartEditPost ${params}"
    asynchttpPost("smartEditHandler", params)
}

void smartEditHandler(resp, data){
    Map respData = [:]
    try{
        def jSlurp = new JsonSlurper()
        respData = (Map)jSlurp.parseText((String)resp.data)
        logDebug "editSaveHandler: ${respData}"
    } catch (EX) {
        log.error "$EX"
    }
    //Refresh List from hub if success
    if (respData.status == "success") { smartListUpdate() }
    else { logDebug(editSaveHandler: ${respData}, "error") }
}

//Delete SmartStart Entry from hub
void smartDelPost(String dsk) {
    params = [
            uri        : "http://127.0.0.1:8080",
            path       : "/mobileapi/zwave/smartstart/delete",
            contentType: "application/json",
            body       : [nodeDSK: dsk]
    ]
    logDebug "smartDelPost ${params}"
    asynchttpPost("smartDelHandler", params)
}

void smartDelHandler(resp, data){
    Map respData = [:]
    try{
        def jSlurp = new JsonSlurper()
        respData = (Map)jSlurp.parseText((String)resp.data)
        logDebug "smartDelHandler: ${respData}"
    } catch (EX) {
        log.error "$EX"
    }
    //Refresh List from hub if success
    if (respData.status == "success") { smartListUpdate() }
    else { logDebug(smartDelHandler: ${respData}, "error") }
}

//Process and Save SmartStart List from hub
void saveSmartList(List ssList) {
    logDebug("saveSmartList ${ssList}", "trace")
    //Processing for Hub List Only
    ssList.each { ss ->
        //Set enabled if in list from hub
        ss.enabled = true
        state.ssEnabled["$ss.dsk"] = "on"

        //Remove from disabled list if its in hub list
        state.smartListDisabled?.removeAll { it.dsk == ss.dsk || it.nodeDSK == ss.dsk }

        //Split node from status
        List statusSplit = (ss.status).split(", ") as List
        ss.status = statusSplit[0]
        ss.node = statusSplit[1]?.replaceAll("Node: ",'')
    }

    //Add in disabled items
    state.smartListDisabled?.each { ssList << it.clone() }

    //Processing for all entries
    ssList.each { ss ->
        //Mask DSK
        ss.dskMasked = "XXXXX" + ss.dsk.substring(5)

        //Node to Decimal
        ss.nodeDec = null
        if (ss.node && ss.node.charAt(0) == '0') {
            ss.nodeDec = hubitat.helper.HexUtils.hexStringToInt(ss.node)
        }

        //grantKeys convert to readable list
        ss.gkList = []
        gKeys.each { k, v ->
            if ((ss.grantKeys as Integer) & k || ss.grantKeys == k) {
                ss.gkList << k
            }
        }
    }
    state.smartListCombined = ssList
}

List getSmartList() {
    return state.smartListCombined as List
}

Map getSmartIndexes() {
    Map ssIndex = [(-1): "--- ADD NEW ---"] as Map
    smartList.eachWithIndex { ss, i ->
        ssIndex[i] = "${ss.nodeName}" + " "
        ssIndex[i] += (ss.nodeDec ? "("+state.zwDevices["${ss.nodeDec}"]['displayName']+")" : "- Not included")
    }
    return ssIndex.sort { it.value }
}

void smartListToggle(String dsk, Boolean enabled = null) {
    if (enabled == null) {
        String cs = state.ssEnabled[dsk]
        enabled = (!cs || cs == "on" ? false : true)
    }

    if (enabled) {
        Map ssNode = state.smartListDisabled?.find { it.dsk == dsk }
        logDebug "Enabling ${ssNode}"

        //Add back to SmartStart
        if (ssNode) smartEditPost(ssNode.clone())
    }
    else {
        Integer idx = state.smartListCombined?.findIndexOf { it.dsk == dsk }
        Map ssNode = state.smartListCombined[idx]
        state.smartListCombined[idx].enabled = false
        logDebug "Disabling ${ssNode}"

        //Copy to disabled list
        state.smartListDisabled << [
                dsk: ssNode.dsk, nodeName: ssNode.nodeName, nodeLocation: ssNode.nodeLocation,
                grantKeys: ssNode.grantKeys, bootMode: ssNode.bootMode,
                status: ssNode.status, node: ssNode.node, enabled: false
        ]

        //Delete from SmartStart
        smartDelPost(ssNode.dsk)
    }
    state.ssEnabled[dsk] = (enabled ? "on" : "off")

    //This will save backup for both enable and disable operations
    disabledSaveBackup(state.smartListDisabled)
}

void disabledSaveBackup(List disabledList) {
    String disabledJson = JsonOutput.toJson(disabledList) as String
    uploadHubFile("$disabledBackupFile",disabledJson.getBytes())
}

void disabledLoadBackup() {
    byte[] dBytes = downloadHubFile("$disabledBackupFile")
    def jSlurp = new JsonSlurper()
    state.smartListDisabled = (List)jSlurp.parseText(new String(dBytes))
}

//Styling Functions
String styleSection(String sectionHeadingText) {
   return """<div style="font-weight:bold; font-size: 120%">$sectionHeadingText</div>""" as String
}

String styleListItem(String text, Long index=0) {
   return """<div style="color: ${index %2 == 0 ? "darkslategray" : "black"}; background-color: ${index %2 == 0 ? 'white' : 'ghostwhite'}">$text</div>""" as String
}

String styleInputTitle(String title) {
    return """<strong>$title</strong>""" as String
}

//Logging Functions
// Writes to log.debug by default if debug logging setting enabled;
// can specify other log level (e.g., "info") if desired
void logDebug(String str, String level="debug") {
   switch(level) {
      default:
        if (settings["debugLevel"] != null && (settings["debugLevel"].toInteger()) >= 1) log."$level"(str)
      case "trace": 
         if (settings["debugLevel"] != null && (settings["debugLevel"].toInteger()) == 2) log.trace(str)
         break
   }
}