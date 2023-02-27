package nachos.threads;
import nachos.machine.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        Machine.interrupt().disable();	

        //inspect the list
    	if(!list.isEmpty()){
    		Enumeration wakeTimes = list.keys();
    		Enumeration threadsToWake = list.elements();

    		while(threadsToWake.hasMoreElements()){
    			Long wakeUpTime = (Long)wakeTimes.nextElement();
    			KThread thread = (KThread)threadsToWake.nextElement();

                //any thread that has wakeUpTime < currentTime, you need to wake up
    			if(wakeUpTime < Machine.timer().getTime()){
    				thread.ready();
    				list.remove(wakeUpTime);
    			}
    		}
    		Machine.interrupt().enable();
    	}
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) { 
    boolean status = Machine.interrupt().disable();	

    //The wakeup time should be the current time plus the time the threads need to sleep
	long wakeUpTime = Machine.timer().getTime() + x;

    //save wakeUpTime
    KThread threadPointer = KThread.currentThread();
	list.put(wakeUpTime, threadPointer);

    //put the thread to sleep
	threadPointer.sleep();
    
	Machine.interrupt().restore(status);
    }
    
    //list that saves at least a tuple <wakeUpTime, threadPointer>
    Hashtable<Long,KThread> list = new Hashtable<Long,KThread>();
}