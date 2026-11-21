/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Thread 1: reads the input file, creates the PCB objects, adds them to the
 * job queue, and then terminates.
 * Line format:  ProcessID:BurstTime:Priority;MemoryMB     (example  1:25:4;500)
 */
public class JobReaderThread extends Thread {

    private String fileName;
    private MemoryManager memory;
    private ArrayList<PCB> allProcesses = new ArrayList<PCB>();  // for the final table

    public JobReaderThread(String fileName, MemoryManager memory) {
        this.fileName = fileName;
        this.memory = memory;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();
            int lineNumber = 1;
            while (line != null) {
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);         // remove the invisible BOM that Notepad can add
                }
                line = line.trim();
                if (line.length() > 0) {              // ignore empty lines
                    handleLine(line, lineNumber);
                }
                line = reader.readLine();
                lineNumber++;
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("ERROR: cannot read the file '" + fileName + "' (" + e.getMessage() + ")");
        }
    }

    /** Reads one line. A bad line is skipped with an explanation. */
    private void handleLine(String line, int lineNumber) {
        try {
            // The -1 keeps empty pieces, so "1:25:4:;500" or "1:25:4;500;" are rejected.
            String[] parts = line.split(";", -1);       // "1:25:4"  and  "500"
            String[] left = parts[0].split(":", -1);    // "1", "25", "4"
            if (parts.length != 2 || left.length != 3) {
                throw new Exception("wrong format, expected  id:burst:priority;memory");
            }
            int id = Integer.parseInt(left[0].trim());
            int burst = Integer.parseInt(left[1].trim());
            int priority = Integer.parseInt(left[2].trim());
            int mem = Integer.parseInt(parts[1].trim());

            if (burst <= 0) {
                throw new Exception("burst time must be greater than 0");
            }
            if (priority < 1 || priority > 30) {
                throw new Exception("priority must be between 1 and 30");
            }
            if (mem <= 0 || mem > MemoryManager.TOTAL_MEMORY) {
                throw new Exception("memory must be between 1 and " + MemoryManager.TOTAL_MEMORY + " MB");
            }
            if (idExists(id)) {
                throw new Exception("process ID " + id + " is already used");
            }

            PCB p = new PCB(id, burst, priority, mem, allProcesses.size() + 1);
            allProcesses.add(p);
            memory.addJob(p);                           // add to the job queue
        } catch (NumberFormatException e) {
            System.out.println("Line " + lineNumber + " skipped: a value is not a valid number.");
        } catch (Exception e) {
            System.out.println("Line " + lineNumber + " skipped: " + e.getMessage());
        }
    }

    private boolean idExists(int id) {
        for (PCB p : allProcesses) {
            if (p.id == id) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<PCB> getProcesses() {
        return allProcesses;
    }
}

