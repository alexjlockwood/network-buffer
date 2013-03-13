package edu.cmu.cs.cs446.wifibuffer;

import edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback;

/**
 * Defines an interface for calling the remote wifi buffer service 
 * (which runs in a separate process).
 */
interface IWifiBufferService {

    /**
     * Register the callback interface with the service.
     */
    void registerCallback(IWifiBufferServiceCallback cb);

    /**
     * Unregister the callback interface with the service.
     */
    void unregisterCallback(IWifiBufferServiceCallback cb);
}
