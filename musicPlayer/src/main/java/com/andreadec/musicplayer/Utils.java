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
import java.text.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.net.*;

public class Utils {
	/* Builds a simple message dialog */
	public static void showMessageDialog(Context context, int title, int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.ok, null);
		builder.show();
	}
	
	/* Gets file size from its uri */
	public static String getFileSize(String uri) {
		try {
			File file = new File(uri);
			return formatFileSize(file.length());
		} catch(Exception e) {
			return "";
		}
	}
	
	/* Converts size in byte to a more readable format */
	public static String formatFileSize(long size) {
		DecimalFormat df = new DecimalFormat("#.##");
		double kb = (double)(size)/1024.0;
		if(kb<1024) return df.format(kb) + " KiB";
		else return df.format(kb/1024.0) + " MiB";
	}
	
	/* Checks if the device is connected to a WiFi network */
	public static boolean isWifiConnected() {
		ConnectivityManager cm = (ConnectivityManager)(MusicPlayerApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE));
		NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isConnected();
	}
	
	public static String formatTime(int milliseconds) {
		String ret = "";
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000*60)) % 60);
		int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
		if(hours>0) ret += hours+":";
		ret += minutes<10 ? "0"+minutes+":" : minutes+":";
		ret += seconds<10 ? "0"+seconds : seconds+"";
		return ret;
	}
	
	public static Bitmap getMusicFileImage(String uri) {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(uri);
		byte[] imageBytes = mmr.getEmbeddedPicture();
		Bitmap image = null;
		if(imageBytes!=null) {
			image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
		}
		mmr.release();
		return image;
	}
}
