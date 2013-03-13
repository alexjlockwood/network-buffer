package edu.cmu.cs.cs446.wifibuffer;

/**
 * Example of a secondary interface associated with a service.  (Note that
 * the interface itself doesn't impact, it is just a matter of how you
 * retrieve it from the service.)
 */
interface ISecondary {
    /**
     * Request the PID of this service, to do evil things with it.
     */
    int getPid();
    
    /**
     * This demonstrates the basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}
