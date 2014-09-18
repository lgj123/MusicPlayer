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

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class BrowserArrayAdapter extends ArrayAdapter<Object> {
	private BrowserSong playingSong;
	private ImagesCache imagesCache;
    private Drawable songImage;
    private ArrayList<Object> values;
    private LayoutInflater inflater;
    private MainActivity activity;
    private BrowserFragment fragment;
	private final static int TYPE_DIRECTORY=0, TYPE_SONG=1;
 
	public BrowserArrayAdapter(BrowserFragment fragment, ArrayList<Object> values, BrowserSong playingSong) {
        super(fragment.getActivity(), R.layout.song_item, values);
        this.values = values;
		this.playingSong = playingSong;
        this.fragment = fragment;
        activity = (MainActivity)fragment.getActivity();
		this.imagesCache = ((MusicPlayerApplication)activity.getApplication()).imagesCache;
        inflater = activity.getLayoutInflater();
        songImage = activity.getResources().getDrawable(R.drawable.audio);
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof File) return TYPE_DIRECTORY;
		else return TYPE_SONG;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		int type = getItemViewType(position);
		Object value = values.get(position);
		ViewHolder viewHolder;
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_DIRECTORY) {
				view = inflater.inflate(R.layout.folder_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewFolderItemFolder);
                viewHolder.menu = (ImageButton)view.findViewById(R.id.buttonMenu);
			} else if(type==TYPE_SONG) {
				view = inflater.inflate(R.layout.song_item, parent, false);
				viewHolder.title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
				viewHolder.artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
				viewHolder.card = view.findViewById(R.id.card);
                viewHolder.menu = (ImageButton)view.findViewById(R.id.buttonMenu);
			}
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		if(type==TYPE_DIRECTORY) {
			final File file = (File)value;
			viewHolder.title.setText(file.getName());
            final PopupMenu popup = new PopupMenu(fragment.getActivity(), viewHolder.menu);
            popup.getMenuInflater().inflate(R.menu.contextmenu_browserdirectory, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_addFolderToPlaylist:
                            addToPlaylist(file);
                            return true;
                        case R.id.menu_setAsBaseFolder:
                            activity.setBaseFolder(file);
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
		} else if(type==TYPE_SONG) {
			final BrowserSong song = (BrowserSong)value;
			String trackNumber = "";
			if(song.getTrackNumber()!=null) trackNumber = song.getTrackNumber() + ". ";
			viewHolder.title.setText(trackNumber + song.getTitle());
			viewHolder.artist.setText(song.getArtist());
			if(song.equals(playingSong)) {
				viewHolder.card.setBackgroundResource(R.drawable.card_playing);
				viewHolder.image.setImageResource(R.drawable.play_orange);
			} else {
				viewHolder.card.setBackgroundResource(R.drawable.card);
                viewHolder.image.setImageDrawable(songImage);
                if(song.hasImage()) {
                    imagesCache.getImageAsync(song, viewHolder.image);
                }
			}
            /*final PopupMenu popup = new PopupMenu(fragment.getActivity(), viewHolder.menu);
            popup.getMenuInflater().inflate(R.menu.contextmenu_browsersong, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_addToPlaylist:
                            addToPlaylist(song);
                            return true;
                        case R.id.menu_reloadSongInfo:
                            BrowserDirectory.reloadSongFromDisk(song);
                            Toast.makeText(activity, R.string.reloadSongInfoDone, Toast.LENGTH_SHORT).show();
                            fragment.updateListView();
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
            viewHolder.menu.setFocusable(false);*/
            viewHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addToPlaylist(song);
                }
            });
		}
        viewHolder.menu.setFocusable(false);
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

    private void addToPlaylist(final Object item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.addToPlaylist);
        ListView list = new ListView(activity);
        builder.setView(list);
        final AlertDialog dialog = builder.create();
        final ArrayAdapter<Playlist> adapter = new ArrayAdapter<Playlist>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, Playlists.getPlaylists());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Playlist playlist = adapter.getItem(position);
                if (item instanceof File) {
                    fragment.addFolderToPlaylist(playlist, (File)item);
                } else {
                    playlist.addSong((BrowserSong)item);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}