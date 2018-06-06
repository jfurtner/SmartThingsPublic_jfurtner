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
	section("Enable device") {
    	input "enableDisableSwitch", "capability.switch", title:"Device"
        input "startTime", "time", title:"Start time"
        input "endTime", "time", title:"End time"
    }
    section("Presence") {
    	input "people", "capability.presenceSensor", multiple:true, title:"People who must be present?"
    }
	section("Alert device") {
    	input "alarm", "capability.switchLevel",  title:"Device to send alerts through", multiple:true
    }
	section("Alarm switch") {
		input "switchDevice", "capability.switch", multiple:true, title:"Devices"
        input "switchAlertNumber", "number", title:"Alarm number"
	}
    section("Alarm system") {	
    	input "securityAlertNumber", "number", title:"Alarm number"
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
	subscribe(switchDevice, "switch", switchHandler)    
    subscribe(location, "alarmSystemStatus", alarmStatusHandler)
}

def alarmStatusHandler(evt) {
	logTrace("Starting alarm handler")
	switch (evt.value)
    {
    	case "away":
        	logDebug("Away mode")
            setAlarm(3, securityAlertNumber)
        	break
        case "stay":
        	logDebug("Stay mode")
            setAlarm(2, securityAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(1, securityAlertNumber)
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
            setAlarm(2, switchAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(1, switchAlertNumber)
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