package edu.cmu.cs.cs446.networkbuffer;

/**
 * Callback interface used to report responses back to the service.
 */
public interface ResponseCallback {
  /**
   * Report the response to the client. This method is called on a background
   * thread, so we must synchronize with the main UI thread ourselves before
   * delivering the response to the client.
   */
  public void onReceive(ParcelableByteArray response);
}
