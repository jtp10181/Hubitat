/*
    Child Central Scene Switch

    This was created because the Generic Component device from Hubitat
    always gets reported to voice control systems as a light instead of a switch

    NOTICE: This file has been modified by *Jeff Page*
        from the original work of *Mike Maxwell (Hubitat)*.

    Copyright 2020 Jeff Page
    Copyright 2016 -> 2020 Hubitat Inc. All Rights Reserved

    Changelog:

## [1.0.0] - 2021-03-12 (@jtp10181)
  - Initial release

*/

import groovy.transform.Field

metadata {
    definition(
        name: "Child Central Scene Switch", 
        namespace: "jtp10181", 
        author: "Jeff Page", 
        importUrl: "https://raw.githubusercontent.com/jtp10181/hubitat/master/Drivers/generic/child-central-scene-switch.groovy",
        component: true
    ) {
        capability "Switch"
        capability "Refresh"
        capability "PushableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        capability "DoubleTapableButton"
    }
    preferences {
        //Logging options similar to other Hubitat drivers
        input name: "txtEnable", type: "bool", title: "Enable Description Text Logging?", defaultValue: false
    }
}

void updated() {
    log.info "Updated..."
    log.warn "Description logging is: ${txtEnable == true}"
}

void installed() {
    log.info "Installed..."
    refresh()
}

void parse(String description) { 
    log.warn "parse(String) is not implemented"
}

void parse(List<Map> description) {
    description.each {
        logTxt "${it.descriptionText}"
        sendEvent(it)
    }
}

void on() {
    parent?.componentOn(this.device)
}

void off() {
    parent?.componentOff(this.device)
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void logTxt(String msg) {
    if (txtEnable) log.info "${device.displayName}: ${msg}"
}