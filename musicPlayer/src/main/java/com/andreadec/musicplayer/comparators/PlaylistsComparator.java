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

/* Helper class to compare two playlists, used to sort them */
public class PlaylistsComparator implements Comparator<Playlist> {
	String sortingMethod;

	public PlaylistsComparator(String sortingMethod) {
		this.sortingMethod = sortingMethod;
	}
	
	@Override
	public int compare(Playlist p1, Playlist p2) {
		if(sortingMethod.equals("name")) {
			return p1.getName().compareTo(p2.getName());
		}
		long id1 = p1.getId();
		long id2 = p2.getId();
		if(id1<id2) return -1;
		else return 1;
	}
}
