/*
 * Copyright 2012-2013 Andrea De Cesare
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

import java.util.*;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class SearchResultsArrayAdapter extends ArrayAdapter<BrowserSong> {
	private final ArrayList<BrowserSong> songs;
	private LayoutInflater inflater;
 
	public SearchResultsArrayAdapter(SearchActivity searchActivity, ArrayList<BrowserSong> songs) {
		super(searchActivity, R.layout.song_item, songs);
		this.songs = songs;
		inflater = searchActivity.getLayoutInflater();
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		BrowserSong song = songs.get(position);
		ViewHolder viewHolder;
		
		if(view==null) {
			viewHolder = new ViewHolder();
			view = inflater.inflate(R.layout.song_item, parent, false);
			viewHolder.title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
			viewHolder.artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		viewHolder.title.setText(song.getTitle());
		viewHolder.artist.setText(song.getArtist());
		
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView artist;
		public TextView title;
		//public ImageView image;
	}
}
