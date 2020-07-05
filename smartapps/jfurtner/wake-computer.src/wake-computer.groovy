/**
 *  Wake computer
 *
 *  Copyright 2018 Jamie Furtner
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
    name: "Wake computer",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Wake up computer if activity between hours and weekday",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment14-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment14-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment14-icn@3x.png"
    )
{
}

preferences {
	section("Motion sensors") {
    	paragraph "Motion on these sensors during the selected timeframe will cause the wake command to be sent"
		input "motionSensors", "capability.motionSensor", required: true, title: "Sensors", multiple: true
	}
    section("Presence sensor") {
    	input "person", "capability.presenceSensor", required: true, title:"Who must be present to trigger", multiple: false
    }
    section("Limits") {
    	input "startTime", "time", title:"From",required:true
        input "endTime", "time", title:"To",required:true
        input "daysOfWeek", "enum",title:"Days of week",required:true,multiple:true,options:["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
    }
    section("Computer/Device to wake") {
    	input "computerMacAddress","text",title:"Computer MAC address (12 hex digits)",required:true,description:"001a2b3c4d5e"
        input "secureCode","text",title:"Secure code (12 hex digits)",required:false,description:"(optional) 009f8e7d6c5b"
    }
    section('Debugging') {
    	input "debugEnabled", 'boolean', title:'Debug messages enabled', required:false, default:false
        input "traceEnabled", 'boolean', title:'Trace messages enabled', required:false, default:false
    }
}

def installed() {
	logDebug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logDebug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	logInfo "initialize"
    logDebug "Subscribing to motion sensors"
    subscribe(motionSensors, "motion", motionSensorActive)
    
    state.lastAlertSentDateTime = 0
}

def motionSensorActive(evt) {
	logDebug "Starting motionSensorActive: ${evt.getDevice()} fired"
    // get day of week
    def dateFormatter = new java.text.SimpleDateFormat("EEEE")
    dateFormatter.setTimeZone(location.timeZone)
    def dayOfWeek = dateFormatter.format(new Date())
    logDebug "Day of week: $dayOfWeek"
    
    def dayCheck = daysOfWeek.contains(dayOfWeek)
    if (dayCheck)
    {
    	logDebug "Valid day of week"
        def todayStart = timeToday(startTime, location.timeZone)
        def todayEnd = timeToday(endTime, location.timeZone)
        logTrace "Calculated start/end: $todayStart / $todayEnd"

        if (timeOfDayIsBetween(todayStart, todayEnd, new Date(), location.timeZone))
        {
            logDebug "current time between start/end"
            logTrace "Presence value: ${person.currentPresence}"
            if (person.currentPresence == "present")
            {
            	logDebug "Person present"
                def lastAlertDifference = now() - state.lastAlertSentDateTime
                if (lastAlertDifference > (5 * 60000))
                {
                    sendNotification("Waking computer", [method: "push", event:"true"])
                    state.lastAlertSentDateTime = now()
                }
            
            	wakeComputer()
            }
        }
    }
}

def wakeComputer() {
	logDebug "Starting wakeComputer"
	def mac = "$computerMacAddress".replaceAll(":","").replaceAll("-","")
    
    logTrace "Using mac address: $mac"
    def hubAction
    //logDebug "Secure code value: '$secureCode'"
    if (secureCode != null && secureCode != '')
    {
    	logInfo "Wake with secure code"
        hubAction = new physicalgraph.device.HubAction (
            "wake on lan $mac",
            physicalgraph.device.Protocol.LAN,
            null, // device network id (dni)
            [secureCode: "$secureCode"]
            )
    }
    else
    {
    	logInfo "Wake with no secure code"
        hubAction = new physicalgraph.device.HubAction (
            "wake on lan $mac",
            physicalgraph.device.Protocol.LAN,
            null, // device network id (dni)
            [:] // can't seem to use null or a variable, must set like this
            )
    }
    
    logTrace "HubAction: $hubAction"
    sendHubCommand(hubAction)
}

def logInfo(msg)
{
	log.info msg
}
def logWarn(msg)
{
	log.warn msg
}

def logTrace(msg)
{
	if (traceEnabled == 'true')
    {
		log.trace msg
    }
}

def logDebug(msg)
{
	if (debugEnabled == 'true')
    {
		log.debug msg
    }
}