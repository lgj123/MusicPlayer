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

package com.andreadec.musicplayer;

import java.util.ArrayList;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.ContextMenu.*;
import android.widget.*;
import android.widget.AdapterView.*;

import com.andreadec.musicplayer.adapters.*;
import com.mobeta.android.dslv.*;
import com.mobeta.android.dslv.DragSortListView.*;

public class PlaylistFragment extends MusicPlayerFragment implements OnItemClickListener, DropListener, RemoveListener {
	private Playlist currentPlaylist = null;
	private DragSortListView listViewPlaylist;
	private PlaylistArrayAdapter playlistArrayAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(container==null) return null;
		View view = inflater.inflate(R.layout.layout_sortable_list, container, false);
		listViewPlaylist = (DragSortListView)view.findViewById(R.id.listView);
		listViewPlaylist.setOnItemClickListener(this);
		listViewPlaylist.setDropListener(this);
		listViewPlaylist.setRemoveListener(this);
		registerForContextMenu(listViewPlaylist);
		updateListView();
		return view;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		int position = ((AdapterContextMenuInfo)menuInfo).position;
		
		Object item = playlistArrayAdapter.getItem(position);
		if(item instanceof Action) return;
		
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.contextmenu_editdelete, menu);

		if(item instanceof Playlist) {
			menu.setHeaderTitle(((Playlist)item).getName());
		} else if(item instanceof PlaylistSong) {
			PlaylistSong song = (PlaylistSong)item;
			menu.setHeaderTitle(song.getArtist()+" - "+song.getTitle());
			menu.findItem(R.id.menu_delete).setTitle(R.string.removeFromPlaylist);
			menu.removeItem(R.id.menu_edit);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		
		Object listItem = playlistArrayAdapter.getItem(position);
		if(listItem instanceof Playlist) {
			switch (item.getItemId()) {
			case R.id.menu_edit:
				editPlaylist((Playlist)listItem);
				return true;
			case R.id.menu_delete:
				deletePlaylist((Playlist)listItem);
				return true;
			}
		} else if(listItem instanceof PlaylistSong) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				deleteSongFromPlaylist((PlaylistSong)listItem);
				return true;
			}
		}
		return false;
	}
	
	private void addPlaylist(String name) {
		Playlists.addPlaylist(name);
		updateListView();
	}
	
	public void editPlaylist(final Playlist playlist) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		int title = playlist==null ? R.string.newPlaylist : R.string.editPlaylist;
		builder.setTitle(getResources().getString(title));
		final View view = getActivity().getLayoutInflater().inflate(R.layout.layout_editplaylist, null);
		builder.setView(view);
		
		final EditText editTextName = (EditText)view.findViewById(R.id.editTextPlaylistName);
		editTextName.setSingleLine();
		if(playlist!=null) editTextName.setText(playlist.getName());
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String name = editTextName.getText().toString();
				if(name==null || name.equals("")) {
					Utils.showMessageDialog(getActivity(), R.string.error, R.string.errorPlaylistName);
					return;
				}
				if(playlist==null) {
					addPlaylist(name);
				} else {
					playlist.editName(name);
					updateListView();
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	private void deletePlaylist(final Playlist playlist) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete);
		builder.setMessage(R.string.deletePlaylistConfirm);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int which) {
		    	  Playlists.deletePlaylist(playlist);
		    	  updateListView();
		      }
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	@Override
	public void updateListView() {
		updateListView(true);
	}
	
	private void updateListView(boolean restoreOldPosition) {
		MainActivity activity = (MainActivity)getActivity();
		PlaylistSong playingSong = null;
        if(activity.getCurrentPlayingItem() instanceof PlaylistSong) {
        	playingSong = (PlaylistSong)(activity.getCurrentPlayingItem());
        }
		ArrayList<Object> values = new ArrayList<Object>();
		if(currentPlaylist==null) { // Show all playlists
			values.add(new Action(Action.ACTION_NEW, getResources().getString(R.string.newPlaylist)));
			ArrayList<Playlist> playlists = Playlists.getPlaylists();
			values.addAll(playlists);
		} else {
			values.add(new Action(Action.ACTION_GO_BACK, currentPlaylist.getName()));
			values.addAll(currentPlaylist.getSongs());
		}
		
		
		playlistArrayAdapter = new PlaylistArrayAdapter(activity, values, playingSong);
		Parcelable state = listViewPlaylist.onSaveInstanceState();
        listViewPlaylist.setAdapter(playlistArrayAdapter);
        listViewPlaylist.onRestoreInstanceState(state);
	}
	
	public void scrollToSong(PlaylistSong song) {
		for(int i=0; i<playlistArrayAdapter.getCount(); i++) {
			Object item = playlistArrayAdapter.getItem(i);
			if(item instanceof PlaylistSong) {
				if(((PlaylistSong)item).equals(song)) {
					final int position = i;
					listViewPlaylist.post(new Runnable() {
						@Override
						public void run() {
							listViewPlaylist.smoothScrollToPosition(position);
						}
					});
					break;
				}
			}
		}
	}
	
	private void deleteSongFromPlaylist(PlaylistSong song) {
		song.getPlaylist().deleteSong(song);
		updateListView();
	}
	
	private void sortPlaylist(int from, int to) {
		if(currentPlaylist==null) { // Sorting playlists
			if(to==0) return;
			Playlists.sortPlaylists(from-1, to-1);
		} else { // Sorting songs in playlist
			if(to==0) return; // The user is trying to put the song above the button to go back to the playlists' list
			currentPlaylist.sort(from-1, to-1); // -1 is due to first element being link to previous folder
		}
		updateListView();
	}
	
	public void showPlaylists() {
		currentPlaylist = null;
		updateListView();
	}
	
	public void showPlaylist(Playlist playlist) {
		currentPlaylist = (playlist);
		updateListView();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = playlistArrayAdapter.getItem(position);
		if(item instanceof Action) {
			Action action = (Action)item;
			if(action.action==Action.ACTION_GO_BACK) showPlaylists();
			else if(action.action==Action.ACTION_NEW) editPlaylist(null);
		} else if(item instanceof PlaylistSong) {
			MainActivity activity = (MainActivity)getActivity();
			activity.playItem((PlaylistSong)item);
			updateListView();
		} else if(item instanceof Playlist) {
			showPlaylist((Playlist)item);
		}
	}

	@Override
	public void drop(int from, int to) {
		sortPlaylist(from, to);
	}

	@Override
	public boolean onBackPressed() {
		if(currentPlaylist!=null) {
			showPlaylists();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void gotoPlayingItemPosition(PlayableItem playingItem) {
		PlaylistSong song = (PlaylistSong)playingItem;
		showPlaylist(song.getPlaylist());
		scrollToSong(song);
	}

	/* Fling-to-remove listener */
	@Override
	public void remove(int which) {
		Object item = playlistArrayAdapter.getItem(which);
		if(item instanceof Playlist) {
			deletePlaylist((Playlist)item);
		} else if(item instanceof PlaylistSong) {
			deleteSongFromPlaylist((PlaylistSong)item);
		}
		playlistArrayAdapter.notifyDataSetChanged();
	}
}
