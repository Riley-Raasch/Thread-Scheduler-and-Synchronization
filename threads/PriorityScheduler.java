package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

//ADDED
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMin &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMin)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static  int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static  int priorityMin = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static  int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
		//ADDED
		 ThreadState ts = getThreadState(thread);
		this.threadsWaiting.add(ts);
		ts.waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	//TODO:Implement nextThread
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // ADDED
             ThreadState nextThread = pickNextThread();

            if (nextThread == null) { 
            	return null;
            }

            threadsWaiting.remove(nextThread);
            acquire(nextThread.thread);
			ThreadState ts = getThreadState(nextThread.thread);
			if (resourceList != null) {
				resourceList.releaseAccess(this);
			}
			resourceList = ts;
			ts.acquire(this);
            return nextThread.thread;
			
	}

	//TODO: Implement pickNextThread
	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // ADDED
		ThreadState next = null;
		int nextPriority = priorityMin;

		for (ThreadState currThread : this.threadsWaiting) {
			int currPriority = currThread.getEffectivePriority();
			if ((nextPriority < currPriority) || next == null) {
				nextPriority = currPriority;
				next = currThread;
			}
		}
		return next;
	}

	//ADDED
	        /**
         * This method returns the effectivePriority of this PriorityQueue.
         * The return value is cached for as long as possible. If the cached value
         * has been invalidated, this method will spawn a series of mutually
         * recursive calls needed to recalculate effectivePriorities across the
         * entire resource graph.
         * @return
         */
        public int getEffectivePriority() {
            if (!transferPriority)
                return priorityMin;

                effectivePriority = priorityMin;
                for (ThreadState curr : threadsWaiting) {
                    effectivePriority = Math.max(
                    		effectivePriority, curr.getEffectivePriority()
                    		);
                }
                priorityChange = false;
				return effectivePriority;

        }

	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	//ADDED
	private void priorityCache() {
		if (!this.transferPriority) return;

		this.priorityChange = true;

		if (this.resourceList != null) {
			//resourceList.priorityCache();

			if (resourceList.priorityChange){
            	return;
            }
            resourceList.priorityChange = true;
            for ( PriorityQueue pq : resourceList.resourceWait) {
                pq.priorityCache();
            }
		}
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

	//ADDED
	protected  LinkedList<ThreadState> threadsWaiting = new LinkedList<ThreadState>();
	protected int effectivePriority = priorityMin;
	protected boolean priorityChange = false;
	protected ThreadState resourceList = null;

	
    }//END OF PRIORITY QUEUE




    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	//TODO: implement getEffectivePriority
	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // ADDED
		if (this.resources.isEmpty()) {
			return this.getPriority();
		} 
		else if (this.priorityChange) {
			this.effectivePriority = this.getPriority();
			for ( PriorityQueue pq : this.resources) {
				this.effectivePriority = Math.max(
						this.effectivePriority, pq.getEffectivePriority()
						);
			}
			this.priorityChange = false;
		}
		return this.effectivePriority;
	}

	//TODO: implement setPriority
	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    
	    // ADDED
		for ( PriorityQueue pq : resourceWait) {
			pq.priorityCache();
		}		
	}

	//TODO: implement waitForAccess
	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // ADDED
		this.resourceWait.add(waitQueue);
		this.resources.remove(waitQueue);
		waitQueue.priorityCache();
	}

	//TODO: implement acquire
	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
	    // ADDED
		this.resources.add(waitQueue);
		this.resourceWait.remove(waitQueue);

		if (this.priorityChange){ 
			return;
		}
		this.priorityChange = true;
		for ( PriorityQueue pq : this.resourceWait) {
			pq.priorityCache();
		}
	}	

	//release access
        public void releaseAccess(PriorityQueue waitQueue) {
            resources.remove(waitQueue);

			if (priorityChange){ 
            	return; 
            }
            priorityChange = true; 
            for ( PriorityQueue pq : resourceWait) {
                pq.priorityCache();
            }
        }

		
        protected  LinkedList<PriorityQueue> resourceWait = new LinkedList<PriorityQueue>();

        protected  LinkedList<PriorityQueue> resources = new LinkedList<PriorityQueue>();
        
		protected int effectivePriority = priorityMin;

        protected boolean priorityChange = false;
        
		//END ADDED

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    }
}