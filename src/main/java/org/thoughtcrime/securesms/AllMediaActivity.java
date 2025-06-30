package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.google.android.material.tabs.TabLayout;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;

public class AllMediaActivity extends PassphraseRequiredActionBarActivity
                             implements DcEventCenter.DcEventDelegate
{

  public static final String CHAT_ID_EXTRA    = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";
  public static final String FORCE_TAB_EXTRA  = "force_tab";

  public static final int TAB_WEBXDC   = 10;
  public static final int TAB_GALLERY  = 20;
  public static final int TAB_DOCS     = 30;
  public static final int TAB_AUDIO    = 40;

  private DcContext            dcContext;
  private int                  chatId;
  private int                  contactId;

  private final ArrayList<Integer> tabs = new ArrayList<>();
  private Toolbar            toolbar;
  private TabLayout          tabLayout;
  private ViewPager          viewPager;

  @Override
  protected void onPreCreate() {
    dynamicTheme = new DynamicNoActionBarTheme();
    super.onPreCreate();
    dcContext = DcHelper.getContext(this);
  }

  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    setContentView(R.layout.all_media_activity);

    initializeResources();

    setSupportActionBar(this.toolbar);
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setTitle(isGlobalGallery() ? R.string.menu_all_media : R.string.apps_and_media);
    }

    this.tabLayout.setupWithViewPager(viewPager);
    this.viewPager.setAdapter(new AllMediaPagerAdapter(getSupportFragmentManager()));
    int forceTab = getIntent().getIntExtra(FORCE_TAB_EXTRA, -1);
    if (forceTab != -1) {
      int forceIndex = tabs.indexOf(forceTab);
      if (forceIndex != -1) {
        this.viewPager.setCurrentItem(forceIndex);
      }
    }

    DcEventCenter eventCenter = DcHelper.getEventCenter(this);
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
  }

  @Override
  public void onDestroy() {
    DcHelper.getEventCenter(this).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
  }

  private void initializeResources() {
    chatId           = getIntent().getIntExtra(CHAT_ID_EXTRA, 0);
    contactId        = getIntent().getIntExtra(CONTACT_ID_EXTRA, 0);

    if (contactId!=0) {
      chatId = dcContext.getChatIdByContactId(contactId);
    }

    if(chatId!=0) {
      DcChat dcChat = dcContext.getChat(chatId);
      if(!dcChat.isMultiUser()) {
        final int[] members = dcContext.getChatContacts(chatId);
        contactId = members.length>=1? members[0] : 0;
      }
    }

    tabs.add(TAB_WEBXDC);
    tabs.add(TAB_GALLERY);
    tabs.add(TAB_DOCS);
    tabs.add(TAB_AUDIO);

    this.viewPager = ViewUtil.findById(this, R.id.pager);
    this.toolbar   = ViewUtil.findById(this, R.id.toolbar);
    this.tabLayout = ViewUtil.findById(this, R.id.tab_layout);
  }

  private boolean isGlobalGallery() {
    return contactId==0 && chatId==0;
  }

  private class AllMediaPagerAdapter extends FragmentStatePagerAdapter {
    private Object currentFragment = null;

    AllMediaPagerAdapter(FragmentManager fragmentManager) {
      super(fragmentManager);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      super.setPrimaryItem(container, position, object);
      if (currentFragment != null && currentFragment != object) {
        ActionMode action = null;
        if (currentFragment instanceof MessageSelectorFragment) {
          action = ((MessageSelectorFragment) currentFragment).getActionMode();
        }
        if (action != null) {
          action.finish();
        }
      }
      currentFragment = object;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
      int tabId = tabs.get(position);
      Fragment fragment;
      Bundle args = new Bundle();

      switch(tabId) {
        case TAB_WEBXDC:
          fragment = new AllMediaDocumentsFragment();
          args.putInt(AllMediaDocumentsFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalGallery())? -1 : chatId);
          args.putBoolean(AllMediaDocumentsFragment.SHOW_WEBXDC_EXTRA, true);
          break;

        case TAB_GALLERY:
          fragment = new AllMediaGalleryFragment();
          args.putInt(AllMediaGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalGallery())? -1 : chatId);
          break;

        case TAB_AUDIO:
          fragment = new AllMediaDocumentsFragment();
          args.putInt(AllMediaDocumentsFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalGallery())? -1 : chatId);
          args.putBoolean(AllMediaDocumentsFragment.SHOW_AUDIO_EXTRA, true);
          break;

        default:
          fragment = new AllMediaDocumentsFragment();
          args.putInt(AllMediaGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalGallery())? -1 : chatId);
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
      switch(tabs.get(position)) {
        case TAB_WEBXDC: return getString(R.string.webxdc_apps);
        case TAB_GALLERY: return getString(R.string.tab_gallery);
        case TAB_AUDIO: return getString(R.string.audio);
        default: return getString(R.string.files);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    super.onOptionsItemSelected(item);

    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      finish();
      return true;
    }

    return false;
  }
}
