package edu.cmu.cs.cs446.networkbuffer;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable container class around a byte[] for our AIDL implementation.
 */
public final class ParcelableByteArray implements Parcelable {
  private byte[] mPayload;

  public ParcelableByteArray(byte[] payload) {
    mPayload = payload;
  }

  public byte[] getPayload() {
    return mPayload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof ParcelableByteArray)) {
      return false;
    }
    return Arrays.equals(mPayload, ((ParcelableByteArray) o).mPayload);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(mPayload);
  }

  @Override
  public String toString() {
    return "[payload=" + new String(mPayload) + "]";
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(mPayload.length);
    dest.writeByteArray(mPayload);
  }

  public static final Parcelable.Creator<ParcelableByteArray> CREATOR = new Parcelable.Creator<ParcelableByteArray>() {
    @Override
    public ParcelableByteArray createFromParcel(Parcel in) {
      byte[] payload = new byte[in.readInt()];
      in.readByteArray(payload);
      return new ParcelableByteArray(payload);
    }

    @Override
    public ParcelableByteArray[] newArray(int size) {
      return new ParcelableByteArray[size];
    }
  };
}
