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

package com.andreadec.musicplayer.comparators;

import java.util.*;
import com.andreadec.musicplayer.*;

/* Helper class to compare songs, used to sort them */
public class BrowserSongsComparator implements Comparator<BrowserSong> {
	String sortingMethod;

	public BrowserSongsComparator(String sortingMethod) {
		this.sortingMethod = sortingMethod;
	}
	
	@Override
	public int compare(BrowserSong s1, BrowserSong s2) {
		if(sortingMethod.equals("nat")) {
			if(s1.getTrackNumber()!=null && s2.getTrackNumber()!=null) return s1.getTrackNumber().compareTo(s2.getTrackNumber()); 
			if(s1.getArtist().equals(s2.getArtist())) return s1.getTitle().compareTo(s2.getTitle());
			else return s1.getArtist().compareTo(s2.getArtist());
		} else if(sortingMethod.equals("at")) {
			if(s1.getArtist().equals(s2.getArtist())) return s1.getTitle().compareTo(s2.getTitle());
			else return s1.getArtist().compareTo(s2.getArtist());
		} else if(sortingMethod.equals("ta")) {
			if(s1.getTitle().equals(s2.getTitle())) return s1.getArtist().compareTo(s2.getArtist());
			else return s1.getTitle().compareTo(s2.getTitle());
		} else if(sortingMethod.equals("f")) {
			return s1.getUri().compareTo(s2.getUri());
		}
		return s1.getTitle().compareTo(s2.getTitle());
	}
}
