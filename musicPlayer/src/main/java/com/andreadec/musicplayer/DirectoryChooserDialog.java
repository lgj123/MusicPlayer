/*
 * Copyright 2012-2014 Andrea De Cesare
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
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;

public class DirectoryChooserDialog implements OnItemClickListener {
	private Activity activity;
	private AlertDialog.Builder dialog;
	private View dialogView;
	private TextView textViewDirectory;
	private ListView listView;
	private File currentDirectory;
	
	public DirectoryChooserDialog(Activity activity, String startDirectory, final OnFileChosen callback) {
		this.activity = activity;
		dialog = new AlertDialog.Builder(activity);
		dialog.setTitle(R.string.chooseDirectory);
		
		dialogView = activity.getLayoutInflater().inflate(R.layout.layout_directorychooser, null, false);
		
		textViewDirectory = (TextView)dialogView.findViewById(R.id.textViewDirectory);
		listView = (ListView)dialogView.findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		dialog.setView(dialogView);
		
		dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callback.onFileChosen(null);
			}
		});
		dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callback.onFileChosen(currentDirectory.getAbsolutePath());
			}
		});
		
		currentDirectory = new File(startDirectory);
		updateList();
	}
	
	private void gotoParentDirectory() {
		File newDirectory = currentDirectory.getParentFile();
		if(newDirectory!=null && newDirectory.listFiles()!=null) {
			currentDirectory = newDirectory;
			updateList();
		}
	}
	
	private void gotoDirectory(String name) {
		File newDirectory = new File(currentDirectory.getAbsolutePath()+"/"+name);
		if(newDirectory!=null && newDirectory.listFiles()!=null) {
			currentDirectory = newDirectory;
			updateList();
		}
	}
	
	private void updateList() {
		textViewDirectory.setText(currentDirectory.getAbsolutePath());
		File[] files = currentDirectory.listFiles();
		ArrayList<String> list = new ArrayList<String>();
		list.add("..");
		for(File f : files) {
			if(f.isDirectory()) list.add(f.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, list);
		listView.setAdapter(adapter);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(position==0) gotoParentDirectory();
		else gotoDirectory((String)listView.getAdapter().getItem(position));
	}
	
	public void show() {
		dialog.show();
	}
	
	public interface OnFileChosen {
		// filename is null if the user canceled the operation
		public void onFileChosen(String directory);
	}
}
