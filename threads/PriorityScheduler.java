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
		       
	Lib.assertTrue(priority >= priorityMinimum &&
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
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

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
		// //ADDED
		threadsWaiting = new LinkedList<ThreadState>();
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
		//ADDED
		final ThreadState ts = getThreadState(thread);
		this.threadsWaiting.add(ts);
		ts.waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
		//ADDED
		final ThreadState ts = getThreadState(thread);
		if (resourceHolder != null) {
			resourceHolder.release(this);
		}
		resourceHolder = ts;
		ts.acquire(this);
	}

	//TODO:Implement nextThread
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // ADDED
            final ThreadState nextThread = pickNextThread();

            if (nextThread == null) { 
            	return null;
            }
			else{
            threadsWaiting.remove(nextThread);
            acquire(nextThread.getThread());
            return nextThread.getThread();
			}
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
		int nextPriority = priorityMinimum;
		ThreadState next = null;
		for (final ThreadState currThread : this.threadsWaiting) {
			int currPriority = currThread.getEffectivePriority();
			if (next == null || (currPriority > nextPriority)) {
				next = currThread;
				nextPriority = currPriority;
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
            if (!this.transferPriority) {
                return priorityMinimum;
            } else if (this.priorityChange) {
                // Recalculate effective priorities
                this.effectivePriority = priorityMinimum;
                for (final ThreadState curr : this.threadsWaiting) {
                    this.effectivePriority = Math.max(
                    		this.effectivePriority, curr.getEffectivePriority()
                    		);
                }//end of for loop
                this.priorityChange = false;
            }//end of If-else statement 
            return effectivePriority;
        }//end of getEffectivePriority() 
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	//ADDED
	private void invalCachedPrio() {
		if (!this.transferPriority) return;

		this.priorityChange = true;

		if (this.resourceHolder != null) {
			resourceHolder.invalCachedPrio();
		}
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

	//ADDED
	protected final LinkedList<ThreadState> threadsWaiting;
	protected ThreadState resourceHolder = null;
	protected int effectivePriority = priorityMinimum;
	protected boolean priorityChange = false;
	
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
		//ADDED
		this.resourcesIHave = new LinkedList<PriorityQueue>();
		this.resourcesIWant = new LinkedList<PriorityQueue>();

	    
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
		if (this.resourcesIHave.isEmpty()) {//if the resource i got is empty
			return this.getPriority();
		} else if (this.priorityChange) {// if priority changedv 
			this.effectivePriority = this.getPriority();
			for (final PriorityQueue pq : this.resourcesIHave) {// for each resource i have, do this 
				this.effectivePriority = Math.max(
						this.effectivePriority, pq.getEffectivePriority()
						);
			}//end of for-each loop
			this.priorityChange = false;
		}//end of if-else statement 
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
		for (final PriorityQueue pq : resourcesIWant) {
			pq.invalCachedPrio();
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
		this.resourcesIWant.add(waitQueue);
		this.resourcesIHave.remove(waitQueue);
		waitQueue.invalCachedPrio();
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
		this.resourcesIHave.add(waitQueue);
		this.resourcesIWant.remove(waitQueue);
		this.invalCachedPrio();
	}	

	//ADDED
	/**
         * Called when the associated thread has relinquished access to whatever
         * is guarded by waitQueue.
          * @param waitQueue The waitQueue corresponding to the relinquished resource.
         */
        public void release(PriorityQueue waitQueue) {
            this.resourcesIHave.remove(waitQueue);
            this.invalCachedPrio();
        }//end of release()

        public KThread getThread() {
            return thread;
        }//end of getThread()

        private void invalCachedPrio() {
            if (this.priorityChange){ //if priority changed, return back 
            	return; //leave function
            }
            this.priorityChange = true; // priority DID change, do this 
            for (final PriorityQueue pq : this.resourcesIWant) { //for-each item, loop
                pq.invalCachedPrio();
            }//end of For loop
        }//end of invalCachedPrio()

        /**
         * True if effective priority has been invalidated for this ThreadState.
         */
        protected boolean priorityChange = false;
        /**
         * Holds the effective priority of this Thread State.
         */
        protected int effectivePriority = priorityMinimum;
        /**
         * A list of the queues for which I am the current resource holder.
         */
        protected final LinkedList<PriorityQueue> resourcesIHave;
        /**
         * A list of the queues in which I am waiting.
         */
        protected final LinkedList<PriorityQueue> resourcesIWant;
		//END ADDED

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    }
}
