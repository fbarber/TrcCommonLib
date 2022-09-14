/*
 * Copyright (c) 2015 Titan Robotics Club (http://www.titanrobotics.com)
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

package TrcCommonLib.trclib;

/**
 * This class implements a platform independent servo motor. Typically, this class is to be extended by a platform
 * dependent servo class. Whoever extends this class must provide a set of abstract methods. This makes sure the rest
 * of the TrcLib classes can access the servo without any knowledge of platform dependent implementations.
 */
public abstract class TrcServo
{
    protected static final String moduleName = "TrcServo";
    protected static final boolean debugEnabled = false;
    protected static final boolean tracingEnabled = false;
    protected static final boolean useGlobalTracer = false;
    protected static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    protected static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    protected TrcDbgTrace dbgTrace = null;

    /**
     * This abstract method inverts the servo motor direction.
     *
     * @param inverted specifies the servo direction is inverted if true.
     */
    public abstract void setInverted(boolean inverted);

    /**
     * This abstract method checks if the servo motor direction is inverted.
     *
     * @return true if the servo direction is inverted, false otherwise.
     */
    public abstract boolean isInverted();

    /**
     * This method sets the servo motor position. By default, the servo maps its physical position the same as its
     * logical position [0.0, 1.0]. However, if setPhysicalRange was called, it could map a real world physical
     * range (e.g. [0.0, 180.0] degrees) to the logical range of [0.0, 1.0].
     * <p>
     * Servo motor operates on logical position. On a 180-degree servo, 0.0 is at 0-degree and 1.0 is at 180-degree.
     * For a 90-degree servo, 0->0deg, 1->90deg. If servo direction is inverted, then 0.0 is at 180-degree and 1.0 is
     * at 0-degree. On a continuous servo, 0.0 is rotating full speed in reverse, 0.5 is to stop the motor and 1.0 is
     * rotating the motor full speed forward. Again, motor direction can be inverted if setInverted is called.
     *
     * @param position specifies the physical position of the servo motor. This value may be in degrees if
     *                 setPhysicalRange is called with the degree range.
     */
    public abstract void setPosition(double position);

    /**
     * This method returns the physical position value set by the last setPosition call. Note that servo motors do not
     * provide real time position feedback. So getPosition doesn't actually return the current position.
     *
     * @return motor position value set by the last setPosition call.
     */
    public abstract double getPosition();

    public static final double CONTINUOUS_SERVO_FORWARD_MAX = 1.0;
    public static final double CONTINUOUS_SERVO_REVERSE_MAX = 0.0;
    public static final double CONTINUOUS_SERVO_STOP        = 0.5;

    private static final double DEF_PHYSICAL_MIN    = 0.0;
    private static final double DEF_PHYSICAL_MAX    = 1.0;
    private static final double DEF_LOGICAL_MIN     = 0.0;
    private static final double DEF_LOGICAL_MAX     = 1.0;
    protected static TrcElapsedTimer servoSetPosElapsedTimer = null;

    private final String instanceName;
    private TrcTimer timer;
    private double physicalMin = DEF_PHYSICAL_MIN;
    private double physicalMax = DEF_PHYSICAL_MAX;
    private double logicalMin = DEF_LOGICAL_MIN;
    private double logicalMax = DEF_LOGICAL_MAX;

    /**
     * Constructor: Creates an instance of the object.
     *
     * @param instanceName specifies the instance name of the servo.
     */
    public TrcServo(final String instanceName)
    {
        if (debugEnabled)
        {
            dbgTrace = useGlobalTracer?
                TrcDbgTrace.getGlobalTracer():
                new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }

        this.instanceName = instanceName;
        timer = new TrcTimer(instanceName);
    }   //TrcServo

    /**
     * This method returns the instance name.
     *
     * @return instance name.
     */
    @Override
    public String toString()
    {
        return instanceName;
    }   //toString

    /**
     * This method enables/disables the elapsed timers for performance monitoring.
     *
     * @param enabled specifies true to enable elapsed timers, false to disable.
     */
    public static void setElapsedTimerEnabled(boolean enabled)
    {
        if (enabled)
        {
            if (servoSetPosElapsedTimer == null)
            {
                servoSetPosElapsedTimer = new TrcElapsedTimer("TrcServo.setPos", 2.0);
            }
        }
        else
        {
            servoSetPosElapsedTimer = null;
        }
    }   //setElapsedTimerEnabled

    /**
     * This method prints the elapsed time info using the given tracer.
     *
     * @param tracer specifies the tracer to use for printing elapsed time info.
     */
    public static void printElapsedTime(TrcDbgTrace tracer)
    {
        if (servoSetPosElapsedTimer != null)
        {
            servoSetPosElapsedTimer.printElapsedTime(tracer);
        }
    }   //printElapsedTime

    /**
     * This method returns the current physical position of the servo as read by an encoder.
     * If there is no encoder (depending on the implementation) it will throw an exception.
     *
     * @return the physical position of the mechanism with an encoder.
     * @throws UnsupportedOperationException if not supported by TrcServo implementation.
     */
    public double getEncoderPosition()
    {
        throw new UnsupportedOperationException("This implementation does not have an encoder!");
    }    //getEncoderPosition

    /**
     * This method sets the servo motor position. If a notifier is given, it calls the notifier after the given amount
     * of time has passed.
     *
     * @param position specifies the physical position of the servo motor. This value may be in degrees if
     *                 setPhysicalRange is called with the degree range.
     * @param timeout specifies a maximum time value the operation should be completed in seconds.
     * @param notifier specifies a notifier to be notified when the timeout event has expired.
     */
    public void setPosition(double position, double timeout, TrcNotifier.Receiver notifier)
    {
        setPosition(position);
        if (notifier != null)
        {
            timer.set(timeout, notifier);
        }
    }   //setPosition

    /**
     * This method sets the servo motor position. If an event is given, it sets an event after the given amount of
     * time has passed.
     *
     * @param position specifies the physical position of the servo motor. This value may be in degrees if
     *                 setPhysicalRange is called with the degree range.
     * @param timeout specifies a maximum time value the operation should be completed in seconds.
     * @param event specifies an event object to signal when the timeout event has expired.
     */
    public void setPosition(double position, double timeout, TrcEvent event)
    {
        setPosition(position);
        if (event != null)
        {
            timer.set(timeout, event);
        }
    }   //setPosition

    /**
     * This method sets the physical range of the servo motor. This is typically
     * used to set a 180-degree servo to have a range of 0.0 to 180.0 instead of
     * the logical range of 0.0 to 1.0.
     *
     * @param physicalMin specifies the minimum value of the physical range.
     * @param physicalMax specifies the maximum value of the physical range.
     */
    public void setPhysicalRange(double physicalMin, double physicalMax)
    {
        final String funcName = "setPhysicalRange";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "phyMin=%f,phyMax=%f", physicalMin, physicalMax);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        if (physicalMin >= physicalMax)
        {
            throw new IllegalArgumentException("max must be greater than min.");
        }

        this.physicalMin = physicalMin;
        this.physicalMax = physicalMax;
    }   //setPhysicalRange

    /**
     * This method sets the logical range of the servo motor. This is typically used to limit the logical range
     * of the servo to less than the 0.0 to 1.0 range. For example, one may limit the logical range to 0.2 to 0.8.
     *
     * @param logicalMin specifies the minimum value of the logical range.
     * @param logicalMax specifies the maximum value of the logical range.
     */
    public void setLogicalRange(double logicalMin, double logicalMax)
    {
        final String funcName = "setLogicalRange";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API,
                                "logicalMin=%f,logicalMax=%f", logicalMin, logicalMax);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        if (logicalMin >= logicalMax)
        {
            throw new IllegalArgumentException("max must be greater than min.");
        }

        this.logicalMin = logicalMin;
        this.logicalMax = logicalMax;
    }   //setLogicalRange

    /**
     * This method is called to convert a physical position to a logical position. It will make sure the physical
     * position is within the physical range and scale it to the logical range. Note: this method is only callable
     * by classes extending this class.
     *
     * @param physicalPosition specifies the physical position to be converted
     * @return converted logical position.
     */
    protected double toLogicalPosition(double physicalPosition)
    {
        final String funcName = "toLogicalPosition";
        physicalPosition = TrcUtil.clipRange(physicalPosition, physicalMin, physicalMax);
        double logicalPosition = TrcUtil.scaleRange(physicalPosition, physicalMin, physicalMax, logicalMin, logicalMax);

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.FUNC, "phyPos=%f", physicalPosition);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.FUNC, "=%f", logicalPosition);
        }

        return logicalPosition;
    }   //toLogicalPosition

    /**
     * This method is called to convert a logical position to a physical position.
     * It will make sure the logical position is within the logical range and scale
     * it to the physical range.
     * Note: this method is only callable by classes extending this class.
     *
     * @param logicalPosition specifies the logical position to be converted.
     * @return converted physical position.
     */
    protected double toPhysicalPosition(double logicalPosition)
    {
        final String funcName = "toPhysicalPosition";

        logicalPosition = TrcUtil.clipRange(logicalPosition, logicalMin, logicalMax);
        double physicalPosition = TrcUtil.scaleRange(logicalPosition, logicalMin, logicalMax, physicalMin, physicalMax);

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.FUNC, "logPos=%f", logicalPosition);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.FUNC, "=%f", physicalPosition);
        }

        return physicalPosition;
    }   //toPhysicalPosition

}   //class TrcServo
