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

  private static final ExecutorService mPool = Executors.newFixedThreadPool(16);
  private static final int mPort = 4444;

  private ServerSocket mServerSocket = null;
  private boolean mRunning = true;

  @Override
  public void run() {
    try {
      mServerSocket = new ServerSocket(mPort);
    } catch (IOException e) {
      Log.e(TAG, "Could not open server socket on port: " + mPort);
      return;
    }

    Log.i(TAG, "Test server waiting for incoming connections...");

    while (mRunning) {
      try {
        Socket clientSocket = mServerSocket.accept();
        Log.i(TAG, "Server received incoming connection!");
        mPool.execute(new RequestHandler(clientSocket));
      } catch (IOException e) {
        Log.e(TAG, "Server received IOException while listening for clients...");
        break;
      }
    }

    Log.i(TAG, "Server closing...");
    close();
  }

  public void close() {
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
        printWriter = new PrintWriter(socket.getOutputStream(), true);

        String text;
        while ((text = bufferedReader.readLine()) != null) {
          printWriter.println(text);
        }
      } catch (IOException e) {
        Log.e(TAG, "Error reading/writing to stream.");
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