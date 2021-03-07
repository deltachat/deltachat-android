package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcContactsLoader;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter;
import org.thoughtcrime.securesms.contacts.ContactSelectionListItem;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Objects;

public class BlockedAndShareContactsActivity extends PassphraseRequiredActionBarActivity {

  public static final String SHOW_ONLY_BLOCKED_EXTRA = "show_only_blocked";
  public static final String SHARE_CONTACT_NAME_EXTRA = "share_contact_name";
  public static final String SHARE_CONTACT_MAIL_EXTRA = "share_contact_mail";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  public void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    boolean showOnlyBlocked = getIntent().getBooleanExtra(SHOW_ONLY_BLOCKED_EXTRA, false);
    getSupportActionBar().setTitle(showOnlyBlocked ? R.string.pref_blocked_contacts : R.string.contacts_headline);
    initFragment(android.R.id.content, new BlockedAndShareContactsFragment(), null, getIntent().getExtras());
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
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

    private boolean showOnlyBlocked;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
      View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);
      recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
      recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
      emptyStateView = ViewUtil.findById(view, android.R.id.empty);
      emptyStateView.setText(R.string.blocked_empty_hint);
      return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      showOnlyBlocked = getArguments().getBoolean(SHOW_ONLY_BLOCKED_EXTRA, false);
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
      return new DcContactsLoader(getActivity(), showOnlyBlocked ? -1 : DcContext.DC_GCL_ADD_SELF, null, false, showOnlyBlocked);
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
    public void handleEvent(DcEvent event) {
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
      if(showOnlyBlocked) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.ask_unblock_contact)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.menu_unblock_contact, (dialog, which) -> unblockContact(item.getContactId())).show();
      } else {
        shareContact(item.getName(), item.getNumber());
      }
    }

    private void unblockContact(int contactId) {
      ApplicationDcContext dcContext = DcHelper.getContext(getContext());
      dcContext.blockContact(contactId, 0);
      restartLoader();
    }

    private void shareContact(String name, String mail) {
      Intent intent = new Intent();
      intent.putExtra(BlockedAndShareContactsActivity.SHARE_CONTACT_NAME_EXTRA, name);
      intent.putExtra(BlockedAndShareContactsActivity.SHARE_CONTACT_MAIL_EXTRA, mail);
      FragmentActivity activity = Objects.requireNonNull(getActivity());
      activity.setResult(RESULT_OK, intent);
      activity.finish();
    }

    @Override
    public void onItemLongClick(ContactSelectionListItem view) {
      // Not needed
    }
  }

}
