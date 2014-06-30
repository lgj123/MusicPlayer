/*
 * Copyright 2012-2014 Andrea De Cesare
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andreadec.musicplayer.adapters;

import com.andreadec.musicplayer.*;

import android.view.*;
import android.widget.*;

public class NavigationDrawerArrayAdapter extends ArrayAdapter<String> {
	private LayoutInflater inflater;
	
	public NavigationDrawerArrayAdapter(MainActivity activity, String[] pages) {
		super(activity, R.layout.song_item, pages);
		inflater = activity.getLayoutInflater();
	}
	
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder viewHolder;
		if(view==null) {
			view = inflater.inflate(R.layout.navigation_row, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.text = (TextView)view.findViewById(R.id.navigation_text);
			viewHolder.image = (ImageView)view.findViewById(R.id.navigation_image);
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		
		viewHolder.text.setText(getItem(position));
		switch(position) {
		case MainActivity.PAGE_BROWSER:
			viewHolder.image.setImageResource(R.drawable.audio);
			break;
		case MainActivity.PAGE_PLAYLISTS:
			viewHolder.image.setImageResource(R.drawable.playlist);
			break;
		case MainActivity.PAGE_PODCASTS:
			viewHolder.image.setImageResource(R.drawable.podcast);
			break;
		case MainActivity.PAGE_RADIOS:
			viewHolder.image.setImageResource(R.drawable.radio);
			break;
		}

		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView text;
		public ImageView image;
	}
}
