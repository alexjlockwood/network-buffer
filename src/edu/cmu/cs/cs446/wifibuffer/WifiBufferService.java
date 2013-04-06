package edu.cmu.cs.cs446.wifibuffer;

import java.util.concurrent.DelayQueue;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import edu.cmu.cs.cs446.wifibuffer.Request.DelayedRequest;
import edu.cmu.cs.cs446.wifibuffer.RequestExecutor.RequestCallback;
import edu.cmu.cs.cs446.wifibuffer.client.ClientActivity;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application. Because it can be in another process,
 * we must use IPC to interact with it. The {@link ClientActivity} class shows
 * how to interact with the service.
 */
@SuppressLint("HandlerLeak")
public class WifiBufferService extends Service implements RequestCallback {
  private static final String TAG = WifiBufferService.class.getSimpleName();
  private static final int RESPOND_TO_CLIENT = 0;

  private DelayQueue<DelayedRequest> mDelayQueue;
  private RequestExecutor mThread;

  /** A list of callbacks that have been registered with the service. */
  private RemoteCallbackList<IWifiBufferServiceCallback> mCallbacks = new RemoteCallbackList<IWifiBufferServiceCallback>();
  private NotificationManager mNotificationManager;

  @Override
  public void onCreate() {
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    showNotification();
    mDelayQueue = new DelayQueue<DelayedRequest>();
    mThread = new RequestExecutor(mDelayQueue, this);
    mThread.start();
  }

  @Override
  public void onDestroy() {
    mNotificationManager.cancel(R.string.remote_service_started);
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    mCallbacks.kill();
    mHandler.removeMessages(RESPOND_TO_CLIENT);
    mThread.close();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    Toast.makeText(this, "Task removed: " + rootIntent, Toast.LENGTH_LONG).show();
  }

  /**
   * The remote interface is defined through AIDL.
   */
  private final IWifiBufferService.Stub mBinder = new IWifiBufferService.Stub() {
    @Override
    public void registerCallback(IWifiBufferServiceCallback cb) {
      if (cb != null) {
        mCallbacks.register(cb);
      }
    }

    @Override
    public void unregisterCallback(IWifiBufferServiceCallback cb) {
      if (cb != null) {
        mCallbacks.unregister(cb);
      }
    }

    @Override
    public void send(Request request) {
      Log.i(TAG, "Service received request: " + request.toString());
      mDelayQueue.put(new DelayedRequest(request));
      mDelayQueue.put(new DelayedRequest(request));
      mDelayQueue.put(new DelayedRequest(request));
      mDelayQueue.put(new DelayedRequest(request));
      mDelayQueue.put(new DelayedRequest(request));
    }
  };

  @Override
  public void onRequestComplete(Response response) {
    Log.i(TAG, "Dispatching response to client: " + response.toString());
    mHandler.dispatchMessage(mHandler.obtainMessage(RESPOND_TO_CLIENT, response));
  }

  /**
   * Our Handler used to execute operations on the main thread. This is used to
   * schedule increments of our value.
   */
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case RESPOND_TO_CLIENT: {
          // Broadcast to all clients the new value.
          final int N = mCallbacks.beginBroadcast();
          for (int i = 0; i < N; i++) {
            try {
              mCallbacks.getBroadcastItem(i).receive((Response) msg.obj);
            } catch (RemoteException e) {
              // The RemoteCallbackList will take care of removing
              // the dead object for us.
            }
          }
          mCallbacks.finishBroadcast();
          break;
        }
        default:
          super.handleMessage(msg);
      }
    }
  };

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {
    Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.device_access_network_wifi);

    @SuppressWarnings("deprecation")
    Notification notification = new Notification.Builder(this).setSmallIcon(R.drawable.device_access_network_wifi)
        .setLargeIcon(largeIcon).setContentTitle(getText(R.string.remote_service_label))
        .setContentText(getText(R.string.remote_service_started)).setWhen(System.currentTimeMillis()).getNotification();

    // Send the notification. We use a string id because it is a unique number.
    // We use it later to cancel.
    mNotificationManager.notify(R.string.remote_service_started, notification);
  }
}
