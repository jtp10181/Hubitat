/**
 * ====================================================
 *  App Name: SmartStart Manager
 *  Platform: Hubitat Elevation
 *
 *  For Support, Information, and Updates:
 *  https://community.hubitat.com/t/smartstart-manager/137492
 *  https://github.com/jtp10181/Hubitat/tree/main/Apps/smartstart-manager
 * ====================================================
 *
 *  Copyright 2024 Jeff Page
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
 *
 * Changelog:
 * 0.4.2 (2024-05-08) - Added validation checks to Edit/Add page
 * 0.4.0 (2024-05-05) - Changed some async calls to regular http calls
 *                      Added links on list view going to device page and edit page
 *                      Added editing/deleting of disabled entries
 *                      Fixed reported null pointer errors
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

@Field static final String VERSION = "0.4.2"
@Field static final String APP_NAME = "SmartStart-Manager"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/smartstart-manager/137492"

definition(
    name: "SmartStart Manager",
    namespace: "jtp10181",
    author: "Jeff Page (@jtp10181)",
    description: "Manage the Z-Wave SmartStart devices list from the hub",
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
    logDebug("Updated with settings: ${settings}")
    initialize()
}

void initialize() {
    logDebug("initialize...")
    subscribe(location, "systemStart", handleReboot)
    updateLastCheckIn()
}

void handleReboot(evt) {
    updateLastCheckIn()
}

void appButtonHandler(String btn) {
    if (btn == "btnEditSave") {
        btnEditSave()
    }
    else if (btn == "btnEditDelete") {
        btnEditDelete()
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
    smartListUpdateAsync()
    zwDetailsUpdateAsync()
    state.remove("editSelection")
    app.removeSetting("selectedDev")
    if (!state.ssEnabled) state.ssEnabled = [:]
    if (!state.smartListDisabled) state.smartListDisabled = []

    dynamicPage(name: "pageMain", title:styleSection("SmartStart Manager"), uninstall: true, install: true) {
        section() {
            href name: "pageViewListHref", page: "pageViewList",
                    title: "View SmartStart List", description: "", width: 4, newLine: true
            href name: "pageEditEntryHref", page: "pageEditEntry",
                    title: "Edit / Add SmartStart Entries", description: "", width: 4, newLine: true
        }

        section("Settings", hideable: true, hidden: true) {
            input name: "maskDSK", type: "bool", title: "Partially Mask DSK (for screenshots)", defaultValue: true, width:6, submitOnChange: true, newLineAfter: true
            paragraph ''
            input name: "debugLevel", type: "enum", title: "Debug Logging Level:", submitOnChange: true,
                    options: [0:"None", 1:"Debug", 2:"Trace"], defaultValue: 0, width: 4, newLineAfter: true
            paragraph ''
            input name: "btnLoadBackupD", type: "button", title: "Load Disabled List Backup from File Manager (Automatically Created)", width: 6, newLine: true
        }
        section() {
            paragraph appInfo()
            paragraph "<span style='font-size:80%;color:#61676b'>" +
                    "Support: <a href='${COMM_LINK}' target='_blank'>Hubitat Community</a><br/>" +
                    "Donations: <a href='https://paypal.me/JPage81?locale.x=en_US' target='_blank'>PayPal.Me</a>" + "</span>"
        }
    }
}

def pageViewList() {
    logDebug "Loading pageViewList()..."
    state.remove("editSelection")

    dynamicPage(name: "pageViewList", title:styleSection("SmartStart Manager: View List"), uninstall: false, install: false) {
        section() {
            href name: "pageEditEntryHref", url: "./pageEditEntry?idx=-1",
                    title: "Add SmartStart Entry", description: "", width: 3, newLine: true
            paragraph listTable()
            paragraph appInfo()
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
            "<thead><tr style='border-bottom:2px solid black'>"
    str += "<th style='font-size:0.8rem !important;border-right:2px solid black'><strong>Enabled</strong></th>" +
            "<th style='text-align:left'><strong>Name</strong></th>"
    tHead.each { str += "<th><strong>${it}</strong></th>" }
    str += "</tr></thead>"

    ssList.sort(false){ it.nodeName?.toLowerCase() }.each { ss ->
        logDebug("Processing Entry [${ss.idx}: ${ss.nodeName}] (${ss.enabled ? "enabled" : "disabled"})", "trace")
        Map nodeInfo = state.zwNodes.find { it.nodeId == ss.nodeDec }
        Map devInfo = state.zwDevices["${ss.nodeDec}"]
        str += "<tr style='color:black'>"
        str += "<td style='border-right:2px solid black'>${buttonLink("btnEnabled:$ss.dsk", state.ssEnabled["$ss.dsk"] == "off" ? O : X, "#1A77C9")}</td>"
        str += "<td style='text-align:left'><a href='./pageEditEntry?idx=${ss.idx}'>${ss.nodeName}</a></td>"
        str += "<td>${bootModes[ss.bootMode as Integer]}</td>" +
                "<td>${ss.gkList.collect{ gKeysShort[it] }.join(', ')}</td>" +
                "<td>${ss.status?.capitalize() ?: '-'}</td>" +
                "<td>${ss.node ?: '-'}" + (ss.nodeDec ? " (${ss.nodeDec})" : "") + "</td>" +
                //"<td>${ss.nodeLocation ?: ''}</td>"
                "<td style='border-left:2px solid black;text-align:left'>"+ (devInfo?.displayName ? "<a href='/device/edit/${devInfo.id}' target='_blank'>${devInfo.displayName} <i class='pi pi-external-link ml-1'></i></a>" : "-") +"</td>" +
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
    Map ssOptions = getSmartEditMenu()
    Integer sel = (settings.selectedDev ? settings.selectedDev as Integer : -1)
    if (hubitatQueryString && state.editSelection == null) {
        List qList = (new JsonSlurper().parseText(hubitatQueryString)) as List
        sel = qList.find{ it.name == "idx" }.value as Integer
        app.removeSetting("hubitatQueryString")
    }
    Map ssEditing = resetEditSettings(sel)
    Boolean addNew = (ssEditing.dsk ? false : true)

    //Validation
    Boolean dskBlank = (!settings.editDSK)
    Boolean nameBlank = (settings.editName == null || settings.editName == '')
    Boolean dskOK = (dskBlank || settings.editDSK ==~ /^\d{5}-\d{5}-\d{5}-\d{5}-\d{5}-\d{5}-\d{5}-\d{5}$/)
    Boolean dSave = (dskBlank || nameBlank || !dskOK)

    dynamicPage(name: "pageEditEntry", title:styleSection("SmartStart Manager: Edit"), uninstall: false, install: false) {
        section() {
            href name: "pageViewListHref", url: "./pageViewList",
                    title: "Switch to List Page", description: "", width: 3, newLine: true
            //Display inputs
            input name: "selectedDev", type: "enum", title: styleInputTitle("Select Entry to Edit:"), options: ssOptions,
                    defaultValue: (-1), width:8, submitOnChange: true, newLine: true, newLineAfter: true, showFilter: true
            if (!addNew) {
                Map nodeInfo = state.zwNodes.find { it.nodeId == ssEditing.nodeDec }
                Map devInfo = state.zwDevices["${ssEditing.nodeDec}"]
                String dsk = (maskDSK == false ? ssEditing.dsk : "XXXXX" + ssEditing.dsk.substring(5))
                if (ssEditing.nodeDec && devInfo) {
                    paragraph (styleInputTitle("Included Node Info: ") + "<br/>" +
                            (devInfo?.displayName ? "${devInfo.displayName} <a href='/device/edit/${devInfo.id}' target='_blank'><i class='pi pi-external-link ml-1'></i></a>" : "-") + "<br/>" +
                            (ssEditing.nodeDec >= 256 ? "Long Range" : "Mesh Mode") + " - " +
                            (nodeInfo?.security ?: "Unknown Security") + " - " +
                            "Node: ${ssEditing.node} (${ssEditing.nodeDec})")
                    //input name: "btnCopyName", type: "button", title: "Copy Name to SmartStart"
                }
                paragraph styleInputTitle("DSK:") + "<br/>${dsk}"
            } else {
                input name: "editDSK", type: "string", title: styleInputTitle("Input DSK:", true) + "&nbsp;&nbsp;&nbsp;&nbsp;" +
                        "<span ${dskOK ? "" : "style='color:red'"}><i>ppppp-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx</i></span>", width:8, newLine: true, submitOnChange: true
            }
            input name: "editName", type: "string", title: styleInputTitle("SmartStart Name:", true), width:8, newLine: true, submitOnChange: true
            input name: "editLocation", type: "string", title: styleInputTitle("SmartStart Location:"), width:8, newLine: true, submitOnChange: true
            input name: "editGrants", type: "enum", title: styleInputTitle("Grant Keys:"), multiple: true, options: gKeys, defaultValue: 0, width:4, newLine: true, submitOnChange: true
            input name: "editBootMode", type: "enum", title: styleInputTitle("Boot Mode:"), options: bootModes, defaultValue: 1, width:4, newLineAfter: true, submitOnChange: true
            //input name: "editEnabled", type: "bool", title: styleInputTitle("SmartStart Status: " + (editEnabled ? "Enabled" : "Disabled")), width:6, submitOnChange: true, newLine: true
            if (!addNew) paragraph "This SmartStart Entry is <span style='font-weight:bold;color:" + (ssEditing.enabled ? "DarkGreen" : "DarkRed") + "'>" +
                    (ssEditing.enabled ? "Enabled" : "Disabled") + "</span> <i>(can be changed in list view)</i>"
            paragraph ""
            input name: "btnEditSave", type: "button", title: "Save to SmartStart", width: 3, textColor: (dSave ? "#f2f2f2" : "white"),
                    backgroundColor: (dSave ? "#a9c7a9" : "DarkGreen"), disabled: dSave
            if (!addNew) {
                input name: "btnEditDelete", type: "button", title: "Delete from SmartStart", width: 3, textColor: "white", backgroundColor: "#cc2d3b"
            }
            paragraph(state.msgEditDel ? "$state.msgEditDel" : "")
            state.remove("msgEditDel")
        }

        section() { paragraph appInfo() }
    }
}

String appInfo() {
    return "<span style='font-size:90%;color:#555a5e;font-weight:bold'>SmartStart Manager v${VERSION}</span>" +
            "<span style='font-size:84%;color:#555a5e'> - &copy; 2024 Jeff Page (@jtp10181)</span>"
}

Map resetEditSettings(Integer selection) {
    logDebug "resetEditSettings(${selection})"
    List ssList = getSmartList()
    Map ssEditing = [dsk:"", nodeName:"", nodeLocation:"", gkList:[1, 2], bootMode:1, enabled:true]

    //Get things ready
    if (selection >= ssList.size()) { selection = -1 }
    if (selection >= 0) { ssEditing = ssList.getAt(selection) }
    logDebug("Checking selectedDev: [${selection}: ${ssEditing?.nodeName}] -- prior selection: ${state.editSelection}")
    if (state.editSelection != selection || state.editSelection == null) {
        state.editSelection = selection
        app.updateSetting("selectedDev", [type: "enum", value: "$selection"])
        app.updateSetting("editDSK", [type: "string", value: ssEditing.dsk])
        app.updateSetting("editName", [type: "string", value: ssEditing.nodeName])
        app.updateSetting("editLocation", [type: "string", value: ssEditing.nodeLocation])
        app.updateSetting("editGrants", [type: "enum", value: ssEditing.gkList])
        app.updateSetting("editBootMode", [type: "enum", value: "$ssEditing.bootMode"])
        app.updateSetting("editEnabled", [type: "bool", value: ssEditing.enabled])
        logDebug("Updated fields for: [${selection}: ${ssEditing?.nodeName}]")
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

    httpGet(params) { resp ->
        saveSmartList(resp.data?.items ?: [] as List)
    }
}

void smartListUpdateAsync() {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/mobileapi/zwave/smartstart/list",
    ]
    logDebug "smartListUpdate ${params}"
    asynchttpGet("smartListHandler", params)
}

void smartListHandler(resp, data) {
    logDebug "Processing smartList Response"
    //logDebug("smartList Response: ${resp.data}", "trace")
    Map respData = [:]
    try {
        def jSlurp = new JsonSlurper()
        respData = jSlurp.parseText(resp.data as String) as Map
    } catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
    saveSmartList(respData?.items ?: [] as List)
}

//Download Z-Wave Details from Endpoint
void zwDetailsUpdate() {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveDetails/json",
    ]
    logDebug "zwDetailsUpdate ${params}"

    httpGet(params) { resp ->
        state.zwDevices = resp.data?.zwDevices as Map
        state.zwNodes = resp.data?.nodes as List
    }
}

void zwDetailsUpdateAsync() {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveDetails/json",
    ]
    logDebug "zwDetailsUpdate ${params}"
    asynchttpGet("zwDetailsHandler", params)
}

void zwDetailsHandler(resp, data) {
    logDebug "Processing zwDetails Response"
    Map respData = [:]
    try {
        def jSlurp = new JsonSlurper()
        respData = jSlurp.parseText(resp.data as String) as Map
    } catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
    state.zwDevices = respData.zwDevices as Map
    state.zwNodes = respData.nodes as List
}

//Save New/Edited SmartStart Entry back to hub
//node = [dsk, nodeName, nodeLocation, grantKeys, bootMode]
Boolean smartEditPost(Map ssNode) {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/mobileapi/zwave/smartstart/edit",
            body   : [ nodeDSK: ssNode.dsk, nodeName:ssNode.nodeName, nodeLocation:ssNode.nodeLocation,
                       grantKeys: ssNode.grantKeys as String, bootMode: ssNode.bootMode as String ]
    ]
    logDebug "smartEditPost ${params.findAll { k,v -> k != 'body'}}"
    logDebug("smartEditPost ${params}", "trace")

    Boolean success = false
    try {
        httpPostJson(params) { resp ->
            //Refresh List from hub if success
            logDebug "editSaveResponse: ${resp.data}"
            success = (resp.data?.status == "success")
            if (!success) { logDebug("smartEditPost Error: ${resp.data}", "error") }
            smartListUpdate()
        }
    } catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
    return success
}

//Delete SmartStart Entry from hub
Boolean smartDelPost(String dsk) {
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/mobileapi/zwave/smartstart/delete",
            body   : [nodeDSK: dsk]
    ]
    logDebug "smartDelPost ${params.findAll { k,v -> k != 'body'}}"
    logDebug("smartDelPost ${params}", "trace")

    Boolean success = false
    try {
        httpPostJson(params) { resp ->
            //Refresh List from hub if success
            logDebug "smartDelResponse: ${resp.data}"
            success = (resp.data?.status == "success")
            if (!success) { logDebug("smartDelPost Error: ${resp.data}", "error") }
            smartListUpdate()
        }
    } catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
    return success
}

//Process and Save SmartStart List from hub
void saveSmartList(List ssList) {
    logDebug("saveSmartList with ${ssList.size() ?: 0} SmartStart records and ${state.smartListDisabled?.size() ?: 0} disabled records")
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
        //Node to Decimal
        ss.nodeDec = null
        if (ss.node && ss.node.charAt(0) == '0') {
            ss.nodeDec = hubitat.helper.HexUtils.hexStringToInt(ss.node)
        }

        //grantKeys convert to readable list
        ss.gkList = []
        ss.grantKeys = ss.grantKeys ?: 0
        gKeys.each { k, v ->
            if ((ss.grantKeys as Integer) & k || ss.grantKeys == k) {
                ss.gkList << k
            }
        }
    }

    //Sort, store index number, and save to state
    ssList.sort{ it.dsk.toLowerCase() }.eachWithIndex { ss, idx -> ss.idx = idx }
    state.smartListCombined = ssList
}

List getSmartList() {
    return state.smartListCombined as List
}

Map getSmartEditMenu() {
    logDebug("Generating Edit Menu...")
    Map ssMenu = [:]
    smartList.each { ss ->
        Map devInfo = state.zwDevices["${ss.nodeDec}"]
        ssMenu[ss.idx] = "${ss.nodeName}" + (devInfo ? " (${devInfo.displayName})" : " - Not included")
    }
    ssMenu = [(-1): "--- ADD NEW ---"] << ssMenu.sort { it.value.toLowerCase() }
    return ssMenu
}

void btnEditSave() {
    Map ssNode = [
            dsk: settings.editDSK, nodeName: settings.editName, nodeLocation: settings.editLocation,
            grantKeys: (settings.editGrants)?.sum { it as Integer } as String,
            bootMode: settings.editBootMode as String
    ]
    Boolean result
    if (settings.editEnabled) {
        logDebug("Saving enabled entry to SmartStart on Hub: ${ssNode.nodeName}")
        result = smartEditPost(ssNode)
    }
    else {
        logDebug("Saving disabled entry to app state: ${ssNode.nodeName}")
        Map dNode = state.smartListDisabled?.find { it.dsk == settings.editDSK }
        if (dNode) {
            dNode.putAll(ssNode)
            smartListUpdate()
            result = true
        }
    }

    //Show Status Message
    if (result) { atomicState.msgEditDel = "<strong>\u2705 Saved Successfully</strong>" }
    else { atomicState.msgEditDel = "<strong>\u274C Save Failed</strong>" }

    //Reset Edit Page
    if (result && state.editSelection == -1) state.remove("editSelection")
}

void btnEditDelete() {
    Boolean result
    if (settings.editEnabled) {
        logDebug("Deleting enabled entry from SmartStart on Hub: ${settings.editName}")
        result = smartDelPost(settings.editDSK)
    } else {
        logDebug("Deleting disabled entry from app state: ${settings.editName}")
        state.smartListDisabled?.removeAll { it.dsk == settings.editDSK }
        smartListUpdate()
        result = true
    }

    //Show Status Message
    if (result) { atomicState.msgEditDel = "<strong>\u2611\uFE0F Deleted Successfully</strong>" }
    else { atomicState.msgEditDel = "<strong>\u26A0 Delete Failed</strong>" }

    //Reset Edit Page
    resetEditSettings(-1)
}

void smartListToggle(String dsk, Boolean enabled = null) {
    if (enabled == null) {
        String cs = state.ssEnabled[dsk]
        enabled = (!cs || cs == "on" ? false : true)
    }

    if (enabled) {
        Map ssNode = state.smartListDisabled?.find { it.dsk == dsk }
        logDebug "Enabling ${ssNode.nodeName} - saving to SmartStart"
        logDebug("Enabling ${ssNode}", "trace")

        //Add back to SmartStart
        if (ssNode) smartEditPost(ssNode.clone())
    }
    else {
        Map ssNode = state.smartListCombined?.find { it.dsk == dsk }
        logDebug "Disabling ${ssNode.nodeName} - removing from SmarStart"
        logDebug("Disabling ${ssNode}", "trace")
        ssNode.enabled = false

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
    logDebug("disabledSaveBackup: Backing up ${disabledList?.size() ?: 0} disabled records")
    String disabledJson = JsonOutput.toJson(disabledList) as String
    uploadHubFile("$disabledBackupFile",disabledJson.getBytes())
}

void disabledLoadBackup() {
    byte[] dBytes = downloadHubFile("$disabledBackupFile")
    state.smartListDisabled = (new JsonSlurper().parseText(new String(dBytes))) as List
    logDebug("disabledLoadBackup: Restored ${state.smartListDisabled?.size() ?: 0} disabled records")
}

//Styling Functions
String styleSection(String sectionHeadingText) {
   return "<div style='font-weight:bold; font-size: 120%'>$sectionHeadingText</div>" as String
}

String styleInputTitle(String title, Boolean required = false) {
    return "<strong>$title</strong>" + (required ? "<span style='color:red'> *</span>" : "") as String
}

//Check-in
void updateLastCheckIn() {
    def nowDate = new Date()
    Long lastExecuted = state.lastCheckInTime ?: 0
    Long allowedMil = 24 * 60 * 60 * 1000   //24 Hours
    if (lastExecuted + allowedMil <= nowDate.time) {
        state.lastCheckInTime = nowDate.time
        runIn(4, doCheckIn)
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
    String checkUri = "http://jtp10181.gateway.scarf.sh/${APP_NAME}/chk-v${VERSION}"
    try {
        httpGet(uri:checkUri, timeout:4) { logDebug "Application ${APP_NAME} v${VERSION}" }
        state.lastCheckInTime = (new Date()).time
    } catch (Exception e) { }
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