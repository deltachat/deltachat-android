/*******************************************************************************
 *
 *                              Delta Chat Android
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


package com.b44t.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.Emoji;
import com.b44t.messenger.EmojiData;
import com.b44t.messenger.Utilities;
import com.b44t.messenger.R;

import com.amulyakhare.textdrawable.TextDrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class EmojiInputView extends FrameLayout {

    public interface Listener {
        boolean onBackspace();
        void onEmojiSelected(String emoji);
        void onClearEmojiRecent();
    }

    private class SingleEmojiView extends ImageView {

        public SingleEmojiView(Context context) {
            super(context);

            setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    sendEmoji(null);
                }
            });
            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (pager.getCurrentItem() == 0) {
                        listener.onClearEmojiRecent();
                    }
                    return false;
                }
            });
            setBackgroundResource(R.drawable.list_selector);
            setScaleType(ImageView.ScaleType.CENTER);
        }

        private void sendEmoji(String override) {
            String code = override != null ? override : (String) getTag();
            if (override == null) {
                if (pager.getCurrentItem() != 0) {
                    String color = emojiColor.get(code);
                    if (color != null) {
                        code += color;
                    }
                }
                Integer count = emojiUseHistory.get(code);
                if (count == null) {
                    count = 0;
                }
                if (count == 0 && emojiUseHistory.size() > 50) {
                    for (int a = recentEmoji.size() - 1; a >= 0; a--) {
                        String emoji = recentEmoji.get(a);
                        emojiUseHistory.remove(emoji);
                        recentEmoji.remove(a);
                        if (emojiUseHistory.size() <= 50) {
                            break;
                        }
                    }
                }
                emojiUseHistory.put(code, ++count);
                if (pager.getCurrentItem() != 0) {
                    sortEmoji();
                }
                saveRecentEmoji();
                adapters.get(0).notifyDataSetChanged();
                if (listener != null) {
                    listener.onEmojiSelected(Emoji.fixEmoji(code));
                }
            } else {
                if (listener != null) {
                    listener.onEmojiSelected(Emoji.fixEmoji(override));
                }
            }

        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(widthMeasureSpec));
        }
    }

    private ArrayList<EmojiGridAdapter>    adapters = new ArrayList<>();
    private HashMap<String, Integer>       emojiUseHistory = new HashMap<>();
    private static HashMap<String, String> emojiColor = new HashMap<>();
    private ArrayList<String>              recentEmoji = new ArrayList<>();

    private Listener            listener;
    private ViewPager           pager;
    private FrameLayout         recentsWrap;
    private ArrayList<GridView> views = new ArrayList<>();
    private ImageView           backspaceButton;
    private LinearLayout        pagerSlidingTabStripContainer;

    private int                 oldWidth;
    private int                 lastNotifyWidth;

    private boolean             backspacePressed;
    private boolean             backspaceOnce;

    public EmojiInputView(final Context context) {
        super(context);

        for (int i = 0; i < EmojiData.dataColored.length + 1; i++) {
            GridView gridView = new GridView(context);
            gridView.setColumnWidth(AndroidUtilities.dp(45));
            gridView.setNumColumns(-1);
            views.add(gridView);

            EmojiGridAdapter emojiGridAdapter = new EmojiGridAdapter(i - 1);
            gridView.setAdapter(emojiGridAdapter);
            adapters.add(emojiGridAdapter);
        }

        setBackgroundColor(0xfff5f6f7);

        pager = new ViewPager(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        pager.setAdapter(new EmojiPagerAdapter());

        pagerSlidingTabStripContainer = new LinearLayout(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        pagerSlidingTabStripContainer.setOrientation(LinearLayout.HORIZONTAL);
        pagerSlidingTabStripContainer.setBackgroundColor(0xfff5f6f7);
        addView(pagerSlidingTabStripContainer, LayoutHelper.createFrame(LayoutParams.MATCH_PARENT, 48));

        PagerSlidingTabStrip pagerSlidingTabStrip = new PagerSlidingTabStrip(context);
        pagerSlidingTabStrip.setViewPager(pager);
        pagerSlidingTabStrip.setShouldExpand(true);
        pagerSlidingTabStrip.setIndicatorHeight(AndroidUtilities.dp(2));
        pagerSlidingTabStrip.setUnderlineHeight(AndroidUtilities.dp(1));
        pagerSlidingTabStrip.setIndicatorColor(0xff2b96e2);
        pagerSlidingTabStrip.setUnderlineColor(0xffe2e5e7);
        pagerSlidingTabStripContainer.addView(pagerSlidingTabStrip, LayoutHelper.createLinear(0, 48, 1.0f));

        FrameLayout frameLayout = new FrameLayout(context);
        pagerSlidingTabStripContainer.addView(frameLayout, LayoutHelper.createLinear(52, 48));

        backspaceButton = new ImageView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    backspacePressed = true;
                    backspaceOnce = false;
                    postBackspaceRunnable(350);
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                    backspacePressed = false;
                    if (!backspaceOnce) {
                        if (listener != null && listener.onBackspace()) {
                            backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        }
                    }
                }
                super.onTouchEvent(event);
                return true;
            }
        };
        backspaceButton.setImageResource(R.drawable.ic_smiles_backspace);
        backspaceButton.setBackgroundResource(R.drawable.ic_emoji_backspace);
        backspaceButton.setScaleType(ImageView.ScaleType.CENTER);
        frameLayout.addView(backspaceButton, LayoutHelper.createFrame(52, 48));

        View view = new View(context);
        view.setBackgroundColor(0xffe2e5e7);
        frameLayout.addView(view, LayoutHelper.createFrame(52, 1, Gravity.START | Gravity.BOTTOM));

        recentsWrap = new FrameLayout(context);
        recentsWrap.addView(views.get(0));

        TextView textView = new TextView(context);
        textView.setText(context.getString(R.string.NoRecent));
        textView.setTextSize(18);
        textView.setTextColor(0xff888888);
        textView.setGravity(Gravity.CENTER);
        recentsWrap.addView(textView);
        views.get(0).setEmptyView(textView);

        addView(pager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 0, 48, 0, 0));

        loadRecents();
    }

    public void clearRecentEmoji() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        preferences.edit().putBoolean("filled_default", true).apply();
        emojiUseHistory.clear();
        recentEmoji.clear();
        saveRecentEmoji();
        adapters.get(0).notifyDataSetChanged();
    }

    private void postBackspaceRunnable(final int time) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!backspacePressed) {
                    return;
                }
                if (listener != null && listener.onBackspace()) {
                    backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                backspaceOnce = true;
                postBackspaceRunnable(Math.max(50, time - 100));
            }
        }, time);
    }

    private void saveRecentEmoji() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        StringBuilder stringBuilder = new StringBuilder();
        for (HashMap.Entry<String, Integer> entry : emojiUseHistory.entrySet()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
        }
        preferences.edit().putString("emojis2", stringBuilder.toString()).apply();
    }

    private void sortEmoji() {
        recentEmoji.clear();
        for (HashMap.Entry<String, Integer> entry : emojiUseHistory.entrySet()) {
            recentEmoji.add(entry.getKey());
        }
        Collections.sort(recentEmoji, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                Integer count1 = emojiUseHistory.get(lhs);
                Integer count2 = emojiUseHistory.get(rhs);
                if (count1 == null) {
                    count1 = 0;
                }
                if (count2 == null) {
                    count2 = 0;
                }
                if (count1 > count2) {
                    return -1;
                } else if (count1 < count2) {
                    return 1;
                }
                return 0;
            }
        });
        while (recentEmoji.size() > 50) {
            recentEmoji.remove(recentEmoji.size() - 1);
        }
    }

    public void loadRecents() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);

        String str;
        try {
            emojiUseHistory.clear();
            if (preferences.contains("emojis")) {
                str = preferences.getString("emojis", "");
                if (str.length() > 0) {
                    String[] args = str.split(",");
                    for (String arg : args) {
                        String[] args2 = arg.split("=");
                        long value = Utilities.parseLong(args2[0]);
                        String string = "";
                        for (int a = 0; a < 4; a++) {
                            char ch = (char) value;
                            string = String.valueOf(ch) + string;
                            value >>= 16;
                            if (value == 0) {
                                break;
                            }
                        }
                        if (string.length() > 0) {
                            emojiUseHistory.put(string, Utilities.parseInt(args2[1]));
                        }
                    }
                }
                preferences.edit().remove("emojis").apply();
                saveRecentEmoji();
            } else {
                str = preferences.getString("emojis2", "");
                if (str.length() > 0) {
                    String[] args = str.split(",");
                    for (String arg : args) {
                        String[] args2 = arg.split("=");
                        emojiUseHistory.put(args2[0], Utilities.parseInt(args2[1]));
                    }
                }
            }
            if (emojiUseHistory.isEmpty()) {
                if (!preferences.getBoolean("filled_default", false)) {
                    String[] newRecent = new String[]{
                            "\uD83D\uDE02", "\uD83D\uDE18", "\u2764", "\uD83D\uDE0D", "\uD83D\uDE0A", "\uD83D\uDE01",
                            "\uD83D\uDC4D", "\u263A", "\uD83D\uDE14", "\uD83D\uDE04", "\uD83D\uDE2D", "\uD83D\uDC8B",
                            "\uD83D\uDE12", "\uD83D\uDE33", "\uD83D\uDE1C", "\uD83D\uDE48", "\uD83D\uDE09", "\uD83D\uDE03",
                            "\uD83D\uDE22", "\uD83D\uDE1D", "\uD83D\uDE31", "\uD83D\uDE21", "\uD83D\uDE0F", "\uD83D\uDE1E",
                            "\uD83D\uDE05", "\uD83D\uDE1A", "\uD83D\uDE4A", "\uD83D\uDE0C", "\uD83D\uDE00", "\uD83D\uDE0B",
                            "\uD83D\uDE06", "\uD83D\uDC4C", "\uD83D\uDE10", "\uD83D\uDE15"};
                    for (int i = 0; i < newRecent.length; i++) {
                        emojiUseHistory.put(newRecent[i], newRecent.length - i);
                    }
                    preferences.edit().putBoolean("filled_default", true).apply();
                    saveRecentEmoji();
                }
            }
            sortEmoji();
            adapters.get(0).notifyDataSetChanged();
        } catch (Exception e) {

        }

        try {
            str = preferences.getString("color", "");
            if (str.length() > 0) {
                String[] args = str.split(",");
                for (int a = 0; a < args.length; a++) {
                    String arg = args[a];
                    String[] args2 = arg.split("=");
                    emojiColor.put(args2[0], args2[1]);
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) pagerSlidingTabStripContainer.getLayoutParams();
        layoutParams.width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (layoutParams.width != oldWidth) {
            pagerSlidingTabStripContainer.setLayoutParams(layoutParams);
            oldWidth = layoutParams.width;
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (lastNotifyWidth != right - left) {
            lastNotifyWidth = right - left;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setListener(Listener value) {
        listener = value;
    }

    public void invalidateViews() {
        for (GridView gridView : views) {
            if (gridView != null) {
                gridView.invalidateViews();
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != GONE) {
            sortEmoji();
            adapters.get(0).notifyDataSetChanged();
        }
    }

    private class EmojiGridAdapter extends BaseAdapter {

        private int emojiPage;

        public EmojiGridAdapter(int page) {
            emojiPage = page;
        }

        public int getCount() {
            if (emojiPage == -1) {
                return recentEmoji.size();
            }
            return EmojiData.dataColored[emojiPage].length;
        }

        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int i, View view, ViewGroup paramViewGroup) {
            SingleEmojiView imageView = (SingleEmojiView) view;
            if (imageView == null) {
                imageView = new SingleEmojiView(getContext());
            }
            String code;
            String coloredCode;
            if (emojiPage == -1) {
                coloredCode = code = recentEmoji.get(i);
            } else {
                coloredCode = code = EmojiData.dataColored[emojiPage][i];
                String color = emojiColor.get(code);
                if (color != null) {
                    coloredCode += color;
                }
            }

            // Draw native Emojis using TextDrawable, not imageView.setImageDrawable(Emoji.getEmojiBigDrawable(code)). See:
            // https://github.com/Jamesits/Moegram/commit/8e52c6222cf00bc9a86d52fed4c06558f48c345c
            // and https://github.com/amulyakhare/TextDrawable
            imageView.setTag(code);
            int bigImgSize;
            bigImgSize = AndroidUtilities.dp(32);
            imageView.setImageDrawable(TextDrawable.builder().beginConfig().textColor(Color.BLACK).fontSize(bigImgSize).endConfig().buildRect(coloredCode, Color.TRANSPARENT));
            return imageView;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

    private class EmojiPagerAdapter extends PagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        public void destroyItem(ViewGroup viewGroup, int position, Object object) {
            View view;
            if (position == 0) {
                view = recentsWrap;
            } else {
                view = views.get(position);
            }
            viewGroup.removeView(view);
        }

        public int getCount() {
            return views.size();
        }

        public int getPageIconResId(int position) {
            int[] icons = {
                    R.drawable.ic_emoji_recent,
                    R.drawable.ic_emoji_smile,
                    R.drawable.ic_emoji_flower,
                    R.drawable.ic_emoji_bell,
                    R.drawable.ic_emoji_car,
                    R.drawable.ic_emoji_symbol};
            return icons[position>=0 && position<icons.length? position : 0];
        }

        public Object instantiateItem(ViewGroup viewGroup, int position) {
            View view;
            if (position == 0) {
                view = recentsWrap;
            } else {
                view = views.get(position);
            }
            viewGroup.addView(view);
            return view;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }
}
