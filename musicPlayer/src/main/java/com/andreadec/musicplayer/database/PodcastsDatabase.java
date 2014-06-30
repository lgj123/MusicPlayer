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

package com.andreadec.musicplayer.database;

import com.andreadec.musicplayer.MusicPlayerApplication;
import android.database.sqlite.*;

public class PodcastsDatabase extends SQLiteOpenHelper {
	private static final String DB_NAME = "Podcasts";
	private static final int DB_VERSION = 1;
	
	public PodcastsDatabase() {
		super(MusicPlayerApplication.getContext(), DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE Podcasts (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, image BLOB)");
		db.execSQL("CREATE TABLE ItemsInPodcast (idItem TEXT PRIMARY KEY, idPodcast INTEGER, title TEXT, status INTEGER, url TEXT, filename TEXT, pubDate INTEGER, duration TEXT, type TEXT)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
