package edu.cmu.cs.cs446.wifibuffer;

import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

public class RequestQueueThread extends Thread {
  private static final String TAG = RequestQueueThread.class.getSimpleName();

  private final Queue<Request> mQueue = new LinkedList<Request>();
  private final Object mTicket = new Object();
  private boolean mRunning;
  private RequestQueueCallback mCallback;

  public RequestQueueThread(RequestQueueCallback callback) {
    mCallback = callback;
    mRunning = false;
  }

  @Override
  public void run() {
    while (mRunning) {
      Request request = poll();
      if (request != null) {
        try {
          mCallback.onRequestComplete(new Response(request.getPayload()));
        } catch (Exception e) {
          // TODO: catch exceptions
        }
      }
    }
  }

  private Request poll() {
    while (mRunning) {
      synchronized (mQueue) {
        if (mQueue.size() > 0) {
          Request request = mQueue.remove();
          if (request != null) {
            return request;
          }
        }
      }
      synchronized (mTicket) {
        try {
          mTicket.wait();
        } catch (InterruptedException ignore) {
          // TODO: handle this case
          Log.e(TAG, "Thread interrupted!");
        }
      }
    }
    return null;
  }

  public synchronized void invokeLater(Request request) {
    synchronized (mQueue) {
      mQueue.add(request);
    }
    synchronized (mTicket) {
      mTicket.notify();
    }
  }

  public synchronized void close() {
    if (mRunning) {
      mRunning = false;
      synchronized (mTicket) {
        mTicket.notify();
      }
    }
  }

  public static interface RequestQueueCallback {
    void onRequestComplete(Response response);
  }
}
