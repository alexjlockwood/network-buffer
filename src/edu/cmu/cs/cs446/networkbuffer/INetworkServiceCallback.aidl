package edu.cmu.cs.cs446.networkbuffer;

import edu.cmu.cs.cs446.networkbuffer.Response;

/**
 * Example of a callback interface used by INetworkService to send
 * synchronous notifications back to its clients.  Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
oneway interface INetworkServiceCallback {
    /**
     * Called when the service has a new value for you.
     */
    void receive(in Response response);
}
