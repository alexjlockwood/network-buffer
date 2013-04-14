package edu.cmu.cs.cs446.networkbuffer.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.cs.cs446.networkbuffer.INetworkService;
import edu.cmu.cs.cs446.networkbuffer.INetworkServiceCallback;
import edu.cmu.cs.cs446.networkbuffer.R;
import edu.cmu.cs.cs446.networkbuffer.Request;
import edu.cmu.cs.cs446.networkbuffer.Response;

/**
 * Example of binding and unbinding to the remote service. This demonstrates the
 * implementation of a service which the client will bind to, interacting with
 * it through an aidl interface.
 */
public class ClientActivity extends Activity {
  private static final String TAG = ClientActivity.class.getSimpleName();

  /** The primary interface we will be calling on the service. */
  private INetworkService mService = null;
  private TextView mTextView;
  private TextView mResponseTextView;
  private boolean mIsBound;

  private int mRequestCounter = 0;
  private int mResponseCounter = 0;

  /**
   * Standard initialization of this activity. Set up the UI, then wait for the
   * user to poke it before doing anything.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button bindButton = (Button) findViewById(R.id.bind);
    bindButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Establish a couple connections with the service, binding
        // by interface names. This allows other applications to be
        // installed that replace the remote service by implementing
        // the same interface.
        Intent intent = new Intent(INetworkService.class.getName());
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mTextView.setText("Binding.");
      }
    });

    Button unBindButton = (Button) findViewById(R.id.unbind);
    unBindButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mIsBound) {
          if (mService != null) {
            try {
              mService.unregisterCallback(mCallback);
            } catch (RemoteException ignore) {
              // The service has crashed.
            }
          }
          unbindService(mConnection);
          mIsBound = false;
          mTextView.setText("Not attached.");
        }
      }
    });

    Button requestButton = (Button) findViewById(R.id.request);
    requestButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mIsBound) {
          if (mService != null) {
            try {
              for (int i=0; i<5; i++) {
                mRequestCounter++;
                Request request = new Request("", -1, ("Request #" + mRequestCounter).getBytes());
                Log.i(TAG, "Client sending request: " + request.toString());
                mService.send(request, i * 1000);
              }
            } catch (RemoteException ignore) {
              // The service has crashed.
            }
          }
        }
      }
    });

    mTextView = (TextView) findViewById(R.id.callback);
    mTextView.setText("Not attached.");

    mResponseTextView = (TextView) findViewById(R.id.response);
    //mResponseTextView.setText("No response yet.");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mIsBound) {
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
      mService = INetworkService.Stub.asInterface(service);
      mTextView.setText("Attached.");

      // Monitor the service for as long as we are connected to it.
      try {
        mService.registerCallback(mCallback);
      } catch (RemoteException ignore) {
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

  /**
   * Rceives callbacks from the remote service.
   */
  private final INetworkServiceCallback mCallback = new INetworkServiceCallback.Stub() {
    /**
     * Called by the remote service on a background thread.
     */
    @Override
    public void receive(final Response response) {
      Log.i(TAG, "Received " + response.toString());
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mResponseCounter++;
          mResponseTextView.append(mResponseCounter + ". " + response.toString() + "\n");
        }
      });
    }
  };
}
