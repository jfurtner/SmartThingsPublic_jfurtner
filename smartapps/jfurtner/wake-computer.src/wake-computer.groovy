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
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment14-icn@3x.png")


preferences {
	section("Motion sensors") {
		input "motionSensors", "capability.motionSensor", required: true, title: "Sensors", multiple: true
	}
    section("Limits") {
    	//input "modesToRunIn", "mode", title:"Select mode(s)", required:true,multiple:true
    	input "startTime", "time", title:"From",required:true
        input "endTime", "time", title:"To",required:true
        input "daysOfWeek", "enum",title:"Days of week",required:true,multiple:true,options:["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
    }
    section("Computer") {
    	input "computerMacAddress","text",title:"Computer MAC address (12 hex digits)",required:true
        input "secureCode","text",title:"Secure code (12 hex digits)",required:false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "initialize"
    log.debug "Subscribing to motion sensors"
    subscribe(motionSensors, "motion", motionSensorActive)
}

def motionSensorActive(evt) {
	log.debug "Motion sensor ${evt.getDevice()} fired"
    // get day of week
    def dateFormatter = new java.text.SimpleDateFormat("EEEE")
    dateFormatter.setTimeZone(location.timeZone)
    def dayOfWeek = dateFormatter.format(new Date())
    log.debug "Day of week: $dayOfWeek"
    
    def dayCheck = daysOfWeek.contains(dayOfWeek)
    if (dayCheck)
    {
    	log.debug "Valid day of week"
        def todayStart = timeToday(startTime, location.timeZone)
        def todayEnd = timeToday(endTime, location.timeZone)
        log.debug "Calculated start/end: $todayStart / $todayEnd"

        if (timeOfDayIsBetween(todayStart, todayEnd, new Date(), location.timeZone))
        {
            log.debug "now between start/end"
            wakeComputer()
        }
    }
}

def wakeComputer() {
	def mac = "$computerMacAddress".replaceAll(":","").replaceAll("-","")
    
    log.debug "Using mac address: $mac 845"
    def hubAction
    log.debug "Secure code value: '$secureCode'"
    if (secureCode != null && secureCode != '')
    {
    	log.debug "Secure code"
        hubAction = new physicalgraph.device.HubAction (
            "wake on lan $mac",
            physicalgraph.device.Protocol.LAN,
            null, // device network id (dni)
            [secureCode: "$secureCode"]
            )
    }
    else
    {
    	log.debug "No secure code"
        hubAction = new physicalgraph.device.HubAction (
            "wake on lan $mac",
            physicalgraph.device.Protocol.LAN,
            null, // device network id (dni)
            [:] // can't seem to use null or a variable, must set like this
            )
    }
    
    log.debug "HubAction: $hubAction"
    sendHubCommand(hubAction)
}
