/**
 *  Delayed alarm
 *
 *  Copyright 2017 Jamie Furtner
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
 */
definition(
    name: "Delayed alarm",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Delayed chime then alarm",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@3x.png")

preferences {
    section('Master alarm') {
    	paragraph("This controls all the alarms. When this is turned on, the overall process starts")
        input "masterAlarm", "capability.alarm",multiple:false,title:"Master alarm switch"
    }
    section("Level 1 chimes") {
    	paragraph("These chimes are sent alerts to tell people that something happened. These are set immediately when the master alarm is turned on.")
        input "level1Chimes", "capability.switchLevel", multiple:true, title:"Chime dimmer switches"
        input "level1ChimeNumber", "number", range:"1..10", title:"Chime number to use"
    }
    section("Level 2 sirens") {
    	paragraph("These sirens are set on after the delay.")
        input "level2Delay", "number", range:"1..300",title:"Siren 1 delay", description:"seconds"
        input "level2Sirens", "capability.alarm", multiple:true, title:"Siren 1 alarms", required:false
    }
    section("Level 3 sirens") {
    	paragraph("These sirens are set on after a delay in seconds from when level 2 sirens are set on.")
        input "level3Delay", "number", range:"0..1800",title:"Siren 2 delay",description:"seconds"
        input "level3Sirens", "capability.alarm", multiple:true, title:"Siren 2 alarms", required:false
    }
    section("Duration") {
		paragraph("This sets the overall duration of all alarms in minutes")
        input "alarmDuration", "number", range:"0..300", title:"Duration of all alarms in minutes", description:"minutes"
    }
    section("Send Notifications?") {
    	paragraph("Send a SmartThings notification")
        input("notificationMessage", 'text', title:'Notification message', required:false)
        paragraph("Send a text message")
        input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
    }
    section('Debugging') {
		input "debugEnabled", "boolean", title:'Log debug events', required: false, default: false
        input "traceEnabled", "boolean", title:'Log trace events', required: false, default: false
	}
}

def installed() {
	logTrace("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	logTrace("Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize() {
	logTrace("Initializing - subscribing to alarm")
	subscribe(masterAlarm, "alarm", alarmHandler)
}

def alarmHandler(evt){
    logTrace("Alarm changed: ${masterAlarm.alarm} ${evt}")
	if (evt.value == 'off')
    {
    	allOff()
    }
    else
    {    
    	startLevel1Chimes()
    }
}

def allOff(){
	logTrace('allOff')
    if (state.alarmStarted != '')
    {
    	logTrace("Disabling all alarms")
        logTrace('unschedule')
        unschedule()
        if (level2Sirens != null)
        {
        	logTrace('level2Sirens off')
        	level2Sirens.off()
        }
        if (level3Sirens != null)
        {
        	logTrace('level3Sirens off')
        	level3Sirens.off()
        }
        logTrace('chimes off')
        level1Chimes.off()

		state.alarmStarted = ''
    }
}

def startLevel1Chimes(){
	if (state.alarmStarted == '')
    {
        logTrace("Starting chimes $level1ChimeNumber")
        state.alarmStarted = new Date()
        level1Chimes.setLevel(level1ChimeNumber*10)
        logTrace("Delay for $level2Delay seconds")
        runIn(level2Delay, startLevel2Sirens)
        // check that Contact Book is enabled and recipients selected
 		if (phone) { // check that the user did select a phone number
            sendSms(phone, notificationMessage)
        }
    }
    else
    {
    	logDebug "Alarm currently active ${state.alarmStarted}. Shouldn't be possible."
    }
}

def startLevel2Sirens(){
	logTrace("startLevel2Sirens")
    logTrace("Alarm: ${masterAlarm.alarm}")
    if (state.alarmStarted != '')
    {
        logTrace("Starting siren 1")
        if (level2Sirens != null)
	        level2Sirens.both()
        runIn(level3Delay, startLevel3Sirens)
    }
}

def startLevel3Sirens(){
	logTrace('startLevel3Sirens')
    logTrace("Alarm: ${masterAlarm.alarm}")
    if (state.alarmStarted != '')
    {
        logTrace("Starting siren 2")
        if (level3Sirens != null)
	        level3Sirens.both()
        runIn(alarmDuration*60, allOff)
	}
}

// debugging helper methods
private getDebugOutputSetting() {
	return (settings?.debugOutput != false)
}


private logDebug(msg) {
	if (debugEnabled == 'true') {
		log.debug "$msg"
	}
}

private logTrace(msg) {
	if (traceEnabled == 'true') {
    	log.trace "$msg"
    }
}