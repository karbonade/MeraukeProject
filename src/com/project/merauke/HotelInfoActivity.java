package com.project.merauke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;

/**A Dialog class to show hotel's info and social features*/
public class HotelInfoActivity extends Dialog {

	int imageCount;
	Context cx;
	
	CommentListAdapter adapter;
	private ArrayList<CommentData> commentArray = new ArrayList<CommentData>();
	String URL_GET_IMAGE_NAME = "http://www.jejaringhotel.com/android/search.php";
	String URL_GET_IMAGE_FILE = "http://www.jejaringhotel.com/android/search2.php";
	String URL_POST = "http://www.jejaringhotel.com/android/comment_post.php";
	String URL_GET = "http://www.jejaringhotel.com/android/comment_get.php";
	String hotelName;
	String hotelId;
	
	LinearLayout layout;
	LinearLayout myGallery;
	Button shareFacebook;
	Button sendComment;
	EditText edComment;
	LinearLayout commentPouch;
	ListView realList;// TODO	
	
	protected HotelInfoActivity(Context context, String id, String name) {
		super(context);
		cx = context;
		hotelId = id;
		hotelName = name;
		getWindow().setBackgroundDrawable(new ColorDrawable(0));
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hotel_info_activity);
        
        myGallery = (LinearLayout)findViewById(R.id.mygallery);
        edComment = (EditText)findViewById(R.id.edComment);
        
        commentPouch = (LinearLayout)findViewById(R.id.lvComment);
        commentPouch.addView(addProgressComm(0));
        
        shareFacebook = (Button)findViewById(R.id.btnFacebookShare);
        shareFacebook.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				createFacebookConnection();
			}
		});        
        
        sendComment = (Button)findViewById(R.id.btnComment);
        sendComment.setClickable(false);
        sendComment.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new CommentTask().execute("1", edComment.getText().toString());
			}
		});
        
        int count = 0;
        while(count < 5) {
        	myGallery.addView(addProgress());
        	count++;
        }
        
        Log.v("COUNT", "sum: "+myGallery.getChildCount());
        new CommentTask().execute("0");
        new FetchImageTask().execute();
        
    }
    
	View addProgressComm(int choose) {
		layout = new LinearLayout(cx);
    	layout.setLayoutParams(new LayoutParams
    			(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	switch(choose) {
		case 0:// TODO we add progress here
			layout.setGravity(Gravity.CENTER);
			ProgressBar listProgress = new ProgressBar(cx);
			listProgress.setLayoutParams(new LayoutParams(40, 40));
			listProgress.setIndeterminate(true);
	    	
	    	layout.addView(listProgress);
			break;
		case 1:// TODO we add text here
			layout.setGravity(Gravity.CENTER);
			TextView tv = new TextView(cx);
			tv.setLayoutParams(new LayoutParams
					(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			tv.setGravity(Gravity.CENTER);
			tv.setTextSize(14.0f);
			tv.setTypeface(Typeface.DEFAULT_BOLD);
			tv.setTextColor(Color.RED);
			tv.setText("Sorry, no comments available");
			
			layout.addView(tv);
			break;
		case 2:// TODO we add text here
			realList = new ListView(cx);
			realList.setLayoutParams(new LayoutParams
					(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.addView(realList);
			break;
			
		}
		
    	return layout;
	}
	
	View addProgress(){
		LinearLayout layout2 = new LinearLayout(cx);
    	layout2.setLayoutParams(new LayoutParams(100, 100));
    	layout2.setGravity(Gravity.CENTER);
    	
    	ProgressBar inProgress = new ProgressBar(cx);
    	inProgress.setLayoutParams(new LayoutParams(40, 40));
    	inProgress.setIndeterminate(true);
    	
    	layout2.addView(inProgress);
    	
    	return layout2;
    }
	
    View insertPhoto(Bitmap bm){
    	LinearLayout layout3 = new LinearLayout(cx);
    	layout3.setLayoutParams(new LayoutParams(100, 100));
    	layout3.setGravity(Gravity.CENTER);
    	
    	ImageView imageView = new ImageView(cx);
    	imageView.setLayoutParams(new LayoutParams(95, 95));
    	imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    	imageView.setImageBitmap(bm);
    	
    	layout3.addView(imageView);
    	return layout3;
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
    
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
    public void createFacebookConnection() {
        Session session = new Session(cx);
        Session.setActiveSession(session);

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        Session.StatusCallback statusCallback = new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                String message = "Facebook session status changed - " + session.getState() + " - Exception: " + exception;
                Log.w("Facebook test", message);

                if (session.isOpened()) {
                	if(session.getPermissions().contains("publish_actions")) {
                		Log.v("Facebook test", "publish");
                        publishToWall();
                	} else {
                		Log.v("Facebook test", "set publish permission");
                		Session.NewPermissionsRequest newPermissionsRequest = new Session
                                .NewPermissionsRequest((Activity) cx, PERMISSIONS);
                		session.requestNewPublishPermissions(newPermissionsRequest);
                	}
                	
                }/* else if (session.isOpened()) {
                	Log.v("Facebook test", "set publish permission");
                    OpenRequest open = new OpenRequest((Activity) cx).setCallback(this);
                    List<String> permission = new ArrayList<String>();
                    permission.add("publish_actions");
                    open.setPermissions(permission);
                    Log.w("Facebook test", "Open for publish");
                    session.openForPublish(open);
                }*/
            }
        };

        if (!session.isOpened() && !session.isClosed() && session.getState() != SessionState.OPENING) {
            session.openForRead(new Session.OpenRequest((Activity) cx).setCallback(statusCallback));
        } else {
            Log.w("Facebook test", "Open active session");
            Session.openActiveSession((Activity) cx, true, statusCallback);
        }
    }

    void publishToWall() {
        Bundle postParams = new Bundle();
        postParams.putString("name", "Merauke Android");
        postParams.putString("caption", hotelName);
        postParams.putString("description", hotelName+" is a luxury hotel which is located in the city of Bandung.");
        postParams.putString("link", "https://developers.facebook.com/android");
        postParams.putString("picture", "http://www.jejaringhotel.com/android/hotelimages/hotel-3.jpg");
        Log.i("DIALOG","feed set");

        Request.Callback callback = new Request.Callback() {
            public void onCompleted(Response response) {
                FacebookRequestError error = response.getError();
                if (error != null) {
                    Toast.makeText(cx
                    		, error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
                    String postId = null;
                    try {
                        postId = graphResponse.getString("id");
                    } catch (JSONException e) {
                        Log.i("Facebook error", "JSON error " + e.getMessage());
                    }
                    //TODO Toast.makeText(context, postId, Toast.LENGTH_LONG).show();
                    Toast.makeText(cx
                    		, "Posted on wall success!", Toast.LENGTH_SHORT).show();
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
    
    public class CommentTask extends AsyncTask<String, Integer, Void> {

    	String postedComment;
    	String response;
    	int operationCase;
    	
		@Override
		protected Void doInBackground(String... arg0) {
			operationCase = Integer.parseInt(arg0[0]);
			switch(operationCase) {
			case 0:
				getCommentFromServer();
				break;
			case 1:
				postedComment = arg0[1];
				postCommentToServer();
				break;
			}
			
			return null;
		}

		private void getCommentFromServer() {
			Log.i("GetComment", hotelId);
			Map<String, String> input = new HashMap<String, String>();
			input.put("id", hotelId);
			
			HttpRequest req = new HttpRequest(URL_GET, input, HttpRequest.Method.POST);
			response = req.sendRequest();
			Log.i("GetComment", response);
		}

		private void postCommentToServer() {
			Log.i("PostComment", hotelId+"----"+postedComment);
			Map<String, String> input = new HashMap<String, String>();
			input.put("id", hotelId);
			input.put("userid", UserInfo.userId);
			input.put("name", UserInfo.userName);
			input.put("comment", postedComment);
			
			HttpRequest req = new HttpRequest(URL_POST, input, HttpRequest.Method.POST);
			response = req.sendRequest();
			Log.i("PostComment", response);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			switch(operationCase) {
			case 0:
				processingJSON(response);
				commentPouch.removeView(layout);// remove progress
				if(commentArray.size() > 0) {
					Log.v("get", "comment not empty");
					if(realList == null) {
						Log.v("get", "listview is not null");
						commentPouch.addView(addProgressComm(2));// TODO add list
					}
					adapter = new CommentListAdapter(commentArray, cx);
					realList.setAdapter(adapter);
				} else {
					commentPouch.addView(addProgressComm(1));// TODO add text
				}
				sendComment.setClickable(true);
				break;
			case 1:
				//if(response == "comment added") {
					Toast.makeText(cx, response, Toast.LENGTH_SHORT).show();
					Log.v("PostComment", "comment is valid to be added!");
					commentArray.add(new CommentData(UserInfo.userId, UserInfo.userName, postedComment));
					if(realList == null) {
						commentPouch.removeView(layout);
						commentPouch.addView(addProgressComm(2));
						adapter = new CommentListAdapter(commentArray, cx);
						realList.setAdapter(adapter);
					}
					adapter.notifyDataSetChanged();
				//}
				break;
			}
		}
		
		private void processingJSON(String strJSON) {
			// TODO
			try {
				JSONArray rowArray = new JSONArray(strJSON);
				
				int count = 0;
				JSONObject jsonElement = null;
				String userid = "";
				String username = "";
				String comment = "";
				Log.v("COMMENT", "total: "+rowArray.length());
				if(rowArray.length() != 0) {
					while(count < rowArray.length()) {
						jsonElement = rowArray.getJSONObject(count);
						userid = jsonElement.getString("userid");
						username = jsonElement.getString("username");
						comment = jsonElement.getString("komentar");
						
						commentArray.add(new CommentData(userid, username, comment));
						count++;
					}
					Log.d("JSON", "loading data finish");
				} else {
					Log.d("JSON", "EMPTY");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    }
    
    
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
}// End of custom Dialog class