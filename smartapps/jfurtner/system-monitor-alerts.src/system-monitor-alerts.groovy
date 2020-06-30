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
    	paragraph "Send arm/disarm alerts though 'level' switches to indicate when system state changes. Can also push alert when main alarm sensor is turned on."
    }
	section("Enable device") {
    	paragraph "This switch controls the alerts"
    	input "enableDisableSwitch", "capability.switch", title:"Device"
        paragraph "Times between which the alarm should alert."
        input "startTime", "time", title:"Start time"
        input "endTime", "time", title:"End time"
    }
    section("Presence") {
    	paragraph "The presence sensors which must be present for the arm/disarm alerts to trigger"
    	input "people", "capability.presenceSensor", multiple:true, title:"People who must be present?"
    }
	section("Alert device") {
    	paragraph "Devices which arm/disarm alerts are sent through."
    	input "alarm", "capability.switchLevel",  title:"Device to send alerts through", multiple:true
    }
    section("Arm/Disarm system") {	
    	paragraph "Alarm number (value*10) to set on Alert device)"
    	input "securityAlertNumber", "number", title:"Alarm number"
    }
	section("Alarm switch") {
    	paragraph "When any switch is set on, also send alert to alert device."
		input "switchDevice", "capability.switch", multiple:true, title:"Devices", required:false
        input "switchAlertNumber", "number", title:"Alarm number"
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
            setAlarm(repeatCount:3, alertNumber:securityAlertNumber)
        	break
        case "stay":
        	logDebug("Stay mode")
            setAlarm(repeatCount:2, alertNumber:securityAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(repeatCount:1, alertNumber:securityAlertNumber)
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
            setAlarm(repeatCount:2, alertNumber:switchAlertNumber)
        	break
        case "off":
        	logDebug("Off")
            setAlarm(repeatCount:1, alertNumber:switchAlertNumber)
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