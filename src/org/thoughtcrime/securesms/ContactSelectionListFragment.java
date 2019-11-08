/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.components.RecyclerViewFastScroller;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactAccessor;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.thoughtcrime.securesms.util.RelayUtil.isRelayingMessageContent;

/**
 * Fragment for selecting a one or more contacts from a list.
 *
 * @author Moxie Marlinspike
 *
 */
public class ContactSelectionListFragment extends    Fragment
                                          implements LoaderManager.LoaderCallbacks<DcContactsLoader.Ret>,
                                                     DcEventCenter.DcEventDelegate
{
  @SuppressWarnings("unused")
  private static final String TAG = ContactSelectionListFragment.class.getSimpleName();

  public static final String MULTI_SELECT          = "multi_select";
  public static final String REFRESHABLE           = "refreshable";
  public static final String RECENTS               = "recents";
  public static final String SELECT_VERIFIED_EXTRA = "select_verified";
  public static final String FROM_SHARE_ACTIVITY_EXTRA = "from_share_activity";
  public static final String PRESELECTED_CONTACTS = "preselected_contacts";

  private ApplicationDcContext dcContext;

  private TextView                  emptyText;
  private Set<String>               selectedContacts;
  private OnContactSelectedListener onContactSelectedListener;
  private SwipeRefreshLayout        swipeRefresh;
  private String                    cursorFilter;
  private RecyclerView              recyclerView;
  private StickyHeaderDecoration    listDecoration;
  private RecyclerViewFastScroller  fastScroller;
  private ActionMode                actionMode;
  private ActionMode.Callback       actionModeCallback;

  @Override
  public void onActivityCreated(Bundle icicle) {
    super.onActivityCreated(icicle);

    dcContext = DcHelper.getContext(getActivity());
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_CONTACTS_CHANGED);
    initializeCursor();
  }

  @Override
  public void onDestroy() {
    DcHelper.getContext(getActivity()).eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onStart() {
    super.onStart();
    this.getLoaderManager().initLoader(0, null, this);
    Permissions.with(this)
               .request(Manifest.permission.READ_CONTACTS)
               .ifNecessary()
               .onAllGranted(this::handleContactPermissionGranted)
               .execute();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);

    emptyText               = ViewUtil.findById(view, android.R.id.empty);
    recyclerView            = ViewUtil.findById(view, R.id.recycler_view);
    swipeRefresh            = ViewUtil.findById(view, R.id.swipe_refresh);
    fastScroller            = ViewUtil.findById(view, R.id.fast_scroller);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    actionModeCallback = new ActionMode.Callback() {
      @Override
      public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.contact_list, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
        }
        actionMode.setTitle("1");
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
          case R.id.menu_select_all:
            handleSelectAll();
            return true;
          case R.id.menu_delete_selected:
            handleDeleteSelected();
            return true;
        }
        return false;
      }

      @Override
      public void onDestroyActionMode(ActionMode actionMode) {
        ContactSelectionListFragment.this.actionMode = null;
        getContactSelectionListAdapter().resetActionModeSelection();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          TypedArray color = getActivity().getTheme().obtainStyledAttributes(new int[] {android.R.attr.statusBarColor});
          getActivity().getWindow().setStatusBarColor(color.getColor(0, Color.BLACK));
          color.recycle();
        }
      }
    };

    // There shouldn't be the need to pull to refresh the contacts
    // swipeRefresh.setEnabled(getActivity().getIntent().getBooleanExtra(REFRESHABLE, true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);
    swipeRefresh.setEnabled(false);

    return view;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    listDecoration.onConfigurationChanged(newConfig);
  }

  private void handleSelectAll() {
    getContactSelectionListAdapter().selectAll();
    updateActionModeTitle();
  }

  private void updateActionModeTitle() {
    actionMode.setTitle(String.valueOf(getContactSelectionListAdapter().getActionModeSelection().size()));
  }

  private void handleDeleteSelected() {
    new AlertDialog.Builder(getActivity())
      .setMessage(R.string.ask_delete_contacts)
      .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
          ContactSelectionListAdapter adapter = getContactSelectionListAdapter();
          final SparseIntArray actionModeSelection = adapter.getActionModeSelection().clone();
          new Thread(() -> {
            boolean failed = false;
            for (int index = 0; index < actionModeSelection.size(); index++) {
              int contactId = actionModeSelection.valueAt(index);
              boolean currentFailed = !dcContext.deleteContact(contactId);
              failed = currentFailed || failed;
            }
            if (failed) {
              Util.runOnMain(()-> {
                Toast.makeText(getActivity(), R.string.cannot_delete_contacts_in_use, Toast.LENGTH_LONG).show();
              });
            }
          }).start();
          adapter.resetActionModeSelection();
          actionMode.finish();
          })
      .setNegativeButton(R.string.cancel, null)
      .show();
  }

  private ContactSelectionListAdapter getContactSelectionListAdapter() {
    return (ContactSelectionListAdapter) recyclerView.getAdapter();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  public @NonNull List<String> getSelectedContacts() {
    List<String> selected = new LinkedList<>();
    if (selectedContacts != null) {
      selected.addAll(selectedContacts);
    }

    return selected;
  }

  private boolean isMulti() {
    return getActivity().getIntent().getBooleanExtra(MULTI_SELECT, false);
  }

  private boolean isSelectVerfied() {
    return getActivity().getIntent().getBooleanExtra(SELECT_VERIFIED_EXTRA, false);
  }

  private boolean isFromShareActivity() {
    return getActivity().getIntent().getBooleanExtra(FROM_SHARE_ACTIVITY_EXTRA, false);
  }

  private void initializeCursor() {
    ContactSelectionListAdapter adapter = new ContactSelectionListAdapter(getActivity(),
            GlideApp.with(this),
            new ListClickListener(),
            isMulti(),
            true);
    selectedContacts = adapter.getSelectedContacts();
    ArrayList<String> preselectedContacts = getActivity().getIntent().getStringArrayListExtra(PRESELECTED_CONTACTS);
    if(preselectedContacts!=null) {
      selectedContacts.addAll(preselectedContacts);
    }
    recyclerView.setAdapter(adapter);
    listDecoration = new StickyHeaderDecoration(adapter, true, true);
    recyclerView.addItemDecoration(listDecoration);
  }

  public void setQueryFilter(String filter) {
    this.cursorFilter = filter;
    this.getLoaderManager().restartLoader(0, null, this);
  }

  public void resetQueryFilter() {
    setQueryFilter(null);
    swipeRefresh.setRefreshing(false);
  }

  public void setRefreshing(boolean refreshing) {
    swipeRefresh.setRefreshing(refreshing);
  }

  public void reset() {
    selectedContacts.clear();

    if (!isDetached() && !isRemoving() && getActivity() != null && !getActivity().isFinishing()) {
      getLoaderManager().restartLoader(0, null, this);
    }
  }

  @Override
  public Loader<DcContactsLoader.Ret> onCreateLoader(int id, Bundle args) {
    boolean addCreateGroupLinks = isFromShareActivity() || isRelayingMessageContent(getActivity()) ? false : !isMulti();
    int listflags = DcContext.DC_GCL_ADD_SELF;
    if(isSelectVerfied()) {
      listflags = DcContext.DC_GCL_VERIFIED_ONLY;
    }
    return new DcContactsLoader(getActivity(), listflags, cursorFilter, addCreateGroupLinks, false);
  }

  @Override
  public void onLoadFinished(Loader<DcContactsLoader.Ret> loader, DcContactsLoader.Ret data) {
    ((ContactSelectionListAdapter) recyclerView.getAdapter()).changeData(data);
    emptyText.setText(R.string.contacts_empty_hint);
    boolean useFastScroller = (recyclerView.getAdapter().getItemCount() > 20);
    recyclerView.setVerticalScrollBarEnabled(!useFastScroller);
    if (useFastScroller) {
      fastScroller.setVisibility(View.VISIBLE);
      fastScroller.setRecyclerView(recyclerView);
    }
  }

  @Override
  public void onLoaderReset(Loader<DcContactsLoader.Ret> loader) {
    ((ContactSelectionListAdapter) recyclerView.getAdapter()).changeData(null);
    fastScroller.setVisibility(View.GONE);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleContactPermissionGranted() {
    new AsyncTask<Void, Void, Void>() {

      @Override
      protected Void doInBackground(Void... voids) {
        loadSystemContacts();
        return null;
      }

    }.execute();
  }

  private void loadSystemContacts() {
    Thread thread = new Thread() {
      @Override
      public void run() {
        ContactAccessor contactAccessor = ContactAccessor.getInstance();
        String allSystemContacts = contactAccessor.getAllSystemContactsAsString(getContext());
        if (!allSystemContacts.isEmpty()) {
          dcContext.addAddressBook(allSystemContacts);
        }
      }
    };
    thread.start();
  }

  private class ListClickListener implements ContactSelectionListAdapter.ItemClickListener {
    @Override
    public void onItemClick(ContactSelectionListItem contact, boolean handleActionMode)
    {
      if (handleActionMode) {
        if (actionMode != null) {
          updateActionModeTitle();
          finishActionModeIfSelectionIsEmpty();
        }
        return;
      }
      int    specialId = contact.getSpecialId();
      String addr      = contact.getNumber();
      if (!isMulti() || !selectedContacts.contains(addr))
      {
        if (isMulti()
         && specialId== DcContact.DC_CONTACT_ID_NEW_CONTACT
         && dcContext.lookupContactIdByAddr(addr)==0) {
          if (dcContext.createContact(null, addr)==0) {
            Toast.makeText(getActivity(), R.string.bad_email_address, Toast.LENGTH_LONG).show();
            return;
          }
        }

        selectedContacts.add(addr);
        contact.setChecked(true);
        if (onContactSelectedListener != null) {
          onContactSelectedListener.onContactSelected(specialId, addr);
        }

        if(isMulti() && specialId==DcContact.DC_CONTACT_ID_NEW_CONTACT) {
          // do not check the "add contact" entry but add a new contact and check this. a reload makes this visible.
          getLoaderManager().restartLoader(0, null, ContactSelectionListFragment.this);
        }
      }
      else
      {
        selectedContacts.remove(addr);
        contact.setChecked(false);
        if (onContactSelectedListener != null) {
          onContactSelectedListener.onContactDeselected(specialId, addr);
        }
      }
    }

    @Override
    public void onItemLongClick(ContactSelectionListItem view) {
      if (actionMode == null) {
        actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
      } else {
          finishActionModeIfSelectionIsEmpty();
      }
    }
  }

    private void finishActionModeIfSelectionIsEmpty() {
        if (getContactSelectionListAdapter().getActionModeSelection().size() == 0) {
            actionMode.finish();
        }
    }

    public void setOnContactSelectedListener(OnContactSelectedListener onContactSelectedListener) {
    this.onContactSelectedListener = onContactSelectedListener;
  }

  public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener onRefreshListener) {
    this.swipeRefresh.setOnRefreshListener(onRefreshListener);
  }

  public interface OnContactSelectedListener {
    void onContactSelected(int specialId, String number);
    void onContactDeselected(int specialId, String number);
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    if (eventId==DcContext.DC_EVENT_CONTACTS_CHANGED) {
      getLoaderManager().restartLoader(0, null, ContactSelectionListFragment.this);
    }
  }
}
