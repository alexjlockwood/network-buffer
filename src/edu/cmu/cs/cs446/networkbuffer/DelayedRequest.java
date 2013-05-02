//package edu.cmu.cs.cs446.networkbuffer;
//
//import java.util.concurrent.Callable;
//import java.util.concurrent.Delayed;
//import java.util.concurrent.TimeUnit;
//
//public class DelayedRequest implements Delayed, Callable<Response> {
//  private final long mOrigin;
//  private final long mDelay;
//  private final Request mRequest;
//
//  public DelayedRequest(Request request) {
//    this(request, 0);
//  }
//
//  public DelayedRequest(Request request, long delay) {
//    mOrigin = System.currentTimeMillis();
//    mRequest = request;
//    mDelay = delay;
//  }
//
//  public Request getRequest() {
//    return mRequest;
//  }
//
//  @Override
//  public Response call() throws Exception {
//    return mRequest.call();
//  }
//
//  @Override
//  public long getDelay(TimeUnit unit) {
//    return unit.convert(mDelay - (System.currentTimeMillis() - mOrigin), TimeUnit.MILLISECONDS);
//  }
//
//  @Override
//  public int compareTo(Delayed delayed) {
//    if (this == delayed) {
//      return 0;
//    }
//
//    long diff;
//    if (delayed instanceof DelayedRequest) {
//      diff = mDelay - ((DelayedRequest) delayed).mDelay;
//    } else {
//      diff = getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS);
//    }
//
//    return (diff > 0) ? 1 : (diff < 0) ? -1 : 0;
//  }
//}
