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

import android.view.*;
import android.widget.*;
import com.andreadec.musicplayer.*;
import com.nhaarman.listviewanimations.ArrayAdapter;

import java.util.ArrayList;

public class PlaylistArrayAdapter extends ArrayAdapter<Object> {
	private PlaylistSong playingSong;
	private ImagesCache imagesCache;
    private ArrayList<Object> values;
    private PlaylistFragment fragment;
    private LayoutInflater inflater;
	private final static int TYPE_PLAYLIST=0, TYPE_SONG=1;
 
	public PlaylistArrayAdapter(PlaylistFragment fragment, ArrayList<Object> values, PlaylistSong playingSong) {
		super(values);
		this.playingSong = playingSong;
        this.fragment = fragment;
		this.imagesCache = ((MusicPlayerApplication)fragment.getActivity().getApplication()).imagesCache;
        this.values = values;
        inflater = fragment.getActivity().getLayoutInflater();
	}

    @Override
    public long getItemId(int position) {
        return (long)getItem(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof Playlist) return TYPE_PLAYLIST;
		else if(value instanceof PlaylistSong) return TYPE_SONG;
		return -1;
	}

    @Override
	public View getView(int position, View view, ViewGroup parent) {
        Object value = getItem(position);
		ViewHolder viewHolder;
		int type = getItemViewType(position);
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_PLAYLIST) {
				view = inflater.inflate(R.layout.playlist_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewName);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
                viewHolder.menu = (ImageButton)view.findViewById(R.id.buttonMenu);
			} else if(type==TYPE_SONG) {
				view = inflater.inflate(R.layout.song_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
				viewHolder.artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
				viewHolder.card = view.findViewById(R.id.card);
                view.findViewById(R.id.buttonMenu).setVisibility(View.GONE);
			}
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		
		
		if(type==TYPE_PLAYLIST) {
			final Playlist playlist = (Playlist)value;
			viewHolder.title.setText(playlist.getName());
            final PopupMenu popup = new PopupMenu(fragment.getActivity(), viewHolder.menu);
            popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_edit:
                            fragment.editPlaylist(playlist);
                            return true;
                        case R.id.menu_delete:
                            fragment.deletePlaylist(playlist);
                            return true;
                    }
                    return true;
                }
            });
            viewHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popup.show();
                }
            });
            viewHolder.menu.setFocusable(false);
        } else if(type==TYPE_SONG) {
			PlaylistSong song = (PlaylistSong)value;
			viewHolder.title.setText(song.getTitle());
			viewHolder.artist.setText(song.getArtist());
			
			if(song.equals(playingSong)) {
				viewHolder.card.setBackgroundResource(R.drawable.card_playing);
				viewHolder.image.setImageResource(R.drawable.play_orange);
			} else {
				viewHolder.card.setBackgroundResource(R.drawable.card);
                viewHolder.image.setImageResource(R.drawable.audio);
                if(song.hasImage()) {
                    imagesCache.getImageAsync(song, viewHolder.image);
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
        public ImageButton menu;
	}
}