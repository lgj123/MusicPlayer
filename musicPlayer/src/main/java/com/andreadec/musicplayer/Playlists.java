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

import java.util.*;

import android.content.ContentValues;
import android.database.*;
import android.database.sqlite.*;

import com.andreadec.musicplayer.database.PlaylistsDatabase;

public class Playlists {
	private static ArrayList<Playlist> playlists;
	
	public static ArrayList<Playlist> getPlaylists() {
		if(playlists==null) loadPlaylists();
		return playlists;
	}
	
	private static void loadPlaylists() {
		playlists = new ArrayList<Playlist>();
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT id, name FROM Playlists ORDER BY position", null);
		while(cursor.moveToNext()) {
			long id = cursor.getLong(0);
			String name = cursor.getString(1);
			Playlist playlist = new Playlist(id, name);
			playlists.add(playlist);
		}
		cursor.close();
		db.close();
	}
	
	public static Playlist addPlaylist(String name) {
		if(playlists==null) loadPlaylists();
		long id = -1;
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", name);
		try {
			id = db.insertOrThrow("Playlists", null, values);
		} catch(Exception e) {
		} finally {
			db.close();
		}
		if(id==-1) return null; // Something went wrong
		
		Playlist playlist = new Playlist(id, name);
		playlists.add(playlist);
		return playlist;
	}
	
	public static void deletePlaylist(Playlist playlist) {
		if(playlists==null) loadPlaylists();
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		db.delete("SongsInPlaylist", "idPlaylist="+playlist.getId(), null);
		db.delete("Playlists", "id="+playlist.getId(), null);
		db.close();
		playlists.remove(playlist);
	}
	
	public static void sortPlaylists(int from, int to) {
		if(playlists==null) loadPlaylists();
		if(to>from) {
			Collections.rotate(playlists.subList(from, to+1), -1);
		} else {
			Collections.rotate(playlists.subList(to, from+1), +1);
		}
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getWritableDatabase();
		for(int i=0; i<playlists.size(); i++) {
			Playlist playlist = playlists.get(i);
			ContentValues values = new ContentValues();
			values.put("position", i);
			db.update("Playlists", values, "id="+playlist.getId(), null);
		}
		db.close();
	}
	
	public static PlaylistSong getSavedSongFromPlaylist(long idSong) {
		PlaylistSong song = null;
		PlaylistsDatabase playlistsDatabase = new PlaylistsDatabase();
		SQLiteDatabase db = playlistsDatabase.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT idPlaylist, uri, artist, title, hasImage FROM SongsInPlaylist WHERE idSong="+idSong, null);
		if(cursor.moveToNext()) {
			Playlist playlist = null;
			long idPlaylist = cursor.getLong(0);
			for(Playlist p : Playlists.getPlaylists()) {
				if(p.getId()==idPlaylist) playlist=p;
				break;
			}
			String uri = cursor.getString(1);
			String artist = cursor.getString(2);
			String title = cursor.getString(3);
			boolean hasImage = cursor.getInt(4)==1;
			song = new PlaylistSong(uri, artist, title, idSong, hasImage, playlist);
		}
		cursor.close();
		db.close();
		return song;
	}
}
