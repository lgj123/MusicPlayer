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

import java.io.*;
import java.util.*;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.v4.util.*;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class BrowserArrayAdapter extends MusicListArrayAdapter {
	private BrowserSong playingSong;
	private boolean showSongImage;
	private LruCache<String,Bitmap> imagesCache;
	private final static int TYPE_DIRECTORY=0, TYPE_SONG=1, TYPE_ACTION=2;
	private Drawable songImage;
 
	public BrowserArrayAdapter(MainActivity activity, ArrayList<Object> values, BrowserSong playingSong) {
		super(activity, values);
		this.playingSong = playingSong;
		showSongImage = activity.getShowSongImage();
		this.imagesCache = activity.getImagesCache();
		songImage = activity.getResources().getDrawable(R.drawable.audio);
	}
	
	@Override
	public int getViewTypeCount() {
		return 3;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof File) return TYPE_DIRECTORY;
		else if(value instanceof BrowserSong) return TYPE_SONG;
		else return TYPE_ACTION;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		int type = getItemViewType(position);
		Object value = values.get(position);
		ViewHolder viewHolder;
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_ACTION) {
				view = inflater.inflate(R.layout.action_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textView);
				viewHolder.title.setTextColor(view.getResources().getColor(R.color.orange1));
				viewHolder.image = (ImageView)view.findViewById(R.id.imageView);
				viewHolder.image.setImageResource(R.drawable.back);
			} else if(type==TYPE_DIRECTORY) {
				view = inflater.inflate(R.layout.folder_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewFolderItemFolder);
			} else if(type==TYPE_SONG) {
				view = inflater.inflate(R.layout.song_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
				viewHolder.artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
				viewHolder.card = view.findViewById(R.id.card);
			}
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		if(type==TYPE_ACTION) {
			Action action = (Action)value;
			viewHolder.title.setText(action.msg);
		} else if(type==TYPE_DIRECTORY) {
			File file = (File)value;
			viewHolder.title.setText(file.getName());
		} else if(type==TYPE_SONG) {
			BrowserSong song = (BrowserSong)value;
			String trackNumber = "";
			if(song.getTrackNumber()!=null) trackNumber = song.getTrackNumber() + ". ";
			viewHolder.title.setText(trackNumber + song.getTitle());
			viewHolder.artist.setText(song.getArtist());
			if(song.equals(playingSong)) {
				viewHolder.card.setBackgroundResource(R.drawable.card_playing);
				viewHolder.image.setImageResource(R.drawable.play_orange);
			} else {
				viewHolder.card.setBackgroundResource(R.drawable.card);
				if(showSongImage) {
					viewHolder.image.setImageDrawable(songImage);
					if(song.hasImage()) {
						Bitmap image;
						synchronized(imagesCache) {
							image = imagesCache.get(song.getUri());
						}
						if(image!=null) {
							viewHolder.image.setImageBitmap(image);
						}
						else new ImageLoaderTask(song, viewHolder.image, imagesCache, listImageSize).execute();
					}
				}
			}
		}
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView artist;
		public TextView title;
		public ImageView image;
		public View card;
	}
}