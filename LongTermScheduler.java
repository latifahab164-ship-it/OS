/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package os_schedulingsimulator;

/**
 * Thread 2: Long-Term Scheduler.
 * In every simulated millisecond it moves jobs from the job queue to the
 * ready queue, but only if there is enough free memory for them.
 */
/**
 * Thread 2: Long-Term Scheduler.
 * In every simulated millisecond it moves jobs from the job queue to the
 * ready queue, but only if there is enough free memory for them.
 */
public class LongTermScheduler extends Thread {

    private MemoryManager memory;
    private SimClock clock;

    public LongTermScheduler(MemoryManager memory, SimClock clock) {
        this.memory = memory;
        this.clock = clock;
        setDaemon(true);      // if the main thread crashes, this thread cannot block the JVM exit
    }

    public void run() {
        while (true) {
            clock.waitForTurn(SimClock.LONG_TERM);      // wait for my turn
            if (clock.isFinished()) {
                break;                                  // simulation is over
            }
            loadJobs();
            clock.giveTurnTo(SimClock.MEDIUM_TERM);     // pass the turn on
        }
    }

    /** Load jobs one by one (in file order) while memory is available. */
    private void loadJobs() {
        int time = clock.getTime();
        boolean loadedSomething = false;
        PCB p = memory.loadNextJob(time);
        while (p != null) {
            loadedSomething = true;
            System.out.println("[Time " + time + " ms] Long-Term Scheduler: loaded Process "
                    + p.id + " (" + p.memoryRequired + " MB) into the ready queue. Memory utilization: "
                    + String.format("%.2f", memory.getUtilization()) + "%");
            p = memory.loadNextJob(time);
        }
        if (loadedSomething) {
            memory.setLimitForNewJobs(time);            // same N for all jobs loaded now
        }
    }
}
