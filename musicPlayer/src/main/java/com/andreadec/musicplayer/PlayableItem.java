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
import android.graphics.*;

public interface PlayableItem {
	public String getTitle();
	public String getArtist(); // Return null if no artist available
	public String getPlayableUri();
	public PlayableItem getNext(boolean repeatAll); // Return null if no next item
	public PlayableItem getPrevious(); // Return null if no previous item
	public PlayableItem getRandom(Random random); // Return null if no random item
	public boolean isLengthAvailable();
	public boolean hasImage();
	public Bitmap getImage(); // Return null if no image available
	public ArrayList<Information> getInformation();
}
