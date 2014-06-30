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
import android.graphics.*;
import android.graphics.drawable.*;
import android.support.v4.util.*;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class PlaylistArrayAdapter extends MusicListArrayAdapter {
	private PlaylistSong playingSong;
	private boolean showSongImage;
	private LruCache<String,Bitmap> imagesCache;
	private Drawable songImage;
	private final static int TYPE_ACTION=0, TYPE_PLAYLIST=1, TYPE_SONG=2;
 
	public PlaylistArrayAdapter(MainActivity activity, ArrayList<Object> values, PlaylistSong playingSong) {
		super(activity, values);
		this.playingSong = playingSong;
		this.imagesCache = activity.getImagesCache();
		showSongImage = activity.getShowSongImage();
		songImage = activity.getResources().getDrawable(R.drawable.audio);
	}
	
	@Override
	public int getViewTypeCount() {
		return 3;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof Action) return TYPE_ACTION;
		else if(value instanceof Playlist) return TYPE_PLAYLIST;
		else if(value instanceof PlaylistSong) return TYPE_SONG;
		return -1;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		Object value = values.get(position);
		ViewHolder viewHolder;
		int type = getItemViewType(position);
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_ACTION) {
				view = inflater.inflate(R.layout.action_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textView);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageView);
			} else if(type==TYPE_PLAYLIST) {
				view = inflater.inflate(R.layout.folder_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewFolderItemFolder);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
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
			if(action.action==Action.ACTION_GO_BACK) {
				viewHolder.image.setImageResource(R.drawable.back);
			} else if(action.action==Action.ACTION_NEW) {
				viewHolder.image.setImageResource(R.drawable.newcontent);
			}
		} else if(type==TYPE_PLAYLIST) {
			Playlist playlist = (Playlist)value;
			viewHolder.title.setText(playlist.getName());
			viewHolder.image.setImageResource(R.drawable.playlist);
		} else if(type==TYPE_SONG) {
			PlaylistSong song = (PlaylistSong)value;
			viewHolder.title.setText(song.getTitle());
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
							image = imagesCache.get(song.getPlayableUri());
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