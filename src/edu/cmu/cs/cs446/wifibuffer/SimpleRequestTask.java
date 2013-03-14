package edu.cmu.cs.cs446.wifibuffer;

import java.util.Random;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import static edu.cmu.cs.cs446.wifibuffer.WifiBufferService.REPORT_RESPONSE;

public class SimpleRequestTask extends AsyncTask<String, Void, String> {

  private Handler mHandler;
  
  public SimpleRequestTask(Handler handler) {
    mHandler = handler;
  }
  
  @Override
  protected String doInBackground(String... params) {
    SystemClock.sleep(1234);
    return "response " + new Random().nextInt(10);
  }
  
  @Override
  protected void onPostExecute(String result) {
    Message msg = mHandler.obtainMessage(REPORT_RESPONSE, result);
    mHandler.sendMessageDelayed(msg, 1234);
  }

}
