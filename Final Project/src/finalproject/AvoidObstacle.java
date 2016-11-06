/* DPM Team 12
 * 
 */
package finalproject;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class AvoidObstacle extends Thread {
	
	//Variable declarations/initializations
	private final int motorHigh = 200;
	private final int motorLow = 100;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	
	private Search searcher;

	private double WHEEL_RADIUS;
	private double TRACK;
	private boolean safe;
	
	//constructor
	public AvoidObstacle(Search searcher) {
		this.searcher = searcher;
		this.safe = false;
		this.leftMotor = searcher.leftMotor;
		this.rightMotor = searcher.rightMotor;
		this.WHEEL_RADIUS = searcher.WHEEL_RADIUS;
		this.TRACK = searcher.TRACK;
	}
	
	//obstacle avoidance - get around the blocks
	public void run(){
	}

	//check if robot is 90 degrees past the initial angle
	//it would mean that the robot made its way around the obstacle
	public boolean isSafe(){
		if(true){ //some condition to check if robot is safe
			return true;
		}else 
			return false;
	}
	
	//returns boolean safe
	public boolean resolved(){
		return safe;
	}
	
	public void forward(int dist){
		leftMotor.setSpeed(150+1);
		rightMotor.setSpeed(150);

		leftMotor.rotate(convertDistance(WHEEL_RADIUS, dist), true);
		rightMotor.rotate(convertDistance(WHEEL_RADIUS, dist), false);
	}

	
	
	//Turning method
	public void turn(double theta){
		//LCD.drawString("Turn: "+Double.toString(theta), 0, 7);
		leftMotor.setSpeed(50);
		rightMotor.setSpeed(50);
		leftMotor.rotate(convertAngle(2.125, 15.6, theta), true);
		rightMotor.rotate(-convertAngle(2.125, 15.6, theta), false);
	}
	
	//convert distance to the angle of rotation of the wheels in degrees
	public int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}
	
	//convert cart angle to the angle of rotation of the wheels in degrees
	public int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
	
}