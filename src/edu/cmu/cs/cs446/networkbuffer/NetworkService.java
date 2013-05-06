package edu.cmu.cs.cs446.networkbuffer;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import edu.cmu.cs.cs446.networkbuffer.client.ClientActivity;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application. Because it can be in another process,
 * we must use IPC to interact with it. The {@link ClientActivity} class shows
 * how to interact with the service.
 *
 * Note that this class relies on the fact that we schedule tasks to be executed
 * sequentially on a single background thread. A more intelligent implementation
 * would allow for the execution of many requests to many remote hosts
 * simultaneously.
 */
@SuppressLint("HandlerLeak")
public class NetworkService extends Service implements ResponseCallback {
  private static final String TAG = NetworkService.class.getSimpleName();

  private ScheduledExecutorService mScheduledExecutor;
  private RemoteCallbackList<INetworkServiceCallback> mCallbacks;
  private NotificationManager mNotificationManager;
  private ConcurrentHashMap<Long, DelaySocket> mDelaySockets;

  @Override
  public void onCreate() {
    // TODO: make this multi-threaded... someday.
    mScheduledExecutor = Executors.newScheduledThreadPool(1);

    // Start a periodic garbage collection task to check for and cleanup any
    // terminated delay sockets.
    mScheduledExecutor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        for (Long handle : mDelaySockets.keySet()) {
          DelaySocket delaySocket = mDelaySockets.get(handle);
          if (delaySocket != null && delaySocket.isTerminated()) {
            delaySocket.close();
            mDelaySockets.remove(handle);
          }
        }
      }
    }, 0, 4000, TimeUnit.MILLISECONDS);

    mCallbacks = new RemoteCallbackList<INetworkServiceCallback>();
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mDelaySockets = new ConcurrentHashMap<Long, DelaySocket>();
    showNotification();
  }

  @Override
  public void onDestroy() {
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    mScheduledExecutor.shutdown();
    mCallbacks.kill();
    mNotificationManager.cancel(R.string.remote_service_started);
    mHandler.removeMessages(RESPOND_TO_CLIENT);
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
      Log.i(TAG, "open(String, int)");
      DelaySocket delaySocket = new DelaySocket(host, port, NetworkService.this);
      long handle = random.nextLong();
      mDelaySockets.put(handle, delaySocket);
      return handle;
    }

    @Override
    public void send(long handle, ParcelableByteArray request, long delay) {
      Log.i(TAG, "send(long, ParcelableByteArray, long)");
      DelaySocket delaySocket = mDelaySockets.get(handle);
      if (delaySocket != null) {
        delaySocket.add(mScheduledExecutor, request, delay);
      }
    }

    @Override
    public void shutdown(long handle) {
      Log.i(TAG, "shutdown(long)");
      mDelaySockets.get(handle).shutdown();
    }
  };

  @Override
  public void onReceive(ParcelableByteArray response) {
    Log.i(TAG, "Dispatching response to client: " + response.toString());
    mHandler.dispatchMessage(mHandler.obtainMessage(RESPOND_TO_CLIENT, response));
  }

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {
    Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.device_access_network_wifi);

    //@formatter:off
    @SuppressWarnings("deprecation")
    Notification notification = new Notification.Builder(this)
        .setSmallIcon(R.drawable.device_access_network_wifi)
        .setLargeIcon(largeIcon)
        .setContentTitle(getText(R.string.remote_service_label))
        .setContentText(getText(R.string.remote_service_started))
        .setWhen(System.currentTimeMillis())
        .getNotification();
    //@formatter:on

    mNotificationManager.notify(R.string.remote_service_started, notification);
  }

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
}