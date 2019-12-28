package org.thoughtcrime.securesms.qr;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrActivity extends BaseActionBarActivity {

	private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();
	private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

	private TabLayout tabLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dynamicTheme.onCreate(this);
		dynamicLanguage.onCreate(this);

		setContentView(R.layout.activity_qr);
		tabLayout = ViewUtil.findById(this, R.id.tab_layout);
		ViewPager viewPager = ViewUtil.findById(this, R.id.pager);
		viewPager.setAdapter(new ProfilePagerAdapter(getSupportFragmentManager()));

		setSupportActionBar(ViewUtil.findById(this, R.id.toolbar));
		assert getSupportActionBar() != null;
		getSupportActionBar().setTitle(R.string.qr_code);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		tabLayout.setupWithViewPager(viewPager);
	}

	@Override
	protected void onResume() {
		super.onResume();

		int lastSelectedTab = PreferenceManager.getDefaultSharedPreferences(this).getInt("qrTab", 0);
		tabLayout.getTabAt(lastSelectedTab).select();

		dynamicTheme.onResume(this);
		dynamicLanguage.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		int currentSelectedTab = tabLayout.getSelectedTabPosition();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("qrTab", currentSelectedTab).apply();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}

		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}


	private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

		ProfilePagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			Fragment fragment;

			switch (position) {
				case 0:
					fragment = new QrShowFragment();
					break;

				default:
					fragment = new QrScanFragment();
					break;
			}

			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return getString(R.string.qr_activity_title_show);

				default:
					return getString(R.string.qr_activity_title_scan);
			}
		}

	}

	public void selectQrShowTab() {
		tabLayout.getTabAt(0).select();
	}
}