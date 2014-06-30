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

import java.io.*;
import java.util.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.view.ContextMenu.*;
import android.widget.*;
import android.widget.AdapterView.*;

import com.andreadec.musicplayer.adapters.*;

public class BrowserFragment extends MusicPlayerFragment implements OnItemClickListener {
	private final static int MENU_SETASBASEFOLDER = -1, MENU_ADDFOLDERTOPLAYLIST = -2;
	private ListView listViewBrowser;
	private BrowserArrayAdapter browserArrayAdapter;
	private SharedPreferences preferences;
	private MainActivity activity;
	private int lastFolderPosition; // Used to save the index of the first visible element in the previous folder list. This info will be used to restore list position when browsing back to the last directory. If <=0 no restore is performed.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = (MainActivity)getActivity();
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_simple_list, container, false);
		listViewBrowser = (ListView)view.findViewById(R.id.listView);
		listViewBrowser.setOnItemClickListener(this);
		registerForContextMenu(listViewBrowser);
		updateListView(false);
		return view;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		int position = ((AdapterContextMenuInfo)menuInfo).position;
		Object item = browserArrayAdapter.getItem(position);
		if(item instanceof String) return;
		
		super.onCreateContextMenu(menu, view, menuInfo);
		
		if(item instanceof File) {
			menu.add(ContextMenu.NONE, MENU_SETASBASEFOLDER, 0, activity.getResources().getString(R.string.setAsBaseFolder));
			ArrayList<Playlist> playlists = Playlists.getPlaylists();
			if(playlists.size()>0) {
				SubMenu playlistMenu = menu.addSubMenu(ContextMenu.NONE, MENU_ADDFOLDERTOPLAYLIST, 1, R.string.addFolderToPlaylist);
				playlistMenu.setHeaderTitle(R.string.addToPlaylist);
				for(int i=0; i<playlists.size(); i++) {
					Playlist playlist = playlists.get(i);
					playlistMenu.add(ContextMenu.NONE, i, i, playlist.getName());
				}
			}
		} else if(item instanceof BrowserSong) {
			menu.setHeaderTitle(R.string.addToPlaylist);
			ArrayList<Playlist> playlists = Playlists.getPlaylists();
			for(int i=0; i<playlists.size(); i++) {
				Playlist playlist = playlists.get(i);
				menu.add(ContextMenu.NONE, i, i, playlist.getName());
			}
		}
	}
	
	int oldPosition;
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int position;
		if(info!=null) {
			position = info.position;
			oldPosition = position;
		} else {
			position = oldPosition;
		}
		Object listItem = browserArrayAdapter.getItem(position);
		
		if(listItem instanceof String) return true;
		
		ArrayList<Playlist> playlists = Playlists.getPlaylists();
		
		if(listItem instanceof BrowserSong) {
			BrowserSong song = (BrowserSong)listItem;
			playlists.get(item.getItemId()).addSong(song);
		} else if(listItem instanceof File) {
			File folder = (File)listItem;
			switch(item.getItemId()) {
			case MENU_SETASBASEFOLDER:
				activity.setBaseFolder(folder);
				break;
			case MENU_ADDFOLDERTOPLAYLIST:
				addFolderToPlaylist(playlists.get(item.getItemId()), folder);
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public void updateListView() {
		updateListView(true);
	}
	
	private void updateListView(boolean restoreOldPosition) {
		if(activity==null) return;
		BrowserDirectory currentDirectory = ((MusicPlayerApplication)activity.getApplication()).getCurrentDirectory();
		
		if(currentDirectory==null) {
			initializeCurrentDirectory();
			return;
		}
		
    	ArrayList<File> browsingSubdirs = currentDirectory.getSubdirs();
        ArrayList<BrowserSong> browsingSongs = currentDirectory.getSongs();
        ArrayList<Object> items = new ArrayList<Object>();
        items.add(new Action(Action.ACTION_GO_BACK, getCurrentDirectoryName(currentDirectory)));
        items.addAll(browsingSubdirs);
        items.addAll(browsingSongs);
        BrowserSong playingSong = null;
        if(activity.getCurrentPlayingItem() instanceof BrowserSong) playingSong = (BrowserSong)activity.getCurrentPlayingItem();
        browserArrayAdapter = new BrowserArrayAdapter(activity, items, playingSong);
		
		if(restoreOldPosition) {
        	Parcelable state = listViewBrowser.onSaveInstanceState();
        	listViewBrowser.setAdapter(browserArrayAdapter);
        	listViewBrowser.onRestoreInstanceState(state);
        } else {
        	listViewBrowser.setAdapter(browserArrayAdapter);
        }
	}
	
	private void initializeCurrentDirectory() {
		final String lastDirectory = preferences.getString(Constants.PREFERENCE_LASTDIRECTORY, Constants.DEFAULT_LASTDIRECTORY); // Read the last used directory from preferences
		File startDir;
		if(lastDirectory==null) {
			startDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		} else {
			startDir = new File(lastDirectory);
		}
		if(!startDir.exists()) startDir = new File("/");
		gotoDirectory(startDir, null);
	}
	
	public void scrollToSong(BrowserSong song) {
		for(int i=0; i<browserArrayAdapter.getCount(); i++) {
			Object item = browserArrayAdapter.getItem(i);
			if(item instanceof BrowserSong) {
				if(((BrowserSong)item).equals(song)) {
					final int position = i;
					listViewBrowser.post(new Runnable() {
						@Override
						public void run() {
							listViewBrowser.smoothScrollToPosition(position);
						}
					});
					break;
				}
			}
		}
	}
	
	private String getCurrentDirectoryName(BrowserDirectory currentDirectory) {
		String currentDirectoryName = currentDirectory.getDirectory().getAbsolutePath();
		if(!preferences.getBoolean(Constants.PREFERENCE_SHOWRELATIVEPATHUNDERBASEDIRECTORY, Constants.DEFAULT_SHOWRELATIVEPATHUNDERBASEDIRECTORY)) {
			return currentDirectoryName;
		}
		
		String baseDirectory = preferences.getString(Constants.PREFERENCE_BASEFOLDER, Constants.DEFAULT_BASEFOLDER);
		
		if(baseDirectory!=null && currentDirectoryName.startsWith(baseDirectory) && !currentDirectoryName.equals(baseDirectory)) {
			return currentDirectoryName.substring(baseDirectory.length()+1); // +1 removes initial "/"
		} else {
			return currentDirectoryName;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = browserArrayAdapter.getItem(position);
		if (item instanceof File) {
			File newDirectory = (File) item;
			gotoDirectory(newDirectory, null);
		} else if (item instanceof BrowserSong) {
			activity.playItem((BrowserSong)item);
		} else {
			gotoParentDir();
		}
	}
	
	public void gotoParentDir() {
		File currentDir = ((MusicPlayerApplication)activity.getApplication()).getCurrentDirectory().getDirectory();
		final File parentDir = currentDir.getParentFile();
		String baseDirectory = preferences.getString(Constants.PREFERENCE_BASEFOLDER, Constants.DEFAULT_BASEFOLDER);
		
		if(baseDirectory!=null && new File(baseDirectory).equals(currentDir)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.baseFolderReachedTitle);
			builder.setMessage(R.string.baseFolderReachedMessage);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					new ChangeDirTask(parentDir, null, -1).execute();
				}
			});
			builder.setNegativeButton(R.string.no, null);
			builder.setNeutralButton(R.string.quitApp, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					activity.quitApplication();
				}
			});
			builder.show();
		} else {
			new ChangeDirTask(parentDir, null, lastFolderPosition).execute();
			lastFolderPosition = -1;
		}
	}
	
	public void gotoBaseFolder() {
		String baseFolder = preferences.getString(Constants.PREFERENCE_BASEFOLDER, Constants.DEFAULT_BASEFOLDER);
		if(baseFolder==null) {
			Utils.showMessageDialog(activity, R.string.baseFolderNotSetTitle, R.string.baseFolderNotSetMessage);
		} else {
			gotoDirectory(new File(baseFolder), null);
		}
	}
	
	public void gotoDirectory(File newDirectory, BrowserSong scrollToSong) {
		lastFolderPosition = listViewBrowser.getFirstVisiblePosition();
		new ChangeDirTask(newDirectory, scrollToSong, -1).execute();
	}
	
	private void addFolderToPlaylist(Playlist playlist, File folder) {
		new AddFolderToPlaylistTask(playlist, folder).execute();
	}
	
	private class ChangeDirTask extends AsyncTask<Void, Void, Boolean> {
		private File newDirectory;
		private BrowserSong gotoSong;
		private int listScrolling;
		public ChangeDirTask(File newDirectory, BrowserSong gotoSong, int listScrolling) {
			this.newDirectory = newDirectory;
			this.gotoSong = gotoSong;
			this.listScrolling = listScrolling;
		}
		@Override
		protected void onPreExecute() {
			activity.setProgressBarIndeterminateVisibility(true);
	    }
		@Override
		protected Boolean doInBackground(Void... params) {
			if (newDirectory!=null && newDirectory.canRead()) {
				((MusicPlayerApplication)activity.getApplication()).gotoDirectory(newDirectory);
				return true;
			} else {
				return false;
			}
		}
		@Override
		protected void onPostExecute(final Boolean success) {
			if(success) {
				updateListView(false);
				if(gotoSong!=null) {
					scrollToSong(gotoSong);
				}
				if(listScrolling>0) {
					listViewBrowser.setSelectionFromTop(listScrolling, 0);
				}
			} else {
				Toast.makeText(activity, R.string.dirError, Toast.LENGTH_SHORT).show();
			}
			activity.setProgressBarIndeterminateVisibility(false);
		}
	}
	
	private class AddFolderToPlaylistTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progressDialog;
		private Playlist playlist;
		private File folder;
		public AddFolderToPlaylistTask(Playlist playlist, File folder) {
			this.playlist = playlist;
			this.folder = folder;
		}
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(activity);
	        progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(false);
	        progressDialog.setMessage(activity.getResources().getString(R.string.addingSongsToPlaylist));
			progressDialog.show();
	    }
		@Override
		protected Void doInBackground(Void... params) {
			List<BrowserSong> songs = BrowserDirectory.getSongsInDirectory(folder, preferences.getString(Constants.PREFERENCE_SONGSSORTINGMETHOD, Constants.DEFAULT_SONGSSORTINGMETHOD), preferences.getBoolean(Constants.PREFERENCE_ENABLECACHE, Constants.DEFAULT_ENABLECACHE), null);
			for(BrowserSong song : songs) {
				playlist.addSong(song);
			}
			return null;
		}
		@Override
		protected void onPostExecute(final Void success) {
			if(progressDialog.isShowing()) progressDialog.dismiss();
		}
	}

	@Override
	public boolean onBackPressed() {
		gotoParentDir();
		return true;
	}

	@Override
	public void gotoPlayingItemPosition(PlayableItem playingItem) {
		BrowserSong song = (BrowserSong)playingItem;
		File songDirectory = new File(playingItem.getPlayableUri()).getParentFile();
		if(!songDirectory.equals(((MusicPlayerApplication)activity.getApplication()).getCurrentDirectory().getDirectory())) {
			gotoDirectory(songDirectory, song);
		} else {
			scrollToSong(song);
		}
	}
}
