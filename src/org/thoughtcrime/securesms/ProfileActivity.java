package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.CursorRecyclerViewAdapter;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import org.thoughtcrime.securesms.database.loaders.ThreadMediaLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class ProfileActivity extends PassphraseRequiredActionBarActivity  {

  @SuppressWarnings("unused")
  private final static String TAG = ProfileActivity.class.getSimpleName();

  public static final String CHAT_ID_EXTRA    = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";
  public static final String FORCE_TAB_EXTRA  = "force_tab";

  private static final String FRAGMENT_LOCALE_EXTRA  = "locale_extra";
  private static final String FRAGMENT_ADDRESS_EXTRA = "address";

  public static final int TAB_SETTINGS = 10;
  public static final int TAB_GALLERY  = 20;
  public static final int TAB_DOCS     = 30;
  public static final int TAB_LINKS    = 40;
  public static final int TAB_MAP      = 50;

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ApplicationDcContext dcContext;
  private int                  chatId;
  private @Nullable DcChat     dcChat;
  private int                  contactId;
  private @Nullable DcContact  dcContact;

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
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  private void initializeResources() {
    chatId    = getIntent().getIntExtra(CHAT_ID_EXTRA, 0);
    contactId = getIntent().getIntExtra(CONTACT_ID_EXTRA, 0);

    if (contactId!=0) {
      dcContact = dcContext.getContact(contactId);
      chatId = dcContext.getChatIdByContactId(contactId);
      if (chatId!=0) {
        dcChat = dcContext.getChat(chatId);
      }
    }
    else if(chatId!=0) {
      dcChat = dcContext.getChat(chatId);
      if(!dcChat.isGroup()) {
        final int[] members = dcContext.getChatContacts(chatId);
        contactId = members.length>=1? members[0] : 0;
        if (contactId!=0) {
          dcContact = dcContext.getContact(contactId);
        }
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
      getSupportActionBar().setTitle(dcContact.getName());
    }
    else if (dcChat != null) {
      getSupportActionBar().setTitle(dcChat.getName());
    }
  }

  private boolean isGlobalProfile() {
    return contactId==0 && chatId==0;
  }

  private boolean isContactProfile() {
    // there may still be a single-chat lined to the contact profile
    return dcContact!=null && (dcChat==null || !dcChat.isGroup());
  }

  private boolean isSelfProfile() {
    return isContactProfile() && contactId==DcContact.DC_CONTACT_ID_SELF;
  }

  private Recipient getRecipient() {
    if(dcChat!=null) {
      return dcContext.getRecipient(dcChat);
    }
    else if(dcContact!=null) {
      return dcContext.getRecipient(dcContact);
    }
    else {
      return dcContext.getRecipient(dcContext.getContact(0));
    }
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
          args.putString(FRAGMENT_ADDRESS_EXTRA, getRecipient().getAddress().serialize());
          args.putSerializable(FRAGMENT_LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        case TAB_GALLERY:
          fragment = new ProfileGalleryFragment();
          args.putString(FRAGMENT_ADDRESS_EXTRA, getRecipient().getAddress().serialize());
          args.putSerializable(FRAGMENT_LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        default:
          fragment = new ProfileDocumentsFragment();
          args.putString(FRAGMENT_ADDRESS_EXTRA, getRecipient().getAddress().serialize());
          args.putSerializable(FRAGMENT_LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
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


  // settings fragment
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public static class ProfileSettingsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected RecyclerView recyclerView;

    protected Recipient    recipient;
    protected Locale       locale;

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);

      Locale       locale       = (Locale)getArguments().getSerializable(FRAGMENT_LOCALE_EXTRA);
      if (locale == null)       throw new AssertionError();
      this.locale       = locale;

      String       address      = getArguments().getString(FRAGMENT_ADDRESS_EXTRA);
      if (address == null)      throw new AssertionError();
      this.recipient    = Recipient.from(getContext(), Address.fromSerialized(address));

      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View                  view    = inflater.inflate(R.layout.profile_settings_fragment, container, false);
      ProfileDocumentsAdapter adapter = new ProfileDocumentsAdapter(getContext(), null, locale);

      this.recyclerView  = ViewUtil.findById(view, R.id.recycler_view);

      this.recyclerView.setAdapter(adapter);
      this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
      this.recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, false, true));
      this.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

      return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      return new ThreadMediaLoader(getContext(), recipient.getAddress(), false);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      //((CursorRecyclerViewAdapter)this.recyclerView.getAdapter()).changeCursor(data);
      getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      ((CursorRecyclerViewAdapter)this.recyclerView.getAdapter()).changeCursor(null);
      getActivity().invalidateOptionsMenu();
    }
  }


  // gallery fragment
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public static class ProfileGalleryFragment
      extends Fragment implements LoaderManager.LoaderCallbacks<BucketedThreadMedia>, ProfileGalleryAdapter.ItemClickListener
  {
    protected TextView                    noMedia;
    protected RecyclerView                recyclerView;
    private StickyHeaderGridLayoutManager gridManager;
    private ActionMode                    actionMode;
    private ActionModeCallback            actionModeCallback = new ActionModeCallback();

    protected Recipient    recipient;
    protected Locale       locale;

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);

      Locale       locale       = (Locale)getArguments().getSerializable(FRAGMENT_LOCALE_EXTRA);
      if (locale == null)       throw new AssertionError();
      this.locale       = locale;

      String       address      = getArguments().getString(FRAGMENT_ADDRESS_EXTRA);
      if (address == null)      throw new AssertionError();
      this.recipient    = Recipient.from(getContext(), Address.fromSerialized(address));

      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.profile_gallery_fragment, container, false);

      this.recyclerView = ViewUtil.findById(view, R.id.media_grid);
      this.noMedia      = ViewUtil.findById(view, R.id.no_images);
      this.gridManager  = new StickyHeaderGridLayoutManager(getResources().getInteger(R.integer.profile_cols));

      this.recyclerView.setAdapter(new ProfileGalleryAdapter(getContext(),
                                                           GlideApp.with(this),
                                                           new BucketedThreadMedia(getContext()),
                                                           locale,
                                                           this));
      this.recyclerView.setLayoutManager(gridManager);
      this.recyclerView.setHasFixedSize(true);

      return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      if (gridManager != null) {
        this.gridManager = new StickyHeaderGridLayoutManager(getResources().getInteger(R.integer.profile_cols));
        this.recyclerView.setLayoutManager(gridManager);
      }
    }

    @Override
    public Loader<BucketedThreadMedia> onCreateLoader(int i, Bundle bundle) {
      return new BucketedThreadMediaLoader(getContext(), recipient.getAddress());
    }

    @Override
    public void onLoadFinished(Loader<BucketedThreadMedia> loader, BucketedThreadMedia bucketedThreadMedia) {
      ((ProfileGalleryAdapter) recyclerView.getAdapter()).setMedia(bucketedThreadMedia);
      ((ProfileGalleryAdapter) recyclerView.getAdapter()).notifyAllSectionsDataSetChanged();

      noMedia.setVisibility(recyclerView.getAdapter().getItemCount() > 0 ? View.GONE : View.VISIBLE);
      getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<BucketedThreadMedia> cursorLoader) {
      ((ProfileGalleryAdapter) recyclerView.getAdapter()).setMedia(new BucketedThreadMedia(getContext()));
    }

    @Override
    public void onMediaClicked(@NonNull DcMsg mediaRecord) {
      if (actionMode != null) {
        handleMediaMultiSelectClick(mediaRecord);
      } else {
        handleMediaPreviewClick(mediaRecord);
      }
    }

    private void handleMediaMultiSelectClick(@NonNull DcMsg mediaRecord) {
      ProfileGalleryAdapter adapter = getListAdapter();

      adapter.toggleSelection(mediaRecord);
      if (adapter.getSelectedMediaCount() == 0) {
        actionMode.finish();
        actionMode = null;
      } else {
        actionMode.setTitle(String.valueOf(adapter.getSelectedMediaCount()));
      }
    }

    private void handleMediaPreviewClick(@NonNull DcMsg mediaRecord) {
      if (mediaRecord.getFile() == null) {
        return;
      }

      Context context = getContext();
      if (context == null) {
        return;
      }

      Intent intent = new Intent(context, MediaPreviewActivity.class);
      intent.putExtra(MediaPreviewActivity.DC_MSG_ID, mediaRecord.getId());
      intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, recipient.getAddress());
      intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, mediaRecord.isOutgoing());
      intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, false);
      context.startActivity(intent);
    }

    @Override
    public void onMediaLongClicked(DcMsg mediaRecord) {
      if (actionMode == null) {
        ((ProfileGalleryAdapter) recyclerView.getAdapter()).toggleSelection(mediaRecord);
        recyclerView.getAdapter().notifyDataSetChanged();

        actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
      }
    }

    @SuppressLint("StaticFieldLeak")
    private void handleDeleteMedia(@NonNull Collection<DcMsg> mediaRecords) {
      int recordCount       = mediaRecords.size();
      Resources res         = getContext().getResources();
      String confirmMessage = res.getQuantityString(R.plurals.ask_delete_messages,
                                                    recordCount,
                                                    recordCount);

      AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
      builder.setMessage(confirmMessage);
      builder.setCancelable(true);
      final DcContext dcContext = DcHelper.getContext(getContext());

      builder.setPositiveButton(R.string.delete, (dialogInterface, i) -> {
        new ProgressDialogAsyncTask<DcMsg, Void, Void>(getContext(),
                                                                           R.string.one_moment,
                                                                           R.string.one_moment)
        {
          @Override
          protected Void doInBackground(DcMsg... records) {
            if (records == null || records.length == 0) {
              return null;
            }

            for (DcMsg record : records) {
              dcContext.deleteMsgs(new int[]{record.getId()});
            }
            return null;
          }

        }.execute(mediaRecords.toArray(new DcMsg[mediaRecords.size()]));
      });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.show();
    }

    private ProfileGalleryAdapter getListAdapter() {
      return (ProfileGalleryAdapter) recyclerView.getAdapter();
    }

    private class ActionModeCallback implements ActionMode.Callback {

      private int originalStatusBarColor;

      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.profile_context, menu);
        mode.setTitle("1");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Window window = getActivity().getWindow();
          originalStatusBarColor = window.getStatusBarColor();
          window.setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
        }
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
          case R.id.delete:
            handleDeleteMedia(getListAdapter().getSelectedMedia());
            mode.finish();
            return true;
        }
        return false;
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        getListAdapter().clearSelection();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getActivity().getWindow().setStatusBarColor(originalStatusBarColor);
        }
      }
    }
  }


  // documents fragment
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public static class ProfileDocumentsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected RecyclerView recyclerView;
    protected TextView     noMedia;

    protected Recipient    recipient;
    protected Locale       locale;

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);

      Locale       locale       = (Locale)getArguments().getSerializable(FRAGMENT_LOCALE_EXTRA);
      if (locale == null)       throw new AssertionError();
      this.locale       = locale;

      String       address      = getArguments().getString(FRAGMENT_ADDRESS_EXTRA);
      if (address == null)      throw new AssertionError();
      this.recipient    = Recipient.from(getContext(), Address.fromSerialized(address));

      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View                  view    = inflater.inflate(R.layout.profile_documents_fragment, container, false);
      ProfileDocumentsAdapter adapter = new ProfileDocumentsAdapter(getContext(), null, locale);

      this.recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
      this.noMedia       = ViewUtil.findById(view, R.id.no_documents);

      this.recyclerView.setAdapter(adapter);
      this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
      this.recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, false, true));
      this.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

      return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      return new ThreadMediaLoader(getContext(), recipient.getAddress(), false);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      //((CursorRecyclerViewAdapter)this.recyclerView.getAdapter()).changeCursor(data);
      getActivity().invalidateOptionsMenu();

      // TODO: onLoadFinished() should no take a cursor but forward the loaded messages in a way
      this.noMedia.setVisibility(/*data.getCount() > 0 ? View.GONE :*/ View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      ((CursorRecyclerViewAdapter)this.recyclerView.getAdapter()).changeCursor(null);
      getActivity().invalidateOptionsMenu();
    }
  }

}
