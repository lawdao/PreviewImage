package example.fussen.image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener, ViewPager.OnPageChangeListener {

    private GridView gridView;
    private ViewPager viewPager;
    private LinearLayout points;
    private PhotoAdapter adapter;
    private View container;
    private boolean isZooming;
    private boolean photoGone = true;

    private List<Rect> itemRect = new ArrayList<>();
    private List<View> itemView = new ArrayList<>();
    private View firstClickView;
    private int firstClickPosition;
    private Animator mCurrentAnimator;
    private PhotoPagerAdapter pagerAdapter;

    private String[] fileNames = {
            "ccc.png", "ddd.jpg", "eee.jpg", "fff.jpg",
            "ggg.JPEG", "hhh.jpg", "iii.jpg", "image.png", "jjj.jpg", "mmm.jpg"

    };

    private List<Bitmap> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        iniView();
    }

    private void iniView() {

        container = findViewById(R.id.container);
        gridView = (GridView) findViewById(R.id.gridview);

        viewPager = (ViewPager) findViewById(R.id.viewpager);

        points = (LinearLayout) findViewById(R.id.ll_points);

        adapter = new PhotoAdapter(this, images);
        gridView.setAdapter(adapter);

        pagerAdapter = new PhotoPagerAdapter();


        gridView.setOnItemClickListener(this);
        viewPager.setOnPageChangeListener(this);

        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < fileNames.length; i++) {
                    images.add(getImageFromAssetsFile(fileNames[i]));

                    if (i == fileNames.length - 1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                pagerAdapter.notifyDataSetChanged();
                            }
                        });

                    }

                }
            }
        }.start();

    }


    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!isZooming && photoGone) {

            //The Photo is Zooming
            isZooming = true;
            photoGone = false;
            itemView.clear();
            itemRect.clear();
            for (int i = 0; i < images.size(); i++) {
                Rect rect = new Rect();
                parent.getChildAt(i).getGlobalVisibleRect(rect);
                itemRect.add(rect);
                itemView.add(parent.getChildAt(i));
            }
            firstClickView = view;

            firstClickPosition = position;
            showBigPhoto(view, position);

        }
    }


    private void showBigPhoto(final View view, int position) {


        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // set adapter and change current item

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(position);

        //prepare points
        if (images.size() > 1) {
            preparePoints(position);
        }

        // Calculate the starting and ending bounds for the zoomed-in item view(ImageView).
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the view, and the
        // final bounds are the global visible rectangle of the container view. Also
        // set the container view's offset as the origin for the bounds, since that's
        // the origin for the positioning animation properties (X, Y).

        view.getGlobalVisibleRect(startBounds);

        container.getGlobalVisibleRect(finalBounds, globalOffset);

        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);


        // Adjust the start bounds to be the same aspect ratio as the final bounds using the
        // "center crop" technique. This prevents undesirable stretching during the animation.
        // Also calculate the start scaling factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the view(ImageView) and show the zoomed-in view. When the animation begins,

        viewPager.setVisibility(View.VISIBLE);

        firstClickView.setVisibility(View.INVISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
        // the zoomed-in view (the default is the center of the view).
        viewPager.setPivotX(0f);
        viewPager.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and scale properties
        // (X, Y, SCALE_X, and SCALE_Y).
        // and change the backgroud color

        ObjectAnimator backgroundColor = ObjectAnimator.ofInt(viewPager, "backgroundColor", Color.TRANSPARENT, Color.BLACK);

        backgroundColor.setEvaluator(new ArgbEvaluator());

        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(viewPager, View.X, startBounds.left,
                        finalBounds.left))
                .with(ObjectAnimator.ofFloat(viewPager, View.Y, startBounds.top,
                        finalBounds.top))
                .with(ObjectAnimator.ofFloat(viewPager, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(viewPager, View.SCALE_Y, startScale, 1f))
                .with(backgroundColor);
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                points.setVisibility(View.VISIBLE);
                mCurrentAnimator = null;
                isZooming = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                points.setVisibility(View.GONE);
                mCurrentAnimator = null;
                isZooming = false;
            }
        });
        set.start();
        mCurrentAnimator = set;

    }

    private void preparePoints(int position) {

        points.removeAllViews();
        for (int i = 0; i < images.size(); i++) {

            ImageView point = new ImageView(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            params.rightMargin = dip2px(8);
            params.bottomMargin = dip2px(6);
            point.setLayoutParams(params);

            if (position == i) {
                point.setImageResource(R.drawable.ic_page_indicator_focused2);
            } else {
                point.setImageResource(R.drawable.ic_page_indicator2);
            }
            points.addView(point);
        }
    }


    // dp--px
    public int dip2px(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5);// 加上0.5 为了四舍五入
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        int childCount = points.getChildCount();

        for (int i = 0; i < childCount; i++) {

            ImageView point = (ImageView) points.getChildAt(i);
            if (i == position) {
                point.setImageResource(R.drawable.ic_page_indicator_focused2);
            } else {
                point.setImageResource(R.drawable.ic_page_indicator2);
            }

        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    public class PhotoPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup group, final int position) {

            View view = View.inflate(MainActivity.this, R.layout.item_pager_photo, null);

            PhotoView image = (PhotoView) view.findViewById(R.id.image_photo);

            //重新获取条目的位置
            final Rect startBounds = itemRect.get(position);
            final Rect finalBounds = new Rect();
            final Point globalOffset = new Point();

            container.getGlobalVisibleRect(finalBounds, globalOffset);
            startBounds.offset(-globalOffset.x, -globalOffset.y);
            finalBounds.offset(-globalOffset.x, -globalOffset.y);


            final float startScale;
            if ((float) finalBounds.width() / finalBounds.height()
                    > (float) startBounds.width() / startBounds.height()) {
                startScale = (float) startBounds.height() / finalBounds.height();
                float startWidth = startScale * finalBounds.width();
                float deltaWidth = (startWidth - startBounds.width()) / 2;
                startBounds.left -= deltaWidth;
                startBounds.right += deltaWidth;
            } else {
                startScale = (float) startBounds.width() / finalBounds.width();
                float startHeight = startScale * finalBounds.height();
                float deltaHeight = (startHeight - startBounds.height()) / 2;
                startBounds.top -= deltaHeight;
                startBounds.bottom += deltaHeight;
            }


            image.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {

                    if (!isZooming) {
                        if (firstClickPosition != position) {
                            firstClickView.setVisibility(View.VISIBLE);
                            itemView.get(position).setVisibility(View.INVISIBLE);
                        }

                        points.setVisibility(View.GONE);
                        if (mCurrentAnimator != null) {
                            mCurrentAnimator.cancel();
                        }


                        ObjectAnimator backgroundColor = ObjectAnimator.ofInt(viewPager, "backgroundColor", Color.BLACK, Color.TRANSPARENT);

                        backgroundColor.setEvaluator(new ArgbEvaluator());

                        AnimatorSet set = new AnimatorSet();
                        set
                                .play(ObjectAnimator.ofFloat(viewPager, View.X, startBounds.left))
                                .with(ObjectAnimator.ofFloat(viewPager, View.Y, startBounds.top))
                                .with(ObjectAnimator
                                        .ofFloat(viewPager, View.SCALE_X, startScale))
                                .with(ObjectAnimator
                                        .ofFloat(viewPager, View.SCALE_Y, startScale))
                                .with(backgroundColor);
                        set.setDuration(250);
                        set.setInterpolator(new DecelerateInterpolator());
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {

                                itemView.get(position).setVisibility(View.VISIBLE);
                                viewPager.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                                photoGone = true;
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                itemView.get(position).setVisibility(View.VISIBLE);
                                viewPager.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                                photoGone = true;
                            }
                        });
                        set.start();
                        mCurrentAnimator = set;
                    }

                }
            });

            image.setImageBitmap(images.get(position));
            group.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }


    @Override
    public void onBackPressed() {
        if (viewPager.getVisibility() == View.VISIBLE) {

            closeBigPhoto();

        } else {
            super.onBackPressed();
        }
    }


    public void closeBigPhoto() {


        final int currentItem = viewPager.getCurrentItem();

        final Rect startBounds = itemRect.get(currentItem);
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        container.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        final float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        //start close
        if (firstClickPosition != currentItem) {
            firstClickView.setVisibility(View.VISIBLE);
            itemView.get(currentItem).setVisibility(View.INVISIBLE);
        }

        points.setVisibility(View.GONE);
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        ObjectAnimator backgroundColor = ObjectAnimator.ofInt(viewPager, "backgroundColor", Color.BLACK, Color.TRANSPARENT);

        backgroundColor.setEvaluator(new ArgbEvaluator());

        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(viewPager, View.X, startBounds.left))
                .with(ObjectAnimator.ofFloat(viewPager, View.Y, startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(viewPager, View.SCALE_X, startScale))
                .with(ObjectAnimator
                        .ofFloat(viewPager, View.SCALE_Y, startScale))
                .with(backgroundColor);
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator());

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                itemView.get(currentItem).setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.GONE);
                mCurrentAnimator = null;

                photoGone = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                itemView.get(currentItem).setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.GONE);
                mCurrentAnimator = null;
                photoGone = true;
            }
        });
        set.start();
        mCurrentAnimator = set;

    }
}
