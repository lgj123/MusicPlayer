/*
 * Copyright 2013-2014 Andrea De Cesare
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
import java.net.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import android.graphics.*;

public class PodcastParser {
	private String title;
	private String link;
	private String imageUrl;
	private ArrayList<PodcastEpisode> episodes;
	
	public boolean parse(String podcastUrl) {
		episodes = new ArrayList<PodcastEpisode>();
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new URL(podcastUrl).openStream());
		    Element root = document.getDocumentElement();
		    
		    NodeList channels = root.getChildNodes();
		    for(int i=0; i<channels.getLength(); i++) {
		    	Node channelNode = channels.item(i);
		    	if(channelNode.getNodeType()==Node.ELEMENT_NODE) {
		    		NodeList items = channelNode.getChildNodes();
		    		for(int j=0; j<items.getLength(); j++) {
		    			Node itemNode = items.item(j);
		    			if(itemNode.getNodeType()==Node.ELEMENT_NODE) {
		    				Element item = (Element)itemNode;
		    				if(item.getTagName().equals("title")) {
		    					title = item.getTextContent();
		    				} else if(item.getTagName().equals("link")) {
		    					link = item.getTextContent();
		    				} else if(item.getTagName().equals("itunes:image")) {
		    					imageUrl = item.getAttribute("href");
		    				} else if(item.getTagName().equals("item")) {
		    					NodeList itemsValues = itemNode.getChildNodes();
			    				String itemTitle=null, itemUrl=null, itemGuid=null, itemPubDate=null, duration=null, type=null;
			    				for(int k=0; k<itemsValues.getLength(); k++) {
			    					Node itemsValuesNode = itemsValues.item(k);
			    					if(itemsValuesNode.getNodeType()==Node.ELEMENT_NODE) {
			    						Element e = (Element)itemsValuesNode;
			    						String tag = e.getTagName();
			    						if(tag.equals("title")) itemTitle = e.getTextContent();
			    						if(tag.equals("guid")) itemGuid = e.getTextContent();
			    						if(tag.equals("pubDate")) itemPubDate = e.getTextContent();
			    						if(tag.equals("itunes:duration")) duration = e.getTextContent();
			    						if(tag.equals("enclosure")) {
			    							itemUrl = e.getAttribute("url");
			    							type = e.getAttribute("type");
			    						}
			    					}
			    				}
			    				
			    				long dateLong = 0;
			    				if(itemTitle==null || itemGuid==null || itemUrl==null || type==null) {
			    					continue; // This is essential information! Can't be missing!
			    				}
			    				try {
			    					DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
				    				Date d = format.parse(itemPubDate);
				    				dateLong = d.getTime();
			    				} catch(Exception e) {}
			    				
			    				PodcastEpisode episode = new PodcastEpisode(itemUrl, null, itemTitle, itemGuid, null, PodcastEpisode.STATUS_NEW, dateLong, duration, type);
			    				episodes.add(episode);
		    				}
		    			}
		    		}
		    	}
		    }
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getLink() {
		return link;
	}
	
	public String getImageUrl() {
		return imageUrl;
	}
	
	public ArrayList<PodcastEpisode> getEpisodes() {
		return episodes;
	}
	
	public byte[] downloadImage() {
		try {
			URLConnection connection = new URL(imageUrl).openConnection();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			InputStream input = connection.getInputStream();
			Bitmap bitmap = BitmapFactory.decodeStream(input);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
			return output.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
