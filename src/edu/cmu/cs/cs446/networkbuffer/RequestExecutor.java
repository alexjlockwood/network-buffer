package edu.cmu.cs.cs446.networkbuffer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import android.util.Log;

class RequestExecutor extends Thread {
  private static final String TAG = RequestExecutor.class.getSimpleName();

  private RequestCallback mCallback;
  private DelayQueue<DelayedRequest> mDelayQueue;
  private boolean mRunning;

  public RequestExecutor(DelayQueue<DelayedRequest> delayQueue, RequestCallback callback) {
    mDelayQueue = delayQueue;
    mRunning = false;
    mCallback = callback;
  }

  @Override
  public void run() {
    Log.i(TAG, "Background daemon running... ");
    mRunning = true;
    while (mRunning) {
      try {
        Log.i(TAG, "Background daemon waiting for next delayed request... ");
        DelayedRequest delayed = mDelayQueue.take();
        Request request = delayed.getRequest();
        Log.i(TAG, "Background daemon found new delayed request: " + request.toString());
        Response response = request.execute();
        Log.i(TAG, "Background daemon executed request and received response: " + response.toString());
        mCallback.onRequestComplete(response);
      } catch (InterruptedException e) {
        Log.e(TAG, "Thread interuptted while waiting for delayed request!");
        mRunning = false;
      }
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
  }

  /**
   * Wrapper class around Request for use in the DelayQueue.
   */
  static class DelayedRequest implements Delayed {
    private final long mOrigin;
    private final long mDelay;
    private final Request mRequest;

    public DelayedRequest(Request request) {
      mOrigin = System.currentTimeMillis();
      mDelay = 5000 - (mOrigin % 5000);
      mRequest = request;
    }

    public Request getRequest() {
      return mRequest;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(mDelay - (System.currentTimeMillis() - mOrigin), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed delayed) {
      if (this == delayed)
        return 0;

      long diff;
      if (delayed instanceof DelayedRequest) {
        diff = mDelay - ((DelayedRequest) delayed).mDelay;
      } else {
        diff = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
      }

      if (diff > 0)
        return 1;
      if (diff < 0)
        return -1;
      return 0;
    }

  }

}
