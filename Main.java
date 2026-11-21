/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

import java.util.Scanner;

/**
 * CSC227 Multithreaded Scheduling Simulator.
 * The MAIN thread asks the user for the options, starts the other threads,
 * and then runs the CPU scheduling algorithm.
 */
public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("=== CSC227 Multithreaded Scheduling Simulator ===");
        String fileName = askFileName(input);
        int algorithm = askAlgorithm(input);
        boolean mediumEnabled = askMediumScheduler(input);

        MemoryManager memory = new MemoryManager();
        SimClock clock = new SimClock();

        // ---- Thread 1: read job.txt and fill the job queue ----
        JobReaderThread reader = new JobReaderThread(fileName, memory);
        reader.start();
        waitForThread(reader);                     // all jobs are in the job queue (time 0)

        if (reader.getProcesses().size() == 0) {
            System.out.println("No valid processes found. The program will stop.");
            return;
        }

        // ---- Thread 2 and Thread 3 ----
        LongTermScheduler longTerm = new LongTermScheduler(memory, clock);
        MediumTermScheduler mediumTerm = new MediumTermScheduler(memory, clock, mediumEnabled);
        longTerm.start();
        mediumTerm.start();

        String algorithmName = getAlgorithmName(algorithm);
        System.out.println();
        System.out.println("Algorithm: " + algorithmName);
        System.out.println("Medium-Term Scheduler: " + (mediumEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("Processes read: " + reader.getProcesses().size());
        System.out.println();
        System.out.println("---------------- SIMULATION LOG ----------------");

        // ---- Main thread: run the CPU scheduling algorithm ----
        Scheduler scheduler = new Scheduler(memory, clock, reader.getProcesses(), algorithm);
        scheduler.run();

        // ---- Stop the other threads ----
        clock.finish();
        waitForThread(longTerm);
        waitForThread(mediumTerm);

        scheduler.printResults(algorithmName);
    }

    private static void waitForThread(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted.");
        }
    }

    private static String getAlgorithmName(int algorithm) {
        if (algorithm == Scheduler.SJF) {
            return "Shortest Job First (SJF)";
        } else if (algorithm == Scheduler.RR) {
            return "Round Robin (q = 5 ms)";
        }
        return "Non-Preemptive Priority Scheduling with Aging";
    }

    // ---------------- Simple menu methods ----------------

    private static String askFileName(Scanner input) {
        System.out.print("Input file name (press Enter for job.txt): ");
        String name = input.nextLine().trim();
        if (name.length() == 0) {
            name = "job.txt";
        }
        return name;
    }

    private static int askAlgorithm(Scanner input) {
        while (true) {
            System.out.println("Choose the scheduling algorithm:");
            System.out.println("  1 - Shortest Job First (SJF)");
            System.out.println("  2 - Round Robin (q = 5 ms)");
            System.out.println("  3 - Priority Scheduling (non-preemptive, with aging)");
            System.out.print("Your choice: ");
            String text = input.nextLine().trim();
            if (text.equals("1") || text.equals("2") || text.equals("3")) {
                return Integer.parseInt(text);
            }
            System.out.println("Invalid choice. Please enter 1, 2 or 3.");
        }
    }

    private static boolean askMediumScheduler(Scanner input) {
        while (true) {
            System.out.print("Enable the Medium-Term Scheduler? (y/n): ");
            String text = input.nextLine().trim().toLowerCase();
            if (text.equals("y")) {
                return true;
            }
            if (text.equals("n")) {
                return false;
            }
            System.out.println("Invalid choice. Please enter y or n.");
        }
    }
}
