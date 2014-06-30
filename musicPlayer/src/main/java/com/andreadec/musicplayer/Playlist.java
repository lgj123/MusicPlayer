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

package com.andreadec.musicplayer;

import java.util.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import com.andreadec.musicplayer.database.*;

public class Playlist {
	private long id;
	private String name;
	private ArrayList<PlaylistSong> songs;
	
	public Playlist(long id, String name) {
		this.id = id;
		this.name = name;
		songs = new ArrayList<PlaylistSong>();
		
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT idSong, uri, artist, title, hasImage FROM SongsInPlaylist WHERE idPlaylist="+id+" ORDER BY position", null);
		while(cursor.moveToNext()) {
			long songId = cursor.getLong(0);
			String uri = cursor.getString(1);
			String artist = cursor.getString(2);
			String title = cursor.getString(3);
			boolean hasImage = cursor.getInt(4)==1;
			PlaylistSong song = new PlaylistSong(uri, artist, title, songId, hasImage, this);
			songs.add(song);
		}
		cursor.close();
		db.close();
	}
	
	public void addSong(BrowserSong song) {
		long songId = -1;
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("idPlaylist", id);
		values.put("uri", song.getUri());
		values.put("artist", song.getArtist());
		values.put("title", song.getTitle());
		values.put("hasImage", song.hasImage());
		try {
			songId = db.insertOrThrow("SongsInPlaylist", null, values);
		} catch(Exception e) {
		} finally {
			db.close();
		}
		
		if(songId==-1) return; // Something went wrong
		PlaylistSong playlistSong = new PlaylistSong(song.getUri(), song.getArtist(), song.getTitle(), songId, song.hasImage(), this);
		songs.add(playlistSong);
	}
	
	public void deleteSong(PlaylistSong song) {
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		db.delete("SongsInPlaylist", "idSong="+song.getId(), null);
		db.close();
		songs.remove(song);
	}
	
	public void sort(int from, int to) {
		if(to>from) {
			Collections.rotate(songs.subList(from, to+1), -1);
		} else {
			Collections.rotate(songs.subList(to, from+1), +1);
		}
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		for(int i=0; i<songs.size(); i++) {
			PlaylistSong song = songs.get(i);
			ContentValues values = new ContentValues();
			values.put("position", i);
			db.update("SongsInPlaylist", values, "idSong="+song.getId(), null);
		}
		db.close();
	}
	
	public void editName(String newName) {
		name = newName;
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", newName);
		db.update("Playlists", values, "id="+id, null);
		db.close();
	}
	
	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<PlaylistSong> getSongs() {
		return songs;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Playlist)) return false;
		Playlist p2 = (Playlist)o;
		return id==p2.id;
	}
}
