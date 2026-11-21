/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package os_schedulingsimulator;

/**
 * The simulation clock and the "turn" controller.
 *
 * Why do we need it?
 * The three scheduler threads must give the same result every time we run the
 * program. If they ran freely, the result would depend on thread timing.
 * So in every simulated millisecond the threads take turns in a fixed order:
 *      Main thread -> Long-Term thread -> Medium-Term thread -> Main thread
 * The turn is passed with the simple wait() / notifyAll() mechanism.
 */
public class SimClock {

    public static final int MAIN = 0;
    public static final int LONG_TERM = 1;
    public static final int MEDIUM_TERM = 2;

    private int time = 0;          // current simulation time in ms
    private int turn = MAIN;       // who is allowed to work now
    private boolean finished = false;

    public synchronized int getTime() {
        return time;
    }

    public synchronized void setTime(int newTime) {
        time = newTime;
    }

    /** Give the turn to another thread and wake up all waiting threads. */
    public synchronized void giveTurnTo(int who) {
        turn = who;
        notifyAll();
    }

    /** Wait until it is my turn (or until the simulation is finished). */
    public synchronized void waitForTurn(int who) {
        while (turn != who && !finished) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Nobody interrupts our threads, so we simply ignore this.
            }
        }
    }

    /** Called by the main thread when the simulation ends. */
    public synchronized void finish() {
        finished = true;
        notifyAll();
    }

    public synchronized boolean isFinished() {
        return finished;
    }
}
