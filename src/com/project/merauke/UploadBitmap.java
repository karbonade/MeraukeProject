package com.project.merauke;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class UploadBitmap extends AsyncTask<String, Integer, Void> {

	String imageId;
	
	public UploadBitmap(String userId) {
		imageId = userId;
		System.out.println(imageId);
	}
	
	@Override
	protected Void doInBackground(String... params) {
		UserInfo.bitmapUser = getBitmapFromURL("http://graph.facebook.com/"
				+imageId+"/picture?type=small&redirect=false");
		uploadImage();
		
		return null;
	}

	public static Bitmap getBitmapFromURL(String src) {
		System.out.println(src);
		
		Bitmap myBitmap = null;
		
		HttpClient httpClient = new DefaultHttpClient();
		HttpRequestBase httpRequest = new HttpGet(src);
		HttpResponse ponse = null;
		try {
			ponse = httpClient.execute(httpRequest);
		} catch (ClientProtocolException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		InputStream responseStream = null;
		String responseString = null;
		if (ponse != null) {
			try {
				responseStream = ponse.getEntity().getContent();
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			InputStreamReader reader = new InputStreamReader(responseStream);
			BufferedReader buffer = new BufferedReader(reader);
			StringBuilder sb = new StringBuilder();

			try {
				String cur;
				while ((cur = buffer.readLine()) != null) {
					sb.append(cur + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				responseStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			responseString = sb.toString();
			System.out.println(responseString);
			
			try {
		    	JSONObject jsonElement = new JSONObject(responseString);
				String data = jsonElement.getString("data");
				JSONObject jsonElement2 = new JSONObject(data);
				String imageURL = jsonElement2.getString("url");
		    	System.out.println(imageURL);
		    	
		        URL url = new URL(imageURL);
		        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		        connection.setDoInput(true);
		        connection.connect();
		        InputStream input = connection.getInputStream();
		        myBitmap = BitmapFactory.decodeStream(input);
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
	    return myBitmap;
	}
	
	public void uploadImage() {
		if(UserInfo.bitmapUser != null) {
			System.out.println("BITMAP NOT NULL!!");
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			   UserInfo.bitmapUser.compress(Bitmap.CompressFormat.JPEG, 90, bao);
			   byte [] ba = bao.toByteArray();
			   String ba1=Base64.encodeToString(ba, 0);
			   ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			   nameValuePairs.add(new BasicNameValuePair("image",ba1));
			   nameValuePairs.add(new BasicNameValuePair("name",imageId+".jpg"));
			   
			   try{
			    HttpClient httpclient = new DefaultHttpClient();
			    HttpPost httppost = new HttpPost("http://www.jejaringhotel.com/android/upload.php");
			    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			    httpclient.execute(httppost);
			   }catch(Exception e){
			    Log.e("log_tag", "Error in http connection "+e.toString());
			   }
		} else {
			System.out.println("BITMAP NULL!!");
		}
	}
}