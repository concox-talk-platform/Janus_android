package com.example.janusandroidtalk.im.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.janusandroidtalk.R;
import com.example.janusandroidtalk.im.view.ImgBrowserViewPager;
import com.example.janusandroidtalk.im.view.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.jiguang.imui.commons.BitmapLoader;

/**
 * IM展示图片的Activity
 */
public class BrowserImageActivity extends Activity {

    private ImgBrowserViewPager mViewPager;
    private List<String> mPathList = new ArrayList<>();
    private List<String> mMsgIdList = new ArrayList<>();
    private LruCache<String, Bitmap> mCache;
    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_browser);
        //mPathList,mMsgIdList分别保存了图片的路径和id，主要是为点击一张图片放大之后，可以左右滑动查看所有图片
        mPathList = getIntent().getStringArrayListExtra("pathList");
        mMsgIdList = getIntent().getStringArrayListExtra("idList");
        mViewPager = (ImgBrowserViewPager) findViewById(R.id.img_browser_viewpager);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        //获取屏幕宽高
        mWidth = dm.widthPixels;
        mHeight = dm.heightPixels;

        int maxMemory = (int) (Runtime.getRuntime().maxMemory());
        int cacheSize = maxMemory / 4;
        mCache = new LruCache<>(cacheSize);
        mViewPager.setAdapter(pagerAdapter);
        initCurrentItem();
    }

    private void initCurrentItem() {
        PhotoView photoView = new PhotoView(true, this);
        String msgId = getIntent().getStringExtra("msgId");
        int position = mMsgIdList.indexOf(msgId);
        String path = mPathList.get(position);
        if (path != null) {
            Bitmap bitmap = mCache.get(path);
            if (bitmap != null) {
                photoView.setImageBitmap(bitmap);
            } else {
                File file = new File(path);
                if (file.exists()) {
                    bitmap = BitmapLoader.getBitmapFromFile(path, mWidth, mHeight);
                    if (bitmap != null) {
                        //显示图片
                        photoView.setImageBitmap(bitmap);
                        mCache.put(path, bitmap);
                    } else {
                        //没图片时的默认页面
                        photoView.setImageResource(R.drawable.aurora_picture_not_found);
                    }
                } else {
                    photoView.setImageResource(R.drawable.aurora_picture_not_found);
                }
            }
        } else {
            photoView.setImageResource(R.drawable.aurora_picture_not_found);
        }
        mViewPager.setCurrentItem(position);
    }

    PagerAdapter pagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return mPathList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(true, BrowserImageActivity.this);
            photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoView.setTag(position);
            String path = mPathList.get(position);
            if (path != null) {
                Bitmap bitmap = mCache.get(path);
                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap);
                } else {
                    File file = new File(path);
                    if (file.exists()) {
                        bitmap = BitmapLoader.getBitmapFromFile(path, mWidth, mHeight);
                        if (bitmap != null) {
                            photoView.setImageBitmap(bitmap);
                            mCache.put(path, bitmap);
                        } else {
                            photoView.setImageResource(R.drawable.aurora_picture_not_found);
                        }
                    } else {
                        photoView.setImageResource(R.drawable.aurora_picture_not_found);
                    }
                }
            } else {
                photoView.setImageResource(R.drawable.aurora_picture_not_found);
            }
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(Object object) {
            View view = (View) object;
            int currentPage = mViewPager.getCurrentItem();
            if (currentPage == (Integer) view.getTag()) {
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
            }
        }
    };


}
