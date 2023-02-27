package nachos.threads;
import nachos.machine.*;
import java.util.LinkedList;


/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        //interrupt thread
        boolean status = Machine.interrupt().disable();
        
        conditionLock.release();

        //add thread to sleep queue
        sleepQueue.addFirst(KThread.currentThread());
        KThread.sleep();

        conditionLock.acquire();
        
        //restore interrupted thread
        Machine.interrupt().restore(status);	
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        //interrupt thread
        boolean status = Machine.interrupt().disable();
        
        //wake thread if there are threads in the sleepQueue
        if(sleepQueue.size() >= 1){
            sleepQueue.removeLast().ready();	
        }

        Machine.interrupt().restore(status);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        while(sleepQueue.size() >= 1){
            wake();
        }
    }

    Lock conditionLock;
    //threads sleeping on condition
    LinkedList<KThread> sleepQueue = new LinkedList<KThread>();
}
