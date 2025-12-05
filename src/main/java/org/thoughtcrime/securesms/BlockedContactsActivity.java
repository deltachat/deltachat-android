package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;

public class BlockedContactsActivity extends PassphraseRequiredActionBarActivity {

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    setContentView(R.layout.activity_blocked_contacts);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.pref_blocked_contacts);
    initFragment(R.id.fragment, new BlockedAndShareContactsFragment(), getIntent().getExtras());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  public static class BlockedAndShareContactsFragment
          extends Fragment
          implements LoaderManager.LoaderCallbacks<DcContactsLoader.Ret>,
          DcEventCenter.DcEventDelegate, ContactSelectionListAdapter.ItemClickListener {


    private RecyclerView recyclerView;
    private TextView emptyStateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
      View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);
      recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
      recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

      // add padding to avoid content hidden behind system bars
      ViewUtil.applyWindowInsets(recyclerView);

      emptyStateView = ViewUtil.findById(view, android.R.id.empty);
      emptyStateView.setText(R.string.blocked_empty_hint);
      return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
      super.onActivityCreated(bundle);
      initializeAdapter();
    }

    private void initializeAdapter() {
      ContactSelectionListAdapter adapter = new ContactSelectionListAdapter(getActivity(),
              GlideApp.with(this),
              this,
              false,
              false);
      recyclerView.setAdapter(adapter);
    }

    @Override
    public Loader<DcContactsLoader.Ret> onCreateLoader(int id, Bundle args) {
      return new DcContactsLoader(getActivity(), -1, null, false, false, false, true);
    }

    @Override
    public void onLoadFinished(Loader<DcContactsLoader.Ret> loader, DcContactsLoader.Ret data) {
      ContactSelectionListAdapter adapter = getContactSelectionListAdapter();
      if (adapter != null) {
        adapter.changeData(data);
        if (emptyStateView != null) {
          emptyStateView.setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
      }
    }

    @Override
    public void onLoaderReset(Loader<DcContactsLoader.Ret> loader) {
      getContactSelectionListAdapter().changeData(null);
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
      if (event.getId()==DcContext.DC_EVENT_CONTACTS_CHANGED) {
        restartLoader();
      }
    }

    private void restartLoader() {
      getLoaderManager().restartLoader(0, null, BlockedAndShareContactsFragment.this);
    }

    private ContactSelectionListAdapter getContactSelectionListAdapter() {
      return (ContactSelectionListAdapter) recyclerView.getAdapter();
    }

    @Override
    public void onItemClick(ContactSelectionListItem item, boolean handleActionMode) {
      new AlertDialog.Builder(getActivity())
              .setMessage(R.string.ask_unblock_contact)
              .setCancelable(true)
              .setNegativeButton(android.R.string.cancel, null)
              .setPositiveButton(R.string.menu_unblock_contact, (dialog, which) -> unblockContact(item.getContactId())).show();
    }

    private void unblockContact(int contactId) {
      DcContext dcContext = DcHelper.getContext(getContext());
      dcContext.blockContact(contactId, 0);
      restartLoader();
    }

    @Override
    public void onItemLongClick(ContactSelectionListItem view) {}
  }

}
