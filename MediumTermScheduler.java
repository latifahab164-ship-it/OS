/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

/**
 * Thread 3: Medium-Term Scheduler.
 * Every simulated millisecond it checks memory utilization:
 *   - above 90 %  -> swap out the READY process with the largest memory
 *   - below 70 %  -> swap a suspended process back in
 * It can be enabled or disabled at startup. When disabled, the thread still
 * runs (so the turn system stays the same) but it never intervenes.
 */
/**
 * Thread 3: Medium-Term Scheduler.
 * Every simulated millisecond it checks memory utilization:
 *   - above 90 %  -> swap out the READY process with the largest memory
 *   - below 70 %  -> swap a suspended process back in
 * It can be enabled or disabled at startup. When disabled, the thread still
 * runs (so the turn system stays the same) but it never intervenes.
 */
public class MediumTermScheduler extends Thread {

    private MemoryManager memory;
    private SimClock clock;
    private boolean enabled;

    public MediumTermScheduler(MemoryManager memory, SimClock clock, boolean enabled) {
        this.memory = memory;
        this.clock = clock;
        this.enabled = enabled;
        setDaemon(true);      // if the main thread crashes, this thread cannot block the JVM exit
    }

    public void run() {
        while (true) {
            clock.waitForTurn(SimClock.MEDIUM_TERM);
            if (clock.isFinished()) {
                break;
            }
            if (enabled) {
                checkMemory();
            }
            clock.giveTurnTo(SimClock.MAIN);            // give the turn back to the main thread
        }
    }

    /** One intervention at most per millisecond: first try swap out, then swap in. */
    private void checkMemory() {
        int time = clock.getTime();

        PCB victim = memory.trySwapOut();               // does nothing if memory <= 90 %
        if (victim != null) {
            double after = memory.getUtilization();
            double before = after + MemoryManager.percentOf(victim.memoryRequired);
            printIntervention(time, before, "Swap out Process " + victim.id,
                    victim, PCB.READY, after);
            return;
        }

        PCB back = memory.trySwapIn(time);              // does nothing if the conditions are not met
        if (back != null) {
            double after = memory.getUtilization();
            double before = after - MemoryManager.percentOf(back.memoryRequired);
            printIntervention(time, before, "Swap in Process " + back.id,
                    back, PCB.SUSPENDED, after);
        }
    }

    /** Prints the intervention details required by the project. */
    private void printIntervention(int time, double before, String action,
                                   PCB p, String stateBefore, double after) {
        System.out.println();
        System.out.println("[Time " + time + " ms]");
        System.out.println("Medium-Term Scheduler Intervention");
        System.out.println("Memory utilization: " + String.format("%.2f", before) + "%");
        System.out.println("Action: " + action);
        System.out.println("Process " + p.id + ": " + stateBefore + " -> " + p.state);
        System.out.println("Memory utilization after intervention: " + String.format("%.2f", after) + "%");
        System.out.println();
    }
}
