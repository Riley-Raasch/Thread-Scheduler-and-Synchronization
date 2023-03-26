package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;

//TODO: add variables (synchronization tools)

//number of children on each island
private static int numChildOnOahu;
private static int numChildOnMolokai;

//number of adults on each island
private static int numAdultOnOahu;
private static int numAdultOnMolokai;

//number of children waiting on each island
private static int numOahuWait;
private static int numMolokaiWait;

//true when boat is at Oahu, false when boat is at Molokai
private static boolean boatStatus;

//locks represented as the islands
private static Lock oahu1 = new Lock();
private static Lock molokai1 = new Lock();

//conditions
private static Condition adultCondition = new Condition(oahu1);

private static Condition boatWait = new Condition(oahu1);

private static Condition oahuWait = new Condition(oahu1);
private static Condition molokaiWait = new Condition(molokai1);

//semaphore used to confirm that the program ends
private static Semaphore completedSemaphore = new Semaphore(0);

//self test
//   public static void selfTest()
//   {
// BoatGrader b = new BoatGrader();
	
// System.out.println("\n ***Testing Boats with only 2 children***");
//  	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
 //  	begin(1, 2, b);

 //  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
 //  	begin(3, 3, b);
   // }

	//TODO: begin
    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	 		numChildOnOahu = children;
			numAdultOnOahu = adults;

			numChildOnMolokai = 0;
			numAdultOnMolokai = 0;

			numOahuWait = 0;
			numMolokaiWait = 0;

			boatStatus = true;

	
	// Create threads here. See section 3.4 of the Nachos for Java
//Walkthrough linked from the projects page.

// Runnable r = new Runnable() {
//     public void run() {
// SampleItinerary();
// }
// };
// KThread t = new KThread(r);
//      t.setName("Sample Boat Thread");
//    t.fork();
	
		for (int i = 0; i < children; i++){
			new KThread(
				new Runnable() {
				public void run() {
					ChildItinerary();
				}
			}
			).setName("c" + i).fork();
		}

		for (int i = 0; i < adults; i++){
			new KThread(
				new Runnable() {public void run() {AdultItinerary();}}
			).setName("a" + i).fork();
		}

		completedSemaphore.P();

    }


	//TODO: AdultItinerary
    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/

		oahu1.acquire();

		//check for >1 children, because they get priority
		while(numChildOnOahu > 1 || !boatStatus){
			adultCondition.sleep();
		}

		//adult boarding
		numAdultOnOahu = numAdultOnOahu - 1;
		boatStatus = false;

		oahu1.release();

		//rowing to Molokai
		bg.AdultRowToMolokai();

		//prep for return trip
		molokai1.acquire();

		numAdultOnMolokai = numAdultOnMolokai + 1;
		molokaiWait.wake();
		molokai1.release();

    }

	//TODO: ChildItinerary
    static void ChildItinerary()
    {
		//check for people
		while(numChildOnOahu + numAdultOnOahu > 1){
			oahu1.acquire();

		//check if there is only 1 child
		if(numChildOnOahu == 1){
		adultCondition.wake();
	}

		//check that there are more than 1 child waiting
		while(numOahuWait > 1 || !boatStatus){
		oahuWait.sleep();
	}

		//the 2 children are boarding
		if(numOahuWait == 0){
			numOahuWait++;
			oahuWait.wake();

			boatWait.sleep();

			bg.ChildRideToMolokai();

			boatWait.wake();
		}
		else{
			numOahuWait++;
			boatWait.wake();

			bg.ChildRowToMolokai();
			boatWait.sleep();
		}

		//deboarding
		numOahuWait--;
		numChildOnOahu--;
		boatStatus = false;
		oahu1.release();

		//prep for return trip
		molokai1.acquire();
		numChildOnMolokai++;
		numMolokaiWait++;

		if(numMolokaiWait == 1){
			molokaiWait.sleep();}

		//return trip

		//prep
		numChildOnMolokai--;
		numMolokaiWait = 0;
		molokai1.release();

		//rowing back
		bg.ChildRowToOahu();

		//arrived
		oahu1.acquire();
		numChildOnOahu++;
		boatStatus = true;
		oahu1.release();

		}
//check for people on Oahu
		oahu1.acquire();
		numChildOnOahu--;
		oahu1.release();

		//going back to Molokai
		bg.ChildRowToMolokai();

		//arrived
		molokai1.acquire();
		numChildOnMolokai++;
		molokai1.release();

		//end
		completedSemaphore.V();
    }

// static void SampleItinerary()
// {
// // Please note that this isn't a valid solution (you can't fit
// // have a single thread calculate a solution and then just play
// // it back at the autograder -- you will be caught.
// System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
// bg.AdultRowToMolokai();
// bg.ChildRideToMolokai();
// bg.AdultRideToMolokai();
// bg.ChildRideToMolokai();
// }
}