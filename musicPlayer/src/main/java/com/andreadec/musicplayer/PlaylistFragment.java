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
import android.widget.*;
import android.widget.AdapterView.*;

import com.andreadec.musicplayer.adapters.*;
import com.nhaarman.listviewanimations.itemmanipulation.*;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.*;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;

public class PlaylistFragment extends MusicPlayerFragment implements OnItemMovedListener {
    private PlaylistArrayAdapter playlistArrayAdapter;
    private Playlist currentPlaylist = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(container==null) return null;
		View view = inflater.inflate(R.layout.layout_sortable_list, container, false);
        initialize(view);
		((DynamicListView)list).enableDragAndDrop();
        ((DynamicListView)list).setDraggableManager(new TouchViewDraggableManager(R.id.imageViewItemImage));
        ((DynamicListView)list).setOnItemMovedListener(this);
        ((DynamicListView)list).enableSwipeToDismiss(new OnDismissCallback() {
            @Override
            public void onDismiss(ViewGroup viewGroup, int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
                    Object item = playlistArrayAdapter.getItem(position);
                    if(currentPlaylist==null) {
                        deletePlaylist((Playlist)item);
                    } else {
                        deleteSongFromPlaylist((PlaylistSong)item);
                    }
                    playlistArrayAdapter.remove(item);
                }
            }
        });
        list.setOnItemLongClickListener(
                new OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        ((DynamicListView)list).startDragging(position);
                        return true;
                    }
                }
        );
        updateListView();
		return view;
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
	
	public void deletePlaylist(final Playlist playlist) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(playlist.getName());
		builder.setMessage(R.string.deletePlaylistConfirm);
        builder.setCancelable(false);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Playlists.deletePlaylist(playlist);
                updateListView();
            }
		});
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                updateListView();
            }
        });
		builder.show();
	}
	
	@Override
	public void updateListView() {
		MainActivity activity = (MainActivity)getActivity();
		PlaylistSong playingSong = null;
        if(activity.getCurrentPlayingItem() instanceof PlaylistSong) {
        	playingSong = (PlaylistSong)(activity.getCurrentPlayingItem());
        }
		ArrayList<Object> values = new ArrayList<Object>();
		if(currentPlaylist==null) { // Show all playlists
            setHeaderVisible(false);
            setFloatingButtonVisible(true);
            setEmptyViewMessage(R.string.noPlaylists);
			ArrayList<Playlist> playlists = Playlists.getPlaylists();
			values.addAll(playlists);
		} else {
            setHeaderVisible(true);
            setFloatingButtonVisible(false);
            setEmptyViewMessage(R.string.playlistEmpty);
            setHeaderText(currentPlaylist.getName());
			values.addAll(currentPlaylist.getSongs());
		}

        playlistArrayAdapter = new PlaylistArrayAdapter(this, values, playingSong);
		Parcelable state = list.onSaveInstanceState();
        list.setAdapter(playlistArrayAdapter);

        /*AlphaInAnimationAdapter animationAdapter = new AlphaInAnimationAdapter(playlistArrayAdapter);
        animationAdapter.setAbsListView(listViewPlaylist);
        listViewPlaylist.setAdapter(animationAdapter);*/

        list.onRestoreInstanceState(state);
	}

    public void scrollToSong(PlaylistSong song) {
        ListAdapter adapter = list.getAdapter();
		for(int i=0; i<adapter.getCount(); i++) {
			Object item = adapter.getItem(i);
			if(item instanceof PlaylistSong) {
				if(item.equals(song)) {
					final int position = i;
					list.post(new Runnable() {
						@Override
						public void run() {
							list.smoothScrollToPosition(position);
						}
					});
					break;
				}
			}
		}
	}
	
	private void deleteSongFromPlaylist(PlaylistSong song) {
		song.getPlaylist().deleteSong(song);
	}
	
	private void sortPlaylist(int from, int to) {
		if(currentPlaylist==null) { // Sorting playlists
			Playlists.sortPlaylists(from, to);
		} else { // Sorting songs in playlist
			currentPlaylist.sort(from, to);
		}
	}
	
	public void showPlaylist(Playlist playlist) {
		currentPlaylist = playlist;
		updateListView();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = list.getAdapter().getItem(position);
		if(item instanceof PlaylistSong) {
			MainActivity activity = (MainActivity)getActivity();
			activity.playItem((PlaylistSong)item);
			updateListView();
		} else if(item instanceof Playlist) {
			showPlaylist((Playlist)item);
		}
	}

	@Override
	public boolean onBackPressed() {
		if(currentPlaylist!=null) {
			showPlaylist(null);
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

    @Override
    public void onItemMoved(int from, int to) {
        sortPlaylist(from, to);
    }

    @Override
    public void onHeaderClick() {
        showPlaylist(null);
    }

    @Override
    public void onFloatingButtonClick() {
        editPlaylist(null);
    }
}
