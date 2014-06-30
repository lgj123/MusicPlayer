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

package com.andreadec.musicplayer.adapters;

import java.util.*;

import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.*;

public class RadioArrayAdapter extends MusicListArrayAdapter {
	private final static int TYPE_ACTION=0, TYPE_RADIO=1;
	private Radio playingRadio;
 
	public RadioArrayAdapter(MainActivity activity, ArrayList<Object> values, Radio playingRadio) {
		super(activity, values);
		this.playingRadio = playingRadio;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof Action) return TYPE_ACTION;
		else return TYPE_RADIO;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		Object item = values.get(position);
		int type = getItemViewType(position);
		ViewHolder viewHolder;
		
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_ACTION) {
				view = inflater.inflate(R.layout.action_item, parent, false);
				viewHolder.text = (TextView)view.findViewById(R.id.textView);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageView);
			} else if(type==TYPE_RADIO) {
				view = inflater.inflate(R.layout.radio_item, parent, false);
				viewHolder.text = (TextView)view.findViewById(R.id.textViewRadioItem);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
				viewHolder.card = view.findViewById(R.id.card);
			}
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		
		if(type==TYPE_ACTION) {
			Action action = (Action)item;
			viewHolder.text.setText(action.msg);
			viewHolder.image.setImageResource(R.drawable.newcontent);
		} else if(type==TYPE_RADIO) {
			Radio radio = (Radio)item;
			viewHolder.text.setText(radio.getTitle());
			if(radio.equals(playingRadio)) {
				viewHolder.card.setBackgroundResource(R.drawable.card_playing);
				viewHolder.image.setImageResource(R.drawable.play_orange);
			} else {
				viewHolder.card.setBackgroundResource(R.drawable.card);
				viewHolder.image.setImageResource(R.drawable.radio);
			}
		}
		
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView text;
		public ImageView image;
		public View card;
	}
}
