package edu.cmu.cs.cs446.networkbuffer;

import java.io.IOException;
import java.util.concurrent.DelayQueue;

import android.util.Log;

public class DelaySocketExecutor extends Thread {
  private static final String TAG = DelaySocketExecutor.class.getSimpleName();

  private final DelayQueue<DelaySocket> mDelayQueue;
  private boolean mRunning;

  DelaySocketExecutor() {
    mDelayQueue = new DelayQueue<DelaySocket>();
  }

  @Override
  public void run() {
    Log.i(TAG, "Background daemon running. ");
    mRunning = true;

    while (mRunning) {
      try {
        Log.i(TAG, "Background daemon waiting for next request... ");
        DelaySocket delaySocket = mDelayQueue.take();
        Log.i(TAG, "Background daemon received a new request! ");
        synchronized (delaySocket) {
          try {
            delaySocket.call();
          } catch (IOException e) {
            Log.e(TAG, "Caught IOException while batch executing requests.");
            delaySocket.close();
          }
          if (delaySocket.isDone()) {
            // Close the socket and leave it for the GC to be reclaimed.
            delaySocket.forceClose();
          } else {
            // Put the delay socket back in the queue.
            mDelayQueue.offer(delaySocket);
          }
        }
      } catch (InterruptedException e) {
        Log.e(TAG, "Background daemon interuptted.");
        mRunning = false;
      }
    }
    Log.i(TAG, "Background daemon closing.");
  }

  /**
   * Adds a {@link DelaySocket} to the queue.
   */
  public void add(DelaySocket delaySocket) {
    mDelayQueue.add(delaySocket);
  }

  /**
   * Shutdown this thread.
   */
  public void close() {
    mRunning = false;
  }

  /**
   * Callback interface used to report responses back to the service.
   */
  static interface RequestCallback {
    /**
     * Report the response to the client. This method is called on a background
     * thread, so we must synchronize with the main UI thread ourselves before
     * delivering the response to the client.
     */
    void onRequestComplete(Response response);

    /**
     * Report to the client that the request resulted in an Exception.
     */
    void onException(Exception exception);
  }
}
