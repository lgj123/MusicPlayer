/*
 * Copyright 2013 Andrea De Cesare
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
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.preference.*;
import com.andreadec.musicplayer.comparators.*;
import com.andreadec.musicplayer.database.*;
import com.andreadec.musicplayer.filters.*;

public class BrowserDirectory {
	private File directory;
	private ArrayList<File> subdirs;
	private ArrayList<BrowserSong> songs;
	private SharedPreferences preferences;
	
	public BrowserDirectory(File directory) {
		preferences = PreferenceManager.getDefaultSharedPreferences(MusicPlayerApplication.getContext());
		this.directory = directory;
		subdirs = getSubfoldersInDirectory(directory);
		songs = getSongsInDirectory(directory, preferences.getString(Constants.PREFERENCE_SONGSSORTINGMETHOD, Constants.DEFAULT_SONGSSORTINGMETHOD), preferences.getBoolean(Constants.PREFERENCE_ENABLECACHE, Constants.DEFAULT_ENABLECACHE), this);
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public ArrayList<File> getSubdirs() {
		return subdirs;
	}
	
	public ArrayList<BrowserSong> getSongs() {
		return songs;
	}
	
	
	
	public static ArrayList<BrowserSong> getSongsInDirectory(File directory, String sortingMethod, boolean enableCache, BrowserDirectory browserDirectory) {
		if(enableCache) {
			return getSongsInDirectoryWithCache(directory, sortingMethod, browserDirectory);
		} else {
			return getSongsInDirectoryWithoutCache(directory, sortingMethod, browserDirectory);
		}
	}
	
	private static ArrayList<BrowserSong> getSongsInDirectoryWithoutCache(File directory, String sortingMethod, BrowserDirectory browserDirectory) {
		ArrayList<BrowserSong> songs = new ArrayList<BrowserSong>();
		File files[] = directory.listFiles(new AudioFileFilter());
		for (File file : files) {
			BrowserSong song = new BrowserSong(file.getAbsolutePath(), browserDirectory);
			songs.add(song);
		}
		Collections.sort(songs, new BrowserSongsComparator(sortingMethod));
		return songs;
	}
	
	private static ArrayList<BrowserSong> getSongsInDirectoryWithCache(File directory, String sortingMethod, BrowserDirectory browserDirectory) {
		ArrayList<BrowserSong> songs = new ArrayList<BrowserSong>();
		File files[] = directory.listFiles(new AudioFileFilter());
		
		SongsDatabase songsDatabase = new SongsDatabase();
		final LinkedList<BrowserSong> songsToInsertInDB = new LinkedList<BrowserSong>();
		SQLiteDatabase db = songsDatabase.getWritableDatabase();
		
		for (File file : files) {
			String uri = file.getAbsolutePath();
			BrowserSong song;
			
			Cursor cursor = db.rawQuery("SELECT artist, title, trackNumber, hasImage FROM Songs WHERE uri=\""+uri+"\"", null);
			if(cursor.moveToNext()) {
	        	Integer trackNumber = cursor.getInt(2);
	        	if(trackNumber==-1) trackNumber=null;
	        	boolean hasImage = cursor.getInt(3)==1;
	        	song = new BrowserSong(uri, cursor.getString(0), cursor.getString(1), trackNumber, hasImage, browserDirectory);
	        } else {
	        	song = new BrowserSong(file.getAbsolutePath(), browserDirectory);
				songsToInsertInDB.add(song);
	        }
			songs.add(song);
		}
		db.close();
		
		Collections.sort(songs, new BrowserSongsComparator(sortingMethod));
		
		if(songsToInsertInDB.size()>0) {
			new Thread() {
				public void run() {
					SongsDatabase songsDatabase2 = new SongsDatabase();
					SQLiteDatabase db2 = songsDatabase2.getWritableDatabase();
					for(BrowserSong song : songsToInsertInDB) {
						ContentValues values = new ContentValues();
						values.put("uri", song.getUri());
						values.put("artist", song.getArtist());
						values.put("title", song.getTitle());
						Integer trackNumber = song.getTrackNumber();
						if(trackNumber==null) trackNumber=-1;
						values.put("trackNumber", trackNumber);
						values.put("hasImage", song.hasImage());
						db2.insertWithOnConflict("Songs", null, values, SQLiteDatabase.CONFLICT_REPLACE);
					}
					db2.close();
				}
			}.start();
		}
		
		return songs;
	}
	
	// Lists all the subfolders of a given directory
	public ArrayList<File> getSubfoldersInDirectory(File directory) {
		ArrayList<File> subfolders = new ArrayList<File>();
		File files[] = directory.listFiles(new DirectoryFilter());
		for (File file : files) {
			subfolders.add(file);
		}
		Collections.sort(subfolders);
		return subfolders;
	}
}
