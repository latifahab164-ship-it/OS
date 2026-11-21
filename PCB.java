/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

/**
 * PCB = Process Control Block.
 * One PCB object stores all the information about one process.
 * The fields are public to keep the code short and easy to read.
 */
public class PCB {

    // The possible process states
    public static final String NEW = "New";
    public static final String READY = "Ready";
    public static final String RUNNING = "Running";
    public static final String SUSPENDED = "Suspended";
    public static final String TERMINATED = "Terminated";

    // Information read from job.txt
    public int id;               // Process ID
    public int burstTime;        // CPU burst time (ms)
    public int priority;         // Current priority (1 = highest). Aging can change it.
    public int memoryRequired;   // Memory needed (MB)

    // Information used by the simulation
    public String state;         // Current process state
    public int remainingTime;    // Remaining CPU burst time
    public int originalPriority; // Priority read from the file (before aging)
    public int arrivalOrder;     // Position in the file (used to break ties)
    public int startTime;        // First time the process got the CPU (-1 = not started)
    public int terminationTime;  // Time when the process finished
    public int waitingTime;      // Turnaround time - burst time
    public int turnaroundTime;   // Termination time - arrival time (arrival is 0)

    // Information used for starvation detection and aging (Priority scheduling)
    public int readyEntryTime;   // Time when the process entered the ready queue
    public int starvationLimit;  // N x 5 ms, where N = ready queue size when it entered
    public boolean starved;      // true once starvation is detected
    public int starvedTime;      // Time when starvation was detected
    public int lastAgingTime;    // Last time aging was applied to this process

    public PCB(int id, int burstTime, int priority, int memoryRequired, int arrivalOrder) {
        this.id = id;
        this.burstTime = burstTime;
        this.priority = priority;
        this.originalPriority = priority;
        this.memoryRequired = memoryRequired;
        this.arrivalOrder = arrivalOrder;

        this.remainingTime = burstTime;
        this.state = NEW;
        this.startTime = -1;
        this.terminationTime = 0;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.starved = false;
    }

    /** Called when the process finishes. All processes arrive at time 0. */
    public void calculateTimes(int endTime) {
        terminationTime = endTime;
        turnaroundTime = terminationTime - 0;      // arrival time is 0
        waitingTime = turnaroundTime - burstTime;  // time spent not running
    }
}
