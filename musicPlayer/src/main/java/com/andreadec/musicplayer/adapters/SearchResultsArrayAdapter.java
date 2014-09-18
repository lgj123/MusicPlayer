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

import java.io.File;
import java.util.*;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class SearchResultsArrayAdapter extends ArrayAdapter<BrowserSong> {
	private final ArrayList<BrowserSong> songs;
	private LayoutInflater inflater;
    private ImagesCache imagesCache;
    private SearchActivity activity;
 
	public SearchResultsArrayAdapter(SearchActivity activity, ArrayList<BrowserSong> songs) {
		super(activity, R.layout.song_item, songs);
        this.activity = activity;
		this.songs = songs;
		inflater = activity.getLayoutInflater();
        imagesCache = ((MusicPlayerApplication)activity.getApplication()).imagesCache;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		final BrowserSong song = songs.get(position);
		ViewHolder viewHolder;
		
		if(view==null) {
			viewHolder = new ViewHolder();
			view = inflater.inflate(R.layout.song_item, parent, false);
			viewHolder.title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
			viewHolder.artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
            viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
            viewHolder.menu = (ImageButton)view.findViewById(R.id.buttonMenu);
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		viewHolder.title.setText(song.getTitle());
		viewHolder.artist.setText(song.getArtist());
        imagesCache.getImageAsync(song, viewHolder.image);

        viewHolder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToPlaylist(song);
            }
        });
        viewHolder.menu.setFocusable(false);
		
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView artist;
		public TextView title;
		public ImageView image;
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
                playlist.addSong((BrowserSong)item);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
