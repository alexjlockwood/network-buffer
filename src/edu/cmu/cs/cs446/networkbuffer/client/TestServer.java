package edu.cmu.cs.cs446.networkbuffer.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public class TestServer extends Thread {
  private static final String TAG = TestServer.class.getSimpleName();
  private static final int DEFAULT_POOL_SIZE = 16;
  private static final int DEFAULT_PORT = 4444;

  private final int port;
  private final ExecutorService pool;

  private ServerSocket mServerSocket = null;
  private boolean mRunning = false;

  public TestServer() {
    this(DEFAULT_PORT, DEFAULT_POOL_SIZE);
  }

  public TestServer(int port) {
    this(port, DEFAULT_POOL_SIZE);
  }

  public TestServer(int port, int poolSize) {
    this.port = port;
    this.pool = Executors.newFixedThreadPool(poolSize);
  }

  @Override
  public void run() {
    try {
      mServerSocket = new ServerSocket(port);
    } catch (IOException e) {
      Log.e(TAG, "Could not open server socket on port: " + port);
      return;
    }

    Log.i(TAG, "Server waiting for incoming connections...");

    mRunning = true;
    while (mRunning) {
      try {
        Socket clientSocket = mServerSocket.accept();
        Log.i(TAG, "Server received incoming connection!");
        pool.execute(new RequestHandler(clientSocket));
      } catch (IOException e) {
        Log.e(TAG, "Server received IOException while listening for clients...");
        mRunning = false;
        break;
      }
    }

    Log.i(TAG, "Server closing...");

    try {
      if (mServerSocket != null) {
        mServerSocket.close();
        mServerSocket = null;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to close server socket.");
    }
  }

  public synchronized void close() {
    mRunning = false;
    try {
      if (mServerSocket != null) {
        mServerSocket.close();
        mServerSocket = null;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to close server socket.");
    }
  }

  private static class RequestHandler implements Runnable {
    private final Socket socket;

    public RequestHandler(Socket s) {
      this.socket = s;
    }

    @Override
    public void run() {
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader = null;
      PrintWriter printWriter = null;
      try {
        inputStreamReader = new InputStreamReader(socket.getInputStream());
        bufferedReader = new BufferedReader(inputStreamReader);
        String text = bufferedReader.readLine();
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.write("Echo: " + text);
      } catch (IOException e) {
        Log.e(TAG, "Error reading/writing to stream.");
        return;
      }

      try {
        if (printWriter != null) {
          printWriter.close();
        }
        if (inputStreamReader != null) {
          inputStreamReader.close();
        }
        socket.close();
      } catch (IOException e) {
        Log.e(TAG, "Error closing sockets and streams.");
      }
    }
  }
}
