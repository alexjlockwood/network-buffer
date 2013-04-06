package edu.cmu.cs.cs446.wifibuffer;

import java.util.concurrent.DelayQueue;

import android.util.Log;
import edu.cmu.cs.cs446.wifibuffer.Request.DelayedRequest;

public class RequestExecutor extends Thread {
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

  public synchronized void close() {
    mRunning = false;
  }

  public static interface RequestCallback {
    void onRequestComplete(Response response);
  }
}
