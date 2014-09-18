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

public class RadioArrayAdapter extends ArrayAdapter<Radio> {
	private Radio playingRadio;
    private ArrayList<Radio> values;
    private LayoutInflater inflater;
    private RadioFragment fragment;
 
	public RadioArrayAdapter(RadioFragment fragment, ArrayList<Radio> values, Radio playingRadio) {
        super(fragment.getActivity(), R.layout.radio_item, values);
        this.fragment = fragment;
        this.values = values;
		this.playingRadio = playingRadio;
        inflater = fragment.getActivity().getLayoutInflater();
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
        final Radio radio = (Radio)values.get(position);
		ViewHolder viewHolder;
		
		if(view==null) {
			viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.radio_item, parent, false);
            viewHolder.text = (TextView)view.findViewById(R.id.textViewRadioItem);
            viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
            viewHolder.card = view.findViewById(R.id.card);
            viewHolder.menu = (ImageButton)view.findViewById(R.id.buttonMenu);
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}

        viewHolder.text.setText(radio.getTitle());
        if(radio.equals(playingRadio)) {
            viewHolder.card.setBackgroundResource(R.drawable.card_playing);
            viewHolder.image.setImageResource(R.drawable.play_orange);
        } else {
            viewHolder.card.setBackgroundResource(R.drawable.card);
            viewHolder.image.setImageResource(R.drawable.radio);
        }
        final PopupMenu popup = new PopupMenu(fragment.getActivity(), viewHolder.menu);
        popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.menu_edit:
                        fragment.editRadio(radio);
                        return true;
                    case R.id.menu_delete:
                        fragment.deleteRadio(radio);
                        return true;
                }
                return true;
            }
        });
        viewHolder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.show();
            }
        });
        viewHolder.menu.setFocusable(false);
		
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView text;
		public ImageView image;
		public View card;
        public ImageButton menu;
	}
}
