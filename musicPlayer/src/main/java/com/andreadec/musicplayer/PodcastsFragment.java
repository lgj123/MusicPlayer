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

import java.io.File;
import java.util.*;

import com.andreadec.musicplayer.adapters.*;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.view.ContextMenu.*;
import android.widget.*;
import android.widget.AdapterView.*;

public class PodcastsFragment extends MusicPlayerFragment implements OnItemClickListener {
	private ListView listViewPodcasts;
	private PodcastsArrayAdapter adapter;
	private Podcast currentPodcast; // if null, show podcasts' list
	private SharedPreferences preferences;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) return null;
		View view = inflater.inflate(R.layout.layout_simple_list, container, false);
		listViewPodcasts = (ListView)view.findViewById(R.id.listView);
		listViewPodcasts.setOnItemClickListener(this);
		registerForContextMenu(listViewPodcasts);
		updateListView();
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateListView(true);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		int position = ((AdapterContextMenuInfo)menuInfo).position;
		
		Object item = adapter.getItem(position);
		if(item instanceof Action) return;
		
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.contextmenu_editdelete, menu);

		if(item instanceof Podcast) {
			menu.setHeaderTitle(((Podcast)item).getName());
			menu.removeItem(R.id.menu_edit);
		} else if(item instanceof PodcastEpisode) {
			PodcastEpisode podcastEpisode = (PodcastEpisode)item;
			menu.setHeaderTitle(podcastEpisode.getTitle());
			menu.removeItem(R.id.menu_edit);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		Object listItem = adapter.getItem(position);
		if(listItem instanceof Podcast) {
			Podcast podcast = (Podcast)listItem;
			switch (item.getItemId()) {
			case R.id.menu_delete:
				deletePodcast(podcast);
				return true;
			}
		} else if(listItem instanceof PodcastEpisode) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				PodcastEpisode podcastEpisode = (PodcastEpisode)listItem;
				podcastEpisode.getPodcast().deleteEpisode(podcastEpisode);
				updateListView();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void updateListView() {
		updateListView(false);
	}
	
	public void updateListView(boolean reloadFromDatabase) {
		ArrayList<Object> items = new ArrayList<Object>();
		if(currentPodcast==null) {
			items.add(new Action(Action.ACTION_NEW, getResources().getString(R.string.addPodcast)));
			ArrayList<Podcast> podcasts = Podcast.getPodcasts();
			items.addAll(podcasts);
		} else {
			if(reloadFromDatabase) currentPodcast.loadItemsFromDatabase();
			items.add(new Action(Action.ACTION_GO_BACK, currentPodcast.getName()));
			items.add(new Action(Action.ACTION_UPDATE, getResources().getString(R.string.update)));
			items.addAll(currentPodcast.getEpisodes());
		}
		PodcastEpisode currentPodcastEpisode = null;
		MainActivity activity = (MainActivity)getActivity();
		if(activity.getCurrentPlayingItem() instanceof PodcastEpisode) currentPodcastEpisode = (PodcastEpisode)activity.getCurrentPlayingItem();
		adapter = new PodcastsArrayAdapter((MainActivity)getActivity(), items, currentPodcastEpisode);
		Parcelable state = listViewPodcasts.onSaveInstanceState();
		listViewPodcasts.setAdapter(adapter);
		listViewPodcasts.onRestoreInstanceState(state);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = adapter.getItem(position);
		if(item instanceof Action) {
			Action action = (Action)item;
			if(action.action==Action.ACTION_GO_BACK) {
				currentPodcast = null;
				updateListView();
			} else if(action.action==Action.ACTION_UPDATE) {
				new UpdatePodcastTask(currentPodcast).execute();
			} else if(action.action==Action.ACTION_NEW) {
				addPodcast();
			}
		} else if(item instanceof Podcast) {
			openPodcast((Podcast)item);
		} else if(item instanceof PodcastEpisode) {
			final PodcastEpisode podcastEpisode = (PodcastEpisode)item;
			int status = podcastEpisode.getStatus();
			if(podcastEpisode.getStatus()==PodcastEpisode.STATUS_NEW) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
				dialog.setTitle(podcastEpisode.getTitle());
				dialog.setMessage(R.string.chooseDownloadMethod);
				dialog.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(Utils.isWifiConnected()) {
							downloadEpisode(podcastEpisode);
						} else {
							downloadEpisodeConfirm(podcastEpisode);
						}
					}
				});
				dialog.setNegativeButton(R.string.streaming, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						podcastEpisode.setStreaming();
						((MainActivity)getActivity()).playPodcastEpisodeStreaming(podcastEpisode);
						updateListView();
					}
				});
				dialog.show();
			} else if(status==PodcastEpisode.STATUS_DOWNLOADED) {
				((MainActivity)getActivity()).playItem(podcastEpisode);
				updateListView();
			}
		}
	}
	
	public void openPodcast(Podcast podcast) {
		currentPodcast = podcast;
		updateListView();
	}
	
	private void downloadEpisodeConfirm(final PodcastEpisode episode) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.podcast);
		builder.setMessage(R.string.podcastDownloadNoWifiConfirm);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				downloadEpisode(episode);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void downloadEpisode(PodcastEpisode episode) {
		String podcastsDirectory = preferences.getString(Constants.PREFERENCE_PODCASTSDIRECTORY, Podcast.DEFAULT_PODCASTS_PATH);
		
		if(!new File(podcastsDirectory).exists()) {
			Utils.showMessageDialog(getActivity(), R.string.error, R.string.podcastDirectoryNotExist);
			return;
		}
		
		episode.setStatus(PodcastEpisode.STATUS_DOWNLOADING);
		Intent downloaderIntent = new Intent(getActivity(), PodcastEpisodeDownloaderService.class);
		downloaderIntent.putExtra("idItem", episode.getId());
		downloaderIntent.putExtra("title", episode.getTitle());
		downloaderIntent.putExtra("url", episode.getUrl());
		downloaderIntent.putExtra("type", episode.getType());
		downloaderIntent.putExtra("podcastsDirectory", podcastsDirectory);
		getActivity().startService(downloaderIntent);
		updateListView();
	}
	
	public void addPodcast() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.addPodcast);
		final View view = getActivity().getLayoutInflater().inflate(R.layout.layout_addpodcast1, null);
		builder.setView(view);
		
		final EditText editTextUrl = (EditText)view.findViewById(R.id.editTextPodcastUrl);
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String url = editTextUrl.getText().toString();
				if(url.equals("") || url.equals("http://")) {
					Toast.makeText(getActivity(), R.string.errorInvalidURL, Toast.LENGTH_SHORT).show();
					return;
				}
				new GetPodcastInformationTask(url).execute();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, null);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	private void addPodcast2(final String url, final String name, final byte[] image) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.addPodcast);
		final View view = getActivity().getLayoutInflater().inflate(R.layout.layout_addpodcast2, null);
		builder.setView(view);
		TextView textViewUrl = (TextView)view.findViewById(R.id.textViewUrl);
		textViewUrl.setText(url);
		final EditText editTextName = (EditText)view.findViewById(R.id.editTextPodcastName);
		if(name!=null) editTextName.setText(name);
		
		ImageView imageViewPodcastImage = (ImageView)view.findViewById(R.id.imageViewPodcastImage);
		if(image==null) {
			imageViewPodcastImage.setVisibility(View.GONE);
		} else {
			Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
			imageViewPodcastImage.setImageBitmap(bitmap);
		}
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if(!editTextName.getText().toString().equals("")) {
					Podcast.addPodcast(getActivity(), url, editTextName.getText().toString(), image);
					updateListView();
				} else {
					Utils.showMessageDialog(getActivity(), R.string.error, R.string.podcastNameError);
				}
			}
		});
		
		builder.setNegativeButton(R.string.cancel, null);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	private void deletePodcast(final Podcast podcast) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(podcast.getName());
		builder.setMessage(R.string.removePodcastConfirm);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				podcast.remove();
				updateListView();
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	public void removeAllEpisodes() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.removeAllEpisodes);
		if(currentPodcast==null) {
			builder.setMessage(R.string.removeAllEpisodesConfirm);
		} else {
			builder.setMessage(getActivity().getResources().getString(R.string.removeEpisodesConfirm, currentPodcast.getName()));
		}
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Podcast.deleteEpisodes(currentPodcast, false);
				Toast.makeText(getActivity(), R.string.removeEpisodesCompletion, Toast.LENGTH_SHORT).show();
				updateListView(true);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	public void removeDownloadedEpisodes() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.removeDownloadedEpisodes);
		if(currentPodcast==null) {
			builder.setMessage(R.string.removeAllDownloadedEpisodesConfirm);
		} else {
			builder.setMessage(getActivity().getResources().getString(R.string.removeDownloadedEpisodesConfirm, currentPodcast.getName()));
		}
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Podcast.deleteEpisodes(currentPodcast, true);
				Toast.makeText(getActivity(), R.string.removeDownloadedEpisodesCompletion, Toast.LENGTH_SHORT).show();
				updateListView(true);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private class UpdatePodcastTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private Podcast podcast;
		public UpdatePodcastTask(Podcast podcast) {
			this.podcast = podcast;
		}
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(getActivity());
	        progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(false);
	        progressDialog.setMessage(getActivity().getString(R.string.updatingPodcast));
			progressDialog.show();
	    }
		@Override
		protected Boolean doInBackground(Void... params) {
			return podcast.update();
		}
		@Override
		protected void onPostExecute(final Boolean success) {
			if(!success) {
				Utils.showMessageDialog(getActivity(), R.string.error, R.string.podcastDownloadError);
			}
			updateListView();
			if(progressDialog.isShowing()) progressDialog.dismiss();
		}
	}
	
	private class GetPodcastInformationTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private String url, name;
		private byte[] image;
		public GetPodcastInformationTask(String url) {
			this.url = url;
		}
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(getActivity());
	        progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(false);
	        progressDialog.setMessage(getActivity().getString(R.string.updatingPodcast));
			progressDialog.show();
	    }
		@Override
		protected Boolean doInBackground(Void... params) {
			PodcastParser parser = new PodcastParser();
			boolean ok = parser.parse(url);
			if(!ok) return false;
			name = parser.getTitle();
			image = parser.downloadImage();
			return true;
		}
		@Override
		protected void onPostExecute(final Boolean success) {
			if(progressDialog.isShowing()) progressDialog.dismiss();
			if(success) {
				addPodcast2(url, name, image);
			} else {
				Utils.showMessageDialog(getActivity(), R.string.error, R.string.podcastDownloadError);
			}
		}
	}

	@Override
	public boolean onBackPressed() {
		if(currentPodcast!=null) {
			openPodcast(null);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void gotoPlayingItemPosition(PlayableItem playingItem) {
		PodcastEpisode playingEpisode = (PodcastEpisode)playingItem;
		openPodcast(playingEpisode.getPodcast());
		for(int i=0; i<adapter.getCount(); i++) {
			Object item = adapter.getItem(i);
			if(item instanceof PodcastEpisode) {
				if(item.equals(playingEpisode)) {
					final int position = i;
					listViewPodcasts.post(new Runnable() {
						@Override
						public void run() {
							listViewPodcasts.smoothScrollToPosition(position);
						}
					});
					break;
				}
			}
		}
	}
}
