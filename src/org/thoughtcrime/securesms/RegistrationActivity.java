package org.thoughtcrime.securesms;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.thoughtcrime.securesms.util.views.ProgressDialog;
import org.w3c.dom.Text;

import java.util.concurrent.ExecutionException;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ADDRESS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_USER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_USER;

public class RegistrationActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

    private enum VerificationType {
        EMAIL,
        SERVER,
        PORT,
    }

    private final DynamicTheme dynamicTheme    = new DynamicTheme();

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Group advancedGroup;
    private ImageView advancedIcon;
    private ProgressDialog progressDialog;
    private boolean gmailDialogShown;

    MenuItem loginMenuItem;
    Spinner imapSecurity;
    Spinner smtpSecurity;
    Spinner authMethod;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        dynamicTheme.onCreate(this);

        setContentView(R.layout.registration_activity);

        emailInput = findViewById(R.id.email_text);
        passwordInput = findViewById(R.id.password_text);
        advancedGroup = findViewById(R.id.advanced_group);
        advancedIcon = findViewById(R.id.advanced_icon);
        TextView advancedTextView = findViewById(R.id.advanced_text);
        TextInputEditText imapServerInput = findViewById(R.id.imap_server_text);
        TextInputEditText imapPortInput = findViewById(R.id.imap_port_text);
        TextInputEditText smtpServerInput = findViewById(R.id.smtp_server_text);
        TextInputEditText smtpPortInput = findViewById(R.id.smtp_port_text);
        TextView viewLogText = findViewById(R.id.view_log_button);

        imapSecurity = findViewById(R.id.imap_security);
        smtpSecurity = findViewById(R.id.smtp_security);
        authMethod = findViewById(R.id.auth_method);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.login_header);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        emailInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.EMAIL));
        imapServerInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.SERVER));
        imapPortInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.PORT));
        smtpServerInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.SERVER));
        smtpPortInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.PORT));
        advancedTextView.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setRotation(45);
        viewLogText.setOnClickListener((view) -> showLog());
        boolean isConfigured = DcHelper.isConfigured(getApplicationContext());
        if (isConfigured) {
            TextInputEditText imapLoginInput = findViewById(R.id.imap_login_text);

            String email = DcHelper.get(this, CONFIG_ADDRESS);
            emailInput.setText(email);
            if(!TextUtils.isEmpty(email)) {
                emailInput.setSelection(email.length(), email.length());
            }

            passwordInput.setText(DcHelper.get(this, CONFIG_MAIL_PASSWORD));
            imapLoginInput.setText(DcHelper.get(this, CONFIG_MAIL_USER));
            imapServerInput.setText(DcHelper.get(this, CONFIG_MAIL_SERVER));
            imapPortInput.setText(DcHelper.get(this, CONFIG_MAIL_PORT));
            TextInputEditText smtpLoginInput = findViewById(R.id.smtp_login_text);
            TextInputEditText smtpPasswordInput = findViewById(R.id.smtp_password_text);
            smtpLoginInput.setText(DcHelper.get(this, CONFIG_SEND_USER));
            smtpPasswordInput.setText(DcHelper.get(this, CONFIG_SEND_PASSWORD));
            smtpServerInput.setText(DcHelper.get(this, CONFIG_SEND_SERVER));
            smtpPortInput.setText(DcHelper.get(this, CONFIG_SEND_PORT));

            int server_flags = DcHelper.getInt(this, "server_flags", 0);

            int sel = 0;
            if((server_flags&DcContext.DC_LP_IMAP_SOCKET_SSL)!=0) sel = 1;
            if((server_flags&DcContext.DC_LP_IMAP_SOCKET_STARTTLS)!=0) sel = 2;
            if((server_flags&DcContext.DC_LP_IMAP_SOCKET_PLAIN)!=0) sel = 3;
            imapSecurity.setSelection(sel);

            sel = 0;
            if((server_flags&DcContext.DC_LP_SMTP_SOCKET_SSL)!=0) sel = 1;
            if((server_flags&DcContext.DC_LP_SMTP_SOCKET_STARTTLS)!=0) sel = 2;
            if((server_flags&DcContext.DC_LP_SMTP_SOCKET_PLAIN)!=0) sel = 3;
            smtpSecurity.setSelection(sel);

            sel = 0;
            if((server_flags&DcContext.DC_LP_AUTH_OAUTH2)!=0) sel = 1;
            authMethod.setSelection(sel);
        }

        DcHelper.getContext(this).eventCenter.addObserver(this, DcContext.DC_EVENT_CONFIGURE_PROGRESS);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
    }

    private void showLog() {
        Intent intent = new Intent(getApplicationContext(), LogViewActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.registration, menu);
        loginMenuItem = menu.findItem(R.id.do_register);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.do_register) {
            checkOauth2start().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean oauth2started) {
                    if(!oauth2started) {
                        onLogin();
                    }
                }

                @Override
                public void onFailure(ExecutionException e) {
                    onLogin();
                }
            });
            return true;
        } else if (id == android.R.id.home) {
            // handle close button click here
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DcHelper.getContext(this).eventCenter.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void focusListener(View view, boolean focused, VerificationType type) {
        if (!focused) {
            TextInputEditText inputEditText = (TextInputEditText) view;
            switch (type) {
                case EMAIL:
                    verifyEmail(inputEditText);
                    checkOauth2start();
                    break;
                case SERVER:
                    verifyServer(inputEditText);
                    break;
                case PORT:
                    verifyPort(inputEditText);
                    break;
            }
        }
    }

    private long oauth2Requested = 0;

    private ListenableFuture<Boolean> checkOauth2start() {
        SettableFuture<Boolean> oauth2started = new SettableFuture<>();

        String email = emailInput.getText().toString();
        if (!TextUtils.isEmpty(email) ) {

            // the redirect-uri is also used as intent-filter in the manifest
            // and should be whitelisted by the supported oauth2 services
            String redirectUrl = "chat.delta:/"+BuildConfig.APPLICATION_ID+"/auth";

            String oauth2url = DcHelper.getContext(this).getOauth2Url(email, redirectUrl);
            if (!TextUtils.isEmpty(oauth2url)) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.login_info_oauth2_title)
                    .setMessage(R.string.login_info_oauth2_text)
                    .setNegativeButton(R.string.cancel, (dialog, which)->{
                        if(isGmail(email)) {
                            showGmailNoOauth2Hint();
                        }
                        oauth2started.set(false);
                    })
                    .setPositiveButton(R.string.perm_continue, (dialog, which)-> {
                        // pass control to browser, we'll be back in business at (**)
                        oauth2Requested = System.currentTimeMillis();
                        IntentUtils.showBrowserIntent(this, oauth2url);
                        oauth2started.set(true);
                    })
                    .setCancelable(false)
                    .show();
            } else if (isGmail(email)) {
                showGmailNoOauth2Hint();
                oauth2started.set(false);
            } else if (isOutlook(email)) {
                showOutlookHint();
                oauth2started.set(false);
            }
            else {
                oauth2started.set(false);
            }
        }
        else {
            oauth2started.set(false);
        }

        return oauth2started;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String path = uri.getPath();
            if(!(path.startsWith("/"+BuildConfig.APPLICATION_ID)||path.startsWith("/auth"))
             || System.currentTimeMillis()-oauth2Requested > 3*60*60*1000) {
                return; // timeout after some hours or a request belonging to a bad path.
            }

            // back in business after we passed control to the browser in (**)
            String code = uri.getQueryParameter("code");
            if(!TextUtils.isEmpty(code)) {
                passwordInput.setText(code);
                authMethod.setSelection(1/*OAuth2*/);
                onLogin();
            }
        }
    }

    private boolean matchesEmailPattern(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isGmail(String email) {
        return email != null && (email.toLowerCase().contains("@gmail.") || email.toLowerCase().contains("@googlemail."));
    }

    private void showGmailNoOauth2Hint()
    {
        if(!gmailDialogShown) {
            gmailDialogShown = true;
            new AlertDialog.Builder(this)
                .setMessage(R.string.login_info_gmail_text)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    private boolean isOutlook(String email) {
        return email != null
           && (email.toLowerCase().contains("@outlook.") || email.toLowerCase().contains("@hotmail."));
    }

    private boolean outlookDialogShown;
    private void showOutlookHint()
    {
        if(!outlookDialogShown) {
            outlookDialogShown = true;
            new AlertDialog.Builder(this)
                .setMessage(
                      "Outlook- and Hotmail-e-mail-addresses "
                    + "may currently not work as expected "
                    + "as these servers may remove some important transport information."
                    + "\n\n"
                    + "Hopefully sooner or later there will be a fix; "
                    + "for now, we suggest to use another e-mail-address "
                    + "or try Delta Chat again when the issue is fixed.")
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    private void verifyEmail(TextInputEditText view) {
        String error = getString(R.string.login_error_mail);
        String email = view.getText().toString();
        if (!matchesEmailPattern(email)) {
            view.setError(error);
        }
    }

    private void verifyServer(TextInputEditText view) {
        String error = getString(R.string.login_error_server);
        String server = view.getText().toString();
        if (!TextUtils.isEmpty(server) && !Patterns.DOMAIN_NAME.matcher(server).matches()
                && !Patterns.IP_ADDRESS.matcher(server).matches()
                && !Patterns.WEB_URL.matcher(server).matches()) {
            view.setError(error);
        }
    }

    private void verifyPort(TextInputEditText view) {
        String error = getString(R.string.login_error_port);
        String portString = view.getText().toString();
        if (!portString.isEmpty()) {
            try {
                int port = Integer.valueOf(portString);
                if (port < 1 || port > 65535) {
                    view.setError(error);
                }
            } catch (NumberFormatException exception) {
                view.setError(error);
            }
        }
    }

    private void onAdvancedSettings() {
        boolean advancedViewVisible = advancedGroup.getVisibility() == View.VISIBLE;
        if (advancedViewVisible) {
            advancedGroup.setVisibility(View.GONE);
            advancedIcon.setRotation(45);
        } else {
            advancedGroup.setVisibility(View.VISIBLE);
            advancedIcon.setRotation(0);
        }
    }

    private void onLogin() {
        if (!verifyRequiredFields()) {
            Toast.makeText(this, R.string.login_error_required_fields, Toast.LENGTH_LONG).show();
            return;
        }
        setupConfig();

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialog, which) -> stopLoginProcess());
        progressDialog.show();
    }

    private boolean verifyRequiredFields() {
        String email = emailInput.getText().toString();
        return !email.isEmpty() && matchesEmailPattern(email)
                && !passwordInput.getText().toString().isEmpty();
    }

    private void setupConfig() {
        setConfig(R.id.email_text, "addr", true);
        setConfig(R.id.password_text, "mail_pw", false);
        setConfig(R.id.imap_server_text, "mail_server", true);
        setConfig(R.id.imap_port_text, "mail_port", true);
        setConfig(R.id.imap_login_text, "mail_user", false);
        setConfig(R.id.smtp_server_text, "send_server", true);
        setConfig(R.id.smtp_port_text, "send_port", true);
        setConfig(R.id.smtp_login_text, "send_user", false);
        setConfig(R.id.smtp_password_text, "send_pw", false);

        int server_flags = 0;
        if(imapSecurity.getSelectedItemPosition()==1) server_flags |= DcContext.DC_LP_IMAP_SOCKET_SSL;
        if(imapSecurity.getSelectedItemPosition()==2) server_flags |= DcContext.DC_LP_IMAP_SOCKET_STARTTLS;
        if(imapSecurity.getSelectedItemPosition()==3) server_flags |= DcContext.DC_LP_IMAP_SOCKET_PLAIN;
        if(smtpSecurity.getSelectedItemPosition()==1) server_flags |= DcContext.DC_LP_SMTP_SOCKET_SSL;
        if(smtpSecurity.getSelectedItemPosition()==2) server_flags |= DcContext.DC_LP_SMTP_SOCKET_STARTTLS;
        if(smtpSecurity.getSelectedItemPosition()==3) server_flags |= DcContext.DC_LP_SMTP_SOCKET_PLAIN;
        if(authMethod.getSelectedItemPosition()==1)   server_flags |= DcContext.DC_LP_AUTH_OAUTH2;
        DcHelper.getContext(this).setConfigInt("server_flags", server_flags);

        // calling configure() results in
        // receiving multiple DC_EVENT_CONFIGURE_PROGRESS events
        DcHelper.getContext(this).captureNextError();
        DcHelper.getContext(this).configure();
    }

    private void setConfig(@IdRes int viewId, String configTarget, boolean doTrim) {
        TextInputEditText view = findViewById(viewId);
        String value = view.getText().toString();
        if(doTrim) {
            value = value.trim();
        }
        DcHelper.getContext(this).setConfig(configTarget, value.isEmpty()? null : value);
    }

    private void stopLoginProcess() {
        DcHelper.getContext(this).stopOngoingProcess();
    }

    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
        if (eventId==DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            ApplicationDcContext dcContext = DcHelper.getContext(this);
            long progress = (Long)data1;
            if (progress==0/*error/aborted*/) {
                dcContext.endCaptureNextError();
                progressDialog.dismiss();
                if (dcContext.hasCapturedError()) {
                    new AlertDialog.Builder(this)
                            .setMessage(dcContext.getCapturedError())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
            else if (progress<1000/*progress in permille*/) {
                int percent = (int)progress / 10;
                progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
            }
            else if (progress==1000/*done*/) {
                dcContext.endCaptureNextError();
                progressDialog.dismiss();
                Intent conversationList = new Intent(getApplicationContext(), ConversationListActivity.class);
                startActivity(conversationList);
                finish();
            }
        }
    }
}
