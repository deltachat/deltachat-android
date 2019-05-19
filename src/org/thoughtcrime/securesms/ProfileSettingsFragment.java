package org.thoughtcrime.securesms;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.CursorRecyclerViewAdapter;
import org.thoughtcrime.securesms.database.loaders.ThreadMediaLoader;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;

public class ProfileSettingsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String ADDRESS_EXTRA = "address";

  protected RecyclerView recyclerView;

  protected Recipient recipient;
  protected Locale locale;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    Locale       locale       = (Locale)getArguments().getSerializable(LOCALE_EXTRA);
    if (locale == null)       throw new AssertionError();
    this.locale       = locale;

    String       address      = getArguments().getString(ADDRESS_EXTRA);
    if (address == null)      throw new AssertionError();
    this.recipient    = Recipient.from(getContext(), Address.fromSerialized(address));

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View                  view    = inflater.inflate(R.layout.profile_settings_fragment, container, false);
    //ProfileDocumentsAdapter adapter = new ProfileDocumentsAdapter(getContext(), null, locale);

    this.recyclerView  = ViewUtil.findById(view, R.id.recycler_view);

    //this.recyclerView.setAdapter(adapter);
    this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    //this.recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, false, true));
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
