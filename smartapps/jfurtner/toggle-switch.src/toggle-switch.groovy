/**
 *  Toggle switch
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
    name: "Toggle switch",
    namespace: "jfurtner",
    author: "Jamie Furtner",
    description: "Toggle a set of switches on/off (based on first in list) when master virtual switch is set on, and toggle master virtual off again.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Run when toggled on, then toggle off") {
        input "virtualSwitch", "capability.switch", required: true, title: "Virtual master switch"
    }
	section("Switches") {
    	input "masterSwitch", "capability.switch", required: true, title: "Master device (used for state of set)"
        input "toggleSwitches", "capability.switch", required: true, multiple: true, title: "Switches to match master"
        input "switchesForceOff", "capability.switch", required: false, multiple: true, title: "Switches to turn off if master on"
        
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
	subscribe(virtualSwitch, "switch.on", virtualSwitchSet)
}

def virtualSwitchSet(evt){
	log.debug "virtualSwitchSet called: $evt"
    
    def masterSwitchState = masterSwitch.currentValue("switch")
    log.debug "$allCurStates : $curState"
    
    if (masterSwitchState == "on")
    {
    	toggleSwitches.off()
    }
    else if (masterSwitchState == "off")
    {
    	toggleSwitches.on()
        switchesForceOff.off()
    }
    else
    {
    	log.error "Unknown switch state: $masterSwitchState"
    }
    
    virtualSwitch.off()
}
