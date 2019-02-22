package frc.robot;

import java.util.*;

import edu.wpi.first.wpilibj.smartdashboard.*;
import edu.wpi.first.wpilibj.*;
import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.command.*;
import edu.wpi.first.wpilibj.GenericHID.Hand;

import frc.robot.subsystems.*;

import com.ctre.phoenix.motorcontrol.*;

import frc.frc2019grip.*;

import org.opencv.core.*;
import org.opencv.imgproc.*;
import edu.wpi.first.wpilibj.vision.VisionThread;
import org.opencv.core.Rect;

/** ss, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
@SuppressWarnings ("deprecation")
public class Robot extends TimedRobot {
  public static DriveTrain driveTrain;
  public static HatchPlacer hatchPlacer;
  public static Intake intake;
  public static LiftSystem liftSystem;
  public static RaiseTheRobot raiseTheRobot;

  public static int heightID;
  public static int height = 0;
  public static boolean hatchPlacerExtended = false;
  public static boolean processVision = true;// manually set value.

  public static OI oi;
  //public static boolean autoPilotArm = false;
  //static boolean heightIncReq = false;
  //static boolean heightDecReq = false;
  public static int[] heights = {
    0, 1500, 7500, 10500, 16500, 19500, 25400,
  };

  private static final int IMG_WIDTH = 640;
	private static final int IMG_HEIGHT = 480;
	
  private VisionThread visionThread;
  private final Object imgLock = new Object();
  public static long rectArea;
  public static long rectArea2;

  static Compressor compressor = new Compressor(0);
  static UsbCamera cam;
  static UsbCamera cam2;

  //Command m_autonomousCommand;
  //SendableChooser<Command> m_chooser = new SendableChooser<>();

  /**
   * This function is run when the robot is first started up and should be
   * used for any initialization code.
   */
  @Override
  public void robotInit() {
    heightID = 0;
    oi = new OI();
    driveTrain = new DriveTrain();
    hatchPlacer = new HatchPlacer();
    intake = new Intake();
    raiseTheRobot = new RaiseTheRobot();
    liftSystem = new LiftSystem();

    SmartDashboard.putNumber("HeightID", heightID);

    cam = CameraServer.getInstance().startAutomaticCapture();
    cam2 = CameraServer.getInstance().startAutomaticCapture();
    if(processVision){
    CvSink cvSink = CameraServer.getInstance().getVideo();
    CvSource outputStream = CameraServer.getInstance().putVideo("Processed", IMG_WIDTH, IMG_HEIGHT);
    cam.setResolution(IMG_WIDTH, IMG_HEIGHT);
    cam.setBrightness(50);
    Mat source = new Mat();
    Mat output = new Mat();
    visionThread = new VisionThread(cam, new GripPipeline(), pipeline -> {
      cvSink.grabFrame(source);
      Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2RGB);
      ArrayList<MatOfPoint> contours = pipeline.filterContoursOutput();
      if (!contours.isEmpty()) {
          //SmartDashboard.putNumber("Contour Area", (r.width * r.height));
          for(int i = 0; i < contours.size(); i++){
            Rect r = Imgproc.boundingRect(contours.get(i));
            Imgproc.rectangle(output, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 0, 255), 5);
          }
          synchronized (imgLock) {
            Rect r2 = Imgproc.boundingRect(contours.get(0));
            rectArea = r2.width * r2.height;
            SmartDashboard.putNumber("rectArea 1", rectArea);

            if(contours.size()>1) {
              Rect r3 = Imgproc.boundingRect(contours.get(1));
              rectArea2 = r3.width * r3.height;
              SmartDashboard.putNumber("rectArea 2", rectArea2);
            }
          }
      }
      outputStream.putFrame(output);
    });
    visionThread.start();
    }
    LiftSystem.liftMotor.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder);
    LiftSystem.liftMotor.configNominalOutputForward(0, 30);
    LiftSystem.liftMotor.configNominalOutputReverse(0, 30);
    LiftSystem.liftMotor.configPeakOutputForward(1, 30);
    LiftSystem.liftMotor.configPeakOutputReverse(-1, 30);
    LiftSystem.liftMotor.configMotionCruiseVelocity(15000, 30);
    LiftSystem.liftMotor.configMotionAcceleration(6000, 30);
    LiftSystem.liftMotor.selectProfileSlot(0, 0);
    LiftSystem.liftMotor.config_kF(0, 0.2, 30);
    LiftSystem.liftMotor.config_kP(0, 0.2, 30);
    LiftSystem.liftMotor.config_kI(0, 0.0, 30);
    LiftSystem.liftMotor.config_kD(0, 0.0, 30);

    compressor.setClosedLoopControl(true);
    //m_chooser.setDefaultOption("Default Auto", new ExampleCommand());
    // chooser.addOption("My Auto", new MyAutoCommand());
    //SmartDashboard.putData("Auto mode", m_chooser);
  }

  /**
   * This function is called every robot packet, no matter the mode. Use
   * this for items like diagnostics that you want ran during disabled,
   * autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before
   * LiveWindow and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    // Drive
    Robot.driveTrain.drive();
    // Hatch placer
    if(oi.mainController.getYButtonPressed()){
      if(hatchPlacerExtended) {
        hatchPlacer.retract();
        hatchPlacerExtended = false;
      }else{
        hatchPlacer.extend();
        hatchPlacerExtended = true;
      }
    }
    // Lift to specific height
    // if(oi.mainController.getPOV(0) == 0 && heightID < 6){
    //   heightIncReq = true;
    // } else if(heightIncReq){
    //   heightID++;
    //   LiftSystem.liftMotor.set(ControlMode.MotionMagic, heights[heightID]);
    //   heightIncReq = false;
    //   autoPilotArm = true;
    // }else if(oi.mainController.getPOV(0) == 180 && heightID > 0){
    //   heightDecReq = true;
    // }else if(heightDecReq){
    //   heightID--;
    //   LiftSystem.liftMotor.set(ControlMode.MotionMagic, heights[heightID]);
    //   heightDecReq = false;
    //   autoPilotArm = true;
    // } else // Normal lift
    // if(oi.mainController.getBumper(Hand.kLeft)){
    //   liftSystem.moveDown();
    //   autoPilotArm = false;
    // }else if(oi.mainController.getBumper(Hand.kRight)){
    //   autoPilotArm = false;
    //   liftSystem.moveUp();
    // }else if (!autoPilotArm){
    //   liftSystem.reset();
    // }
    if(oi.mainController.getBumperPressed(Hand.kRight)){
      if(heightID < heights.length - 1)  {
        heightID++;
      }
    }else if(oi.mainController.getBumperPressed(Hand.kLeft)){
      if(heightID > 0) {
        heightID--;
      }
    }
    LiftSystem.liftMotor.set(ControlMode.MotionMagic, heights[heightID]);

    if (oi.mainController.getPOV() == 180) {
      LiftSystem.liftMotor.set(ControlMode.PercentOutput, -1);  
    }
    if (oi.mainController.getPOV() == 0) {
      LiftSystem.liftMotor.set(ControlMode.PercentOutput, 1);  
    }
    if(LiftSystem.liftSensors.isRevLimitSwitchClosed()){
      LiftSystem.liftSensors.setQuadraturePosition(0, 500);
      System.out.println("**** RESET ZERO ****");
    }

    // Intake
    if(oi.mainController.getTriggerAxis(Hand.kLeft) > 0.3){
      intake.pullInBall();
    }else if(oi.mainController.getTriggerAxis(Hand.kRight) > 0.3){
      intake.yeetOutBall();
    }else{
      intake.setIdle();
    }
    // Reset encoder at home position to eliminate error
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable
   * chooser code works with the Java SmartDashboard. If you prefer the
   * LabVIEW Dashboard, remove all of the chooser code and uncomment the
   * getString code to get the auto name from the text box below the Gyro
   *
   * <p>You can add additional auto modes by adding additional commands to the
   * chooser code above (like the commented example) or additional comparisons
   * to the switch structure below with additional strings & commands.
   */
  @Override
  public void autonomousInit() {
    //m_autonomousCommand = m_chooser.getSelected();

    /*
     * String autoSelected = SmartDashboard.getString("Auto Selector",
     * "Default"); switch(autoSelected) { case "My Auto": autonomousCommand
     * = new MyAutoCommand(); break; case "Default Auto": default:
     * autonomousCommand = new ExampleCommand(); break; }
     */

    // schedule the autonomous command (example)
    //if (m_autonomousCommand != null) {
      //m_autonomousCommand.start();
    //}
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    // Display on lift motor position & velocity on dashboard.
    SmartDashboard.putNumber("Arm Encoder Position", LiftSystem.liftSensors.getQuadraturePosition());
    SmartDashboard.putNumber("Arm Encoder Velocity", LiftSystem.liftSensors.getQuadratureVelocity());
    SmartDashboard.putBoolean("Upper Arm Limit Switch", LiftSystem.liftSensors.isFwdLimitSwitchClosed());
    SmartDashboard.putBoolean("Lower Arm Limit Switch", LiftSystem.liftSensors.isRevLimitSwitchClosed());
    SmartDashboard.putNumber("HeightID", heightID);
    Scheduler.getInstance().run();
  }

  @Override
  public void teleopInit() {
    // This makes sure that the autonomous stops running when
    // teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove
    // this line or comment it out.
    //if (m_autonomousCommand != null) {
      //m_autonomousCommand.cancel();
    //}
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {
    // Display on lift motor position & velocity on dashboard.
    SmartDashboard.putNumber("Arm Encoder Position", LiftSystem.liftSensors.getQuadraturePosition());
    SmartDashboard.putNumber("Arm Encoder Velocity", LiftSystem.liftSensors.getQuadratureVelocity());
    SmartDashboard.putBoolean("Upper Arm Limit Switch", LiftSystem.liftSensors.isFwdLimitSwitchClosed());
    SmartDashboard.putBoolean("Lower Arm Limit Switch", LiftSystem.liftSensors.isRevLimitSwitchClosed());
    SmartDashboard.putNumber("HeightID", heightID);
    Scheduler.getInstance().run();
  }

  /**
   * This function is called once each time the robot enters Disabled mode.
   * You can use it to reset any subsystem information you want to clear when
   * the robot is disabled.
   */
  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
    Scheduler.getInstance().run();
  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }
}
