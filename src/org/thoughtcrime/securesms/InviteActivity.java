package org.thoughtcrime.securesms;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.ViewUtil;

public class InviteActivity extends PassphraseRequiredActionBarActivity {

  private EditText                     inviteText;
  private Animation                    slideInAnimation;
  private Animation                    slideOutAnimation;
  private ImageView                    heart;

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
    getIntent().putExtra(ContactSelectionListFragment.REFRESHABLE, false);

    setContentView(R.layout.invite_activity);
    assert getSupportActionBar() != null;
    getSupportActionBar().setTitle(R.string.AndroidManifest__invite_friends);

    initializeResources();
  }

  private void initializeResources() {
    slideInAnimation  = loadAnimation(R.anim.slide_from_bottom);
    slideOutAnimation = loadAnimation(R.anim.slide_to_bottom);

    View                 shareButton     = ViewUtil.findById(this, R.id.share_button);

    inviteText        = ViewUtil.findById(this, R.id.invite_text);
    heart             = ViewUtil.findById(this, R.id.heart);

    String selfMail = DcHelper.getContext(this).getConfig("addr", "");
    inviteText.setText(getString(R.string.InviteActivity_lets_switch_to_delta_chat, "https://delta.chat", selfMail));

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      heart.getViewTreeObserver().addOnPreDrawListener(new HeartPreDrawListener());
    }
    shareButton.setOnClickListener(new ShareClickListener());
  }

  private Animation loadAnimation(@AnimRes int animResId) {
    final Animation animation = AnimationUtils.loadAnimation(this, animResId);
    animation.setInterpolator(new FastOutSlowInInterpolator());
    return animation;
  }

  private class ShareClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      Intent sendIntent = new Intent();
      sendIntent.setAction(Intent.ACTION_SEND);
      sendIntent.putExtra(Intent.EXTRA_TEXT, inviteText.getText().toString());
      sendIntent.setType("text/plain");
      if (sendIntent.resolveActivity(getPackageManager()) != null) {
        startActivity(Intent.createChooser(sendIntent, getString(R.string.InviteActivity_invite_to_signal)));
      } else {
        Toast.makeText(InviteActivity.this, R.string.InviteActivity_no_app_to_share_to, Toast.LENGTH_LONG).show();
      }
    }
  }

  private class HeartPreDrawListener implements OnPreDrawListener {
    @Override
    @TargetApi(VERSION_CODES.LOLLIPOP)
    public boolean onPreDraw() {
      heart.getViewTreeObserver().removeOnPreDrawListener(this);
      final int w = heart.getWidth();
      final int h = heart.getHeight();
      Animator reveal = ViewAnimationUtils.createCircularReveal(heart,
                                                                w / 2, h,
                                                                0, (float)Math.sqrt(h*h + (w*w/4)));
      reveal.setInterpolator(new FastOutSlowInInterpolator());
      reveal.setDuration(800);
      reveal.start();
      return false;
    }
  }
}
