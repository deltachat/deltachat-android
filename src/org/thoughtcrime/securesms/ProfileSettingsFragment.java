package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Locale;

public class ProfileSettingsFragment extends Fragment {

  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";

  private RecyclerView           recyclerView;
  private ProfileSettingsAdapter adapter;

  private Locale               locale;
  private ApplicationDcContext dcContext;
  protected int                chatId;
  private DcChat               dcChat;
  private int                  contactId;
  private DcContact            dcContact;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    locale = (Locale)getArguments().getSerializable(LOCALE_EXTRA);
    if (locale == null) throw new AssertionError();
    chatId = getArguments().getInt(CHAT_ID_EXTRA, -1);
    contactId = getArguments().getInt(CONTACT_ID_EXTRA, -1);
    dcContext = DcHelper.getContext(getContext());

    // if given, the ids really belong together, this is checked in ProfileActivity
    if (contactId>0) { dcContact = dcContext.getContact(contactId); }
    if (chatId>0)    { dcChat    = dcContext.getChat(chatId); }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.profile_settings_fragment, container, false);
    adapter = new ProfileSettingsAdapter(getContext(), GlideApp.with(this), null, false);

    recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    //this.recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, false, true));
    //recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

    if (chatId>0) {
      adapter.changeData(dcContext.getChatContacts(chatId));
    }

    return view;
  }
}
