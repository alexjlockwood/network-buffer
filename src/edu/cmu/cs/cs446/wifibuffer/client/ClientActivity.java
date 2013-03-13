package edu.cmu.cs.cs446.wifibuffer.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.cs.cs446.wifibuffer.IWifiBufferService;
import edu.cmu.cs.cs446.wifibuffer.IWifiBufferServiceCallback;
import edu.cmu.cs.cs446.wifibuffer.R;

/**
 * Example of binding and unbinding to the remote service. This demonstrates
 * the implementation of a service which the client will bind to, interacting
 * with it through an aidl interface.
 */
@SuppressLint("HandlerLeak")
public class ClientActivity extends Activity {
  
  /** 
   * The primary interface we will be calling on the service. 
   */
  private IWifiBufferService mService = null;
  private TextView mTextView;
  private boolean mIsBound;

  /**
   * Standard initialization of this activity. Set up the UI, then wait for
   * the user to poke it before doing anything.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button bindButton = (Button) findViewById(R.id.bind);
    bindButton.setOnClickListener(mBindListener);
    
    Button unBindButton = (Button) findViewById(R.id.unbind);
    unBindButton.setOnClickListener(mUnbindListener);

    mTextView = (TextView) findViewById(R.id.callback);
    mTextView.setText("Not attached.");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mIsBound) {
      // Our client activity should unbind from the service
      unbindService(mConnection);
    }
  }
  
  /**
   * Used to interact with the service's interface.
   */
  private final ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      // This is called when the connection with the service has been
      // established, giving us the service object we can use to
      // interact with the service. We are communicating with our
      // service through an IDL interface, so get a client-side
      // representation of that from the raw service object.
      mService = IWifiBufferService.Stub.asInterface(service);
      mTextView.setText("Attached.");

      // We want to monitor the service for as long as we are
      // connected to it.
      try {
        mService.registerCallback(mCallback);
      } catch (RemoteException e) {
        // In this case the service has crashed before we could even
        // do anything with it; we can count on soon being
        // disconnected (and then reconnected if it can be restarted)
        // so there is no need to do anything here.
      }

      Toast.makeText(ClientActivity.this, R.string.remote_service_connected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been
      // unexpectedly disconnected -- that is, its process crashed.
      mService = null;
      mTextView.setText("Disconnected.");

      Toast.makeText(ClientActivity.this, R.string.remote_service_disconnected, Toast.LENGTH_SHORT).show();
    }
  };

  private final OnClickListener mBindListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      // Establish a couple connections with the service, binding
      // by interface names. This allows other applications to be
      // installed that replace the remote service by implementing
      // the same interface.
      bindService(new Intent(IWifiBufferService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
      mIsBound = true;
      mTextView.setText("Binding.");
    }
  };

  private final OnClickListener mUnbindListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      if (mIsBound) {
        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
          try {
            mService.unregisterCallback(mCallback);
          } catch (RemoteException e) {
            // There is nothing special we need to do if the service
            // has crashed.
          }
        }

        // Detach our existing connection.
        unbindService(mConnection);
        mIsBound = false;
        mTextView.setText("Unbinding.");
      }
    }
  };

  // ----------------------------------------------------------------------
  // Code showing how to deal with callbacks.
  // ----------------------------------------------------------------------

  /**
   * This implementation is used to receive callbacks from the remote service.
   */
  private final IWifiBufferServiceCallback mCallback = new IWifiBufferServiceCallback.Stub() {
    /**
     * This is called by the remote service regularly to tell us about new
     * values. Note that IPC calls are dispatched through a thread pool
     * running in each process, so the code executing here will NOT be running
     * in our main thread like most other things -- so, to update the UI, we
     * need to use a Handler to hop over there.
     */
    @Override
    public void onServiceResponse(String data) {
      mHandler.sendMessage(mHandler.obtainMessage(BUMP_MSG, Integer.valueOf(data), 0));
    }
  };

  private static final int BUMP_MSG = 1;

  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BUMP_MSG:
          mTextView.setText("Received from service: " + msg.arg1);
          break;
        default:
          super.handleMessage(msg);
      }
    }
  };
}
