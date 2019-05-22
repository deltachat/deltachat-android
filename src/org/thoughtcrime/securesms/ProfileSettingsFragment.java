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
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
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
    adapter = new ProfileSettingsAdapter(getContext(), GlideApp.with(this), locale,null);

    recyclerView  = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, false, true));

    update();

    return view;
  }

  private void update()
  {
    int[]      memberList = null;
    DcChatlist sharedChats = null;

    if(dcChat!=null && dcChat.isGroup()) {
      memberList = dcContext.getChatContacts(chatId);
    }
    else if(contactId>0) {
      sharedChats = dcContext.getChatlist(0, null, contactId);
    }


    adapter.changeData(memberList, dcContact, sharedChats, dcChat);
  }
}
