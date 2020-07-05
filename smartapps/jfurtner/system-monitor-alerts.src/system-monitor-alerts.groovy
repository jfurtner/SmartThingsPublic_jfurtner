/**
 *  System monitor &amp; alerts
 *
 *  Copyright 2018 Jamie Furtner
 *
 */
definition(
    name: "System monitor & alerts",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Monitor system events and alert when things happen",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/secondary/strobe.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/secondary/strobe@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/secondary/strobe@3x.png")


preferences {
	section("Description") {
    	paragraph "Send arm/disarm chime though level switches to indicate when system state changes. Can also notify with chime when external alarm system is turned on."
    }
	section("This switch controls the alerts") {
    	input "enableDisableSwitch", "capability.switch", title:"Device"
    }
    section("Times between which the alarm should send notifications") {
        input "startTime", "time", title:"Start time"
        input "endTime", "time", title:"End time"
    }
    section("Presence") {
    	paragraph "The presence sensors which must be present for the arm/disarm alerts to trigger. At least one of these must be present for the notifications to be sent."
    	input "people", "capability.presenceSensor", multiple:true, title:"People who must be present"
    }
	section("Hub state changes") {
    	paragraph "Chime devices which arm/disarm notifications are sent through."
    	input "alarm", "capability.switchLevel",  title:"Device to send alerts through", multiple:true
    	paragraph "Alarm number (value*10) to set on Alert device)"
    	input "securityAlertNumber", "number", title:"Alarm number"
    	paragraph "The number of times to repeat for arm/disarm/stay state changes"
        input "hubRepeatDisarm", "number", range:"1..10",title:"Disarm/Home repeat",defaultValue:1
        input "hubRepeatStay", "number", range:"1..10",title:"Stay/Night repeat",defaultValue:2
        input "hubRepeatArm", "number", range:"1..10",title:"Arm/Away repeat",defaultValue:3
    }
	section("External alarm integration") {
    	paragraph "When integrating with external alarm systems, can also alert when the alarm system is armed or disarmed. Assumes the SmartThings representation of the external alarm system is a on/off switch."
		input "externalAlarmMainSwitch", "capability.switch", multiple:true, title:"External alarm devices", required:false
        input "externalAlarmAlertNumber", "number", title:"Alarm number", required:false, description:"4"
        paragraph "The number of times to repeat for arm/disarm (switch on/off) state changes"
        input "externalRepeatDisarm", "number", range:"1..10",title:"Disarm repeat",required:false, description:"1"
        input "externalRepeatArm", "number", range:"1..10",title:"Arm repeat",required:false, description:"2"
	}
}

public def installed() {
	logDebug("Installed with settings: ${settings}")

	initialize()
}

public def updated() {
	logDebug("Updated with settings: ${settings}")

	unsubscribe()
    unschedule()
	initialize()
}

public def initialize() {
	logTrace("Initialize, subscribing to events")
	subscribe(externalAlarmMainSwitch, "switch", switchHandler)    
    subscribe(location, "alarmSystemStatus", alarmStatusHandler)
}

def alarmStatusHandler(evt) {
	logTrace("Starting alarm handler")
	switch (evt.value)
    {
    	case "away":
        	logDebug("Away mode")
            setAlarm(hubRepeatArm, securityAlertNumber)
        	break
        case "stay":
        	logDebug("Stay mode")
            setAlarm(hubRepeatStay, securityAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(hubRepeatDisarm, securityAlertNumber)
        	break
        default:
        	logInfo("Unknown event value: ${evt.value}")
            return
    }
    
    sendPushMessage("Alarm state is ${evt.value}")
}

def switchHandler(evt) {
	logTrace("Starting switch handler")
	switch (evt.value)
    {
    	case "on":
        	logDebug("On")
            setAlarm(externalRepeatArm, externalAlarmAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(externalRepeatDisarm, externalAlarmAlertNumber)
        	break
        default:
        	logInfo("Unknown event value: ${evt.value}")
            return
    }
    
    sendPushMessage("${evt.displayName} is ${evt.value}")    
}

private setAlarm(repeatCount, alertNumber) {
	if (enableDisableSwitch.currentSwitch == "on")
    {
    	logDebug("Enable switch set on")
        if (someoneIsPresent())
        {        
        	logDebug("Someone is present")
            def between = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone)
            logDebug("Time between ${startTime} and ${endTime}: ${between}")
            if (between)
            {
                logDebug("Alerting ${repeatCount} times with alert number ${alertNumber}")
                def i
                for(i=0; i<repeatCount; i++)
                {
                    alarm.setLevel(alertNumber * 10)
                }
            }
        }
    }
}

private someoneIsPresent() {
	logTrace("someoneIsPresent")
    def result = false
    // iterate over our people variable that we defined
    // in the preferences method
    for (person in people) {
        if (person.currentPresence == "present") {
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = true
            break
        }
    }
    logDebug("Someone is present: ${result}")
    return result
}

private def logInfo(m) {
	log.info m
}

private def logTrace(m) {
	log.trace m
}
private def logDebug(m) {
	log.debug m
}