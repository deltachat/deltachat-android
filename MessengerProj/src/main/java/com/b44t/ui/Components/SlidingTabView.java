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


package com.b44t.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.ui.ActionBar.Theme;

public class SlidingTabView extends LinearLayout {

    public interface SlidingTabViewDelegate {
        void didSelectTab(int tab);
    }

    private SlidingTabViewDelegate delegate;
    private int selectedTab = 0;
    private int tabCount = 0;
    private float tabWidth = 0;
    private float tabX = 0;
    private float animateTabXTo = 0;
    private Paint paint = new Paint();
    private long startAnimationTime = 0;
    private long totalAnimationDiff = 0;
    private float startAnimationX = 0;
    private DecelerateInterpolator interpolator;

    public SlidingTabView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setWeightSum(100);
        paint.setColor(0xffffffff);
        setWillNotDraw(false);
        interpolator = new DecelerateInterpolator();
    }

    public void addTextTab(final int position, String title) {
        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setFocusable(true);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        tab.setTextColor(0xffffffff);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setBackgroundDrawable(Theme.createBarSelectorDrawable(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR, false));

        tab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                didSelectTab(position);
            }
        });
        addView(tab);
        LayoutParams layoutParams = (LayoutParams)tab.getLayoutParams();
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.width = 0;
        layoutParams.weight = 50;
        tab.setLayoutParams(layoutParams);

        tabCount++;
    }

    public void setDelegate(SlidingTabViewDelegate delegate) {
        this.delegate = delegate;
    }

    public int getSeletedTab() {
        return selectedTab;
    }

    private void didSelectTab(int tab) {
        if (selectedTab == tab) {
            return;
        }
        selectedTab = tab;
        animateToTab(tab);
        if (delegate != null) {
            delegate.didSelectTab(tab);
        }
    }

    private void animateToTab(int tab) {
        animateTabXTo = tab * tabWidth;
        startAnimationX = tabX;
        totalAnimationDiff = 0;
        startAnimationTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        tabWidth = (r - l) / (float)tabCount;
        animateTabXTo = tabX = tabWidth * selectedTab;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (tabX != animateTabXTo) {
            long dt = System.currentTimeMillis() - startAnimationTime;
            startAnimationTime = System.currentTimeMillis();
            totalAnimationDiff += dt;
            if (totalAnimationDiff > 200) {
                totalAnimationDiff = 200;
                tabX = animateTabXTo;
            } else {
                tabX = startAnimationX + interpolator.getInterpolation(totalAnimationDiff / 200.0f) * (animateTabXTo - startAnimationX);
                invalidate();
            }
        }

        canvas.drawRect(tabX, getHeight() - AndroidUtilities.dp(2), (tabX + tabWidth), getHeight(), paint);
    }
}
