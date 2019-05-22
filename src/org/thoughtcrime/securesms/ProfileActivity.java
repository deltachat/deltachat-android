package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;

public class ProfileActivity extends PassphraseRequiredActionBarActivity
                             implements DcEventCenter.DcEventDelegate
{

  @SuppressWarnings("unused")
  private final static String TAG = ProfileActivity.class.getSimpleName();

  public static final String CHAT_ID_EXTRA    = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";
  public static final String FORCE_TAB_EXTRA  = "force_tab";

  public static final int TAB_SETTINGS = 10;
  public static final int TAB_GALLERY  = 20;
  public static final int TAB_DOCS     = 30;
  public static final int TAB_LINKS    = 40;
  public static final int TAB_MAP      = 50;

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ApplicationDcContext dcContext;
  private int                  chatId;
  private boolean              chatIsGroup;
  private int                  contactId;

  private ArrayList<Integer> tabs = new ArrayList<>();
  private Toolbar            toolbar;
  private TabLayout          tabLayout;
  private ViewPager          viewPager;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    dcContext = DcHelper.getContext(this);
  }

  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    setContentView(R.layout.profile_activity);

    initializeResources();
    initializeToolbar();

    this.tabLayout.setupWithViewPager(viewPager);
    this.viewPager.setAdapter(new ProfilePagerAdapter(getSupportFragmentManager()));
    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    updateToolbar();
  }

  private void initializeResources() {
    chatId      = getIntent().getIntExtra(CHAT_ID_EXTRA, 0);
    contactId   = getIntent().getIntExtra(CONTACT_ID_EXTRA, 0);
    chatIsGroup = false;

    if (contactId!=0) {
      chatId = dcContext.getChatIdByContactId(contactId);
    }
    else if(chatId!=0) {
      DcChat dcChat = dcContext.getChat(chatId);
      chatIsGroup = dcChat.isGroup();
      if(!chatIsGroup) {
        final int[] members = dcContext.getChatContacts(chatId);
        contactId = members.length>=1? members[0] : 0;
      }
    }

    if(!isGlobalProfile() && !isSelfProfile()) {
      tabs.add(TAB_SETTINGS);
    }
    tabs.add(TAB_GALLERY);
    tabs.add(TAB_DOCS);
    //tabs.add(TAB_LINKS);
    //if(Prefs.isLocationStreamingEnabled(this)) {
    //  tabs.add(TAB_MAP);
    //}

    this.viewPager = ViewUtil.findById(this, R.id.pager);
    this.toolbar   = ViewUtil.findById(this, R.id.toolbar);
    this.tabLayout = ViewUtil.findById(this, R.id.tab_layout);
  }

  private void initializeToolbar()
  {
    setSupportActionBar(this.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    updateToolbar();
  }

  private void updateToolbar() {
    if (isGlobalProfile()){
      getSupportActionBar().setTitle(R.string.menu_all_media);
    }
    else if (isContactProfile()){
      getSupportActionBar().setTitle(dcContext.getContact(contactId).getName());
    }
    else if (chatId >= 0) {
      getSupportActionBar().setTitle(dcContext.getChat(chatId).getName());
    }
  }

  private boolean isGlobalProfile() {
    return contactId==0 && chatId==0;
  }

  private boolean isContactProfile() {
    // there may still be a single-chat lined to the contact profile
    return contactId!=0 && (chatId==0 || !chatIsGroup);
  }

  private boolean isSelfProfile() {
    return isContactProfile() && contactId==DcContact.DC_CONTACT_ID_SELF;
  }

  private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

    ProfilePagerAdapter(FragmentManager fragmentManager) {
      super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
      int tabId = tabs.get(position);
      Fragment fragment;
      Bundle args = new Bundle();

      switch(tabId) {
        case TAB_SETTINGS:
          fragment = new ProfileSettingsFragment();
          args.putInt(ProfileSettingsFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putInt(ProfileSettingsFragment.CONTACT_ID_EXTRA, (contactId==0&&!isGlobalProfile())? -1 : contactId);
          args.putSerializable(ProfileSettingsFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        case TAB_GALLERY:
          fragment = new ProfileGalleryFragment();
          args.putInt(ProfileGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putSerializable(ProfileGalleryFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        default:
          fragment = new ProfileDocumentsFragment();
          args.putInt(ProfileGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putSerializable(ProfileDocumentsFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;
      }

      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getCount() {
      return tabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      int tabId = tabs.get(position);
      switch(tabId) {
        case TAB_SETTINGS:
          if(isContactProfile()) {
            return getString(contactId==DcContact.DC_CONTACT_ID_SELF? R.string.self : R.string.tab_contact);
          }
          else {
            return getString(R.string.tab_group);
          }

        case TAB_GALLERY:
          return getString(R.string.tab_gallery);

        case TAB_DOCS:
          return getString(R.string.tab_docs);

        case TAB_LINKS:
          return getString(R.string.tab_links);

        case TAB_MAP:
          return getString(R.string.tab_map);

        default:
          throw new AssertionError();
      }
    }
  }
}
