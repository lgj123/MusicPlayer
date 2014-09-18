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
import android.view.*;
import android.widget.*;
import com.andreadec.musicplayer.adapters.*;

public class BrowserFragment extends MusicPlayerFragment {
    private int lastFolderPosition; // Used to save the index of the first visible element in the previous folder list. This info will be used to restore list position when browsing back to the last directory. If <=0 no restore is performed.
    private MainActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity)getActivity();
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_simple_list, container, false);
        initialize(view);
        setFloatingButtonVisible(false);
        setEmptyViewMessage(R.string.folderEmpty);
        updateListView(false);
		return view;
	}
	
	@Override
	public void updateListView() {
		updateListView(true);
	}

    @Override
    public void onHeaderClick() {
        gotoParentDir();
    }

    @Override public void onFloatingButtonClick() {}

    private void updateListView(boolean restoreOldPosition) {
		if(activity==null) return;
		BrowserDirectory currentDirectory = ((MusicPlayerApplication)activity.getApplication()).getCurrentDirectory();
		
		if(currentDirectory==null) {
			initializeCurrentDirectory();
			return;
		}

        setHeaderText(getCurrentDirectoryName(currentDirectory));
		
    	ArrayList<File> browsingSubdirs = currentDirectory.getSubdirs();
        ArrayList<BrowserSong> browsingSongs = currentDirectory.getSongs();
        ArrayList<Object> items = new ArrayList<Object>();
        items.addAll(browsingSubdirs);
        items.addAll(browsingSongs);
        BrowserSong playingSong = null;
        if(activity.getCurrentPlayingItem() instanceof BrowserSong) playingSong = (BrowserSong)activity.getCurrentPlayingItem();
        BrowserArrayAdapter browserArrayAdapter = new BrowserArrayAdapter(this, items, playingSong);

		if(restoreOldPosition) {
        	Parcelable state = list.onSaveInstanceState();
        	list.setAdapter(browserArrayAdapter);
        	list.onRestoreInstanceState(state);
        } else {
        	list.setAdapter(browserArrayAdapter);
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
        ListAdapter adapter = list.getAdapter();
		for(int i=0; i<adapter.getCount(); i++) {
			Object item = adapter.getItem(i);
			if(item instanceof BrowserSong) {
				if(((BrowserSong)item).equals(song)) {
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
		Object item = list.getAdapter().getItem(position);
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
		lastFolderPosition = list.getFirstVisiblePosition();
		new ChangeDirTask(newDirectory, scrollToSong, -1).execute();
	}
	
	public void addFolderToPlaylist(Playlist playlist, File folder) {
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
					list.setSelectionFromTop(listScrolling, 0);
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
