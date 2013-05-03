package edu.cmu.cs.cs446.networkbuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import edu.cmu.cs.cs446.networkbuffer.DelaySocketExecutor.ResponseCallback;
import edu.cmu.cs.cs446.networkbuffer.client.ClientActivity;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application. Because it can be in another process,
 * we must use IPC to interact with it. The {@link ClientActivity} class shows
 * how to interact with the service.
 */
@SuppressLint("HandlerLeak")
public class NetworkService extends Service implements ResponseCallback {
  private static final String TAG = NetworkService.class.getSimpleName();

  private RemoteCallbackList<INetworkServiceCallback> mCallbacks;
  private NotificationManager mNotificationManager;
  private Map<Long, DelaySocket> mDelaySockets;
  private DelaySocketExecutor mExecutorThread;

  @Override
  public void onCreate() {
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mCallbacks = new RemoteCallbackList<INetworkServiceCallback>();
    mDelaySockets = new HashMap<Long, DelaySocket>();
    mExecutorThread = new DelaySocketExecutor();
    mExecutorThread.start();
    showNotification();
  }

  @Override
  public void onDestroy() {
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    mNotificationManager.cancel(R.string.remote_service_started);
    mCallbacks.kill();
    mHandler.removeMessages(RESPOND_TO_CLIENT);
    mExecutorThread.close();
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
   * We may assume that all of these methods will be called on the client
   * application's main UI thread.
   */
  private final INetworkService.Stub mBinder = new INetworkService.Stub() {

    // TODO: figure out a more reliable way of guaranteeing uniqueness.
    private final Random random = new Random();

    @Override
    public void registerCallback(INetworkServiceCallback callback) {
      Log.i(TAG, "registerCallback(INetworkServiceCallback)");
      if (callback != null) {
        mCallbacks.register(callback);
      }
    }

    @Override
    public void unregisterCallback(INetworkServiceCallback callback) {
      Log.i(TAG, "unregisterCallback(INetworkServiceCallback)");
      if (callback != null) {
        mCallbacks.unregister(callback);
      }
    }

    @Override
    public long open(String host, int port) {
      Log.i(TAG, "Service received open request!");
      long handle = random.nextLong();
      DelaySocket delaySocket = new DelaySocket(host, port, NetworkService.this);
      mDelaySockets.put(handle, delaySocket);
      // mExecutorThread.add(delaySocket);
      return handle;
    }

    @Override
    public void send(long handle, ParcelableByteArray request, long delay) {
      // Log.i(TAG, "Service received send request!");
      DelaySocket delaySocket = mDelaySockets.get(handle);
      synchronized (delaySocket) {
        delaySocket.add(request, delay);
        mExecutorThread.replace(delaySocket);
      }
    }

    @Override
    public void close(long handle) {
      Log.i(TAG, "Service received close request!");
      DelaySocket delaySocket = mDelaySockets.get(handle);
      delaySocket.close();
    }
  };

  @Override
  public void onReceive(ParcelableByteArray response) {
    Log.i(TAG, "Dispatching response to client: " + response.toString());
    mHandler.dispatchMessage(mHandler.obtainMessage(RESPOND_TO_CLIENT, response));
  }

  // @Override
  // public void onException(Exception exception) {
  // Log.i(TAG, "Notifying client that an exception occurred!");
  // Response response = new
  // Response("An exception occurred while processing the request!".getBytes());
  // mHandler.dispatchMessage(mHandler.obtainMessage(RESPOND_TO_CLIENT,
  // response));
  // }

  private static final int RESPOND_TO_CLIENT = 0;

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
              mCallbacks.getBroadcastItem(i).onReceive((ParcelableByteArray) msg.obj);
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
