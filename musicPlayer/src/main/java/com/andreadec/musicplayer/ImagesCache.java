package com.andreadec.musicplayer;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.*;
import android.view.View;
import android.widget.ImageView;

public class ImagesCache {
    private int imagesSize;
    private LruCache<String,Bitmap> cache;
    private Drawable songImage;

    public ImagesCache(Context context) {
        imagesSize = (int)context.getResources().getDimension(R.dimen.songImageSize);
        cache = new LruCache<String,Bitmap>(Constants.IMAGES_CACHE_SIZE);
        songImage = context.getResources().getDrawable(R.drawable.audio);
    }

    public void getImageAsync(PlayableItem item, ImageView imageView) {
        Bitmap image = null;
        synchronized(cache) {
            image = cache.get(item.getPlayableUri());
        }
        if(image==null) {
            imageView.setImageDrawable(songImage);
            ImageLoaderTask imageLoader = new ImageLoaderTask(item, imageView);
            imageLoader.execute();
        } else {
            imageView.setImageBitmap(image);
        }
    }

    public Bitmap getImageSync(PlayableItem item) {
        synchronized(cache) {
            Bitmap image = cache.get(item.getPlayableUri());
            if(image==null) {
                Bitmap originalImage = item.getImage();
                if(originalImage==null) return null;
                image = Bitmap.createScaledBitmap(originalImage, imagesSize, imagesSize, true);
                cache.put(item.getPlayableUri(), image);
            }
            return image.copy(image.getConfig(), true); // Necessary to avoid recycled bitmap to be used.
        }
    }

    private class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private PlayableItem item;
        private ImageView imageView;

        public ImageLoaderTask(PlayableItem item, ImageView imageView) {
            this.item = item;
            this.imageView = imageView;
        }

        /*@Override
        protected void onPreExecute() {
            imageView.setVisibility(View.INVISIBLE);
        }*/

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap originalImage = item.getImage();
            if(originalImage==null) return null;
            Bitmap image = Bitmap.createScaledBitmap(originalImage, imagesSize, imagesSize, true);
            synchronized(cache) {
                cache.put(item.getPlayableUri(), image);
            }
            image = image.copy(image.getConfig(), true); // Necessary to avoid recycled bitmap to be used.
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            if(image!=null) {
                imageView.setImageBitmap(image);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }
}
