package edu.cmu.cs.cs446.networkbuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import edu.cmu.cs.cs446.networkbuffer.DelaySocketExecutor.ResponseCallback;

/**
 * Handles all requests for a distinct host/port address.
 *
 * TODO: Document the thread safety of this class.
 */
public class DelaySocket implements Delayed, Callable<Void> {
  private static final String TAG = DelaySocket.class.getSimpleName();

  private final String mHost;
  private final int mPort;
  private final ResponseCallback mCallback;
  private Socket mSocket;
  private BufferedReader mIn;
  private PrintWriter mOut;
  private NetworkThread mNetworkThread;

  private final ConcurrentLinkedQueue<ParcelableByteArray> mRequests;
  private volatile long mOrigin;
  private volatile long mDelay;
  private volatile boolean mClosed;

  /**
   * Loops forever, reading responses from a server and forwarding them to the
   * client.
   */
  private class NetworkThread extends Thread {
    private final String TAG = NetworkThread.class.getSimpleName();
    private volatile boolean mRunning = true;

    @Override
    public void run() {
      // Log.i(TAG, "Running...");
      while (mRunning) {
        try {
          // Log.i(TAG, "Waiting for next read...");
          String response = mIn.readLine();
          if (response != null) {
            byte[] bytes = response.getBytes();
            ParcelableByteArray responseParcel = new ParcelableByteArray(bytes);
            mCallback.onReceive(responseParcel);
          }
        } catch (IOException e) {
          mRunning = false;
        }
      }
      // Log.i(TAG, "Closing...");
    }

    public void close() {
      mRunning = false;
    }
  }

  /**
   * Each DelaySocket handles all requests made to a distinct host/port address.
   *
   * @param host
   * @param port
   */
  DelaySocket(String host, int port, ResponseCallback callback) {
    mCallback = callback;
    mHost = host;
    mPort = port;
    mRequests = new ConcurrentLinkedQueue<ParcelableByteArray>();
    mDelay = Long.MAX_VALUE;
    mClosed = false;
  }

  /**
   * Adds a request to this delay socket's request queue to be executed before
   * the given delay.
   *
   * @param request
   * @param delay
   */
  public synchronized void add(ParcelableByteArray request, long delay) {
    Log.i(TAG, "add(ParcelableByteArray, long)");
    if (!mClosed) {
      mRequests.add(request);
      if (delay < mDelay) {
        mOrigin = System.currentTimeMillis();
        mDelay = delay;
      }
      Log.i(TAG, "Request size: " + mRequests.size() + ", " + "Delay: " + mDelay);
    }
  }

  /**
   * Prevent this queue from accepting further requests.
   */
  public synchronized void close() {
    Log.i(TAG, "close()");
    mClosed = true;
  }

  /**
   * Returns true if this delayed socket has been closed and all requests have
   * been executed.
   */
  public synchronized boolean isDone() {
    Log.i(TAG, "isDone()");
    return mClosed && mRequests.isEmpty();
  }

  /**
   * Batch executes all requests present in this delay socket's request queue.
   * This method is not thread safe and should only ever be called on the
   * NetworkService's main executor thread.
   */
  @Override
  public synchronized Void call() throws IOException {
    Log.i(TAG, "Background daemon executing outstanding requests...");

    if (mSocket == null) {
      mSocket = new Socket(mHost, mPort);
      mOut = new PrintWriter(mSocket.getOutputStream(), true);
      mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
      mNetworkThread = new NetworkThread();
      mNetworkThread.start();
    }

    while (!mRequests.isEmpty()) {
      ParcelableByteArray request = mRequests.poll();
      mOut.println(new String(request.getPayload()));
    }
    mDelay = Long.MAX_VALUE;

    return null;
  }

  void forceClose() {
    Log.i(TAG, "forceClose()");

    mNetworkThread.close();

    if (mOut != null) {
      mOut.close();
    }

    if (mIn != null) {
      try {
        mIn.close();
      } catch (IOException ignore) {
      }
    }

    if (mSocket != null) {
      try {
        mSocket.close();
      } catch (IOException ignore) {
      }
    }
  }

  @Override
  public synchronized long getDelay(TimeUnit unit) {
    return unit.convert(mDelay - (System.currentTimeMillis() - mOrigin), TimeUnit.MILLISECONDS);
  }

  @Override
  public synchronized int compareTo(Delayed delayed) {
    long diff = mDelay - ((DelaySocket) delayed).mDelay;
    return diff > 0 ? 1 : diff < 0 ? -1 : 0;
  }
}
