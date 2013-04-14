package edu.cmu.cs.cs446.networkbuffer;

import edu.cmu.cs.cs446.networkbuffer.Request;
import edu.cmu.cs.cs446.networkbuffer.INetworkServiceCallback;

/**
 * Defines an interface for calling the remote network service 
 * (which runs in a separate process).
 */
interface INetworkService {

    /**
     * Register the callback interface with the service.
     */
    void registerCallback(INetworkServiceCallback callback);

    /**
     * Unregister the callback interface with the service.
     */
    void unregisterCallback(INetworkServiceCallback callback);

    /**
     * Send a simple network request to be performed asynchronously
     * by the service (at some point in the near future).
     */
	void send(in Request request, long delay);
}
