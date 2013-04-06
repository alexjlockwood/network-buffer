package edu.cmu.cs.cs446.networkbuffer;

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
import edu.cmu.cs.cs446.networkbuffer.RequestExecutor.DelayedRequest;
import edu.cmu.cs.cs446.networkbuffer.RequestExecutor.RequestCallback;
import edu.cmu.cs.cs446.networkbuffer.client.ClientActivity;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application. Because it can be in another process,
 * we must use IPC to interact with it. The {@link ClientActivity} class shows
 * how to interact with the service.
 */
@SuppressLint("HandlerLeak")
public class NetworkService extends Service implements RequestCallback {
  private static final String TAG = NetworkService.class.getSimpleName();

  private static final int RESPOND_TO_CLIENT = 0;
  private RemoteCallbackList<INetworkServiceCallback> mCallbacks;
  private NotificationManager mNotificationManager;
  private DelayQueue<DelayedRequest> mDelayQueue;
  private RequestExecutor mThread;

  @Override
  public void onCreate() {
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mCallbacks = new RemoteCallbackList<INetworkServiceCallback>();
    mDelayQueue = new DelayQueue<DelayedRequest>();
    mThread = new RequestExecutor(mDelayQueue, this);
    mThread.start();
    showNotification();
  }

  @Override
  public void onDestroy() {
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    mNotificationManager.cancel(R.string.remote_service_started);
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

  private final INetworkService.Stub mBinder = new INetworkService.Stub() {
    @Override
    public void registerCallback(INetworkServiceCallback callback) {
      if (callback != null) {
        mCallbacks.register(callback);
      }
    }

    @Override
    public void unregisterCallback(INetworkServiceCallback callback) {
      if (callback != null) {
        mCallbacks.unregister(callback);
      }
    }

    @Override
    public void send(Request request) {
      Log.i(TAG, "Service received request: " + request.toString());
      // Queue up 10 dummy requests in rapid succession
      for (int i=0; i<10; i++) {
        mDelayQueue.put(new DelayedRequest(request));
      }
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
          // TODO: Broadcast requests to the original calling client only...
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
    Notification notification = new Notification.Builder(this)
        .setSmallIcon(R.drawable.device_access_network_wifi)
        .setLargeIcon(largeIcon)
        .setContentTitle(getText(R.string.remote_service_label))
        .setContentText(getText(R.string.remote_service_started))
        .setWhen(System.currentTimeMillis())
        .getNotification();

    // Send the notification. We use a string id because it is a unique number.
    // We use it later to cancel.
    mNotificationManager.notify(R.string.remote_service_started, notification);
  }
}
