/*
 * Copyright 2014 Andrea De Cesare
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

package com.andreadec.musicplayer.adapters;

import java.util.*;

import com.andreadec.musicplayer.*;
import android.view.*;
import android.widget.*;

public class MusicListArrayAdapter extends ArrayAdapter<Object> {
	protected final ArrayList<Object> values;
	protected LayoutInflater inflater;
	protected int listImageSize;

	public MusicListArrayAdapter(MainActivity activity, ArrayList<Object> values) {
		super(activity, R.layout.song_item, values);
		this.values = values;
		inflater = activity.getLayoutInflater();
		listImageSize = (int)activity.getResources().getDimension(R.dimen.listImageSize);
	}
}
