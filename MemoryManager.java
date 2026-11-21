/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Shared data of the whole system: main memory + the three queues.
 *   jobQueue     : jobs waiting outside memory (filled by Thread 1)
 *   readyQueue   : processes that are in memory and ready for the CPU
 *   suspendQueue : processes swapped out by the medium-term scheduler
 *
 * Every method is "synchronized", so only one thread can use the queues or the
 * memory counter at a time. This prevents race conditions.
 * Each "check and then change" action (load a job, swap out, swap in) is done
 * inside ONE synchronized method, so no other thread can interrupt it.
 */
public class MemoryManager {

    public static final int TOTAL_MEMORY = 2048;   // MB
    public static final int HIGH_LIMIT = 90;       // swap out above 90 %
    public static final int LOW_LIMIT = 70;        // swap in below 70 %

    private int usedMemory = 0;          // MB currently used
    private int processesInMemory = 0;   // ready + running processes in memory

    private Queue<PCB> jobQueue = new LinkedList<PCB>();
    private ArrayList<PCB> readyQueue = new ArrayList<PCB>();
    private ArrayList<PCB> suspendQueue = new ArrayList<PCB>();

    /** What percent of the total memory is this amount of MB? */
    public static double percentOf(int mb) {
        return mb * 100.0 / TOTAL_MEMORY;
    }

    // ---------------- Job queue ----------------

    public synchronized void addJob(PCB p) {
        jobQueue.add(p);
    }

    /**
     * Used by the Long-Term Scheduler.
     * Loads the FIRST job of the job queue into memory and the ready queue,
     * but only if there is enough free memory. Returns the process, or null.
     * Rule: while a process is suspended, no new job is loaded (the suspended
     * process has priority). This avoids swap-out/load loops.
     */
    public synchronized PCB loadNextJob(int time) {
        if (!suspendQueue.isEmpty()) {
            return null;
        }
        PCB p = jobQueue.peek();
        if (p == null) {
            return null;                                   // no more jobs
        }
        if (usedMemory + p.memoryRequired > TOTAL_MEMORY) {
            return null;                                   // not enough memory
        }
        jobQueue.poll();
        usedMemory = usedMemory + p.memoryRequired;
        processesInMemory++;
        addToReady(p, time);
        return p;
    }

    // ---------------- Ready queue ----------------

    /** Put a process in the ready queue and remember N for starvation detection. */
    public synchronized void addToReady(PCB p, int time) {
        p.state = PCB.READY;
        readyQueue.add(p);
        p.readyEntryTime = time;
        p.lastAgingTime = time;                      // the aging clock starts again
        p.starvationLimit = readyQueue.size() * 5;   // N x 5 ms (N includes p itself)
    }

    /**
     * Jobs that are loaded in the SAME millisecond arrive together, so they
     * must all use the same N (the ready queue size after all of them entered).
     * Without this, N would depend on the order in which they were loaded.
     */
    public synchronized void setLimitForNewJobs(int time) {
        int limit = readyQueue.size() * 5;
        for (PCB p : readyQueue) {
            if (p.readyEntryTime == time) {
                p.starvationLimit = limit;
            }
        }
    }

    /** Returns a COPY of the ready queue, so the caller can look at it safely. */
    public synchronized ArrayList<PCB> getReadyList() {
        return new ArrayList<PCB>(readyQueue);
    }

    public synchronized void removeFromReady(PCB p) {
        readyQueue.remove(p);
    }

    /** A process finished: free its memory. */
    public synchronized void releaseMemory(PCB p) {
        usedMemory = usedMemory - p.memoryRequired;
        processesInMemory--;
    }

    // ---------------- Memory information ----------------

    public synchronized double getUtilization() {
        return percentOf(usedMemory);
    }

    // ---------------- Medium-term scheduler operations ----------------

    /**
     * SWAP OUT (one atomic step).
     * Works only if utilization is MORE than 90 % (integer arithmetic, so there
     * is no rounding problem). The victim is the READY process with the largest
     * memory. The running process is never chosen. If only one process is in
     * memory, swapping it out would not help, so nothing is done.
     * Returns the swapped-out process, or null if nothing was swapped out.
     */
    public synchronized PCB trySwapOut() {
        boolean above90 = usedMemory * 100 > HIGH_LIMIT * TOTAL_MEMORY;
        if (!above90 || processesInMemory < 2 || readyQueue.isEmpty()) {
            return null;
        }
        PCB victim = readyQueue.get(0);
        for (PCB p : readyQueue) {
            if (p.memoryRequired > victim.memoryRequired) {
                victim = p;                                // largest memory wins
            }
        }
        readyQueue.remove(victim);
        usedMemory = usedMemory - victim.memoryRequired;
        processesInMemory--;
        victim.state = PCB.SUSPENDED;
        suspendQueue.add(victim);
        return victim;
    }

    /**
     * SWAP IN (one atomic step).
     * The first suspended process comes back if utilization is below 70 % AND
     * after loading it the utilization will not be above 90 % (otherwise it
     * would be swapped out again at once). If memory is completely empty, it
     * can always come back, so no process waits forever.
     * Returns the swapped-in process, or null if nothing was swapped in.
     */
    public synchronized PCB trySwapIn(int time) {
        if (suspendQueue.isEmpty()) {
            return null;
        }
        PCB first = suspendQueue.get(0);
        int newUsed = usedMemory + first.memoryRequired;
        boolean below70 = usedMemory * 100 < LOW_LIMIT * TOTAL_MEMORY;
        boolean stays90OrLess = newUsed * 100 <= HIGH_LIMIT * TOTAL_MEMORY;
        if (usedMemory != 0 && !(below70 && stays90OrLess)) {
            return null;
        }
        suspendQueue.remove(0);
        usedMemory = newUsed;
        processesInMemory++;
        addToReady(first, time);
        return first;
    }
}
