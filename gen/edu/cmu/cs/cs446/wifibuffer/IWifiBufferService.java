/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/alex/Documents/workspace/NetworksPracticum/src/edu/cmu/cs/cs446/wifibuffer/IWifiBufferService.aidl
 */
package edu.cmu.cs.cs446.wifibuffer;
/**
 * Defines an interface for calling the remote wifi buffer service 
 * (which runs in a separate process).
 */
public interface IWifiBufferService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.cmu.cs.cs446.wifibuffer.IWifiBufferService
{
private static final java.lang.String DESCRIPTOR = "edu.cmu.cs.cs446.wifibuffer.IWifiBufferService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.cmu.cs.cs446.wifibuffer.IWifiBufferService interface,
 * generating a proxy if needed.
 */
public static edu.cmu.cs.cs446.wifibuffer.IWifiBufferService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.cmu.cs.cs446.wifibuffer.IWifiBufferService))) {
return ((edu.cmu.cs.cs446.wifibuffer.IWifiBufferService)iin);
}
return new edu.cmu.cs.cs446.wifibuffer.IWifiBufferService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback _arg0;
_arg0 = edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback _arg0;
_arg0 = edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback.Stub.asInterface(data.readStrongBinder());
this.unregisterCallback(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.cmu.cs.cs446.wifibuffer.IWifiBufferService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
     * Register the callback interface with the service.
     */
@Override public void registerCallback(edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback cb) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Unregister the callback interface with the service.
     */
@Override public void unregisterCallback(edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback cb) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((cb!=null))?(cb.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
/**
     * Register the callback interface with the service.
     */
public void registerCallback(edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback cb) throws android.os.RemoteException;
/**
     * Unregister the callback interface with the service.
     */
public void unregisterCallback(edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback cb) throws android.os.RemoteException;
}
