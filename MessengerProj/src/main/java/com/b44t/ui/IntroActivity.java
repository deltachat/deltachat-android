/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.Theme;

import java.util.Locale;

import static java.lang.Math.min;

public class IntroActivity extends Activity {

    private ViewPager viewPager;
    private ImageView topImage1;
    private ImageView topImage2;
    private ViewGroup bottomPages;
    private int lastPage = 0;
    private boolean justCreated = false;
    private boolean startPressed = false;
    private int[] icons;
    private int[] titles;
    private int[] messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MessengerProj);
        super.onCreate(savedInstanceState);
        Theme.loadRecources(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        float heightOnLandscape = min(AndroidUtilities.displaySize.x,AndroidUtilities.displaySize.y)/AndroidUtilities.density;
        if( heightOnLandscape < 450 ) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.intro_layout);

        if (LocaleController.isRTL) {
            icons = new int[]{
                    R.drawable.intro7,
                    R.drawable.intro6,
                    R.drawable.intro5,
                    R.drawable.intro4,
                    R.drawable.intro3,
                    R.drawable.intro2,
                    R.drawable.intro1
            };
            titles = new int[]{
                    R.string.Intro7Headline,
                    R.string.Intro6Headline,
                    R.string.Intro5Headline,
                    R.string.Intro4Headline,
                    R.string.Intro3Headline,
                    R.string.Intro2Headline,
                    R.string.Intro1Headline
            };
            messages = new int[]{
                    R.string.Intro7Message,
                    R.string.Intro6Message,
                    R.string.Intro5Message,
                    R.string.Intro4Message,
                    R.string.Intro3Message,
                    R.string.Intro2Message,
                    R.string.Intro1Message
            };
        } else {
            icons = new int[]{
                    R.drawable.intro1,
                    R.drawable.intro2,
                    R.drawable.intro3,
                    R.drawable.intro4,
                    R.drawable.intro5,
                    R.drawable.intro6,
                    R.drawable.intro7
            };
            titles = new int[]{
                    R.string.Intro1Headline,
                    R.string.Intro2Headline,
                    R.string.Intro3Headline,
                    R.string.Intro4Headline,
                    R.string.Intro5Headline,
                    R.string.Intro6Headline,
                    R.string.Intro7Headline
            };
            messages = new int[]{
                    R.string.Intro1Message,
                    R.string.Intro2Message,
                    R.string.Intro3Message,
                    R.string.Intro4Message,
                    R.string.Intro5Message,
                    R.string.Intro6Message,
                    R.string.Intro7Message
            };
        }
        viewPager = (ViewPager) findViewById(R.id.intro_view_pager);
        TextView startMessagingButton = (TextView) findViewById(R.id.start_messaging_button);

        String buttonTitle = ApplicationLoader.applicationContext.getString(R.string.IntroStartMessaging);
        Bundle extras = getIntent().getExtras();
        if( extras != null && extras.getString("buttonTitle")!=null ) {
            buttonTitle = extras.getString("buttonTitle");
        }
        startMessagingButton.setText(buttonTitle);

        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(startMessagingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(startMessagingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            startMessagingButton.setStateListAnimator(animator);
        }
        topImage1 = (ImageView) findViewById(R.id.icon_image1);
        topImage2 = (ImageView) findViewById(R.id.icon_image2);
        bottomPages = (ViewGroup) findViewById(R.id.bottom_pages);
        topImage2.setVisibility(View.GONE);
        viewPager.setAdapter(new IntroAdapter());
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {
                if (i == ViewPager.SCROLL_STATE_IDLE || i == ViewPager.SCROLL_STATE_SETTLING) {
                    if (lastPage != viewPager.getCurrentItem()) {
                        lastPage = viewPager.getCurrentItem();

                        final ImageView fadeoutImage;
                        final ImageView fadeinImage;
                        if (topImage1.getVisibility() == View.VISIBLE) {
                            fadeoutImage = topImage1;
                            fadeinImage = topImage2;

                        } else {
                            fadeoutImage = topImage2;
                            fadeinImage = topImage1;
                        }

                        fadeinImage.bringToFront();
                        fadeinImage.setImageResource(icons[lastPage]);
                        fadeinImage.clearAnimation();
                        fadeoutImage.clearAnimation();

                        Animation outAnimation = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.icon_anim_fade_out);
                        outAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                fadeoutImage.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        Animation inAnimation = AnimationUtils.loadAnimation(IntroActivity.this, R.anim.icon_anim_fade_in);
                        inAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                fadeinImage.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });


                        fadeoutImage.startAnimation(outAnimation);
                        fadeinImage.startAnimation(inAnimation);
                    }
                }
            }
        });

        startMessagingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startPressed) {
                    return;
                }
                startPressed = true;
                Intent intent2 = new Intent(IntroActivity.this, LaunchActivity.class);
                intent2.putExtra("fromIntro", true);
                startActivity(intent2);
                finish();
            }
        });

        justCreated = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (justCreated) {
            if (LocaleController.isRTL) {
                viewPager.setCurrentItem(6);
                lastPage = 6;
            } else {
                viewPager.setCurrentItem(0);
                lastPage = 0;
            }
            justCreated = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class IntroAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 7;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = View.inflate(container.getContext(), R.layout.intro_view_layout, null);
            TextView headerTextView = (TextView) view.findViewById(R.id.header_text);
            TextView messageTextView = (TextView) view.findViewById(R.id.message_text);
            container.addView(view, 0);

            headerTextView.setText(getString(titles[position]));
            messageTextView.setText(AndroidUtilities.replaceTags(getString(messages[position])));

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            int count = bottomPages.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = bottomPages.getChildAt(a);
                if (a == position) {
                    child.setBackgroundColor(0xff2ca5e0);
                } else {
                    child.setBackgroundColor(0xffbbbbbb);
                }
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }
}
