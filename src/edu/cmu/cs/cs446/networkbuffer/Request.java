package edu.cmu.cs.cs446.networkbuffer;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public final class Request implements Parcelable {
  private String mHost;
  private int mPort;
  private byte[] mPayload;

  public Request(String host, int port, byte[] payload) {
    mHost = host;
    mPort = port;
    mPayload = payload;
  }

  public String getHost() {
    return mHost;
  }

  public int getPort() {
    return mPort;
  }

  public byte[] getPayload() {
    return mPayload;
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
    } else if (!(o instanceof Request)) {
      return false;
    }
    Request other = (Request) o;
    return mHost.equals(other.mHost)
        && mPort == other.mPort
        && Arrays.equals(mPayload, other.mPayload);
  }

  @Override
  public int hashCode() {
    // TODO: don't be dumb
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return "[host=" + mHost + ", port=" + mPort + ", payload=" + new String(mPayload) + "]";
  }
}
