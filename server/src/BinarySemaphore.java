package com.example.wrd;

public class BinarySemaphore {
    boolean value;

    public BinarySemaphore(boolean initValue) {
        value = initValue;
    }

    public synchronized void P() {
        while (!value)
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("BinarySemaphore interrupted");
            }
        value = false;
    }

    public synchronized void V() {
        value = true;
        notifyAll();
    }
}