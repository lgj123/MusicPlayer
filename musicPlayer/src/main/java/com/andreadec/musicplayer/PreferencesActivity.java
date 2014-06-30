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

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xmlpull.v1.*;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.database.sqlite.*;
import android.os.*;
import android.preference.*;
import android.preference.Preference.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.database.*;

public class PreferencesActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener {
	private final static String DEFAULT_IMPORTEXPORT_FILENAME = Environment.getExternalStorageDirectory() + "/musicplayer_info.xml";
	
	private SharedPreferences preferences;
	private Preference preferenceClearCache, preferenceIndexBaseFolder, preferenceAbout, preferenceImport, preferenceExport, preferencePodcastsDirectory;
	private Preference preferenceTranslucentStatusBar, preferenceTranslucentNavigationBar, preferenceDisableLockScreen, preferenceEnableGestures, preferenceShowPlaybackControls;
	
	private boolean needsRestart;
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    	if(Build.VERSION.SDK_INT >= 11) {
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
    	
    	preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	// Translucent system bars are only available on Android 4.4+
    	if(Build.VERSION.SDK_INT < 19) {
    		//getPreferenceScreen().removePreference(findPreference("translucentStatusBar"));
    		//getPreferenceScreen().removePreference(findPreference("translucentNavigationBar"));
    		findPreference("translucentStatusBar").setEnabled(false);
    		findPreference("translucentNavigationBar").setEnabled(false);
    	}
    	
    	preferenceClearCache = findPreference("clearCache");
    	preferenceIndexBaseFolder = findPreference("indexBaseFolder");
    	preferenceAbout = findPreference("about");
    	preferenceImport = findPreference("import");
    	preferenceExport = findPreference("export");
    	preferencePodcastsDirectory = findPreference("podcastsDirectory");
    	
    	updateCacheSize();
    	updateBaseFolder();
    	
    	preferenceClearCache.setOnPreferenceClickListener(this);
    	preferenceIndexBaseFolder.setOnPreferenceClickListener(this);
    	preferenceAbout.setOnPreferenceClickListener(this);
    	preferenceImport.setOnPreferenceClickListener(this);
    	preferenceExport.setOnPreferenceClickListener(this);
    	preferencePodcastsDirectory.setOnPreferenceClickListener(this);
    	
    	preferenceTranslucentStatusBar = findPreference("translucentStatusBar");
    	preferenceTranslucentNavigationBar = findPreference("translucentNavigationBar");
    	preferenceDisableLockScreen = findPreference("disableLockScreen");
    	preferenceEnableGestures = findPreference("enableGestures");
    	preferenceShowPlaybackControls = findPreference("showPlaybackControls");
    	preferenceTranslucentStatusBar.setOnPreferenceChangeListener(this);
    	preferenceTranslucentNavigationBar.setOnPreferenceChangeListener(this);
    	preferenceDisableLockScreen.setOnPreferenceChangeListener(this);
    	preferenceEnableGestures.setOnPreferenceChangeListener(this);
    	preferenceShowPlaybackControls.setOnPreferenceChangeListener(this);
	}
	
	@Override
	public void onBackPressed() {
		close();
	}
	
	@SuppressLint("InlinedApi")
	private void close() {
		final Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(needsRestart) {
			if(Build.VERSION.SDK_INT>=11) {
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			} else {
				Utils.showMessageDialog(this, R.string.restartNeeded, R.string.restartNeededMessage);
			}
		}
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			close();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.equals(preferenceClearCache)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.clearCache);
			builder.setMessage(R.string.clearCacheConfirm);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialog, int which) {
			    	  clearCache();
			      }
			});
			builder.setNegativeButton(R.string.no, null);
			builder.show();
			return true;
		} else if(preference.equals(preferenceIndexBaseFolder)) {
			String baseFolder = preferences.getString(Constants.PREFERENCE_BASEFOLDER, "/");
			if(baseFolder.equals("/")) {
				Utils.showMessageDialog(this, R.string.baseFolderNotSetTitle, R.string.baseFolderNotSetMessage);
				return true;
			}
			updateBaseFolder();
			preferenceIndexBaseFolder.setEnabled(false);
			Intent indexIntent = new Intent(this, IndexFolderService.class);
			indexIntent.putExtra("folder", baseFolder);
			startService(indexIntent);
		} else if(preference.equals(preferenceAbout)) {
			startActivity(new Intent(this, AboutActivity.class));
		} else if(preference.equals(preferenceImport)) {
			doImport();
		} else if(preference.equals(preferenceExport)) {
			doExport();
		} else if(preference.equals(preferencePodcastsDirectory)) {
			String podcastsDirectory = preferences.getString(Constants.PREFERENCE_PODCASTSDIRECTORY, null);
	    	if(podcastsDirectory==null || podcastsDirectory.equals("")) {
	    		podcastsDirectory = Podcast.DEFAULT_PODCASTS_PATH;
	    	}
	    	DirectoryChooserDialog chooser = new DirectoryChooserDialog(this, podcastsDirectory, new DirectoryChooserDialog.OnFileChosen() {
				@Override
				public void onFileChosen(String directory) {
					if(directory==null) return;
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(Constants.PREFERENCE_PODCASTSDIRECTORY, directory);
					editor.commit();
				}
			});
	    	chooser.show();
		}
		return false;
	}
	
	private void updateCacheSize() {
		File f = getDatabasePath("Songs");
    	double dbSize = f.length()/1024.0;
    	preferenceClearCache.setSummary(getResources().getString(R.string.clearCacheSummary, dbSize+""));
	}
	
	private void updateBaseFolder() {
		String baseFolder = preferences.getString(Constants.PREFERENCE_BASEFOLDER, null);
		String summary = getResources().getString(R.string.indexBaseFolderSummary) + "\n\n";
		summary += getResources().getString(R.string.currentBaseFolder) + " ";
		if(baseFolder==null) {
			summary += getResources().getString(R.string.notSet);
			summary += "\n\n";
			summary += getResources().getString(R.string.baseFolderInstructions);
		} else {
			summary += baseFolder;
		}
		preferenceIndexBaseFolder.setSummary(summary);
	}
	
	private void clearCache() {
		SQLiteDatabase db = new SongsDatabase().getWritableDatabase();
		db.delete("Songs", "", null);
		db.close();
		Toast.makeText(this, R.string.cacheCleared, Toast.LENGTH_LONG).show();
		updateCacheSize();
	}
	
	private void doImport() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.importMsg);
		builder.setMessage(getResources().getString(R.string.importConfirm, DEFAULT_IMPORTEXPORT_FILENAME));
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				doImport(DEFAULT_IMPORTEXPORT_FILENAME);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void doImport(String filename) {
		if(filename==null) return;
		Log.i("Import file", filename);
		File file = new File(filename.replace("file://", ""));
		
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			doc.getDocumentElement().normalize();
	
			NodeList radios = doc.getElementsByTagName("radio");
			for(int i=0; i<radios.getLength(); i++) {
				Element radio = (Element)radios.item(i);
				String url = radio.getAttribute("url");
				String name = radio.getAttribute("name");
				if(url==null || url.equals("")) continue;
				if(name==null || name.equals("")) name = url;
				Radio.addRadio(new Radio(url, name));
			}
			
			NodeList podcasts = doc.getElementsByTagName("podcast");
			for(int i=0; i<podcasts.getLength(); i++) {
				Element podcast = (Element)podcasts.item(i);
				String url = podcast.getAttribute("url");
				String name = podcast.getAttribute("name");
				byte[] image = Base64.decode(podcast.getAttribute("image"), Base64.DEFAULT);
				if(url==null || url.equals("")) continue;
				if(name==null || name.equals("")) name = url;
				Podcast.addPodcast(this, url, name, image);
			}
			
			Toast.makeText(this, R.string.importSuccess, Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			Toast.makeText(this, R.string.importError, Toast.LENGTH_LONG).show();
			Log.e("WebRadioAcitivity", "doImport", e);
		}
	}
	
	private void doExport() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.export);
		builder.setMessage(getResources().getString(R.string.exportConfirm, DEFAULT_IMPORTEXPORT_FILENAME));
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				doExport(DEFAULT_IMPORTEXPORT_FILENAME);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void doExport(String filename) {
		ArrayList<Radio> radios = Radio.getRadios();
		ArrayList<Podcast> podcasts = Podcast.getPodcasts();
		
		File file = new File(filename);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(fos, "UTF-8");
	        serializer.startDocument(null, Boolean.valueOf(true));
	        serializer.startTag(null, "info");
	        
	        serializer.startTag(null, "radios");
	        for(Radio radio : radios) {
	        	serializer.startTag(null, "radio");
	        	serializer.attribute(null, "url", radio.getUrl());
	        	serializer.attribute(null, "name", radio.getName());
		        serializer.endTag(null, "radio");
	        }
	        serializer.endTag(null, "radios");
	        
	        
	        serializer.startTag(null, "podcasts");
	        for(Podcast podcast : podcasts) {
	        	serializer.startTag(null, "podcast");
	        	serializer.attribute(null, "url", podcast.getUrl());
	        	serializer.attribute(null, "name", podcast.getName());
	        	serializer.attribute(null, "image", Base64.encodeToString(podcast.getImageBytes(), Base64.DEFAULT));
	        	serializer.endTag(null, "podcast");
	        }
	        serializer.endTag(null, "podcasts");
	        
	        serializer.endTag(null, "info");
	        serializer.endDocument();
	        serializer.flush();
			fos.close();
			
			Toast.makeText(this, R.string.exportSuccess, Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			Toast.makeText(this, R.string.exportError, Toast.LENGTH_LONG).show();
			Log.e("WebRadioAcitivity", "doExport", e);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.equals(preferenceTranslucentNavigationBar) || preference.equals(preferenceTranslucentStatusBar) || preference.equals(preferenceDisableLockScreen) || preference.equals(preferenceEnableGestures) || preference.equals(preferenceShowPlaybackControls)) {
			needsRestart = true;
		}
		return true;
	}
}
