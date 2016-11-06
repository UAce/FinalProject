/* DPM Team 12
 *
 */
package finalproject;

import finalproject.localization.USLocalizer;
import finalproject.poller.USPoller;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.*;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;


public class Main {

	// Static Resources:
	// Left motor connected to output A
	// Right motor connected to output D
	// Ultrasonic sensor port connected to input S2
	// Color sensor port connected to input S1
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor hook = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3MediumRegulatedMotor turner = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("D"));	
	private static final Port usPort = LocalEV3.get().getPort("S3");		
	private static final Port colorPort = LocalEV3.get().getPort("S4");		
	
	public static void main(String[] args) throws InterruptedException {
		int buttonChoice;
		int [] a = {4, 25, 500, 7000, 5};	// Array that determines instrument: Piano
		
		//Setup ultrasonic sensor
		// 1. Create a port object attached to a physical port (done above)
		// 2. Create a sensor instance and attach to port
		// 3. Create a sample provider instance for the above and initialize operating mode
		// 4. Create a buffer for the sensor data
		@SuppressWarnings("resource")							    // Because we don't bother to close this resource
		SensorModes usSensor = new EV3UltrasonicSensor(usPort);		// usSensor is the instance
		SampleProvider usValue = usSensor.getMode("Distance");	// usDistance provides samples from this instance
		float[] usData = new float[usValue.sampleSize()];		// usData is the buffer in which data are returned

		//Setup color sensor
		// 1. Create a port object attached to a physical port (done above)
		// 2. Create a sensor instance and attach to port
		// 3. Create a sample provider instance for the above and initialize operating mode
		// 4. Create a buffer for the sensor data
		@SuppressWarnings("resource")
		SensorModes colorSensor = new EV3ColorSensor(colorPort);	// colorSensor is the instance
		SampleProvider colorValue = colorSensor.getMode("RGB");		// colorValue provides samples from this instance
		float[] colorData = new float[colorValue.sampleSize()];		// colorData is the buffer in which data are returned

		// Setup Ultrasonic Poller									// This thread samples the US and invokes
		USPoller usPoller = null;							// the selected controller on each cycle
		
		// some objects that need to be instantiated
		final TextLCD t = LocalEV3.get().getTextLCD();
		Odometer odo = new Odometer(leftMotor, rightMotor, 2.1, 15);
		OdometryDisplay odometryDisplay = new OdometryDisplay(odo,t);
		Navigation nav = new Navigation(odo);
		Search searcher = new Search(odo, nav, turner, hook, colorValue, colorData);
		usPoller = new USPoller(usValue, usData, searcher);
		
		
		do {
			// clear the display
			t.clear();

			// ask the user whether the motors should navigate or to Navigate with obstacles
			t.drawString("< 	Center    >", 0, 0);
			t.drawString("_________________", 0, 1);
			t.drawString("      	       ", 0, 2);
			t.drawString("  	 GAME	   ", 0, 3);
			t.drawString("  	START! 	   ", 0, 4);
			t.drawString("_________________", 0, 5);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT
				&& buttonChoice != Button.ID_RIGHT);

		if (buttonChoice == Button.ID_LEFT) {
			
			
		}else{
			
			// start the odometer, the odometry display
			odo.start();
			odometryDisplay.start();
			usPoller.start();
			searcher.start();
		
			//0.5sec delay
			Delay.msDelay(500);

			// perform the ultrasonic localization
			//USLocalizer usl = new USLocalizer(odo, nav, USLocalizer.LocalizationType.FALLING_EDGE, usPoller);
			//usl.doLocalization();
//			Victory(a);
			Delay.msDelay(1000);				
		}
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
	}
	
	// Victory sound!
	private static void Victory(int[] a){
		Sound.playNote(a, 440, 110);
		Sound.playNote(a, 587, 110);
		Sound.playNote(a, 740, 110);
		Sound.playNote(a, 880, 220);
		Sound.playNote(a, 740, 110);
		Sound.playNote(a, 880, 320);
	}
}
