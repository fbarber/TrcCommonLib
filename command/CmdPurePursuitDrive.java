/*
 * Copyright (c) 2019 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package TrcCommonLib.command;

import TrcCommonLib.trclib.TrcDriveBase;
import TrcCommonLib.trclib.TrcEvent;
import TrcCommonLib.trclib.TrcHolonomicPurePursuitDrive;
import TrcCommonLib.trclib.TrcPath;
import TrcCommonLib.trclib.TrcPathBuilder;
import TrcCommonLib.trclib.TrcPidController;
import TrcCommonLib.trclib.TrcPose2D;
import TrcCommonLib.trclib.TrcRobot;

/**
 * This class implements a generic Pure Pursuit Drive command. It allows the caller to specify the drive path by
 * calling start with an array of Waypoint poses.
 */
public class CmdPurePursuitDrive implements TrcRobot.RobotCommand
{
    private static final double DEF_FOLLOWING_DISTANCE = 6.0;
    private static final double DEF_POS_TOLERANCE = 3.0;
    private static final double DEF_TURN_TOLERANCE = 2.0;

    private final TrcHolonomicPurePursuitDrive purePursuitDrive;
    private final TrcEvent event;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param driveBase specifies the drive base object.
     * @param followingDistance specifies the following distance.
     * @param posTolerance specifies the position tolerance
     * @param turnTolerance specifies the turn tolerance.
     * @param posPidCoeff specifies the PID coefficients for position control.
     * @param turnPidCoeff specifies the PID coefficients for turn control.
     * @param velPidCoeff specifies the PID coefficients for velocity control.
     */
    public CmdPurePursuitDrive(
            TrcDriveBase driveBase, double followingDistance, double posTolerance, double turnTolerance,
            TrcPidController.PidCoefficients posPidCoeff, TrcPidController.PidCoefficients turnPidCoeff,
            TrcPidController.PidCoefficients velPidCoeff)
    {
        purePursuitDrive = new TrcHolonomicPurePursuitDrive(
                "PurePursuitDrive", driveBase, followingDistance, posTolerance, turnTolerance,
                posPidCoeff, turnPidCoeff, velPidCoeff);
        event = new TrcEvent("event");
    }   //CmdPurePursuitDrive

    /**
     * Constructor: Create an instance of the object.
     *
     * @param driveBase specifies the drive base object.
     * @param posPidCoeff specifies the PID coefficients for position control.
     * @param turnPidCoeff specifies the PID coefficients for turn control.
     * @param velPidCoeff specifies the PID coefficients for velocity control.
     */
    public CmdPurePursuitDrive(
            TrcDriveBase driveBase, TrcPidController.PidCoefficients posPidCoeff,
            TrcPidController.PidCoefficients turnPidCoeff, TrcPidController.PidCoefficients velPidCoeff)
    {
        this(driveBase, DEF_FOLLOWING_DISTANCE, DEF_POS_TOLERANCE, DEF_TURN_TOLERANCE,
             posPidCoeff, turnPidCoeff, velPidCoeff);
    }   //CmdPurePursuitDrive

    /**
     * This method starts the Pure Pursuit drive with the specified drive path.
     *
     * @param timeout specifies the maximum time allowed for this operation.
     * @param path specifies the drive path with waypoints.
     */
    public void start(double timeout, TrcPath path)
    {
        purePursuitDrive.start(path, event, timeout);
    }   //start

    /**
     * This method starts the Pure Pursuit drive with the specified drive path.
     *
     * @param path specifies the drive path with waypoints.
     */
    public void start(TrcPath path)
    {
        purePursuitDrive.start(path, event, 0.0);
    }   //start

    /**
     * This method starts the Pure Pursuit drive with the specified poses in the drive path.
     *
     * @param timeout specifies the maximum time allowed for this operation.
     * @param startingPose specifies the starting pose at the beginning of the path.
     * @param incrementalPath specifies true if appending point is relative to the previous point in the path,
     *                        false if appending point is in the same reference frame as startingPose.
     * @param poses specifies an array of waypoint poses in the drive path.
     */
    public void start(double timeout, TrcPose2D startingPose, boolean incrementalPath, TrcPose2D... poses)
    {
        TrcPathBuilder pathBuilder = new TrcPathBuilder(startingPose, incrementalPath);

        for (TrcPose2D pose: poses)
        {
            pathBuilder.append(pose);
        }

        purePursuitDrive.start(pathBuilder.toRelativeStartPath(), event, timeout);
    }   //start

    /**
     * This method starts the Pure Pursuit drive with the specified poses in the drive path.
     *
     * @param startingPose specifies the starting pose at the beginning of the path.
     * @param incrementalPath specifies true if appending point is relative to the previous point in the path,
     *                        false if appending point is in the same reference frame as startingPose.
     * @param poses specifies an array of waypoint poses in the drive path.
     */
    public void start(TrcPose2D startingPose, boolean incrementalPath, TrcPose2D... poses)
    {
        start(0.0, startingPose, incrementalPath, poses);
    }   //start

    //
    // Implements the TrcRobot.RobotCommand interface.
    //

    /**
     * This method checks if the current RobotCommand  is running.
     *
     * @return true if the command is running, false otherwise.
     */
    @Override
    public boolean isActive()
    {
        return purePursuitDrive.isActive();
    }   //isActive

    /**
     * This method cancels the command if it is active.
     */
    @Override
    public void cancel()
    {
        purePursuitDrive.cancel();
    }   //cancel

    /**
     * This method must be called periodically by the caller to drive the command sequence forward.
     *
     * @param elapsedTime specifies the elapsed time in seconds since the start of the robot mode.
     * @return true if the command sequence is completed, false otherwise.
     */
    @Override
    public boolean cmdPeriodic(double elapsedTime)
    {
        return event.isSignaled();
    }   //cmdPeriodic

}   //CmdPurePursuitDrive
