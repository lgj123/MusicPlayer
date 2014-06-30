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

import java.util.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.ContextMenu.*;
import android.widget.AdapterView.*;
import android.widget.*;

import com.andreadec.musicplayer.adapters.*;

public class RadioFragment extends MusicPlayerFragment implements OnItemClickListener {
	private ListView listViewRadios;
	private RadioArrayAdapter adapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) return null;
		View view = inflater.inflate(R.layout.layout_simple_list, container, false);
		listViewRadios = (ListView)view.findViewById(R.id.listView);
		listViewRadios.setOnItemClickListener(this);
		registerForContextMenu(listViewRadios);
		updateListView();
		return view;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		int position = ((AdapterContextMenuInfo)menuInfo).position;
		if(position==0) return;
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.contextmenu_editdelete, menu);
		menu.setHeaderTitle(((Radio)(adapter.getItem(position))).getTitle());
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		switch (item.getItemId()) {
		case R.id.menu_edit:
			editRadio((Radio)adapter.getItem(position));
			return true;
		case R.id.menu_delete:
			deleteRadio((Radio)adapter.getItem(position));
			return true;
		}
		return false;
	}
	
	@Override
	public void updateListView() {
		ArrayList<Object> items = new ArrayList<Object>();
		items.add(new Action(Action.ACTION_NEW, getResources().getString(R.string.addRadio)));
		items.addAll(Radio.getRadios());
		MainActivity activity = (MainActivity)getActivity();
		Radio playingRadio = null;
		if(activity.getCurrentPlayingItem() instanceof Radio) playingRadio = (Radio)activity.getCurrentPlayingItem();
	    adapter = new RadioArrayAdapter((MainActivity)getActivity(), items, playingRadio);
	    listViewRadios.setAdapter(adapter);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = adapter.getItem(position);
		if(item instanceof Action) {
			editRadio(null);
		} else if(item instanceof Radio) {
			((MainActivity)getActivity()).playRadio((Radio)item);
		}
	}
	
	private void deleteRadio(final Radio radio) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete);
		builder.setMessage(getResources().getString(R.string.deleteRadioConfirm));
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int which) {
		    	  Radio.deleteRadio(radio);
		    	  updateListView();
		      }
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void editRadio(final Radio oldRadio) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		int title = oldRadio==null ? R.string.addRadio : R.string.edit;
		builder.setTitle(getResources().getString(title));
		final View view = getActivity().getLayoutInflater().inflate(R.layout.layout_editwebradio, null);
		builder.setView(view);
		
		final EditText editTextUrl = (EditText)view.findViewById(R.id.editTextUrl);
		final EditText editTextName = (EditText)view.findViewById(R.id.editTextName);
		if(oldRadio!=null) {
			editTextUrl.setText(oldRadio.getUrl());
			editTextName.setText(oldRadio.getName());
		}
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String url = editTextUrl.getText().toString();
				String name = editTextName.getText().toString();
				if(url.equals("") || url.equals("http://")) {
					Toast.makeText(getActivity(), R.string.errorInvalidURL, Toast.LENGTH_SHORT).show();
					return;
				}
	        	if(name.equals("")) name = url;
	        	
	        	if(oldRadio!=null) {
	        		Radio.deleteRadio(oldRadio);
	        	}
				
	        	Radio newRadio = new Radio(url, name);
	        	Radio.addRadio(newRadio);
				updateListView();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, null);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	public void newRadio() {
		editRadio(null);
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public void gotoPlayingItemPosition(PlayableItem playingItem) {
		Radio playingRadio = (Radio)playingItem;
		for(int i=0; i<adapter.getCount(); i++) {
			Object item = adapter.getItem(i);
			if(item instanceof Radio) {
				if(item.equals(playingRadio)) {
					final int position = i;
					listViewRadios.post(new Runnable() {
						@Override
						public void run() {
							listViewRadios.smoothScrollToPosition(position);
						}
					});
					break;
				}
			}
		}
	}
}
