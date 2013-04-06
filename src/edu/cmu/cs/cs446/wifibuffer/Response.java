package edu.cmu.cs.cs446.wifibuffer;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class Response implements Parcelable {
  private byte[] mPayload;

  public Response(byte[] payload) {
    mPayload = payload;
  }

  public byte[] getPayload() {
    return mPayload;
  }

  public Response(Parcel in) {
    readFromParcel(in);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public void readFromParcel(Parcel in) {
    mPayload = new byte[in.readInt()];
    in.readByteArray(mPayload);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(mPayload.length);
    dest.writeByteArray(mPayload);
  }

  public static final Parcelable.Creator<Response> CREATOR = new Parcelable.Creator<Response>() {
    @Override
    public Response createFromParcel(Parcel in) {
      return new Response(in);
    }

    @Override
    public Response[] newArray(int size) {
      return new Response[size];
    }
  };

  @Override
  public String toString() {
    return "[payload=" + new String(mPayload) + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Response)) {
      return false;
    }
    Response other = (Response) o;
    return Arrays.equals(mPayload, other.mPayload);
  }

  @Override
  public int hashCode() {
    // TODO: don't be dumb
    return toString().hashCode();
  }
}
