package com.example.wrd;

public class CountingSemaphore {
    int value;

    public CountingSemaphore(int initValue) {
        value = initValue;
    }

    public synchronized void P() {
        while (value == 0)
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("CountingSemaphore interrupted");
            }
        value--;
    }

    public synchronized void V() {
        value++;
        notify();
    }

    // Helper method to get current value (for checking ready count)
    public synchronized int getValue() {
        return value;
    }

    // Helper method to reset value (for game restart)
    public synchronized void reset(int newValue) {
        value = newValue;
        notifyAll();
    }
}