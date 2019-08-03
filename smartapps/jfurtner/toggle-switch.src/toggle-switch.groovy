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
    description: "Toggle a set of switches on/off (based on first in list) when master virtual switch is set on.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
        input "masterSwitch", "capability.switch", required: true, title: "Master switch"
        input "toggleSwitches", "capability.switch", required: true, multiple: true, title: "Toggle switches"
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
	subscribe(masterSwitch, "switch.on", masterSwitchSet)
}

def masterSwitchSet(evt){
	log.debug "masterSwitchSet called: $evt"
    
    def allCurStates = toggleSwitches.currentValue("switch")
    def curState = allCurStates[0]
    log.debug "$allCurStates : $curState"
    
    if (curState == "on")
    {
    	toggleSwitches.off()
    }
    else if (curState == "off")
    {
    	toggleSwitches.on()
    }
    else
    {
    	log.error "Unknown switch state: $curState (all states: $allCurStates)"
    }
    
    masterSwitch.off()
    
}

// TODO: implement event handlers