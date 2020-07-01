/**
 *  Vibration to open/close switch
 *
 *  Copyright 2020 Jamie Furtner
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
    name: "Vibration to open/close switch",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Translate a vibration from a multipurpose sensor to an open/close switch",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Description") {
    	paragraph "When vibration triggered, open the contact switch"
    }
	section("Vibration sensors") {
    	paragraph "Acceleration sensors to monitor"
		input "accelerationSensors", "capability.accelerationSensor", title:"Acceleration sensors", multiple:true, required:true
	}
    section("Virtual open/close sensor") {
    	input "contactSensor", "capability.contactSensor", title:"Virtual contact sensor", required:true
    }
    
    section("Debugging") {
    	input "debugEnabled", "bool", title:"Debugging logs enabled"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(accelerationSensors, "acceleration", accelerationHandler)
}

def accelerationHandler(evt) {
	logDebug("Acceleration event: ${evt.value} from ${evt.displayName}")
    if (evt.value == "active")
    {
    	contactSensor.open()
    } else if (evt.value == "inactive")
    {
    	contactSensor.close()
    }
}

def logDebug(m) {
	if (debugEnabled) {	
    	log.debug m
    }
}