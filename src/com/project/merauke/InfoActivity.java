package com.project.merauke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionState;
import com.facebook.Settings;

public class InfoActivity extends Activity {

	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	
	int imageCount;
	//Context cx;
	
	String URL_GET_IMAGE_NAME = "http://www.jejaringhotel.com/android/search.php";
	String URL_GET_IMAGE_FILE = "http://www.jejaringhotel.com/android/search2.php";
	
	ProgressBar inProgress;
	LinearLayout myGallery;
	Button shareFacebook;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hotel_info_activity);
        
        myGallery = (LinearLayout)findViewById(R.id.mygallery);
        shareFacebook = (Button)findViewById(R.id.btnFacebookShare);
        shareFacebook.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				createFacebookConnection();
			}
		});        
        
        int count = 0;
        while(count < 6) {
        	myGallery.addView(addProgress());
        	count++;
        }
        
        Log.v("COUNT", "sum: "+myGallery.getChildCount());
        new FetchImageTask().execute();
        
    }
    
	View addProgress(){
		LinearLayout layout = new LinearLayout(this);
    	layout.setLayoutParams(new LayoutParams(180, 180));
    	layout.setGravity(Gravity.CENTER);
    	
    	inProgress = new ProgressBar(this);
    	inProgress.setLayoutParams(new LayoutParams(40, 40));
    	inProgress.setIndeterminate(true);
    	
    	layout.addView(inProgress);
    	
    	return layout;
    }
	
    View insertPhoto(Bitmap bm){
    	LinearLayout layout = new LinearLayout(this);
    	layout.setLayoutParams(new LayoutParams(180, 180));
    	layout.setGravity(Gravity.CENTER);
    	
    	ImageView imageView = new ImageView(this);
    	imageView.setLayoutParams(new LayoutParams(160, 160));
    	imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    	imageView.setImageBitmap(bm);
    	
    	layout.addView(imageView);
    	return layout;
    }
    
    public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {
    	Bitmap bm = null;
    	
    	// First decode with inJustDecodeBounds=true to check dimensions
    	final BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inJustDecodeBounds = true;
    	BitmapFactory.decodeFile(path, options);
    	
    	// Calculate inSampleSize
    	options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    	
    	// Decode bitmap with inSampleSize set
    	options.inJustDecodeBounds = false;
    	bm = BitmapFactory.decodeFile(path, options); 
    	
    	return bm; 	
    }
    
    public int calculateInSampleSize(
    		
    	BitmapFactory.Options options, int reqWidth, int reqHeight) {
    	// Raw height and width of image
    	final int height = options.outHeight;
    	final int width = options.outWidth;
    	int inSampleSize = 1;
        
    	if (height > reqHeight || width > reqWidth) {
    		if (width > height) {
    			inSampleSize = Math.round((float)height / (float)reqHeight);  	
    		} else {
    			inSampleSize = Math.round((float)width / (float)reqWidth);  	
    		}  	
    	}
    	
    	return inSampleSize;  	
    }
    

    public void createFacebookConnection() {
        Session session = new Session(this);
        Session.setActiveSession(session);

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                String message = "Facebook session status changed - " + session.getState() + " - Exception: " + exception;
                //Toast.makeText(FacebookShareActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.w("Facebook test", message);

                if (session.isOpened() || session.getPermissions().contains("publish_actions")) {
                    publishToWall();
                } else if (session.isOpened()) {
                    OpenRequest open = new OpenRequest(InfoActivity.this).setCallback(this);
                    List<String> permission = new ArrayList<String>();
                    permission.add("publish_actions");
                    open.setPermissions(permission);
                    Log.w("Facebook test", "Open for publish");
                    session.openForPublish(open);
                }
            }
        };

        if (!session.isOpened() && !session.isClosed() && session.getState() != SessionState.OPENING) {
            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
        } else {
            Log.w("Facebook test", "Open active session");
            Session.openActiveSession(this, true, statusCallback);
        }
    }

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    void publishToWall() {
        Session session = Session.getActiveSession();

        Bundle postParams = new Bundle();
        postParams.putString("name", "Merauke Android");
        postParams.putString("caption", "Share your hotels here!");
        postParams.putString("description", "The Facebook SDK for Android makes it easier and faster to develop Facebook integrated Android apps.");
        postParams.putString("link", "https://developers.facebook.com/android");
        postParams.putString("picture", "https://raw.github.com/fbsamples/ios-3.x-howtos/master/Images/iossdk_logo.png");
        Log.i("DIALOG","feed set");

        Request.Callback callback = new Request.Callback() {
            public void onCompleted(Response response) {
                FacebookRequestError error = response.getError();
                if (error != null) {
                    Toast.makeText(InfoActivity.this, error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
                    String postId = null;
                    try {
                        postId = graphResponse.getString("id");
                    } catch (JSONException e) {
                        Log.i("Facebook error", "JSON error " + e.getMessage());
                    }
                    //TODO Toast.makeText(context, postId, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        };

        Request request = new Request(Session.getActiveSession(), "me/feed", postParams, HttpMethod.POST, callback);

        RequestAsyncTask task = new RequestAsyncTask(request);
        task.execute();
    }
    
    ArrayList<Bitmap> photosImage = new ArrayList<Bitmap>();
    
    Handler displayImage = new Handler() {
    	@Override
    	public void handleMessage(android.os.Message msg) {
    		switch(msg.what) {
    		case 1:
    			Log.d("Display", "incoming image!!!");
    			myGallery.removeViewAt(imageCount);
    			myGallery.addView(insertPhoto(photosImage.get(0)), imageCount);
    			imageCount++;
    			
    			photosImage.remove(0);
    			break;
    		}
    	};
    };
    
    public class FetchImageTask extends AsyncTask<String, Integer, Void> {

    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		
    	}
    	
		@Override
		protected Void doInBackground(String... params) {
			Map<String, String> input = new HashMap<String, String>();
			input.put("id", "2");
			
			HttpRequest req = new HttpRequest(URL_GET_IMAGE_NAME, input, HttpRequest.Method.POST);
			String response = req.sendRequest();
			Log.i("FetchImage", response);
			
			processingStringResponse(response);
			
			return null;
		}
    	
		

		private void processingStringResponse(String response) {
			int count=0;
			while(count < response.length()-1) {
				int semicolonIndex = response.indexOf(";", count);
				String realName = response.substring(count
						, semicolonIndex);
				Log.v("FetchImage", "name: "+realName);
				
				Map<String, String> input = new HashMap<String, String>();
				input.put("name", realName);
				
				HttpRequest req = new HttpRequest(URL_GET_IMAGE_FILE
						, input, HttpRequest.Method.POST);
				
				photosImage.add(BitmapFactory.decodeStream(req.getConnection()));
				
				displayImage.sendEmptyMessage(1);
				
				count = semicolonIndex + 1;
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
		}
		
    }
}

