package com.project.merauke;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapScreen extends SherlockActivity {

	GoogleMap map;
	LatLng pointMin;
	LatLng pointMax;
	LatLng pointCenter;
	LocationManager locationManager;
	ActionMode actionMode;
	
	private int mProgress = 100;
	int ScreenWidth;
	int ScreenHeight;
	float zoomFactor;
	double factor;
	boolean hasSetup = false;// just setup it for the first time
	boolean isMarkerClick = false;
	
	double projectionPointRange;// use this to measure range from center to 30% of edge
	LatLng boundCenterPoint;// the center point that we keep and it only changes when our app loads new data
	// LatLng cameraCenterPoint;// this store camera current position everytime the camera changes
		
	HashMap<Marker, MarkerInfo> hashMapInfo = new HashMap<Marker, MarkerInfo>();
	ArrayList<MarkerInfo> listInfo = new ArrayList<MarkerInfo>();
	ArrayList<Marker> listMarkers = new ArrayList<Marker>();
	Marker marker;
	
	Point size = new Point();
	Marker bandung;
	// MapTabsScreen tabScreen;
	
	// String coordinates for Jl.Dipatiukur near ITHB initial to zoom
	static String strOrigin[] = {"-6.888435", "107.615631"};
	static String HTTP_URL = "http://www.jejaringhotel.com/android/showme.php";
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	LatLng userCoordinate;// = new LatLng(-6.888435, 107.615631);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		setContentView(R.layout.map_activity);
				
		// tabScreen = (MapTabsScreen) this.getParent();
		// TODO next time check if there is any location provider
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationListener mlocListener = new CustomLocationListener();
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mlocListener);
		// locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
		
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getSize(size);
		ScreenWidth = size.x;
		ScreenHeight = size.y;
		
		setUpMapIfNeeded();
		map.setMyLocationEnabled(true);
		
		map.setOnMarkerClickListener(new OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				isMarkerClick = true;
				
				return false;
			}
		});
		
		map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick(Marker marker) {
				if(!marker.equals(bandung)) {
					MarkerInfo eventInfo = hashMapInfo.get(marker);
					//startActivity(new Intent(MapScreen.this, InfoActivity.class));
				
					HotelInfoActivity hotel = new HotelInfoActivity(MapScreen.this, eventInfo.infoTitle);
					hotel.setTitle(eventInfo.infoTitle);
					hotel.show();
					
					//Fragment.instantiate(getBaseContext(), HotelInfoActivity.class.getName(), null);
					//MapScreen.this.startActivity(new Intent(MapScreen.this, HotelInfoActivity.class));
					// Toast.makeText(getBaseContext(), "name: "+eventInfo.infoTitle, Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		map.setOnCameraChangeListener(new OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
				if(hasSetup) {
					if(!isMarkerClick && isCameraOutOfBounds(position)) {
						System.out.println("it's moving");
						new FetchDataTask().execute();
					} else {
						isMarkerClick = false;
					}
				}
			}
		});
	}
	
	
	private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
        	map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (map != null && !hasSetup) {
                setUpMap();
            }
        }
        
        // System.out.println("parent: "+this.getParent());
    }
	
	private void setUpMap() {
		final View mapView = getFragmentManager().findFragmentById(R.id.map).getView();
		if (mapView.getViewTreeObserver().isAlive()) {
		    mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		        @Override
		        public void onGlobalLayout() {
		            // remove the listener
		            // ! before Jelly Bean:
		            mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		            
		            // Move the camera instantly to kiel with a zoom of 15.
		    		//map.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoordinate, 14));

		    		// Zoom in, animating the camera.
		    		//map.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
		    		
		            // ! you can query Projection object here
		            // Point markerScreenPosition = map.getProjection().toScreenLocation(marker.getPosition());
		    		hasSetup = true;
		    		// doInBackground()
		    		new FetchDataTask().execute();
		        }
		    });
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	 }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		setUpMapIfNeeded();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put dark icons on light action bar
        // boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;

        menu.add("Filter")
            .setIcon(R.drawable.options_48)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		actionMode = startActionMode(new AnActionModeOfEpicProportions());
		
		return super.onOptionsItemSelected(item);
	}
	
	/**check if the camera's point is out of bounds*/
	public boolean isCameraOutOfBounds(CameraPosition camPosition) {
		boolean value = false;
		LatLng thisCameraPosition = camPosition.target;
		
		// calculate
		// (y2-y1)2 + (x2-x1)2
		double currentRange = Math.pow((thisCameraPosition.latitude - boundCenterPoint.latitude), 2)
				+ Math.pow((thisCameraPosition.longitude - boundCenterPoint.longitude), 2);
		
		currentRange = Math.sqrt(currentRange);// square root
		
		if(currentRange >= projectionPointRange) {
			System.out.println("this center point is out of bounds! value true");
			value = true;
		}
		
		return value;
	}
	
	/**get range distance*/
	public void calculateRangeDistance() {
		Projection projection = map.getProjection();
		
		// get minimum points
		pointMin = projection.fromScreenLocation(new Point(0, 0));
		Log.v("MapScreen", "pointMin: "+pointMin);

		// get maximum points
		pointMax = projection.fromScreenLocation(new Point(ScreenWidth, ScreenHeight));
		Log.v("MapScreen", "pointMax: "+pointMax);
		
		CameraPosition camPosition = map.getCameraPosition();
		boundCenterPoint = camPosition.target;// get the center
		
		// (y2-y1)2 + (x2-x1)2
		projectionPointRange = 0.5 * (Math.pow((pointMax.latitude - boundCenterPoint.latitude), 2)
				+ Math.pow((pointMax.longitude - boundCenterPoint.longitude), 2));
		
		projectionPointRange = Math.sqrt(projectionPointRange);// square root
	}
	
	public void filterByStars(String stars) {
		System.out.println("filter by stars: "+stars
				+"sizei: "+listInfo.size()
				+"sizem: "+listMarkers.size());
		int count = 0;
		while(count < listInfo.size()) {
			if(!((listInfo.get(count)).rank).contains(stars)) {
				System.out.println("star not match");
				listMarkers.get(count).setVisible(false);
			} else {
				System.out.println("star match");
				listMarkers.get(count).setVisible(true);
			}
			count++;
		}
	}
	
	public void clearJustHotels() {
		int count = 0;
		int total = listMarkers.size();
		while(count < total) {
			listMarkers.get(count).remove();// remove marker from map
			
			count++;
		}
		listMarkers.clear();// remove marker from list
	}
	
	Handler progressHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			int count = 0; 
			while(count < msg.what) {
				
				mProgress += 2;// increment progress
				int add = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * mProgress;
				setSupportProgress(add);
				
				count += 2;// increase loop
			}
		};
	};
	
	/**
	 * get inputstream data from http request
	 * */
	private InputStream getConnection(String url) {
		// TODO
		progressHandler.sendEmptyMessage(20);
		InputStream is = null;
	
		HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(url);
	    try {
	      HttpResponse response = client.execute(httpGet);
	      StatusLine statusLine = response.getStatusLine();
	      int statusCode = statusLine.getStatusCode();
	      if (statusCode == 200) {
	    	  // TODO
	    	  progressHandler.sendEmptyMessage(30);
	        HttpEntity entity = response.getEntity();
	        is = entity.getContent();
	      } else {
	    	  progressHandler.sendEmptyMessage(100);// quickly dismiss
	        Log.e("ANGKOT", "Failed to download file");
	      }
	    } catch (ClientProtocolException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return is;		
	}
	
	private void processingJSON(String strJSON) {
		
		try {
			JSONArray rowArray = new JSONArray(strJSON);
			
			int count = 0;
			String number;
			JSONObject jsonElement = null;
			double posLat = 0;
			double posLng = 0;
			String name = "";
			String alamat = "";
			Log.v("MapScreen", "total: "+rowArray.length());
			if(rowArray.length() != 0) {
				listInfo.clear();
				hashMapInfo.clear();
				
				while(count < rowArray.length()) {
					
					jsonElement = rowArray.getJSONObject(count);
					posLat = Double.parseDouble
							(jsonElement.getString("lat"));
					posLng = Double.parseDouble
							(jsonElement.getString("lng"));
					name = jsonElement.getString("namalokasi");
					alamat = jsonElement.getString("alamat");
					number = jsonElement.getString("jenis");
					
					// convert double coord to lat and lng
					LatLng markPos = new LatLng(posLat, posLng);
					// add the markinfo to the list
					listInfo.add(new MarkerInfo(markPos, name, alamat, number));
					
					count++;
				}
				progressHandler.sendEmptyMessage(50);
				Log.d("JSON", "loading data finish");
			} else {
				progressHandler.sendEmptyMessage(100);// quickly dismiss
				Log.d("JSON", "EMPTY");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**this is drop down list class*/
	private final class AnActionModeOfEpicProportions implements ActionMode.Callback {
		MenuItem subMenuItem;
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //Used to put dark icons on light action bar
            // boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;
/*
            menu.add("Stars")
                .setIcon(R.drawable.star_48)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
*/
            menu.add("Powers")
                .setIcon(R.drawable.light_48)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add("Atm")
                .setIcon(R.drawable.disc_48)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            SubMenu star = menu.addSubMenu("Stars");
            star.add("1 star");
            star.add("2 stars");
            star.add("3 stars");
            star.add("4 stars");
            star.add("5 stars");
            star.add("All stars");

            subMenuItem = star.getItem();
            subMenuItem.setIcon(R.drawable.star_48);
            subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        	Toast.makeText(getBaseContext(), "item: "+item.getTitle(), Toast.LENGTH_SHORT).show();
        	if(!item.equals(subMenuItem)) {
        		if(item.getTitle().toString().equals("1 star")) {
        			filterByStars("1");
        		} else if(item.getTitle().toString().equals("2 stars")) {
        			filterByStars("2");
        		} else if(item.getTitle().toString().equals("3 stars")) {
        			filterByStars("3");
        		} else if(item.getTitle().toString().equals("4 stars")) {
        			filterByStars("4");
        		} else if(item.getTitle().toString().equals("5 stars")) {
        			filterByStars("5");
        		} else if(item.getTitle().toString().equals("All stars")) {
        			filterByStars("");
        		}
        		mode.finish();
        	} else {
        		
        	}
            
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
	
	/**A Dialog class to show hotel's info and social features*/
	public class HotelInfoActivity extends Dialog {

		int imageCount;
		Context cx;
		
		String URL_GET_IMAGE_NAME = "http://www.jejaringhotel.com/android/search.php";
		String URL_GET_IMAGE_FILE = "http://www.jejaringhotel.com/android/search2.php";
		String hotelName;
		
		ProgressBar inProgress;
		LinearLayout myGallery;
		Button shareFacebook;
		
		protected HotelInfoActivity(Context context, String name) {
			super(context);
			cx = context;
			hotelName = name;
			getWindow().setBackgroundDrawable(new ColorDrawable(0));
			// TODO Auto-generated constructor stub
		}

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
			LinearLayout layout = new LinearLayout(cx);
	    	layout.setLayoutParams(new LayoutParams(180, 180));
	    	layout.setGravity(Gravity.CENTER);
	    	
	    	inProgress = new ProgressBar(cx);
	    	inProgress.setLayoutParams(new LayoutParams(40, 40));
	    	inProgress.setIndeterminate(true);
	    	
	    	layout.addView(inProgress);
	    	
	    	return layout;
	    }
		
	    View insertPhoto(Bitmap bm){
	    	LinearLayout layout = new LinearLayout(cx);
	    	layout.setLayoutParams(new LayoutParams(180, 180));
	    	layout.setGravity(Gravity.CENTER);
	    	
	    	ImageView imageView = new ImageView(cx);
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
	        Session session = new Session(MapScreen.this);
	        Session.setActiveSession(session);

	        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

	        Session.StatusCallback statusCallback = new Session.StatusCallback() {
	            @Override
	            public void call(Session session, SessionState state, Exception exception) {
	                String message = "Facebook session status changed - " + session.getState() + " - Exception: " + exception;
	                Log.w("Facebook test", message);

	                if (session.isOpened() || session.getPermissions().contains("publish_actions")) {
	                    publishToWall();
	                } else if (session.isOpened()) {
	                    OpenRequest open = new OpenRequest(MapScreen.this).setCallback(this);
	                    List<String> permission = new ArrayList<String>();
	                    permission.add("publish_actions");
	                    open.setPermissions(permission);
	                    Log.w("Facebook test", "Open for publish");
	                    session.openForPublish(open);
	                }
	            }
	        };

	        if (!session.isOpened() && !session.isClosed() && session.getState() != SessionState.OPENING) {
	            session.openForRead(new Session.OpenRequest(MapScreen.this).setCallback(statusCallback));
	        } else {
	            Log.w("Facebook test", "Open active session");
	            Session.openActiveSession(MapScreen.this, true, statusCallback);
	        }
	    }

	    void publishToWall() {
	        Session session = Session.getActiveSession();

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
	                    Toast.makeText(MapScreen.this, error.getErrorMessage(), Toast.LENGTH_SHORT).show();
	                } else {
	                    JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
	                    String postId = null;
	                    try {
	                        postId = graphResponse.getString("id");
	                    } catch (JSONException e) {
	                        Log.i("Facebook error", "JSON error " + e.getMessage());
	                    }
	                    //TODO Toast.makeText(context, postId, Toast.LENGTH_LONG).show();
	                    Toast.makeText(MapScreen.this, "Posted on wall success!", Toast.LENGTH_SHORT).show();
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
	}// End of custom Dialog class
	
	
	public class CustomLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			System.out.println("Location Updated");
			userCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
			//map.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoordinate, 14));
			if(bandung == null) {
				// Move the camera instantly to kiel with a zoom of 15.
	    		map.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoordinate, 14));

	    		// TODO Zoom in, animating the camera.
	    		map.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
				
				bandung = map.addMarker(new MarkerOptions()
		        .position(userCoordinate)
		        .title("Bandung")
		        .snippet("You're here")
		        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
			}
			
			bandung.setPosition(userCoordinate);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			System.out.println("provider disabled");
			Toast.makeText(getBaseContext(), "GPS mode disabled", Toast.LENGTH_SHORT).show();
			/*
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MapScreen.this);
			alertBuilder.setCancelable(true);
			alertBuilder.setMessage("Do you want to turn GPS on?");
			alertBuilder.setTitle("GPS disabled");
			alertBuilder.setPositiveButton("Yes", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MapScreen.this.startActivity(new Intent
							(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				}
			});
			alertBuilder.setNegativeButton("No", new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			alertBuilder.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					return;
				}
			});
			alertBuilder.show();
			*/
		}

		@Override
		public void onProviderEnabled(String provider) {
			System.out.println("provider enabled");
			Toast.makeText(getBaseContext(), "GPS mode enabled", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			
		}
		
	}
	
	private class FetchDataTask extends AsyncTask<String, Integer, Void> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			if (mProgress >= 100) {
                mProgress = 0;
            }
			
			// tabScreen.setSupportProgressBarIndeterminateVisibility(true);
			// setSupportProgressBarIndeterminateVisibility(true);
			
			calculateRangeDistance();
			clearJustHotels();
		}
		
	    @Override
	    protected Void doInBackground(String... arg0) {
	    	StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader
					(getConnection(HTTP_URL+"?maxLat="+pointMax.latitude+"&minLat="
			+pointMin.latitude+"&minLng="+pointMin.longitude+"&maxLng="+pointMax.longitude)));
	        String line;
	        try {
				while ((line = reader.readLine()) != null) {
				  builder.append(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	        String strJSON = builder.toString();
	        processingJSON(strJSON);	        
	        
	        return null;
	    }
	    
	    @Override
	    protected void onPostExecute(Void result) {
	    	super.onPostExecute(result);
	    	
	    	Log.d("MapScreen", "onPost:"+mProgress);
			int count = 0;
			while(count < listInfo.size()) {
				
					// get the info from info list
					MarkerInfo mo = listInfo.get(count);
					
					float color = 0;
					if(mo.rank.contains("1")) {
						color = BitmapDescriptorFactory.HUE_AZURE;
					} else if(mo.rank.contains("2")){
						color = BitmapDescriptorFactory.HUE_VIOLET;
					} else if(mo.rank.contains("3")){
						color = BitmapDescriptorFactory.HUE_GREEN;
					} else if(mo.rank.contains("4")){
						color = BitmapDescriptorFactory.HUE_YELLOW;
					} else if(mo.rank.contains("5")){
						color = BitmapDescriptorFactory.HUE_RED;
					} else {
						color = BitmapDescriptorFactory.HUE_MAGENTA;
					}
					
					// add marker to map
					marker = map.addMarker(new MarkerOptions()
							.position(mo.infoCoord)
							.title(mo.infoTitle)
							.snippet(mo.infoComment)
							.icon(BitmapDescriptorFactory.defaultMarker
									(color)));
					// add data marker to list
					listMarkers.add(marker);
					// add data to hashmap
					hashMapInfo.put(marker, mo);
					count++;
			}
			// tabScreen.setSupportProgressBarIndeterminateVisibility(false);
			// setSupportProgressBarIndeterminateVisibility(false);
	    }
	    
	    
	}

	
		
}