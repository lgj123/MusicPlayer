/*
 * Copyright 2013-2014 Andrea De Cesare
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
import android.graphics.*;
import android.os.*;

import com.andreadec.musicplayer.database.*;

public class Podcast {
	public final static String DEFAULT_PODCASTS_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).getAbsolutePath();
	private long id;
	private String name;
	private String url;
	private ArrayList<PodcastEpisode> episodes;
	private byte[] image;
	
	public Podcast(long id, String url, String name, byte[] image) {
		this.id = id;
		this.url = url;
		this.name = name;
		this.image = image;
		loadItemsFromDatabase();
	}
	
	public void loadItemsFromDatabase() {
		episodes = new ArrayList<PodcastEpisode>();
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT idItem, url, title, status, filename, pubDate, duration, type FROM ItemsInPodcast WHERE idPodcast="+id+" ORDER BY pubDate DESC", null);
		while(cursor.moveToNext()) {
			String itemId = cursor.getString(0);
			String itemUrl = cursor.getString(1);
			String title = cursor.getString(2);
			int status = cursor.getInt(3);
			String filename = cursor.getString(4);
			long pubDate = cursor.getLong(5);
			String duration = cursor.getString(6);
			String type = cursor.getString(7);
			PodcastEpisode item = new PodcastEpisode(itemUrl, filename, title, itemId, this, status, pubDate, duration, type);
			episodes.add(item);
		}
		cursor.close();
		db.close();
	}
	
	public void addItem(SQLiteDatabase db, PodcastEpisode episode) {
		ContentValues values = new ContentValues();
		values.put("idPodcast", id);
		values.put("idItem", episode.getId());
		values.put("url", episode.getUrl());
		values.put("title", episode.getTitle());
		values.put("status", episode.getStatus());
		values.put("pubDate", episode.getPubDate());
		values.put("duration", episode.getDuration());
		values.put("type", episode.getType());
		try {
			db.insertOrThrow("ItemsInPodcast", null, values);
		} catch(Exception e) {}
	}
	
	public void deleteEpisode(PodcastEpisode episode) {
		episode.deleteDownloadedFile();
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		db.delete("ItemsInPodcast", "idItem='"+episode.getId()+"'", null);
		db.close();
		episodes.remove(episode);
	}
	
	public void remove() {
		for(PodcastEpisode episode : episodes) {
			episode.deleteDownloadedFile();
		}
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		db.delete("Podcasts", "id=" + id, null);
		db.delete("ItemsInPodcast", "idPodcast=" + id, null);
		db.close();
	}
	
	public void editName(String newName) {
		name = newName;
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", newName);
		db.update("Podcasts", values, "id="+id, null);
		db.close();
	}
	
	public static ArrayList<Podcast> getPodcasts() {
		ArrayList<Podcast> podcasts = new ArrayList<Podcast>();
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, url, name, image FROM Podcasts ORDER BY name", null);
        while (cursor.moveToNext()) {
        	Podcast podcast = new Podcast(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getBlob(3));
        	podcasts.add(podcast);
        }
        db.close();
        return podcasts;
	}
	
	public static void addPodcast(Context context, String url, String name, byte[] image) {
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("name", name);
		if(image!=null) {
			values.put("image", image);
		}
		try {
			db.insertOrThrow("Podcasts", null, values);
		} catch(Exception e) {
		} finally {
			db.close();
		}
	}
	
	public static void deleteEpisodes(Podcast podcast, boolean downloadedOnly) {
		ArrayList<Podcast> podcasts;
		if(podcast==null) { // Remove downloaded items from all podcasts
			podcasts = Podcast.getPodcasts();
		} else {
			podcasts = new ArrayList<Podcast>();
			podcasts.add(podcast);
		}
		if(downloadedOnly) deleteDownloadedEpisodes(podcasts);
		else deleteEpisodes(podcasts);
	}
	private static void deleteEpisodes(ArrayList<Podcast> podcasts) {
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		
		for(Podcast podcast : podcasts) {
			for(PodcastEpisode episode : podcast.getEpisodes()) {
				episode.deleteDownloadedFile();
			}
			db.delete("ItemsInPodcast", "idPodcast=" + podcast.id, null);
		}
		
		db.close();
	}
	private static void deleteDownloadedEpisodes(ArrayList<Podcast> podcasts) {
		for(Podcast podcast : podcasts) {
			for(PodcastEpisode episode : podcast.getEpisodes()) {
				if(episode.getStatus()==PodcastEpisode.STATUS_DOWNLOADED) {
					episode.deleteDownloadedFile();
					episode.setStatus(PodcastEpisode.STATUS_NEW);
				}
			}
		}
	}
	
	public boolean update() {
		PodcastParser parser = new PodcastParser();
		boolean ok = parser.parse(url);
		if(!ok) return false;
		ArrayList<PodcastEpisode> items = parser.getEpisodes();
		
		PodcastsDatabase podcastsDatabase = new PodcastsDatabase();
		SQLiteDatabase db = podcastsDatabase.getWritableDatabase();
		
		for(PodcastEpisode item : items) {
			item.setPodcast(this);
			addItem(db, item);
		}
		
		db.close();
		
		loadItemsFromDatabase();
		return true;
	}
	
	public String getUrl() {
		return url;
	}
	
	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<PodcastEpisode> getEpisodes() {
		return episodes;
	}
	
	public byte[] getImageBytes() {
		return image;
	}
	public Bitmap getImage() {
		if(image==null) return null;
		return BitmapFactory.decodeByteArray(image, 0, image.length);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Podcast)) return false;
		Podcast p2 = (Podcast)o;
		return id==p2.id;
	}
}
