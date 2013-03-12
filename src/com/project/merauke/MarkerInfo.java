package com.project.merauke;

import com.google.android.gms.maps.model.LatLng;

public class MarkerInfo {

	LatLng infoCoord;
	String infoTitle;
	String infoComment;
	String rank;
	
	public MarkerInfo(LatLng coord, String title, String comment, String number) {
		infoCoord = coord;
		infoTitle = title;
		infoComment = comment;
		rank = number;
	}
	
	
}
