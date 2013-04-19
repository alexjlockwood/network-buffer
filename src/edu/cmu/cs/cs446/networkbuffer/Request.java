package edu.cmu.cs.cs446.networkbuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Callable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Request implements Parcelable, Callable<Response> {
  private static final String TAG = Request.class.getSimpleName();

  private String mHost;
  private int mPort;
  private byte[] mPayload;

  public Request(String dstName, int dstPort, byte[] payload) {
    mHost = dstName;
    mPort = dstPort;
    mPayload = payload;
  }

  public String getDstName() {
    return mHost;
  }

  public int getDstPort() {
    return mPort;
  }

  public byte[] getPayload() {
    return mPayload;
  }

  @Override
  public Response call() throws IOException {
    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;

    try {
      socket = new Socket(mHost, mPort);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (UnknownHostException e) {
      Log.e(TAG, "Could not resolve host: " + mHost);
      throw new UnknownHostException("Could not resolve host: " + mHost);
    } catch (IOException e) {
      Log.e(TAG, "IOException while trying to connect to connect to " + mHost + " on port " + mPort);
      throw new IOException("Could not establish connection with host " + mHost + " on port " + mPort);
    }

    out.println(new String(mPayload));
    String response = in.readLine();

    try {
      out.close();
      in.close();
      socket.close();
    } catch (IOException e) {
      Log.e(TAG, "Could not close client/server connection.");
    }

    return new Response(response.getBytes());
  }

  public Request(Parcel in) {
    readFromParcel(in);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public void readFromParcel(Parcel in) {
    mHost = in.readString();
    mPort = in.readInt();
    mPayload = new byte[in.readInt()];
    in.readByteArray(mPayload);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(mHost);
    dest.writeInt(mPort);
    dest.writeInt(mPayload.length);
    dest.writeByteArray(mPayload);
  }

  public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
    @Override
    public Request createFromParcel(Parcel in) {
      return new Request(in);
    }

    @Override
    public Request[] newArray(int size) {
      return new Request[size];
    }
  };

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Request)) {
      return false;
    }
    Request other = (Request) o;
    return mHost.equals(other.mHost) && mPort == other.mPort && Arrays.equals(mPayload, other.mPayload);
  }

  @Override
  public int hashCode() {
    // TODO: don't be dumb
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return "[dstName=" + mHost + ", dstPort=" + mPort + ", payload=" + new String(mPayload) + "]";
  }
}
