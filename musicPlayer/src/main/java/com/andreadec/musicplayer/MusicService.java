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

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.media.MediaPlayer.*;
import android.media.audiofx.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import android.support.v4.util.LruCache;
import android.telephony.*;
import android.view.KeyEvent;
import android.widget.RemoteViews;

@SuppressLint("NewApi")
public class MusicService extends Service implements OnCompletionListener {
	private final static int METADATA_KEY_ARTWORK = 100;
	
	private final IBinder musicBinder = new MusicBinder();	
	private NotificationManager notificationManager;
	private Notification notification;
	private SharedPreferences preferences;
	
	private PendingIntent pendingIntent;
	private PendingIntent quitPendingIntent;
	private PendingIntent previousPendingIntent;
	private PendingIntent playpausePendingIntent;
	private PendingIntent nextPendingIntent;
	
	private PlayableItem currentPlayingItem;
	
	private MediaPlayer mediaPlayer;
	private BassBoost bassBoost;
	private Equalizer equalizer;
	private boolean bassBoostAvailable;
	private boolean equalizerAvailable;
	
	private boolean shuffle, repeat, repeatAll;
	private Random random;
	
	private TelephonyManager telephonyManager;
	private MusicPhoneStateListener phoneStateListener;
	private AudioManager audioManager;
	private ComponentName mediaButtonReceiverComponent;
	private BroadcastReceiver broadcastReceiver;
	
	private ShakeListener shakeListener;
	
	private Bitmap icon;
	private RemoteControlClient remoteControlClient;
	
	private PowerManager.WakeLock wakeLock;
	
	/**
	 * Called when the service is created.
	 */
	@Override
	public void onCreate() {
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicServiceWakelock");
		
		// Initialize the telephony manager
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		phoneStateListener = new MusicPhoneStateListener();
		notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		// Initialize pending intents
		quitPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.andreadec.musicplayer.quit"), 0);
		previousPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.andreadec.musicplayer.previous"), 0);
		playpausePendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.andreadec.musicplayer.playpause"), 0);
		nextPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.andreadec.musicplayer.next"), 0);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
		
		// Read saved user preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
				
		// Initialize the media player
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK); // Enable the wake lock to keep CPU running when the screen is switched off
		
		shuffle = preferences.getBoolean(Constants.PREFERENCE_SHUFFLE, Constants.DEFAULT_SHUFFLE);
		repeat = preferences.getBoolean(Constants.PREFERENCE_REPEAT, Constants.DEFAULT_REPEAT);
		repeatAll = preferences.getBoolean(Constants.PREFERENCE_REPEATALL, Constants.DEFAULT_REPEATALL);
		try { // This may fail if the device doesn't support bass boost
			bassBoost = new BassBoost(1, mediaPlayer.getAudioSessionId());
			bassBoost.setEnabled(preferences.getBoolean(Constants.PREFERENCE_BASSBOOST, Constants.DEFAULT_BASSBOOST));
			setBassBoostStrength(preferences.getInt(Constants.PREFERENCE_BASSBOOSTSTRENGTH, Constants.DEFAULT_BASSBOOSTSTRENGTH));
			bassBoostAvailable = true;
		} catch(Exception e) {
			bassBoostAvailable = false;
		}
		try { // This may fail if the device doesn't support equalizer
			equalizer = new Equalizer(1, mediaPlayer.getAudioSessionId());
			equalizer.setEnabled(preferences.getBoolean(Constants.PREFERENCE_EQUALIZER, Constants.DEFAULT_EQUALIZER));
			setEqualizerPreset(preferences.getInt(Constants.PREFERENCE_EQUALIZERPRESET, Constants.DEFAULT_EQUALIZERPRESET));
			equalizerAvailable = true;
		} catch(Exception e) {
			equalizerAvailable = false;
		}
		random = new Random(System.nanoTime()); // Necessary for song shuffle
		
		shakeListener = new ShakeListener(this);
		if(preferences.getBoolean(Constants.PREFERENCE_SHAKEENABLED, Constants.DEFAULT_SHAKEENABLED)) {
			shakeListener.enable();
		}
		
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); // Start listen for telephony events
		
		// Inizialize the audio manager
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);
		audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		
		// Initialize remote control client
        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
        audioManager.registerRemoteControlClient(remoteControlClient);
		
		updateNotificationMessage();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.andreadec.musicplayer.quit");
		intentFilter.addAction("com.andreadec.musicplayer.previous");
		intentFilter.addAction("com.andreadec.musicplayer.previousNoRestart");
		intentFilter.addAction("com.andreadec.musicplayer.playpause");
		intentFilter.addAction("com.andreadec.musicplayer.next");
		intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	String action = intent.getAction();
            	if(action.equals("com.andreadec.musicplayer.quit")) {
            		sendBroadcast(new Intent("com.andreadec.musicplayer.quitactivity"));
                    sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            		stopSelf();
            		return;
            	} else if(action.equals("com.andreadec.musicplayer.previous")) {
            		previousItem(false);
            	} else if(action.equals("com.andreadec.musicplayer.previousNoRestart")) {
            		previousItem(true);
            	} else if(action.equals("com.andreadec.musicplayer.playpause")) {
            		playPause();
            	} else if(action.equals("com.andreadec.musicplayer.next")) {
            		nextItem();
            	} else if(action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            		if(preferences.getBoolean(Constants.PREFERENCE_STOPPLAYINGWHENHEADSETDISCONNECTED, Constants.DEFAULT_STOPPLAYINGWHENHEADSETDISCONNECTED)) {
	            		pause();
            		}
            	}
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
        
        if(!isPlaying()) {
        	loadLastSong();
        }
        
        startForeground(Constants.NOTIFICATION_MAIN, notification);
	}
	
	/* Called when service is started. */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	// Returns true if the song has been successfully loaded
	private void loadLastSong() {
		if(preferences.getBoolean(Constants.PREFERENCE_OPENLASTSONGONSTART, Constants.DEFAULT_OPENLASTSONGONSTART)) {
	        String lastPlayingSong = preferences.getString(Constants.PREFERENCE_LASTPLAYINGSONG, Constants.DEFAULT_LASTPLAYINGSONG);
        	long lastPlayingSongFromPlaylistId = preferences.getLong(Constants.PREFERENCE_LASTPLAYINGSONGFROMPLAYLISTID, Constants.DEFAULT_LASTPLAYINGSONGFROMPLAYLISTID);
        	if(lastPlayingSong!=null && (new File(lastPlayingSong).exists())) {
        		if(lastPlayingSongFromPlaylistId!=-1) {
        			PlaylistSong savedSong = Playlists.getSavedSongFromPlaylist(lastPlayingSongFromPlaylistId);
        			if(savedSong!=null) {
        				playItem(savedSong, false);
        			}
        		} else {
        			File songDirectory = new File(lastPlayingSong).getParentFile();
        			BrowserSong song = new BrowserSong(lastPlayingSong, new BrowserDirectory(songDirectory));
        			((MusicPlayerApplication)getApplication()).gotoDirectory(songDirectory);
        			playItem(song, false);
        		}
		        if(preferences.getBoolean(Constants.PREFERENCE_SAVESONGPOSITION, Constants.DEFAULT_SAVESONGPOSITION)) {
		        	int lastSongPosition = preferences.getInt(Constants.PREFERENCE_LASTSONGPOSITION, Constants.DEFAULT_LASTSONGPOSITION);
		        	if(lastSongPosition<getDuration()) seekTo(lastSongPosition);
		        }
        	}
        }
	}
	
	/* Called when the activity is destroyed. */
	@Override
	public void onDestroy() {
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE); // Stop listen for telephony events
		notificationManager.cancel(Constants.NOTIFICATION_MAIN);
		unregisterReceiver(broadcastReceiver); // Disable broadcast receiver
		
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Constants.PREFERENCE_SHUFFLE, shuffle);
		editor.putBoolean(Constants.PREFERENCE_REPEAT, repeat);
		editor.putBoolean(Constants.PREFERENCE_REPEATALL, repeatAll);
		if(bassBoostAvailable) {
			editor.putBoolean(Constants.PREFERENCE_BASSBOOST, getBassBoostEnabled());
			editor.putInt(Constants.PREFERENCE_BASSBOOSTSTRENGTH, getBassBoostStrength());
		} else {
			editor.remove(Constants.PREFERENCE_BASSBOOST);
			editor.remove(Constants.PREFERENCE_BASSBOOSTSTRENGTH);
		}
		if(equalizerAvailable) {
			editor.putBoolean(Constants.PREFERENCE_EQUALIZER, getEqualizerEnabled());
			editor.putInt(Constants.PREFERENCE_EQUALIZERPRESET, getEqualizerPreset());
		} else {
			editor.remove(Constants.PREFERENCE_EQUALIZER);
			editor.remove(Constants.PREFERENCE_EQUALIZERPRESET);
		}
		editor.putBoolean(Constants.PREFERENCE_SHAKEENABLED, isShakeEnabled());
		if(currentPlayingItem!=null) {
			if(currentPlayingItem instanceof BrowserSong) {
				editor.putString(Constants.PREFERENCE_LASTPLAYINGSONG, currentPlayingItem.getPlayableUri());
				editor.putInt(Constants.PREFERENCE_LASTSONGPOSITION, getCurrentPosition());
				editor.putLong(Constants.PREFERENCE_LASTPLAYINGSONGFROMPLAYLISTID, -1);
			} else if(currentPlayingItem instanceof PlaylistSong) {
				editor.putString(Constants.PREFERENCE_LASTPLAYINGSONG, currentPlayingItem.getPlayableUri());
				editor.putInt(Constants.PREFERENCE_LASTSONGPOSITION, getCurrentPosition());
				editor.putLong(Constants.PREFERENCE_LASTPLAYINGSONGFROMPLAYLISTID, ((PlaylistSong)currentPlayingItem).getId());
			} else {
				editor.putString(Constants.PREFERENCE_LASTPLAYINGSONG, null);
				editor.putLong(Constants.PREFERENCE_LASTPLAYINGSONGFROMPLAYLISTID, -1);
			}
		} else {
			editor.putString(Constants.PREFERENCE_LASTPLAYINGSONG, null);
			editor.putLong(Constants.PREFERENCE_LASTPLAYINGSONGFROMPLAYLISTID, -1);
		}
		editor.putString(Constants.PREFERENCE_LASTDIRECTORY, ((MusicPlayerApplication)getApplication()).getCurrentDirectory().getDirectory().getAbsolutePath());
		editor.commit();
		
		audioManager.unregisterRemoteControlClient(remoteControlClient);
		audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverComponent);
		audioManager.abandonAudioFocus(null);
		shakeListener.disable();
		mediaPlayer.release();
		stopForeground(true);
		
		wakeLockRelease(); // Just to be sure the wakelock has been released! ;-)
	}
	
	private void wakeLockAcquire() {
		if(!wakeLock.isHeld()) wakeLock.acquire();
	}
	private void wakeLockRelease() {
		if(wakeLock.isHeld()) wakeLock.release();
	}
	
	public boolean playItem(PlayableItem item) {
		return playItem(item, true);
	}
	
	public boolean playItem(PlayableItem item, boolean startPlaying) {
		wakeLockAcquire();
		currentPlayingItem = item;
		mediaPlayer.reset();
		mediaPlayer.setOnCompletionListener(null);
		try {
			mediaPlayer.setDataSource(item.getPlayableUri());
			try {
				mediaPlayer.prepare();
			} catch (IOException e) {
				currentPlayingItem = null;
				return false;
			}
			mediaPlayer.setOnCompletionListener(this);
			if(startPlaying) mediaPlayer.start();
			
			updateNotificationMessage();
			if(startPlaying) {
				sendBroadcast(new Intent("com.andreadec.musicplayer.newsong")); // Sends a broadcast to the activity
			}
			
			return true;
		} catch (Exception e) {
			currentPlayingItem = null;
			updateNotificationMessage();
			sendBroadcast(new Intent("com.andreadec.musicplayer.newsong")); // Sends a broadcast to the activity
			return false;
		}
	}
	

	/* BASS BOOST */
	public boolean getBassBoostAvailable() {
		return bassBoostAvailable;
	}
	public boolean toggleBassBoost() {
		boolean newState = !bassBoost.getEnabled();
		bassBoost.setEnabled(newState);
		return newState;
	}
	public boolean getBassBoostEnabled() {
		if(!bassBoostAvailable || bassBoost==null) return false;
		return bassBoost.getEnabled();
	}
	public void setBassBoostStrength(int strength) {
		bassBoost.setStrength((short)strength);
	}
	public int getBassBoostStrength() {
		return bassBoost.getRoundedStrength();
	}
	
	
	/* EQUALIZER */
	public boolean getEqualizerAvailable() {
		return equalizerAvailable;
	}
	public boolean toggleEqualizer() {
		boolean newState = !equalizer.getEnabled();
		equalizer.setEnabled(newState);
		return newState;
	}
	public boolean getEqualizerEnabled() {
		if(!equalizerAvailable || equalizer==null) return false;
		return equalizer.getEnabled();
	}
	public void setEqualizerPreset(int preset) {
		equalizer.usePreset((short)preset);
	}
	public short getEqualizerPreset() {
		return equalizer.getCurrentPreset();
	}
	public String[] getEqualizerAvailablePresets() {
		int n = equalizer.getNumberOfPresets();
		String[] presets = new String[n];
		for(short i=0; i<n; i++) {
			presets[i] = equalizer.getPresetName(i);
		}
		return presets;
	}
	
	
	/* SHAKE SENSOR */
	public boolean isShakeEnabled() {
		return shakeListener.isEnabled();
	}
	
	public void toggleShake() {
		if(shakeListener.isEnabled()) shakeListener.disable();
		else shakeListener.enable();
	}	
	
	
	/* Updates the notification and the remote control client. */
	private void updateNotificationMessage() {
        Bitmap image = null;
        if(currentPlayingItem!=null && currentPlayingItem.hasImage()) {
            image = ((MusicPlayerApplication)getApplication()).imagesCache.getImageSync(currentPlayingItem);
        }

		/* Update remote control client */
        if(currentPlayingItem==null) {
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        } else {
            if(isPlaying()) {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            } else {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
            RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, currentPlayingItem.getTitle());
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, currentPlayingItem.getArtist());
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, currentPlayingItem.getArtist());
            metadataEditor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, getDuration());
            if(currentPlayingItem.hasImage()) {
                metadataEditor.putBitmap(METADATA_KEY_ARTWORK, image);
            } else {
                metadataEditor.putBitmap(METADATA_KEY_ARTWORK, icon.copy(icon.getConfig(), false));
            }
            metadataEditor.apply();
        }
		
		/* Update notification */
		Notification.Builder notificationBuilder = new Notification.Builder(this);
		notificationBuilder.setSmallIcon(R.drawable.audio_white);
		//notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        notificationBuilder.setOngoing(true);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setPriority(Notification.PRIORITY_MAX);
		
		int playPauseIcon = isPlaying() ? R.drawable.pause : R.drawable.play;
		
		RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.layout_notification);
		
		if(currentPlayingItem==null) {
			notificationLayout.setTextViewText(R.id.textViewArtist, getString(R.string.app_name));
			notificationLayout.setTextViewText(R.id.textViewTitle, getString(R.string.noSong));
			notificationLayout.setImageViewBitmap(R.id.imageViewNotification, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		} else {
			notificationLayout.setTextViewText(R.id.textViewArtist, currentPlayingItem.getArtist());
			notificationLayout.setTextViewText(R.id.textViewTitle, currentPlayingItem.getTitle());
			if(image!=null) {
				notificationLayout.setImageViewBitmap(R.id.imageViewNotification, image);
			} else {
				notificationLayout.setImageViewBitmap(R.id.imageViewNotification, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
			}
		}
		notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationQuit, quitPendingIntent);
		notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationPrevious, previousPendingIntent);
		notificationLayout.setImageViewResource(R.id.buttonNotificationPlayPause, playPauseIcon);
		notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationPlayPause, playpausePendingIntent);
		notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationNext, nextPendingIntent);

        //notificationBuilder.setContent(notificationLayout);
        notification = notificationBuilder.build();
        notification.bigContentView = notificationLayout;
        //notification.contentView = notificationLayout;
		
		notificationManager.notify(Constants.NOTIFICATION_MAIN, notification);
	}
	
	/* Toggles play/pause status. */
	public void playPause() {
		if(currentPlayingItem==null) return;
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			wakeLockRelease();
		} else {
			wakeLockAcquire();
			mediaPlayer.start();
		}
		updateNotificationMessage();
		sendBroadcast(new Intent("com.andreadec.musicplayer.playpausechanged"));
	}
	
	/* Starts playing song. */
	public void play() {
		if(currentPlayingItem==null) return;
		if(!mediaPlayer.isPlaying()) mediaPlayer.start();
		updateNotificationMessage();
		sendBroadcast(new Intent("com.andreadec.musicplayer.playpausechanged"));
	}
	
	/* Pauses playing song. */
	public void pause() {
		if(currentPlayingItem==null) return;
		if (mediaPlayer.isPlaying()) mediaPlayer.pause();
		updateNotificationMessage();
		sendBroadcast(new Intent("com.andreadec.musicplayer.playpausechanged"));
	}
	
	/* Seeks to a position. */
	public void seekTo(int progress) {
		mediaPlayer.seekTo(progress);
	}
	
	/* Plays the previous song */
	public void previousItem(boolean noRestart) {
		if(currentPlayingItem==null) return;
		
		if(!noRestart && getCurrentPosition()>2000) {
			playItem(currentPlayingItem);
			return;
		}
		
		if(repeat) {
			playItem(currentPlayingItem);
			return;
		}
		
		if(shuffle) {
			randomItem();
			return;
		}
		
		PlayableItem previousItem = currentPlayingItem.getPrevious();
		if(previousItem!=null) {
			playItem(previousItem);
		}
	}
	
	/* Plays the next song */
	public void nextItem() {
		if(currentPlayingItem==null) {
			return;
		}
		
		if(repeat) {
			playItem(currentPlayingItem);
			return;
		}
		
		if(shuffle) {
			randomItem();
			return;
		}
		
		PlayableItem nextItem = currentPlayingItem.getNext(repeatAll);
		if(nextItem==null) {
			if(!isPlaying()) wakeLockRelease();
		} else {
			playItem(nextItem);
		}
	}
	
	private void randomItem() {
		PlayableItem randomItem = currentPlayingItem.getRandom(random);
		if(randomItem!=null) {
			playItem(randomItem);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return musicBinder;
	}
	
	public class MusicBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}
	
	/* Gets current playing item. */
	public PlayableItem getCurrentPlayingItem() {
		return currentPlayingItem;
	}
	
	/* Gets current song durations. */
	public int getDuration() {
		if(currentPlayingItem==null) return 100;
		return mediaPlayer.getDuration();
	}
	/* Gets current position in the song. */
	public int getCurrentPosition() {
		if(currentPlayingItem==null) return 0;
		return mediaPlayer.getCurrentPosition();
	}
	/* Checks if a song is currently being played */
	public boolean isPlaying() {
		if(currentPlayingItem==null) return false;
		return mediaPlayer.isPlaying();
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		nextItem();
	}
	
	public boolean getRepeat() {
		return repeat;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public boolean getShuffle() {
		return shuffle;
	}

	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}

	public boolean getRepeatAll() {
		return repeatAll;
	}

	public void setRepeatAll(boolean repeatAll) {
		this.repeatAll = repeatAll;
	}

	/* Phone state listener class. */
	private class MusicPhoneStateListener extends PhoneStateListener {
		private boolean wasPlaying = false;
		public void onCallStateChanged(int state, String incomingNumber) {
	    	switch(state) {
	            case TelephonyManager.CALL_STATE_IDLE:
	            	if(preferences.getBoolean(Constants.PREFERENCE_RESTARTPLAYBACKAFTERPHONECALL, Constants.DEFAULT_RESTARTPLAYBACKAFTERPHONECALL) && wasPlaying) play();
	                break;
	            case TelephonyManager.CALL_STATE_OFFHOOK:
	            	wasPlaying = isPlaying();
	            	pause();
	                break;
	            case TelephonyManager.CALL_STATE_RINGING:
	            	wasPlaying = isPlaying();
	            	pause();
	            	break;
	        }
	    }
	}
	
	public static class MediaButtonReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if(event.getAction()!=KeyEvent.ACTION_DOWN) return;
			switch(event.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_PLAY:
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				context.sendBroadcast(new Intent("com.andreadec.musicplayer.playpause"));
				return;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				context.sendBroadcast(new Intent("com.andreadec.musicplayer.previousNoRestart"));
				return;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				context.sendBroadcast(new Intent("com.andreadec.musicplayer.next"));
				return;
			}
		}
	}
}
