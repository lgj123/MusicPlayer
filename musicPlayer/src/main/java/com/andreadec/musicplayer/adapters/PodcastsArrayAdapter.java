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

package com.andreadec.musicplayer.adapters;

import java.util.*;

import com.andreadec.musicplayer.*;

import android.graphics.*;
import android.view.*;
import android.widget.*;

public class PodcastsArrayAdapter extends MusicListArrayAdapter {
	private PodcastEpisode currentEpisode;
	private final static int TYPE_ACTION=0, TYPE_PODCAST=1, TYPE_PODCAST_ITEM=2;
	
	public PodcastsArrayAdapter(MainActivity activity, ArrayList<Object> values, PodcastEpisode currentEpisode) {
		super(activity, values);
		this.currentEpisode = currentEpisode;
	}
	
	@Override
	public int getViewTypeCount() {
		return 3;
	}
	@Override
	public int getItemViewType(int position) {
		Object value = values.get(position);
		if(value instanceof Action) return TYPE_ACTION;
		else if(value instanceof Podcast) return TYPE_PODCAST;
		else return TYPE_PODCAST_ITEM;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		Object value = values.get(position);
		int type = getItemViewType(position);
		ViewHolder viewHolder;
		
		if(view==null) {
			viewHolder = new ViewHolder();
			if(type==TYPE_ACTION) {
				view = inflater.inflate(R.layout.action_item, parent, false);
				viewHolder.textTitle = (TextView)view.findViewById(R.id.textView);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageView);
			} else if(type==TYPE_PODCAST) {
				view = inflater.inflate(R.layout.folder_item, parent, false);
				viewHolder.textTitle = (TextView)view.findViewById(R.id.textViewFolderItemFolder);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
			} else {
				view = inflater.inflate(R.layout.podcast_item, parent, false);
				viewHolder.textTitle = (TextView)view.findViewById(R.id.textViewPodcastTitle);
				viewHolder.textInfo = (TextView)view.findViewById(R.id.textViewPodcastInfo);
				viewHolder.textStatus = (TextView)view.findViewById(R.id.textViewPodcastStatus);
				viewHolder.image = (ImageView)view.findViewById(R.id.imageViewItemImage);
				viewHolder.imageStatus = (ImageView)view.findViewById(R.id.imageViewPodcastStatus);
				viewHolder.card = view.findViewById(R.id.card);
			}
		} else {
			viewHolder = (ViewHolder)view.getTag();
		}
		
		if (value instanceof Action) {
			Action action = (Action)value;
			viewHolder.textTitle.setText(action.msg);
			if(action.action==Action.ACTION_GO_BACK) {
				viewHolder.image.setImageResource(R.drawable.back);
			} else if(action.action==Action.ACTION_UPDATE) {
				viewHolder.image.setImageResource(R.drawable.refresh);
			} else if(action.action==Action.ACTION_NEW) {
				viewHolder.image.setImageResource(R.drawable.newcontent);
			}
		} else if(value instanceof Podcast) {
			Podcast podcast = (Podcast)value;
			viewHolder.textTitle.setText(podcast.getName());
			Bitmap podcastImage = podcast.getImage();
			if(podcastImage!=null) {
				viewHolder.image.setImageBitmap(podcastImage);
			}
		} else if(value instanceof PodcastEpisode) {
			PodcastEpisode episode = (PodcastEpisode)value;
			viewHolder.textTitle.setText(episode.getTitle());
			String duration = episode.getDuration();
			if(duration!=null) {
				viewHolder.textInfo.setText(duration);
			} else {
				viewHolder.textInfo.setVisibility(View.GONE);
			}
			viewHolder.textStatus.setText(episode.getStatusString());
			switch(episode.getStatus()) {
			case PodcastEpisode.STATUS_NEW:
				viewHolder.imageStatus.setImageResource(R.drawable.accept);
				break;
			case PodcastEpisode.STATUS_DOWNLOADING:
				viewHolder.imageStatus.setImageResource(R.drawable.download);
				break;
			case PodcastEpisode.STATUS_DOWNLOADED:
				viewHolder.imageStatus.setImageResource(R.drawable.save);
				break;
			default:
				viewHolder.imageStatus.setImageDrawable(null);
			}
			if(episode.equals(currentEpisode)) {
				viewHolder.card.setBackgroundResource(R.drawable.card_playing);
				viewHolder.image.setImageResource(R.drawable.play_orange);
			} else {
				viewHolder.card.setBackgroundResource(R.drawable.card);
				viewHolder.image.setImageResource(R.drawable.audio);
			}
		}
		
		view.setTag(viewHolder);
		return view;
	}
	
	private class ViewHolder {
		public TextView textTitle;
		public TextView textInfo;
		public TextView textStatus;
		public ImageView image;
		public ImageView imageStatus;
		public View card;
	}
}
