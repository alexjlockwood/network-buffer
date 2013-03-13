package edu.cmu.cs.cs446.wifibuffer;

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
import android.widget.Toast;
import edu.cmu.cs.cs446.wifibuffer.client.ClientActivity;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application. Because it can be in another process,
 * we must use IPC to interact with it. The {@link Controller} and
 * {@link ClientActivity} classes show how to interact with the service.
 *
 * Note that most applications <strong>do not</strong> need to deal with the
 * complexity shown here. If your application simply has a service running in
 * its own process, the {@link LocalService} sample shows a much simpler way to
 * interact with it.
 */
@SuppressLint("HandlerLeak")
public class WifiBufferService extends Service {

  private static final int REPORT_MSG = 1;

  /**
   * Our Handler used to execute operations on the main thread. This is used to
   * schedule increments of our value.
   */
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case REPORT_MSG: {
          int value = mValue++;

          // Broadcast to all clients the new value.
          final int numCallbacks = mCallbacks.beginBroadcast();
          for (int i = 0; i < numCallbacks; i++) {
            try {
              mCallbacks.getBroadcastItem(i).onServiceResponse("" + value);
            } catch (RemoteException e) {
              // The RemoteCallbackList will take care of removing
              // the dead object for us.
            }
          }
          mCallbacks.finishBroadcast();

          // Repeat every 1 second.
          sendMessageDelayed(obtainMessage(REPORT_MSG), 1 * 1000);
        }
          break;
        default:
          super.handleMessage(msg);
      }
    }
  };

  
  /**
   * A list of callbacks that have been registered with the service.
   */
  private RemoteCallbackList<IWifiBufferServiceCallback> mCallbacks = new RemoteCallbackList<IWifiBufferServiceCallback>();

  private int mValue = 0;
  private NotificationManager mNM;

  @Override
  public void onCreate() {
    mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    // Display a notification about us starting.
    showNotification();

    // While this service is running, it will continually increment a
    // number. Send the first message that is used to perform the
    // increment.
    mHandler.sendEmptyMessage(REPORT_MSG);
  }

  @Override
  public void onDestroy() {
    // Cancel the persistent notification.
    mNM.cancel(R.string.remote_service_started);

    // Tell the user we stopped.
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();

    // Unregister all callbacks.
    mCallbacks.kill();

    // Remove the next pending message to increment the counter, stopping
    // the increment loop.
    mHandler.removeMessages(REPORT_MSG);
  }

  @Override
  public IBinder onBind(Intent intent) {  
    return mBinder;
  }

  /**
   * The remote interface is defined through IDL.
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
  };

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    Toast.makeText(this, "Task removed: " + rootIntent, Toast.LENGTH_LONG).show();
  }

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {  
    Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.device_access_network_wifi);
    
    @SuppressWarnings("deprecation")
    Notification notification = new Notification.Builder(this)
        .setSmallIcon(R.drawable.device_access_network_wifi)
        .setLargeIcon(largeIcon)
        .setContentTitle(getText(R.string.remote_service_label))
        .setContentText(getText(R.string.remote_service_started))
        .setWhen(System.currentTimeMillis())
        .getNotification();

    // Send the notification. We use a string id because it is a unique number. We use it later to cancel.
    mNM.notify(R.string.remote_service_started, notification);
  }
}
