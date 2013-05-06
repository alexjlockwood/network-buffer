package edu.cmu.cs.cs446.networkbuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.util.Log;

/**
 * Handles all requests for a distinct host/port address.
 */
public class DelaySocket implements Callable<Void> {
  private static final String TAG = DelaySocket.class.getSimpleName();

  private final String mHost;
  private final int mPort;
  private final ResponseCallback mCallback;

  private final Queue<ParcelableByteArray> mRequests = new LinkedList<ParcelableByteArray>();
  private final Queue<ScheduledFuture<Void>> mFutures = new LinkedList<ScheduledFuture<Void>>();
  private boolean mShutdown = false;

  private Socket mSocket;
  private BufferedReader mIn;
  private PrintWriter mOut;
  private NetworkThread mNetworkThread;

  /**
   * Each DelaySocket handles all requests made to a distinct host/port address.
   */
  DelaySocket(String host, int port, ResponseCallback callback) {
    mCallback = callback;
    mHost = host;
    mPort = port;
  }

  /**
   * Adds a request to this delay socket's request queue to be executed before
   * the given delay.
   */
  public synchronized void add(ScheduledExecutorService scheduler, ParcelableByteArray request, long delay) {
    Log.i(TAG, "add(ParcelableByteArray, long)");
    if (!mShutdown) {
      ScheduledFuture<Void> future = scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
      mRequests.add(request);
      mFutures.add(future);
    }
  }

  /**
   * Prevent this queue from accepting further requests.
   */
  public synchronized void shutdown() {
    Log.i(TAG, "close()");
    mShutdown = true;
    if (mRequests.isEmpty() && mFutures.isEmpty()) {
      close();
    }
  }

  public synchronized boolean isTerminated() {
    Log.i(TAG, "isTerminated()");
    return mShutdown && mRequests.isEmpty() && mFutures.isEmpty();
  }

  /**
   * Batch executes all requests present in this delay socket's request queue.
   * This method should only ever be called on the NetworkService's main
   * executor thread.
   */
  @Override
  public synchronized Void call() throws IOException {
    Log.i(TAG, "Background daemon executing pending requests...");

    if (mSocket == null) {
      mSocket = new Socket(mHost, mPort);
      mOut = new PrintWriter(mSocket.getOutputStream(), true);
      mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
      mNetworkThread = new NetworkThread();
      mNetworkThread.start();
    }

    // Execute pending requests
    for (ParcelableByteArray request : mRequests) {
      Log.i(TAG, "Sending request: " + new String(request.getPayload()));
      mOut.println(new String(request.getPayload()));
    }

    // Cancel all other future executions
    for (ScheduledFuture<Void> future : mFutures) {
      future.cancel(false);
    }

    mRequests.clear();
    mFutures.clear();

    return null;
  }

  public void close() {
    Log.i(TAG, "close()");

    synchronized (this) {
      mShutdown = true;
      mRequests.clear();
      mFutures.clear();
    }

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

  /**
   * Loops forever, reading responses from a server and forwarding them to the
   * client.
   */
  private class NetworkThread extends Thread {
    @Override
    public void run() {
      try {
        String response;
        while ((response = mIn.readLine()) != null) {
          mCallback.onReceive(new ParcelableByteArray(response.getBytes()));
        }
      } catch (IOException e) {
      }
    }
  }
}
