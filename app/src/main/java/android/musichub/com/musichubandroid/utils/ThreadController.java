package android.musichub.com.musichubandroid.utils;

public class ThreadController extends Thread {
	protected Object mPauseLock;
	protected boolean mPaused;
	protected boolean mFinished;

	
	public ThreadController(){
		super();
		mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
	}

	/**
	 * Call this on pause.
	 */
	public void onPause() {
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}

	/**
	 * Call this on resume.
	 */
	public void onResume() {
		synchronized (mPauseLock) {
			mPaused = false;
			mPauseLock.notifyAll();
		}
	}

	public void onStop() {
		mFinished = true;
	}
	
	public boolean isFinished(){
		return mFinished;
	}
	
	public Object getLock(){
		return mPauseLock;
	}
	
	public boolean isPaused(){
		return mPaused;
	}
}
