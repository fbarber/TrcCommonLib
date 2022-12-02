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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class implements the TrcEvent. TrcEvent is very important in our event driven asynchronous architecture where
 * things only happen when an event is signaled.
 */
public class TrcEvent
{
    private static final TrcDbgTrace globalTracer = TrcDbgTrace.getGlobalTracer();
    private static final boolean debugEnabled = false;

    /**
     * An event has three possible states:
     *  - CLEARED: event should be in this state before starting an asynchronous operation. This is also the default
     *             state when an event is created.
     *  - SIGNALED: when an asynchronous operation is completed, the event is set to this state.
     *  - CANCELED: when an asynchronous operation is canceled, the event is set to this state.
     */
    public enum EventState
    {
        CLEARED,
        SIGNALED,
        CANCELED
    }   //enum EventState

    private final String instanceName;
    private final AtomicReference<EventState> eventState = new AtomicReference<>(EventState.CLEARED);

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param state specifies the initial state of the event.
     */
    public TrcEvent(String instanceName, EventState state)
    {
        this.instanceName = instanceName;
        eventState.set(state);
    }   //TrcEvent

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     */
    public TrcEvent(String instanceName)
    {
        this(instanceName, EventState.CLEARED);
    }   //TrcEvent

    /**
     * This method returns the instance name.
     *
     * @return instance name.
     */
    @Override
    public String toString()
    {
        return String.format("(%s=%s)", instanceName, eventState.get());
    }   //toString

    /**
     * This method clears an event.
     */
    public void clear()
    {
        eventState.set(EventState.CLEARED);
    }   //clear

    /**
     * This method signals an event if it is not canceled.
     */
    public void signal()
    {
        eventState.compareAndSet(EventState.CLEARED, EventState.SIGNALED);
    }   //signal

    /**
     * This method cancels an event if it is not already signaled. An event is either signaled or canceled by the
     * event source either of which will cause whoever is waiting for it to move on.
     */
    public void cancel()
    {
        eventState.compareAndSet(EventState.CLEARED, EventState.CANCELED);
    }   //cancel

    /**
     * This method checks if the event is signaled.
     *
     * @return true if the event is signaled, false otherwise.
     */
    public boolean isSignaled()
    {
        return eventState.get() == EventState.SIGNALED;
    }   //isSignaled

    /**
     * This method checks if the event was canceled.
     *
     * @return true if the event was canceled, false otherwise.
     */
    public boolean isCanceled()
    {
        return eventState.get() == EventState.CANCELED;
    }   //isCanceled

    //
    // Callback Management.
    //

    /**
     * This interface is implemented by the caller so that it can be notified when the event is signaled.
     */
    public interface Callback
    {
        void notify(Object context);
    }   //interface Callback

    private static final HashMap<Thread, ArrayList<TrcEvent>> callbackEventListMap = new HashMap<>();
    private Callback callback;
    private Object callbackContext;

    /**
     * This method sets a callback handler so that when the event is signaled, the callback handler is called on
     * the same thread as original caller. This could be very useful if the caller wants to perform some minor actions
     * after an asynchronous operation is completed. Without the callback, the caller would have to set up a state
     * machine waiting for the event to signal, then perform the action. Since the callback is done on the same
     * thread, the caller doesn't have to worry about thread safety. Note: this method is called by another thread
     * on behalf of the original caller.
     *
     * @param thread specifies the thread of the original caller to do the callback.
     * @param callback specifies the callback handler, null for removing previous callback handler.
     * @param callbackContext specifies the context object passing back to the callback handler.
     */
    public void setCallback(Thread thread, Callback callback, Object callbackContext)
    {
        final String funcName = "setCallback";
        ArrayList<TrcEvent> eventList;

        clear();
        synchronized (callbackEventListMap)
        {
            eventList = callbackEventListMap.get(thread);
        }

        // eventList is only accessed by the thread that created it, so it doesn't need thread-safe protection.
        if (eventList != null)
        {
            boolean inList = eventList.contains(this);

            this.callback = callback;
            this.callbackContext = callbackContext;
            if (callback != null && !inList)
            {
                // Callback handler is not already in the callback list, add it.
                eventList.add(this);
                if (debugEnabled)
                {
                    globalTracer.traceInfo(
                        funcName, "Adding event %s to the callback list for thread %s.",
                        instanceName, thread.getName());
                }
            }
            else if (callback == null && inList)
            {
                // Remove the callback from the list.
                eventList.remove(this);
                this.callbackContext = null;
                if (debugEnabled)
                {
                    globalTracer.traceInfo(
                        funcName, "Removing event %s from the callback list for thread %s.",
                        instanceName, thread.getName());
                }
            }
        }
        else
        {
            globalTracer.traceWarn(funcName, "Thread %s is not registered.", thread.getName());
            TrcDbgTrace.printThreadStack();
        }
    }   //setCallback

    /**
     * This method sets a callback handler so that when the event is signaled, the callback handler is called on
     * the same thread as this call. This could be very useful if the caller wants to perform some minor actions
     * after an asynchronous operation is completed. Without the callback, the caller would have to set up a state
     * machine waiting for the event to signal, then perform the action. Since the callback is done on the same
     * thread, the caller doesn't have to worry about thread safety.
     *
     * @param callback specifies the callback handler, null for removing previous callback handler.
     * @param callbackContext specifies the context object passing back to the callback handler.
     */
    public void setCallback(Callback callback, Object callbackContext)
    {
        setCallback(Thread.currentThread(), callback, callbackContext);
    }   //setCallback

    /**
     * This method is called by a periodic thread when the thread has just been started and before it enters its
     * thread loop to register for event callback. When a callback handler is set for an event, the event is added
     * to the event list for the thread. The periodic thread will then periodically call checkForEventCallback
     * to check if any events in the list are signaled or canceled. When that happens, the callback will be performed
     * and the event will be removed from the event list.
     *
     * @return true if registration was successful, false if the thread has already registered an event list before.
     */
    public static boolean registerEventCallback()
    {
        final String funcName = "registerEventCallback";
        final Thread thread = Thread.currentThread();
        boolean alreadyRegistered;

        synchronized (callbackEventListMap)
        {
            alreadyRegistered = callbackEventListMap.containsKey(thread);

            if (!alreadyRegistered)
            {
                callbackEventListMap.put(thread, new ArrayList<>());
                if (debugEnabled)
                {
                    globalTracer.traceInfo(funcName, "Registering thread %s for event callback.", thread.getName());
                }
            }
            else
            {
                globalTracer.traceWarn(funcName, "Thread %s is already registered.", thread.getName());
                TrcDbgTrace.printThreadStack();
            }
        }

        return !alreadyRegistered;
    }   //registerEventCallback

    /**
     * This method is called by a periodic thread when the thread has exited its thread loop and before it is
     * terminated to unregister its thread from event callback.
     *
     * @return true if unregister is successful, false if the thread was never registered.
     */
    public static boolean unregisterEventCallback()
    {
        final String funcName = "unregisterEventCallback";
        final Thread thread = Thread.currentThread();
        ArrayList<TrcEvent> callbackEventList;

        synchronized (callbackEventListMap)
        {
            callbackEventList = callbackEventListMap.remove(thread);
        }

        if (callbackEventList == null)
        {
            globalTracer.traceWarn(funcName, "Thread %s was never registered.", thread.getName());
            TrcDbgTrace.printThreadStack();
        }
        else if (debugEnabled)
        {
            globalTracer.traceInfo(funcName, "Unregistering thread %s for event callback.", thread.getName());
        }

        return callbackEventList != null;
    }   //unregisterEventCallback

    /**
     * This method is called by a periodic thread in its thread loop to check if any events in the list are signaled
     * or canceled. When that happens, it performs the event callback on the periodic thread.
     */
    public static void performEventCallback()
    {
        final String funcName = "performEventCallback";
        final Thread thread = Thread.currentThread();
        ArrayList<TrcEvent> callbackEventList;

        synchronized (callbackEventListMap)
        {
            callbackEventList = callbackEventListMap.get(thread);
        }

        if (callbackEventList != null)
        {
            // Use a list Iterator because it is fail-fast and allows removing entries while iterating the list.
//            Iterator<TrcEvent> listIterator = callbackEventList.iterator();
//            while (listIterator.hasNext())
            for (int i = callbackEventList.size() - 1; i >= 0; i--)
            {
//                TrcEvent event = listIterator.next();
                TrcEvent event = callbackEventList.get(i);
                if (event.isSignaled() || event.isCanceled())
                {
                    if (debugEnabled)
                    {
                        globalTracer.traceInfo(
                            funcName, "Doing event callback for %s on thread %s.", event, thread.getName());
                    }

                    Callback callback = event.callback;
                    Object context = event.callbackContext;
                    // Clear the callback stuff before doing the callback since the callback may reuse and chain
                    // to another callback.
                    event.callback = null;
                    event.callbackContext = null;
                    callback.notify(context);
//                    listIterator.remove();
                    callbackEventList.remove(i);
                }
            }
        }
        else
        {
            globalTracer.traceWarn(funcName, "%s was never registered.", thread.getName());
            TrcDbgTrace.printThreadStack();
        }
    }   //performEventCallback

}   //class TrcEvent
