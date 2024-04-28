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
 * 0.1.0 (2024-04-28) - Initial beta release
 *
 */

//import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

definition(
    name: "Smart Start Manager",
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
@Field static final LinkedHashMap<Integer,String> gKeys = [0:"None", 128:"S0 Unauth", 1:"S2 Unauth", 2:"S2 Auth", 4:"S2 Access Control"]

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
    switch (btn) {
        case "btnEditSave":
            smartEditPost()
            break
        default:
            log.warn "Unhandled button press: $btn"
    }
}

def pageMain() {
    logDebug "Loading pageMain()..."
    smartListUpdate()
    zwDetailsUpdate()
    state.editSelection = -1

    dynamicPage(name: "pageMain", title:styleSection("Smart Start Manager: Main Page"), uninstall: true, install: true) {
        section() {
            href(name: "pageViewListHref",
                    page: "pageViewList",
                    title: "View SmartStart List",
                    description: "Click to view list"
            )
            href(name: "pageEditEntryHref",
                    page: "pageEditEntry",
                    title: "Edit SmartStart Entries",
                    description: "Click to edit entries"
            )
        }

        section("Settings", hideable: true, hidden: true) {
            input name: "debugLevel", type: "enum", title: "Debug Logging Level:", submitOnChange: true,
                    options: [0:"None", 1:"Debug", 2:"Trace"], defaultValue: 0
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
//    String X = "<i class='he-checkbox-checked'></i>"
//    String O = "<i class='he-checkbox-unchecked'></i>"
    List ssList = getSmartList()

    if (!ssList) { return "SmartStart List is Empty"}

    String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
    str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
            "</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
            "<thead><tr style='border-bottom:2px solid black'>" +
            "<th style='border-right:2px solid black'><strong>Name</strong></th>"
    tHead.each{str += "<th><strong>${it}</strong></th>"}
    str += "</tr></thead>"

    ssList.sort{it.nodeName.toLowerCase()}.eachWithIndex { ss, index ->
        str += "<tr style='color:black'><td style='border-right:2px solid black'>${ss.nodeName}</td>"
        str += "<td>${bootModes[ss.bootMode]}</td>" +
                "<td>${ss.gkListName.join(', ')}</td>" +
//                "<td>${ss.dskMasked}</td>" +
                "<td>${ss.status2.capitalize()}</td>" +
                "<td>${ss.node}" + (ss.nodeDec ? " (${ss.nodeDec})" : "") + "</td>" +
                //"<td>${ss.nodeLocation ?: ''}</td>"
                "<td style='border-left:2px solid black'>"+ (ss.nodeDec ? state.zwDevices["${ss.nodeDec}"]['displayName'] : "N/A") +"</td>" +
                "<td>"+ (ss.nodeDec ? state.zwNodes.find{ it.nodeId == ss.nodeDec }.security : "N/A") +"</td>" +
                "<td>"+ (ss.nodeDec>=256 ? "Long Range" : (ss.nodeDec ? "Mesh" : "N/A")) +"</td>"
    }

    str += "</tr></table></div>"
    return str
}

def pageEditEntry() {
    logDebug "Loading pageEditEntry()..."
    Map ssOptions = getSmartIndexes()
    List ssList = getSmartList()
    Map ssEditing = [dsk:"", nodeName:"", nodeLocation:"", gkList:[1, 2], bootMode:1] as Map

    dynamicPage(name: "pageEditEntry", title:styleSection("Smart Start Manager: Edit"), uninstall: false, install: false) {
        section() {
            //Get things ready
            Integer sel = (settings.selectedDev != null ? settings.selectedDev as Integer : -1)
            if (sel >= 0) {
                ssEditing = ssList[sel]
            }
            logDebug("selectedDev: ${sel} -- ${ssEditing}")
            if (state.editSelection != sel) {
                state.editSelection = sel
                app.updateSetting("editDSK", [type: "string", value: ssEditing.dsk])
                app.updateSetting("editName", [type: "string", value: ssEditing.nodeName])
                app.updateSetting("editLocation", [type: "string", value: ssEditing.nodeLocation])
                app.updateSetting("editGrants", [type: "enum", value: ssEditing.gkList])
                app.updateSetting("editBootMode", [type: "enum", value: "$ssEditing.bootMode"])
            }

            //Display inputs
            input name: "selectedDev", type: "enum", title: styleInputTitle("Select Entry to Edit:"),
                    options: ssOptions, defaultValue: (-1), width:8, submitOnChange: true, newLineAfter: true
            if (sel >= 0) {
                if (ssEditing.nodeDec) {
                    paragraph styleInputTitle("Included Node Info: ") + "<br/>" +
                            state.zwDevices["${ssEditing.nodeDec}"]['displayName'] + "<br/>" +
                            (ssEditing.nodeDec >= 256 ? "Long Range" : "Mesh Mode") + " - " +
                            state.zwNodes.find { it.nodeId == ssEditing.nodeDec }.security + " - " +
                            "Node: ${ssEditing.node} (${ssEditing.nodeDec})"
                }
                paragraph styleInputTitle("DSK:") + "<br/>${ssEditing.dskMasked}"
            } else {
                input name: "editDSK", type: "string", title: styleInputTitle("DSK:"), width:8, newLine: true, submitOnChange: true
            }
            input name: "editName", type: "string", title: styleInputTitle("SmartStart Name:"), width:8, newLine: true, submitOnChange: true
            input name: "editLocation", type: "string", title: styleInputTitle("SmartStart Location:"), width:8, newLine: true, submitOnChange: true
            input name: "editGrants", type: "enum", title: styleInputTitle("Grant Keys:"), multiple: true, options: gKeys, width:4, newLine: true, submitOnChange: true
            input name: "editBootMode", type: "enum", title: styleInputTitle("Boot Mode:"), options: bootModes, width:4, submitOnChange: true
        }

        section() {
            input name: "btnEditSave", type: "button", title: "Save to SmartStart"
        }
    }
}

//Download SS List From Endpoint
void smartListUpdate() {
//    if(security) cookie = getCookie()
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/mobileapi/zwave/smartstart/list",
//            headers: ["Cookie": cookie]
    ]
    logDebug "smartListUpdate ${params}"
    asynchttpGet("smartListHandler", params)
}

void smartListHandler(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map respData = (Map)jSlurp.parseText((String)resp.data)
        saveSmartList(respData.items as List)
    } catch (EX) {
        log.error "$EX"
    }
}

//Download Z-Wave Details from Endpoint
void zwDetailsUpdate(){
//    if(security) cookie = getCookie()
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveDetails/json",
//            headers: ["Cookie": cookie]
    ]
    logDebug "zwDetailsUpdate ${params}"
    asynchttpGet("zwDetailsHandler", params)
}

void zwDetailsHandler(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map respData = (Map)jSlurp.parseText((String)resp.data)
        state.zwDevices = respData.zwDevices as Map
        state.zwNodes = respData.nodes as List
    } catch (EX) {
        log.error "$EX"
    }
}

//Save New/Edited SmartStart Entry back to hub
void smartEditPost() {
    String gk = (settings.editGrants).sum { it as Integer } as String
    String bm = settings.editBootMode as String
    params = [
            uri        : "http://127.0.0.1:8080",
            path       : "/mobileapi/zwave/smartstart/edit",
            contentType: "application/json",
            body       : [nodeDSK: settings.editDSK, nodeName: settings.editName, nodeLocation: settings.editLocation, grantKeys: gk, bootMode: bm]
    ]
    logDebug "smartEditPost ${params}"
    asynchttpPost("smartEditHandler", params)
}

void smartEditHandler(resp, data){
    try{
        def jSlurp = new JsonSlurper()
        Map respData = (Map)jSlurp.parseText((String)resp.data)
        logDebug "editSaveHandler: ${respData}"
        //Refresh List from hub if success
        if (respData.status == "success") { smartListUpdate() }
        else { logDebug(editSaveHandler: ${respData}, "error") }
    } catch (EX) {
        log.error "$EX"
    }
}

//Process and Save SmartStart List
void saveSmartList(List ssList) {
    logDebug("saveSmartList ${ssList}", "trace")
    ssList.each { ss ->

        //Mask DSK
        ss.dskMasked = "XXXXX" + ss.dsk.substring(5)

        //Split node from status
        List statusSplit = (ss.status).split(", ") as List
        if (statusSplit[1]) {statusSplit[1] = statusSplit[1].replaceAll("Node: ",'')}
        ss.status2 = statusSplit[0]
        ss.node = statusSplit[1]

        //Node to Decimal
        ss.nodeDec = null
        if (ss.node && ss.node.charAt(0) == '0') {
            ss.nodeDec = hubitat.helper.HexUtils.hexStringToInt(ss.node)
        }

        //grantKeys convert to readable list
        ss.gkList = []
        ss.gkListName = []
        if (ss.grantKeys == 0) ss.gkListName << gKeys[0]
        gKeys.each { k, v ->
            if (ss.grantKeys & k) {
                ss.gkList << k
                ss.gkListName << v
            }
        }
    }
    state.smartListHub = ssList
}

List getSmartList() {
    return state.smartListHub as List
}

Map getSmartIndexes() {
    Map ssIndex = [(-1): "--- ADD NEW ---"] as Map
    (state.smartListHub).eachWithIndex { ss, i ->
        ssIndex[i] = "${ss.nodeName}"
        ssIndex[i] += " " + (ss.nodeDec ? "("+state.zwDevices["${ss.nodeDec}"]['displayName']+")" : "- Not included")
    }
    return ssIndex
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