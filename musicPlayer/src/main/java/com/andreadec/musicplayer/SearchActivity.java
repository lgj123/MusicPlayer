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

import java.io.File;
import java.util.*;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.AdapterView.*;

import com.andreadec.musicplayer.adapters.*;
import com.andreadec.musicplayer.database.*;

public class SearchActivity extends Activity implements OnClickListener, OnItemClickListener, OnKeyListener {
	private EditText editTextSearch;
	private ImageButton buttonSearch;
	private ListView listViewSearch;
	private SharedPreferences preferences;
	private MusicPlayerApplication application;
	private String lastSearch;
	
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        if(preferences.getBoolean(Constants.PREFERENCE_SHOWHELPOVERLAYINDEXING, true) && preferences.getString(Constants.PREFERENCE_BASEFOLDER, "/").equals("/")) {
        	final FrameLayout frameLayout = new FrameLayout(this);
        	LayoutInflater layoutInflater = getLayoutInflater();
        	layoutInflater.inflate(R.layout.layout_search, frameLayout);
        	layoutInflater.inflate(R.layout.layout_helpoverlay_indexing, frameLayout);
        	final View overlayView = frameLayout.getChildAt(1);
        	overlayView.setOnClickListener(new OnClickListener() {
				@Override public void onClick(View v) {
					frameLayout.removeView(overlayView);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean("showHelpOverlayIndexing", false);
					editor.commit();
				}
             	});
        	setContentView(frameLayout);
        } else {
        	setContentView(R.layout.layout_search);
        }
        
        if(Build.VERSION.SDK_INT >= 11) {
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
        
        editTextSearch = (EditText)findViewById(R.id.editTextSearch);
        editTextSearch.setOnKeyListener(this);
        buttonSearch = (ImageButton)findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(this);
        listViewSearch = (ListView)findViewById(R.id.listViewSearch);
        listViewSearch.setOnItemClickListener(this);
        registerForContextMenu(listViewSearch);
        
        application = (MusicPlayerApplication)getApplication();
        lastSearch = application.getLastSearch();
        
        setResult(0, getIntent());
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		if(lastSearch==null || lastSearch.equals("")) {
			menu.findItem(R.id.menu_repeatLastSearch).setVisible(false);
        }
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.menu_repeatLastSearch:
			if(lastSearch==null) return true;
			editTextSearch.setText(lastSearch);
			search();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        int position = ((AdapterContextMenuInfo)menuInfo).position;
        SearchResultsArrayAdapter adapter = (SearchResultsArrayAdapter)listViewSearch.getAdapter();
        Object item = adapter.getItem(position);

        super.onCreateContextMenu(menu, view, menuInfo);

        menu.setHeaderTitle(R.string.addToPlaylist);
        ArrayList<Playlist> playlists = Playlists.getPlaylists();
        for(int i=0; i<playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            menu.add(ContextMenu.NONE, i, i, playlist.getName());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        if(info==null) return true;

        SearchResultsArrayAdapter adapter = (SearchResultsArrayAdapter)listViewSearch.getAdapter();
        BrowserSong song = adapter.getItem(info.position);

        ArrayList<Playlist> playlists = Playlists.getPlaylists();
        playlists.get(item.getItemId()).addSong(song);

        return true;
    }

	@Override
	public void onClick(View view) {
		if(view.equals(buttonSearch)) {
			search();
		}
	}
	
	private void search() {
		String text = editTextSearch.getText().toString();
		search(text);
		application.setLastSearch(text);
	}
	
	private void search(String str) {
		str = str.replace("\"", "");
		str = str.trim();
		ArrayList<BrowserSong> results = new ArrayList<BrowserSong>();
		
		SongsDatabase songsDatabase = new SongsDatabase();
		SQLiteDatabase db = songsDatabase.getWritableDatabase();
		
		Cursor cursor = db.rawQuery("SELECT uri, artist, title, trackNumber, hasImage FROM Songs WHERE artist LIKE \"%"+str+"%\" OR title LIKE \"%"+str+"%\"", null);
		while(cursor.moveToNext()) {
			String uri = cursor.getString(0);
			String artist = cursor.getString(1);
			String title = cursor.getString(2);
        	Integer trackNumber = cursor.getInt(3);
        	if(trackNumber==-1) trackNumber=null;
        	boolean hasImage = cursor.getInt(4)==1;
        	BrowserSong song = new BrowserSong(uri, artist, title, trackNumber, hasImage, null);
        	results.add(song);
        }
		db.close();
		
		if(results.size()==0) {
			Utils.showMessageDialog(this, R.string.noResultsFoundTitle, R.string.noResultsFoundMessage);
		} else {
			SearchResultsArrayAdapter adapter = new SearchResultsArrayAdapter(this, results);
			listViewSearch.setAdapter(adapter);
		}
	}
	
	private void deleteSongFromCache(BrowserSong song) {
		SongsDatabase songsDatabase = new SongsDatabase();
		SQLiteDatabase db = songsDatabase.getWritableDatabase();
		db.delete("Songs", "uri=\""+song.getUri()+"\"", null);
		db.close();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SearchResultsArrayAdapter adapter = (SearchResultsArrayAdapter)listViewSearch.getAdapter();
		BrowserSong song = adapter.getItem(position);
		Intent intent = getIntent();
		intent.putExtra("song", song);
		File songFile = new File(song.getUri());
		if(!songFile.exists()) {
			Utils.showMessageDialog(this, R.string.notFound, R.string.songNotFound);
			deleteSongFromCache(song);
			return;
		}
		setResult(1, intent);
		finish();
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// Manage "enter" key on keyboard
		if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode==KeyEvent.KEYCODE_ENTER) {
			search();
			return true;
		}
		return false;
	}
}
