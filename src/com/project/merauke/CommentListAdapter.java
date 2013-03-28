package com.project.merauke;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**our custom comment list adapter*/
public class CommentListAdapter extends BaseAdapter {

	Context cx;
	Bitmap photo;
	ImageView userImage;
	public ImageLoader imageLoader; 
	private LayoutInflater inflater;
	private ArrayList<CommentData> commentData = new ArrayList<CommentData>();
	
	public CommentListAdapter(ArrayList<CommentData> data, Context context) {
		cx = context;
		commentData = data;
		imageLoader=new ImageLoader(cx);
		inflater = (LayoutInflater)cx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return commentData.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public static class ViewHolder{
        public TextView name;
        public TextView comment;
        public ImageView image;
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi=convertView;
        ViewHolder holder;
        if(convertView==null){
            vi = inflater.inflate(R.layout.comment_list_row, null);
            holder=new ViewHolder();
            holder.name = (TextView)vi.findViewById(R.id.tvCommName);
            holder.comment = (TextView)vi.findViewById(R.id.tvCommComment);
            holder.image=(ImageView)vi.findViewById(R.id.ivCommPhoto);
            vi.setTag(holder);
        }
        else
            holder=(ViewHolder)vi.getTag();
        
        holder.name.setText(commentData.get(position).name);
        holder.comment.setText(commentData.get(position).comment);
        holder.image.setTag("http://www.jejaringhotel.com/android/userimages/"
        		+commentData.get(position).id+".jpg");
        imageLoader.DisplayImage("http://www.jejaringhotel.com/android/userimages/"
        		+commentData.get(position).id+".jpg", holder.image);
        return vi;
	}
	
}