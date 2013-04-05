package edu.cmu.cs.cs446.wifibuffer;

import android.os.Parcel;
import android.os.Parcelable;

public class Request implements Parcelable {

  public static final int PRIORITY_LOW = 0;
  public static final int PRIORITY_HIGH = 1;
  public static final int PRIORITY_IMMEDIETE = 2;

  private String mDstName;
  private int mDstPort;
  private byte[] mPayload;

  public Request(String dstName, int dstPort, byte[] payload) {
    mDstName = dstName;
    mDstPort = dstPort;
    mPayload = payload;
  }

  public String getDstName() {
    return mDstName;
  }

  public int getDstPort() {
    return mDstPort;
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
    mDstName = in.readString();
    mDstPort = in.readInt();
    mPayload = new byte[in.readInt()];
    in.readByteArray(mPayload);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(mDstName);
    dest.writeInt(mDstPort);
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
}
