package nachos.threads;
import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
	private Lock communicatorLock = new Lock();
	private Condition speaker = new Condition(communicatorLock);
	private Condition listener = new Condition(communicatorLock);
	private Condition listenerStatus = new Condition(communicatorLock);
	private int mail = 0; 
	private int numOfListeners = 0; 
	private boolean mailStatus = false;
	
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    
    
    public void speak(int word) {
    	communicatorLock.acquire();
    	
		//speaker sleeps until listener is ready
    	while(mailStatus){
    		speaker.sleep(); 
    	}
    	
    	mailStatus = true; 
    	mail = word;

    	//if there are no sleepers then make the listenerStatus sleep
    	while(numOfListeners == 0 ) {
    		listenerStatus.sleep(); 
    	}

    	listener.wake(); 
    	listenerStatus.sleep(); 

    	mailStatus= false;
    	speaker.wake();
    	communicatorLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	communicatorLock.acquire();

		//add listener
    	numOfListeners++; 

    	if(numOfListeners >=1 && mailStatus) {
    		listenerStatus.wake();
    	}

    	listener.sleep(); 
    	listenerStatus.wake(); 

		//you've got mail
    	int recieved = mail;
    	communicatorLock.release();
    	return recieved;

    }
    
}