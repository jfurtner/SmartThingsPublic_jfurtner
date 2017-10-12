/**
 *  Copyright 2015 SmartThings
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
 *  Virtual Thermostat
 *
 *  Author: SmartThings
 */
definition(
    name: "Virtual Thermostat",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
	section("When there's been movement from (optional, leave blank to not require motion)..."){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Within this number of minutes..."){
		input "minutes", "number", title: "Minutes", required: false
	}
	section("But never go below (or above if A/C) this value with or without motion..."){
		input "emergencySetpoint", "decimal", title: "Emer Temp", required: false
	}
	section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
	}
    section('Debugging') {
        input "debugEnabled", 'boolean', required:false, default: false, title:'Debugging enabled'
        input "traceEnabled", 'boolean', required:false, default: false, title:'Tracing enabled'
    }
}

def installed()
{
	updated()
}

def updated()
{
	logDebug("Installing with settings: ${settings}")
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
}

def temperatureHandler(evt)
{
	logTrace("Temperature changed: ${evt.doubleValue}")
	def isActive = hasBeenRecentMotion()
    logTrace("Active: $isActive; emergencySetpoint: $emergencySetpoint")
	if (isActive || emergencySetpoint) {
		evaluate(evt.doubleValue, isActive ? setpoint : emergencySetpoint)
	}
	else {
		enableDisableOutlets(false)
	}
}

def enableDisableOutlets(Boolean onOff)
{
	logTrace("outlets enable:$onOff")
	if (onOff)
    	outlets.on()
    else
    	outlets.off()
}

def motionHandler(evt)
{
	logTrace "MOTIONHANDLER (${evt})"
	if (evt.value == "active") {
    	logTrace("Motion active")
		def lastTemp = sensor.currentTemperature
		if (lastTemp != null) {
			evaluate(lastTemp, setpoint)
		}
	} else if (evt.value == "inactive") {
		def isActive = hasBeenRecentMotion()
		logTrace "Motion currently inactive. isActive: $isActive"
		if (isActive || emergencySetpoint) {
			def lastTemp = sensor.currentTemperature
            logTrace("lastTemp: $lastTemp")
			if (lastTemp != null) {
				evaluate(lastTemp, isActive ? setpoint : emergencySetpoint)
			}
		}
		else {
			enableDisableOutlets(false)
		}
	}
}

private evaluate(currentTemp, desiredTemp)
{
	logDebug "EVALUATE(current:$currentTemp, desired:$desiredTemp)"
	def threshold = 1.0
	if (mode == "cool") {
		// air conditioner
		if (currentTemp - desiredTemp >= threshold) {
			enableDisableOutlets(true)
		}
		else if (desiredTemp - currentTemp >= threshold) {
			enableDisableOutlets(false)
		}
        else
        	logTrace "no change"
	}
	else {
    	logDebug("heat mode")
		// heater
		if (desiredTemp - currentTemp >= threshold) {
			enableDisableOutlets(true)
		}
		else if (currentTemp - desiredTemp >= threshold) {
			enableDisableOutlets(false)
		}
        else
        	logTrace "no change"
	}
}

private hasBeenRecentMotion()
{
	def isActive = false
	if (motion && minutes) {
		def deltaMinutes = minutes as Long
		if (deltaMinutes) {
			def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
			log.trace "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
			if (motionEvents.find { it.value == "active" }) {
				isActive = true
			}
		}
	}
	else {
		isActive = true
	}
	isActive
}

def logDebug(msg) {
	if (debugEnabled == 'true')
    {
		log.debug msg
    }
}

def logTrace(msg) {
	if (traceEnabled == 'true')
    {
		log.debug msg
    }
}