package edu.cmu.cs.cs446.networkbuffer;

import java.util.PriorityQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

class RequestExecutor extends Thread {
  private static final String TAG = RequestExecutor.class.getSimpleName();

  private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  private final PriorityQueue<DelayedRequest> mRequests;
  private final DelayQueue<DelayedRequest> mDelayQueue;
  private final RequestCallback mCallback;
  private boolean mRunning;

  public RequestExecutor(RequestCallback callback) {
    mRequests = new PriorityQueue<DelayedRequest>();
    mDelayQueue = new DelayQueue<DelayedRequest>();
    mCallback = callback;
    mRunning = false;
  }

  @Override
  public void run() {
    Log.i(TAG, "Background daemon running... ");
    mRunning = true;

    while (mRunning) {
      try {
        Log.i(TAG, "Background daemon waiting for delayed request... ");
        mDelayQueue.take();
        synchronized (mDelayQueue) {
          Log.i(TAG, "Executing requests in batch...");
          for (final DelayedRequest request : mRequests) {
            mExecutor.execute(new Runnable() {
              @Override
              public void run() {
                Log.i(TAG, "Background daemon executing request on Executor: " + request.toString());
                Response response;
                try {
                  response = request.call();
                  Log.i(TAG, "Background daemon executed request and received response: " + response.toString());
                  mCallback.onRequestComplete(response);
                } catch (Exception e) {
                  mCallback.onException(e);
                }
              }
            });
          }
          mDelayQueue.clear();
          mRequests.clear();
        }
      } catch (InterruptedException e) {
        Log.e(TAG, "Thread interuptted while waiting for delayed request!");
        mRunning = false;
      }
    }
  }

  public void put(DelayedRequest request) {
    synchronized (mDelayQueue) {
      mDelayQueue.put(request);
      mRequests.add(request);
    }
  }

  /**
   * Shutdown this thread.
   */
  public synchronized void close() {
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
