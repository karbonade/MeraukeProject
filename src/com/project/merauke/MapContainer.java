package com.project.merauke;

import android.app.LocalActivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public class MapContainer extends Fragment {

	// private static final String KEY_CONTENT = "MapFragment:Content";
	// private String mContent = "???";
	
	private static final String TAG = MapContainer.class.getSimpleName();
	private static final String KEY_STATE_BUNDLE = "localActivityManagerState";
	private LocalActivityManager mLocalActivityManager;

    protected LocalActivityManager getLocalActivityManager() {
        return mLocalActivityManager;
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
            mContent = savedInstanceState.getString(KEY_CONTENT);
        }
        */
		Log.d(TAG, "onCreate(): " + getClass().getSimpleName());
		
		Bundle state = null;
        if (savedInstanceState != null) {
            state = savedInstanceState.getBundle(KEY_STATE_BUNDLE);
        }

        mLocalActivityManager = new LocalActivityManager(getActivity(), true);
        mLocalActivityManager.dispatchCreate(state);
	}

	
	/*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.v("CR View", "onCreateView");
		Intent i = new Intent(getActivity(), MapScreen.class);
		
		Window w = mLocalActivityManager.startActivity("tag", i); 
		View currentView = w.getDecorView(); 
        currentView.setVisibility(View.VISIBLE); 
        currentView.setFocusableInTouchMode(true); 
        ((ViewGroup) currentView).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        return currentView;
	}
	 */
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_STATE_BUNDLE, mLocalActivityManager.saveInstanceState());
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume(): " + getClass().getSimpleName());
        mLocalActivityManager.dispatchResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause(): " + getClass().getSimpleName());
        mLocalActivityManager.dispatchPause(getActivity().isFinishing());
    }    
    
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): " + getClass().getSimpleName());
        mLocalActivityManager.dispatchStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy(): " + getClass().getSimpleName());
        mLocalActivityManager.dispatchDestroy(getActivity().isFinishing());
    }
	
}
