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


import static org.thoughtcrime.securesms.util.ShareUtil.isRelayingMessageContent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.contacts.NewContactActivity;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
  private static final String TAG = ContactSelectionListFragment.class.getSimpleName();

  public static final String MULTI_SELECT          = "multi_select";
  public static final String SELECT_UNENCRYPTED_EXTRA = "select_unencrypted_extra";
  public static final String ALLOW_CREATION = "allow_creation";
  public static final String PRESELECTED_CONTACTS = "preselected_contacts";
  public static final int CONTACT_ADDR_RESULT_CODE = 61123;

  private DcContext dcContext;

  private Set<Integer>              selectedContacts;
  private OnContactSelectedListener onContactSelectedListener;
  private String                    cursorFilter;
  private RecyclerView              recyclerView;
  private ActionMode                actionMode;
  private ActionMode.Callback       actionModeCallback;

  @Override
  public void onActivityCreated(Bundle icicle) {
    super.onActivityCreated(icicle);

    dcContext = DcHelper.getContext(getActivity());
    DcHelper.getEventCenter(getActivity()).addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    initializeCursor();
  }

  @Override
  public void onDestroy() {
    DcHelper.getEventCenter(getActivity()).removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onStart() {
    super.onStart();
    this.getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);

    recyclerView            = ViewUtil.findById(view, R.id.recycler_view);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(recyclerView, true, false, true, true);

    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    actionModeCallback = new ActionMode.Callback() {
      @Override
      public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.contact_list, menu);
        setCorrectMenuVisibility(menu);
        actionMode.setTitle("1");
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_select_all) {
          handleSelectAll();
          return true;
        } else if (itemId == R.id.menu_view_profile) {
          handleViewProfile();
          return true;
        } else if (itemId == R.id.menu_delete_selected) {
          handleDeleteSelected();
          return true;
        }
        return false;
      }

      @Override
      public void onDestroyActionMode(ActionMode actionMode) {
        ContactSelectionListFragment.this.actionMode = null;
        getContactSelectionListAdapter().resetActionModeSelection();
      }
    };

    return view;
  }

  private void handleSelectAll() {
    getContactSelectionListAdapter().selectAll();
    updateActionModeTitle();
  }

  private void updateActionModeTitle() {
    actionMode.setTitle(String.valueOf(getContactSelectionListAdapter().getActionModeSelection().size()));
  }

  private void setCorrectMenuVisibility(Menu menu) {
    ContactSelectionListAdapter adapter = getContactSelectionListAdapter();
    if (adapter.getActionModeSelection().size() > 1) {
      menu.findItem(R.id.menu_view_profile).setVisible(false);
    } else {
      menu.findItem(R.id.menu_view_profile).setVisible(true);
    }
  }

  private void handleViewProfile() {
    ContactSelectionListAdapter adapter = getContactSelectionListAdapter();
    if (adapter.getActionModeSelection().size() == 1) {
      int contactId = adapter.getActionModeSelection().valueAt(0);

      Intent intent = new Intent(getContext(), ProfileActivity.class);
      intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, contactId);
      getContext().startActivity(intent);
    }
  }

  private void handleDeleteSelected() {
    AlertDialog dialog = new AlertDialog.Builder(getActivity())
      .setMessage(R.string.ask_delete_contacts)
      .setPositiveButton(R.string.delete, (d, i) -> {
          ContactSelectionListAdapter adapter = getContactSelectionListAdapter();
          final SparseIntArray actionModeSelection = adapter.getActionModeSelection().clone();
          new Thread(() -> {
            for (int index = 0; index < actionModeSelection.size(); index++) {
              int contactId = actionModeSelection.valueAt(index);
              dcContext.deleteContact(contactId);
            }
          }).start();
          adapter.resetActionModeSelection();
          actionMode.finish();
          })
      .setNegativeButton(R.string.cancel, null)
      .show();
    Util.redPositiveButton(dialog);
  }

  private ContactSelectionListAdapter getContactSelectionListAdapter() {
    return (ContactSelectionListAdapter) recyclerView.getAdapter();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  public @NonNull List<Integer> getSelectedContacts() {
    List<Integer> selected = new LinkedList<>();
    if (selectedContacts != null) {
      selected.addAll(selectedContacts);
    }

    return selected;
  }

  private boolean isMulti() {
    return getActivity().getIntent().getBooleanExtra(MULTI_SELECT, false);
  }

  private boolean isUnencrypted() {
    return getActivity().getIntent().getBooleanExtra(SELECT_UNENCRYPTED_EXTRA, false);
  }

  private void initializeCursor() {
    ContactSelectionListAdapter adapter = new ContactSelectionListAdapter(getActivity(),
            GlideApp.with(this),
            new ListClickListener(),
            isMulti(),
            true);
    selectedContacts = adapter.getSelectedContacts();
    ArrayList<Integer> preselectedContacts = getActivity().getIntent().getIntegerArrayListExtra(PRESELECTED_CONTACTS);
    if(preselectedContacts!=null) {
      selectedContacts.addAll(preselectedContacts);
    }
    recyclerView.setAdapter(adapter);
  }

  public void setQueryFilter(String filter) {
    this.cursorFilter = filter;
    this.getLoaderManager().restartLoader(0, null, this);
  }

  @Override
  public Loader<DcContactsLoader.Ret> onCreateLoader(int id, Bundle args) {
    final boolean allowCreation = getActivity().getIntent().getBooleanExtra(ALLOW_CREATION, true);
    final boolean addCreateContactLink = allowCreation && isUnencrypted();
    final boolean addCreateGroupLinks = allowCreation && !isRelayingMessageContent(getActivity()) && !isMulti();
    final boolean addScanQRLink = allowCreation && !isMulti();

    final int listflags = DcContext.DC_GCL_ADD_SELF | (isUnencrypted()? DcContext.DC_GCL_ADDRESS : 0);
    return new DcContactsLoader(getActivity(), listflags, cursorFilter, addCreateGroupLinks, addCreateContactLink, addScanQRLink, false);
  }

  @Override
  public void onLoadFinished(Loader<DcContactsLoader.Ret> loader, DcContactsLoader.Ret data) {
    ((ContactSelectionListAdapter) recyclerView.getAdapter()).changeData(data);
  }

  @Override
  public void onLoaderReset(Loader<DcContactsLoader.Ret> loader) {
    ((ContactSelectionListAdapter) recyclerView.getAdapter()).changeData(null);
  }

  private class ListClickListener implements ContactSelectionListAdapter.ItemClickListener {
    @Override
    public void onItemClick(ContactSelectionListItem contact, boolean handleActionMode)
    {
      if (handleActionMode) {
        if (actionMode != null) {
          Menu menu = actionMode.getMenu();
          setCorrectMenuVisibility(menu);
          updateActionModeTitle();
          finishActionModeIfSelectionIsEmpty();
        }
        return;
      }
      int    contactId = contact.getSpecialId();
      if (!isMulti() || !selectedContacts.contains(contactId)) {
        if (contactId == DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT) {
          Intent intent = new Intent(getContext(), NewContactActivity.class);
          if (dcContext.mayBeValidAddr(cursorFilter)) {
            intent.putExtra(NewContactActivity.ADDR_EXTRA, cursorFilter);
          }
          if (isMulti()) {
            startActivityForResult(intent, CONTACT_ADDR_RESULT_CODE);
          } else {
            requireContext().startActivity(intent);
          }
          return;
        }

        selectedContacts.add(contactId);
        contact.setChecked(true);
        if (onContactSelectedListener != null) {
          onContactSelectedListener.onContactSelected(contactId);
        }
      } else {
        selectedContacts.remove(contactId);
        contact.setChecked(false);
        if (onContactSelectedListener != null) {
          onContactSelectedListener.onContactDeselected(contactId);
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

  public interface OnContactSelectedListener {
    void onContactSelected(int contactId);
    void onContactDeselected(int contactId);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (event.getId()==DcContext.DC_EVENT_CONTACTS_CHANGED) {
      getLoaderManager().restartLoader(0, null, ContactSelectionListFragment.this);
    }
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && reqCode == CONTACT_ADDR_RESULT_CODE) {
      int contactId = data.getIntExtra(NewContactActivity.CONTACT_ID_EXTRA, 0);
      if (contactId != 0) {
        selectedContacts.add(contactId);
      }
      getLoaderManager().restartLoader(0, null, ContactSelectionListFragment.this);
    }
  }
}
