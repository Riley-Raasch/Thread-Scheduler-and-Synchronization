# Thread-Scheduler-and-Synchronization

Implementation of boat and PriorityScheduler

Outline:
1. Boat.java
(1) Add whichever synchronization tools (variable members) you need to the Boat class.
(2) The begin() method: the runnable defined inside may no longer be needed once you are done.
(3) The AdultItinerary() method
(4) The ChildItinerary() method

2. PriorityScheduler.java
(1) nextThread() method from PriorityQueue inner class
(2) pickNextThread() method from PriorityQueue inner class
(3) getEffectivePriority() from ThreadState class
(4) setPriority() from ThreadState class
(5) waitForAccess() from ThreadState class
(6) acquire() from ThreadState class


Instructions from class:

Task 1 - Synchronization
Now that you have all the synchronization devices implemented by project 1 and the original
Nachos, use them to complete boat.java. You will find condition variables to be the most useful
synchronization method for this task:
A number of Hawaiian adults and children are trying to get from Oahu to Molokai.
Unfortunately, they have only one boat which can carry maximally two children or one adult
(but not one child and one adult). The boat can be rowed back to Oahu, but it requires a pilot
to do so. Arrange a solution to transfer everyone from Oahu to Molokai. Assume that there are
at least two children.
The method Boat.begin() should fork off a thread for each child and adult. Your mechanism
cannot rely on knowing how many children or adults are present beforehand, although you are
free to attempt to determine this among the threads (i.e., you cannot pass the values to your
threads, but you are free to have each thread increment a shared variable, if you wish).
To show that the trip is properly synchronized, make calls to the appropriate BoatGrader methods
every time someone crosses the channel. When a child pilots the boat from Oahu to Molokai, call
ChildRowToMolokai. When a child rides as a passenger from Oahu to Molokai, call
ChildRideToMolokai. Make sure that when a boat with two people on it crosses, the pilot calls the
…RowTo… method before the passenger calls the …RideTo… method.
Your solution must have no busy waiting, and it must eventually end. Note that it is not necessary
to terminate all the threads – you can leave them blocked waiting for a condition variable. The
threads representing the adults and children cannot have access to the numbers of threads that
were created, but you will probably need to use these numbers in begin() in order to determine
when all the adults and children are across and you can return.
The idea behind this task is to use independent threads to solve a problem. You are to program
the logic that a child or an adult would follow if that person were in this situation. For example, it
is reasonable to allow a person to see how many children or adults are on the same island they
are on. A person could see whether the boat is at their island. A person can know which island
they are on. All of this information may be stored with each individual thread or in shared
variables. So a counter that holds the number of children on Oahu would be allowed, so long as
only threads that represent people on Oahu could access it.
What is not allowed is a thread which executes a “top-down” strategy for the situation. For example, you
may not create threads for children and adults, then have a controller thread simply send commands to
them through communicators. The threads must act as if they were individuals.
Information which is not possible in the real world is also not allowed. For example, a child on Molokai
cannot magically see all of the people on Oahu. That child may remember the number of people that he
or she has seen leaving, but the child may not view people on Oahu as if it were there. (Assume that the
people do not have any technology other than a boat!)
The one exception to these rules is that you may use the number of people in the total simulation to
determine when to terminate. This number must only be used for this purpose.


Task 2 – Priority Scheduling
Implement priority scheduling in Nachos by completing the PriorityScheduler.java. Priority
scheduling is a key building block in real-time systems.
Priority in nachos ranges from 0 to 7 (integer values), with 7 being the maximum priority. Note
that all scheduler classes extend the abstract class nachos.threads.Scheduler. You must
implement the methods:
- nextThread and pickNextThread in the inner class PriorityQueue
- getEffectivePriority, setPriority, waitForAccess and acquire in the inner class ThreadState
In choosing which thread to dequeue from a PriorityQueue-typed queue, your implementation
should always choose a thread of the highest effective priority.
An issue with priority scheduling is priority inversion. If a high priority thread needs to wait for a
low priority thread (for instance, for some shared resources temporarily held by a low priority
thread), and another high priority thread is on the ready list, then the first high priority thread
will never get the CPU because the low priority thread will not get the CPU time. A fix adopted
by this project is to have the waiting thread donate its priority to the low priority thread while it
is holding the lock.
When implementing your priority scheduler, make sure the priority is donated by the waiting
thread wherever possible. The implementation of the getEffectivePriority method in the
ThreadState inner class should return the priority of a thread after taking into account all the
donations it is receiving.
