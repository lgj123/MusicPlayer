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

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager.*;
import android.content.res.*;
import android.os.*;
import android.text.Html;
import android.text.method.*;
import android.view.*;
import android.widget.*;

@SuppressLint("NewApi")
public class AboutActivity extends Activity {
	private TextView textViewAbout;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.layout_about);
        if(Build.VERSION.SDK_INT >= 11) {
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
        textViewAbout = (TextView)findViewById(R.id.textViewAbout);
        textViewAbout.setMovementMethod(LinkMovementMethod.getInstance());
        Resources resources = getResources();
        
        String version = "";
        try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {}
        
        String about = "<h1>"+resources.getString(R.string.app_name)+"</h1>";
        about += "<p>"+resources.getString(R.string.version, version)+"</p>";
        about += "<p>&copy; 2012-2014 Andrea De Cesare</p>";
        about += "<p><a href=\"https://github.com/andreadec/MusicPlayer\">https://github.com/andreadec/MusicPlayer</a></p>";
        
        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.license)+"</h2>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.libraries)+"</h2>";
        about += "<p>"+resources.getString(R.string.librariesUsed)+"</p>";
        
        about += "<h5>Android Support Library</h5>";
        about += "<p>Copyright (c) 2005-2008, The Android Open Source Project<br>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h5>DragSortListView</h5>";
        about += "<p>Copyright 2012 Carl Bauer</p>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h5>System Bar Tint</h5>";
        about += "<p>Copyright 2013 readyState Software Limited</p>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h5>"+resources.getString(R.string.icons)+"</h5>";
        about += "<p>Some icons are from the <a href=\"http://tango.freedesktop.org/\">Tango Desktop Project</a>, released into the Public Domain.</p>";
        textViewAbout.setText(Html.fromHtml(about));
        
        
        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.specialThanks)+"</h2>";
        about += "<p>Spierpa</p>";
        about += "<p>Matteo</p>";
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, PreferencesActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
