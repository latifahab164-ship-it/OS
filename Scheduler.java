/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

import java.util.ArrayList;

/**
 * The Short-Term Scheduler (CPU scheduler). It runs in the MAIN thread.
 * It supports three algorithms: SJF, Round Robin, Non-Preemptive Priority.
 *
 * The simulation moves forward 1 ms at a time. In every millisecond:
 *   1. The Long-Term and Medium-Term threads do their work (we wait for them).
 *   2. (Priority only) starvation is detected and aging is applied.
 *   3. If the CPU is free, the next process is selected.
 *   4. The running process executes for 1 ms.
 */
public class Scheduler {

    public static final int SJF = 1;
    public static final int RR = 2;
    public static final int PRIORITY = 3;

    private static final int QUANTUM = 5;         // Round Robin time quantum (ms)
    private static final int AGING_PERIOD = 4;    // aging every 4 ms
    private static final int MAX_TIME = 1000000;  // safety limit against infinite loops

    private MemoryManager memory;
    private SimClock clock;
    private ArrayList<PCB> allProcesses;
    private int algorithm;

    // Simulation state
    private int time = 0;
    private PCB running = null;      // process that is using the CPU now
    private int sliceUsed = 0;       // ms used by the running process in this CPU turn
    private int sliceStart = 0;      // start time of the current CPU turn
    private int finishedCount = 0;

    // Gantt chart data: one Slice for every CPU allocation
    private ArrayList<Slice> gantt = new ArrayList<Slice>();

    /** One block of the Gantt chart (one CPU allocation). */
    private static class Slice {
        int processId;
        int start;
        int end;

        Slice(int processId, int start, int end) {
            this.processId = processId;
            this.start = start;
            this.end = end;
        }

        String label() {
            return "P" + processId;
        }
    }

    public Scheduler(MemoryManager memory, SimClock clock, ArrayList<PCB> allProcesses, int algorithm) {
        this.memory = memory;
        this.clock = clock;
        this.allProcesses = allProcesses;
        this.algorithm = algorithm;
    }

    // =====================================================================
    //                         MAIN SIMULATION LOOP
    // =====================================================================

    public void run() {
        int total = allProcesses.size();
        while (finishedCount < total) {
            if (time > MAX_TIME) {
                System.out.println("Simulation stopped: time limit reached.");
                break;
            }

            // Step 1: let the Long-Term and Medium-Term threads work for this ms.
            clock.setTime(time);
            clock.giveTurnTo(SimClock.LONG_TERM);
            clock.waitForTurn(SimClock.MAIN);

            // Step 2: starvation detection and aging (Priority scheduling only).
            if (algorithm == PRIORITY) {
                detectStarvation();
                applyAging();
            }

            // Step 3: if the CPU is free, choose the next process.
            if (running == null) {
                chooseProcess();
            }

            // Step 4: run for 1 ms.
            // The CPU cannot stay idle: if no process is ready, either a job gets loaded
            // or a suspended process gets swapped in (memory is empty), in this same ms.
            // The "else" below is only a safety net so that time always moves forward.
            if (running != null) {
                runOneMs();
            } else {
                time++;
            }
        }
    }

    /** Select a process from the ready queue and give it the CPU. */
    private void chooseProcess() {
        PCB next = pickNext();
        if (next == null) {
            return;                                   // nothing is ready
        }
        running = next;
        running.state = PCB.RUNNING;
        if (running.startTime == -1) {
            running.startTime = time;                 // first time on the CPU
        }
        sliceStart = time;
        sliceUsed = 0;
    }

    private void runOneMs() {
        running.remainingTime--;
        sliceUsed++;
        time++;

        if (running.remainingTime == 0) {
            finishProcess();
        } else if (algorithm == RR && sliceUsed == QUANTUM) {
            gantt.add(new Slice(running.id, sliceStart, time));
            memory.addToReady(running, time);         // back to the END of the ready queue
            running = null;
        }
    }

    private void finishProcess() {
        gantt.add(new Slice(running.id, sliceStart, time));
        running.state = PCB.TERMINATED;
        running.calculateTimes(time);
        memory.releaseMemory(running);                // free the memory
        finishedCount++;
        running = null;
    }

    // =====================================================================
    //                       SELECTING THE NEXT PROCESS
    // =====================================================================

    /** Choose and remove the next process from the ready queue (null if empty). */
    private PCB pickNext() {
        ArrayList<PCB> readyList = memory.getReadyList();
        if (readyList.isEmpty()) {
            return null;
        }
        PCB best = readyList.get(0);          // for Round Robin: the first in the queue
        if (algorithm != RR) {
            for (PCB p : readyList) {
                if (isBetter(p, best)) {
                    best = p;
                }
            }
        }
        memory.removeFromReady(best);
        return best;
    }

    /** Is process a better choice than process b for SJF or Priority? */
    private boolean isBetter(PCB a, PCB b) {
        if (algorithm == SJF) {
            if (a.burstTime != b.burstTime) {
                return a.burstTime < b.burstTime;       // shorter burst first
            }
        } else {
            if (a.priority != b.priority) {
                return a.priority < b.priority;         // smaller number = higher priority
            }
        }
        return a.arrivalOrder < b.arrivalOrder;         // tie: arrival order
    }

    // =====================================================================
    //                 STARVATION DETECTION  and  AGING
    //                 (these are two separate steps)
    // =====================================================================

    /**
     * STARVATION DETECTION: a process is starved if it stayed in the ready
     * queue for MORE than N x 5 ms without getting the CPU.
     * This method only marks the process. It does NOT change the priority.
     */
    private void detectStarvation() {
        for (PCB p : memory.getReadyList()) {
            int waited = time - p.readyEntryTime;
            if (!p.starved && waited > p.starvationLimit) {
                p.starved = true;
                p.starvedTime = time;
                p.lastAgingTime = time;
                System.out.println("[Time " + time + " ms] STARVATION DETECTED: Process " + p.id
                        + " waited " + waited + " ms in the ready queue (limit = "
                        + p.starvationLimit + " ms).");
            }
        }
    }

    /**
     * AGING: every 4 ms, a starved process that is still waiting gets a
     * better priority (priority number decreased by 1, never below 1).
     */
    private void applyAging() {
        for (PCB p : memory.getReadyList()) {
            if (p.starved && time - p.lastAgingTime >= AGING_PERIOD) {
                p.lastAgingTime = time;
                if (p.priority > 1) {
                    int oldPriority = p.priority;
                    p.priority = p.priority - 1;
                    System.out.println("[Time " + time + " ms] AGING: Process " + p.id
                            + " priority " + oldPriority + " -> " + p.priority);
                }
            }
        }
    }

    // =====================================================================
    //                            PRINTING RESULTS
    // =====================================================================

    public void printResults(String algorithmName) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println(" RESULTS - " + algorithmName);
        System.out.println("==================================================");
        printGantt();
        printTable();
        if (algorithm == PRIORITY) {
            printStarvation();
        }
    }

    private void printGantt() {
        System.out.println();
        System.out.println("GANTT CHART (P = process, numbers = time in ms)");
        int cellWidth = 8;
        int perRow = 10;                                  // slices per console line
        int index = 0;
        while (index < gantt.size()) {
            int last = Math.min(index + perRow, gantt.size());
            String bar = "";
            String times = "";
            for (int i = index; i < last; i++) {
                Slice s = gantt.get(i);
                bar = bar + String.format("|%-" + (cellWidth - 1) + "s", " " + s.label());
                times = times + String.format("%-" + cellWidth + "d", s.start);
            }
            bar = bar + "|";
            times = times + gantt.get(last - 1).end;
            System.out.println(bar);
            System.out.println(times);
            System.out.println();
            index = last;
        }
    }

    private void printTable() {
        System.out.println("PROCESS TABLE");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-8s %-7s %-7s %-12s %-9s %-10s%n",
                "Process", "Burst", "Start", "Termination", "Waiting", "Turnaround");
        System.out.println("------------------------------------------------------------");

        int totalWaiting = 0;
        int totalTurnaround = 0;
        for (PCB p : allProcesses) {
            System.out.printf("%-8d %-7d %-7d %-12d %-9d %-10d%n",
                    p.id, p.burstTime, p.startTime, p.terminationTime, p.waitingTime, p.turnaroundTime);
            totalWaiting = totalWaiting + p.waitingTime;
            totalTurnaround = totalTurnaround + p.turnaroundTime;
        }
        System.out.println("------------------------------------------------------------");
        double count = allProcesses.size();
        System.out.printf("Average Waiting Time    = %.2f ms%n", totalWaiting / count);
        System.out.printf("Average Turnaround Time = %.2f ms%n", totalTurnaround / count);
    }

    private void printStarvation() {
        System.out.println();
        System.out.println("STARVATION REPORT (Priority Scheduling)");
        boolean found = false;
        for (PCB p : allProcesses) {
            if (p.starved) {
                found = true;
                String aging = "aging did not change its priority (" + p.priority + ")";
                if (p.priority != p.originalPriority) {
                    aging = "aging changed its priority " + p.originalPriority + " -> " + p.priority;
                }
                System.out.println("Process " + p.id + " was starved (detected at " + p.starvedTime
                        + " ms); " + aging + ".");
            }
        }
        if (!found) {
            System.out.println("No process suffered from starvation.");
        }
    }
}
