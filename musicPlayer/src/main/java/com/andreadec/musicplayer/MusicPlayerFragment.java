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

import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.v4.app.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;

import org.w3c.dom.Text;

public abstract class MusicPlayerFragment extends Fragment implements AdapterView.OnItemClickListener {
    protected ListView list;
    private View header;
    private ImageButton floatingButton;
    private TextView textViewHeader;
    private TextView emptyView;

    private int screenHeight, headerHeight;
    private int lastItem = 0;
    private float buttonPosition;
    private Space headerSpacingView;
    private boolean hidden = false; // Floating button and header currently hidden because of scrolling
    private boolean headerVisible = true, floatingButtonVisible = true;

    protected SharedPreferences preferences;

	public abstract boolean onBackPressed(); // Return false if no action was executed
	public abstract void gotoPlayingItemPosition(PlayableItem playingItem);
	public abstract void updateListView();
    public abstract void onHeaderClick();
    public abstract void onFloatingButtonClick();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    public void initialize(View view) {
        header = view.findViewById(R.id.layoutHeader);
        list = (ListView)view.findViewById(R.id.listView);
        list.setOnItemClickListener(this);
        emptyView = (TextView)view.findViewById(R.id.emptyView);
        list.setEmptyView(emptyView);
        textViewHeader = (TextView)view.findViewById(R.id.textViewHeader);
        floatingButton = (ImageButton)view.findViewById(R.id.floatingButton);

        HeadingFloatingClickListener clickListener = new HeadingFloatingClickListener();
        header.setOnClickListener(clickListener);
        floatingButton.setOnClickListener(clickListener);

        screenHeight = ((MainActivity)getActivity()).screenSizeY;
        headerSpacingView = new Space(getActivity());
        if(list.getHeaderViewsCount()==0) list.addHeaderView(headerSpacingView, "a", false);

        final ViewTreeObserver observer = header.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(headerHeight==0) headerHeight = header.getHeight();
                if(buttonPosition==0) buttonPosition = floatingButton.getY();
                if(headerHeight!=0 && buttonPosition!=0) {
                    header.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(headerVisible) {
                        headerSpacingView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, headerHeight));
                    }
                    list.setOnScrollListener(new ScrollListener());
                }
            }
        });
    }

    public void setHeaderVisible(boolean visible) {
        if(visible==headerVisible) return;
        if(visible) {
            headerSpacingView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, headerHeight));
            header.setY(0);
        } else {
            headerSpacingView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, 0));
            header.setY(-300);
        }
        headerVisible = visible;
        lastItem = 0;
    }

    public void setHeaderText(String msg) {
        header.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                header.removeOnLayoutChangeListener(this);
                if(headerHeight==header.getHeight()) return;
                headerHeight = header.getHeight();
                headerSpacingView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, headerHeight));
            }
        });
        textViewHeader.setText(msg);
    }

    public void setFloatingButtonImage(int res) {
        floatingButton.setImageResource(res);
    }

    public void setFloatingButtonVisible(boolean visible) {
        if(visible==floatingButtonVisible) return;
        if(visible) floatingButton.setY(buttonPosition);
        else floatingButton.setY(screenHeight);
        floatingButtonVisible = visible;
        lastItem = 0;
    }

    public void setEmptyViewMessage(int res) {
        emptyView.setText(res);
    }

    private class ScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if(!hidden && lastItem<firstVisibleItem) { // Scrolling down
                if(headerVisible) header.animate().y(-headerHeight).start();
                if(floatingButtonVisible) floatingButton.animate().y(screenHeight).start();
                hidden = true;
            } else if(hidden && lastItem>firstVisibleItem) { // Scrolling up
                if(headerVisible) header.animate().y(0).start();
                if(floatingButtonVisible) floatingButton.animate().y(buttonPosition).start();
                hidden = false;
            }
            lastItem = firstVisibleItem;
        }
        @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
    }

    private class HeadingFloatingClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            if(view.equals(header)) {
                onHeaderClick();
            } else if(view.equals(floatingButton)) {
                onFloatingButtonClick();
            }
        }
    }
}
