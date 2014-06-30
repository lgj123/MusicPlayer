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

import java.io.*;
import java.util.*;
import android.annotation.SuppressLint;
import android.graphics.*;
import android.media.*;
import android.os.Build;

public class BrowserSong implements PlayableItem, Serializable {
	private static final long serialVersionUID = 1L;
	private String title, artist;
	private Integer trackNumber;
	private String uri;
	private BrowserDirectory browserDirectory;
	private boolean hasImage;
	
	public BrowserSong(String uri, String artist, String title, Integer trackNumber, boolean hasImage, BrowserDirectory browserDirectory) {
		this.uri = uri;
		this.artist = artist;
		this.title = title;
		this.trackNumber = trackNumber;
		this.hasImage = hasImage;
		this.browserDirectory = browserDirectory;
	}

	public BrowserSong(String uri, BrowserDirectory browserDirectory) {
		this.uri = uri;
		this.browserDirectory = browserDirectory;
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		try {
			mmr.setDataSource(uri);
			title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
			artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			if (title == null || title.equals("")) title = new File(uri).getName();
			if (artist == null) artist = "";
			try {
				trackNumber = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
			} catch(Exception ex) {}
			hasImage = mmr.getEmbeddedPicture()!=null;
		} catch(Exception e) {
			title = new File(uri).getName();
			artist = "";
		} finally {
			mmr.release();
		}
	}
	
	public BrowserSong(String uri) {
		this(uri, new BrowserDirectory(new File(uri).getParentFile()));
	}

	public void setBrowser(BrowserDirectory browserDirectory) {
		this.browserDirectory = browserDirectory;
	}
	
	@Override
	public String getArtist() {
		return artist;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	public Integer getTrackNumber() {
		return trackNumber;
	}

	public String getUri() {
		return uri;
	}
	
	@Override
	public boolean hasImage() {
		return hasImage;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	@Override
	public Bitmap getImage() {
		return Utils.getMusicFileImage(uri);
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof BrowserSong)) return false;
		BrowserSong s2 = (BrowserSong)o;
		return uri.equals(s2.uri);
	}

	@Override
	public String getPlayableUri() {
		return uri;
	}

	@Override
	public PlayableItem getNext(boolean repeatAll) {
		ArrayList<BrowserSong> songs = browserDirectory.getSongs();
		int index = songs.indexOf(this);
		if(index<songs.size()-1) {
			return songs.get(index+1);
		} else {
			if(repeatAll) return songs.get(0);
		}
		return null;
	}

	@Override
	public PlayableItem getPrevious() {
		ArrayList<BrowserSong> songs = browserDirectory.getSongs();
		int index = songs.indexOf(this);
		if(index>0) {
			return songs.get(index-1);
		} else {
			return null;
		}
	}

	@Override
	public PlayableItem getRandom(Random random) {
		ArrayList<BrowserSong> songs = browserDirectory.getSongs();
		return songs.get(random.nextInt(songs.size()));
	}

	@Override
	public boolean isLengthAvailable() {
		return true;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public ArrayList<Information> getInformation() {
		String bitrate=null, album=null, year=null;
		
		// Get additional information from file
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		try {
			mmr.setDataSource(uri);
			if(Build.VERSION.SDK_INT>=14) bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
			album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
			year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
		} catch(Exception e) {
		} finally {
			mmr.release();
		}
		
		ArrayList<Information> info = new ArrayList<Information>();
		info.add(new Information(R.string.artist, artist));
		info.add(new Information(R.string.title, title));
		if(year!=null) info.add(new Information(R.string.year, year));
		if(album!=null) info.add(new Information(R.string.album, album));
		if(trackNumber!=null) {
			info.add(new Information(R.string.trackNumber, trackNumber+""));
		} else {
			info.add(new Information(R.string.trackNumber, "-"));
		}
		info.add(new Information(R.string.fileName, uri));
		info.add(new Information(R.string.fileSize, Utils.getFileSize(uri)));
		if(bitrate!=null) {
			try {
				int kbps = Integer.parseInt(bitrate)/1000;
				info.add(new Information(R.string.bitrate, kbps+" kbps"));
			} catch(Exception e) {}
		}
		
		return info;
	}
}
