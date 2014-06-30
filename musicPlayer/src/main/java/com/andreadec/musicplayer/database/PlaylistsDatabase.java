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

package com.andreadec.musicplayer.database;

import com.andreadec.musicplayer.MusicPlayerApplication;
import android.database.sqlite.*;

public class PlaylistsDatabase extends SQLiteOpenHelper {
	private static final String DB_NAME = "Playlists";
	private static final int DB_VERSION = 3;
	
	public PlaylistsDatabase() {
		super(MusicPlayerApplication.getContext(), DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE Playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, position INTEGER DEFAULT 0)");
		db.execSQL("CREATE TABLE SongsInPlaylist (idSong INTEGER, idPlaylist INTEGER, uri TEXT, artist TEXT, title TEXT, position INTEGER DEFAULT 0, hasImage INTEGER DEFAULT 1, PRIMARY KEY(idSong), FOREIGN KEY(idPlaylist) REFERENCES Playlists (id))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion==1 && newVersion==2) from1to2(db);
		if(oldVersion==2 && newVersion==3) from2to3(db);
		if(oldVersion==1 && newVersion==3) {from1to2(db);from2to3(db);}
	}
	
	private void from1to2(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE Playlists ADD position INTEGER DEFAULT 0");
		db.execSQL("ALTER TABLE SongsInPlaylist ADD position INTEGER DEFAULT 0");
	}
	
	private void from2to3(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE SongsInPlaylist ADD hasImage INTEGER DEFAULT 1");
	}
}
