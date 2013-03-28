package com.project.merauke;

import com.google.android.gms.maps.model.LatLng;

public class MarkerInfo {

	LatLng infoCoord;
	String infoTitle;
	String infoComment;
	String rank;
	String infoId;
	
	public MarkerInfo(LatLng coord, String id, String title, String comment, String number) {
		infoCoord = coord;
		infoTitle = title;
		infoComment = comment;
		infoId = id;
		rank = number;
	}
	
	
}
