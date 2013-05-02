package edu.cmu.cs.cs446.networkbuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import android.util.Log;

/**
 * Handles all requests for a distinct host/port address.
 *
 * TODO: Document the thread safety of this class.
 */
public class DelaySocket implements Delayed, Callable<Void> {
  private static final String TAG = DelaySocket.class.getSimpleName();

  // DelaySocket
  // - Socket
  // - SendQueue (sorted by arrival)
  // - state (don't accept any sends if closing, have a close() method
  // in the client API)
  // - the minimum deadline
  // - handle (specifies the id for the delay socket). for now can just be a
  // random number.

  // Sort DelaySockets in queue (so you can keep min global DelaySocket) and
  // then delay execute the delay sockets.

  private final String mHost;
  private final int mPort;
  private Socket mSocket;

  private final Queue<Request> mRequests = new ConcurrentLinkedQueue<Request>();
  private volatile long mDeadline = Long.MAX_VALUE;
  private volatile boolean mClosed = false;

  /**
   * Each DelaySocket handles all requests made to a distinct host/port address.
   *
   * @param host
   * @param port
   */
  DelaySocket(String host, int port) {
    mHost = host;
    mPort = port;
  }

  /**
   * Adds a request to this delay socket's request queue to be executed before
   * the given delay.
   *
   * @param request
   * @param delay
   */
  public void add(Request request, long delay) {
    if (!mClosed) {
      synchronized (this) {
        mRequests.add(request);
        mDeadline = Math.min(mDeadline, System.currentTimeMillis() + delay);
      }
    }
  }

  /**
   * Prevent this queue from accepting further requests.
   */
  public void close() {
    mClosed = true;
  }

  /**
   * Returns true if this delayed socket has been closed and all requests have
   * been executed.
   */
  public synchronized boolean isDone() {
    return mClosed && mRequests.isEmpty();
  }

  /**
   * Batch executes all requests present in this delay socket's request queue.
   * This method is not thread safe.
   */
  @Override
  public Void call() throws IOException {
    Log.i(TAG, "Background daemon executing outstanding requests...");

    if (mSocket == null) {
      mSocket = new Socket(mHost, mPort);
    }

    PrintWriter out = new PrintWriter(mSocket.getOutputStream(), true);

    synchronized (this) {
      for (Request request : mRequests) {
        out.println(new String(request.getPayload()));
      }
      mDeadline = Long.MAX_VALUE;
      mRequests.clear();
    }

    out.close();
    return null;
  }

  void forceClose() {
    Log.i(TAG, "Force closing delay socket...");

    if (mSocket != null) {
      try {
        mSocket.close();
      } catch (IOException ignore) {
      }
      mSocket = null;
    }
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return mDeadline - System.currentTimeMillis();
  }

  @Override
  public int compareTo(Delayed delayed) {
    long deadline = ((DelaySocket) delayed).mDeadline;
    return mDeadline > deadline ? 1 : mDeadline < deadline ? -1 : 0;
  }
}
