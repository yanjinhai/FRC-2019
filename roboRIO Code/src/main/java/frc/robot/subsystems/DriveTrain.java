package frc.robot.subsystems;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.command.Subsystem;
import frc.robot.*;
import edu.wpi.first. wpilibj.GenericHID.Hand;

/**
 * Drive train subsystem for the robot.
 */
public class DriveTrain extends Subsystem {
  // Put methods for controlling this subsystem
  // here. Call these from Commands.

  static Talon leftDriveMotor = RobotMap.driveMotor1;
  static Talon rightDriveMotor = RobotMap.driveMotor2;
  DifferentialDrive driveControl = new DifferentialDrive(leftDriveMotor, rightDriveMotor); 

  public void enableTankDrive(){
    //driveControl.tankDrive(Robot.oi.leftStick.getY(), Robot.oi.rightStick.getY()); 
  }

  public void enableArcadeDrive(){
    if(!Robot.oi.mainController.getAButton()){
      driveControl.arcadeDrive(Robot.oi.mainController.getY(Hand.kLeft), Robot.oi.mainController.getX(Hand.kRight));
    }else if (Robot.rectArea < 10000) {
      driveControl.arcadeDrive(-0.5, 0);
    }
  }

  @Override
  public void initDefaultCommand() {
    // Default command for the drive train.
    //setDefaultCommand(new TeleopDrive());
  }
}
