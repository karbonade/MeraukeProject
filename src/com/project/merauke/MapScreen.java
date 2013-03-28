package com.project.merauke;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

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

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
import com.facebook.Session;
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
	CustomLocationListener mlocListener;
	ActionMode actionMode;
	
	private int mProgress = 100;
	int ScreenWidth;
	int ScreenHeight;
	float zoomFactor;
	double factor;
	boolean hasSetup = false;// just setup it for the first time
	boolean isMarkerClick = false;
	String provider;	
		
	double projectionPointRange;// use this to measure range from center to 30% of edge
	LatLng boundCenterPoint;// the center point that we keep and it only changes when our app loads new data
	// LatLng cameraCenterPoint;// this store camera current position everytime the camera changes
		
	HashMap<Marker, MarkerInfo> hashMapInfo = new HashMap<Marker, MarkerInfo>();
	ArrayList<MarkerInfo> listInfo = new ArrayList<MarkerInfo>();
	ArrayList<Marker> listMarkers = new ArrayList<Marker>();
	Marker marker;
	
	Point size = new Point();
	Marker bandung;
	
	// String coordinates for Jl.Dipatiukur near ITHB initial to zoom
	//static String strOrigin[] = {"-6.888435", "107.615631"};
	static String HTTP_URL = "http://www.jejaringhotel.com/android/showme.php";
	LatLng userCoordinate;// = new LatLng(-6.888435, 107.615631);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		setContentView(R.layout.map_activity);
		provider = LocationManager.GPS_PROVIDER;
		
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getSize(size);
		ScreenWidth = size.x;
		ScreenHeight = size.y;
		
		setUpMapIfNeeded();
		map.setMyLocationEnabled(true);
		//mlocListener.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		
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
					
					HotelInfoActivity hotel = new HotelInfoActivity
							(MapScreen.this, eventInfo.infoId, eventInfo.infoTitle);
					hotel.setTitle(eventInfo.infoTitle);
					hotel.show();
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
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(provider);
		mlocListener = new CustomLocationListener();
		if(location != null) {
			Log.v("LOCATION", "location is not null!!!");
		    mlocListener.onLocationChanged(location);
		}
	    locationManager.requestLocationUpdates(provider, 0, 500, mlocListener);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Filter")
            .setIcon(R.drawable.options_48)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
				//System.out.println("star not match");
				listMarkers.get(count).setVisible(false);
			} else {
				//System.out.println("star match");
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
	
	public class CustomLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			System.out.println("Location Updated");
			userCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
			// Move the camera instantly to kiel with a zoom of 15.
    		map.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoordinate, 14));

    		// Zoom in, animating the camera.
    		map.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
		}

		public void onProviderDisabled(String provider) {
			System.out.println("provider disabled");
			Toast.makeText(getBaseContext(), "GPS mode disabled", Toast.LENGTH_SHORT).show();
		}

		public void onProviderEnabled(String provider) {
			System.out.println("provider enabled");
			Toast.makeText(getBaseContext(), "GPS mode enabled", Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			
		}
		
	}
	
	/**
	 * get inputstream data from http request
	 * */
	private InputStream getConnection(String url) {
		progressHandler.sendEmptyMessage(20);
		InputStream is = null;
	
		HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(url);
	    try {
	      HttpResponse response = client.execute(httpGet);
	      StatusLine statusLine = response.getStatusLine();
	      int statusCode = statusLine.getStatusCode();
	      if (statusCode == 200) {
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
			String id = "";
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
					id = String.valueOf(jsonElement.getInt("id"));
					name = jsonElement.getString("namalokasi");
					alamat = jsonElement.getString("alamat");
					number = jsonElement.getString("jenis");
					
					// convert double coord to lat and lng
					LatLng markPos = new LatLng(posLat, posLng);
					// add the markinfo to the list
					listInfo.add(new MarkerInfo(markPos, id, name, alamat, number));
					
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
	
	
	private class FetchDataTask extends AsyncTask<String, Integer, Void> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			if (mProgress >= 100) {
                mProgress = 0;
            }
			
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
		}
	}
}