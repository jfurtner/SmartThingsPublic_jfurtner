/**
 *  Wyze State trigger
 *
 *  Copyright 2019 Jamie Furtner
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
    name: "Wyze State trigger",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Enable / disable Wyze detection by invoking IFTTT maker hook",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section() {
		input "makerKey", "text", title: "IFTTT maker key"
        input "enableEventName", "text", title: "Enable event name", defaultValue: "enable_wyze"
        input "disableEventName", "text", title: "Disable event name", defaultValue: "disable_wyze"
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
	subscribe(location, modeChangeHandler)
}

def modeChangeHandler(evt) {
	log.debug "mode: ${evt.name} ${evt.value}"
    if (evt.isStateChange())
    {
        switch (evt.value)
        {
            case "Home":
            	invoke(disableEventName)
                break
            case "Night":
            	invoke(enableEventName)
                break
            case "Away":
            	invoke(enableEventName)
                break
        }
    }
}

def invoke(eventName) {
	log.debug "starting invoke: $eventName"
    def url = "https://maker.ifttt.com/trigger/$eventName/with/key/$makerKey"    
    try {
        httpGet(url) { resp -> log.trace "Response: ${resp.data}" }
	}
    catch (e) {
    	log.error "Error: $e"
    }
}