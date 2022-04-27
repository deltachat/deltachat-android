package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ADDRESS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_SECURITY;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_USER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_SECURITY;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_USER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SERVER_FLAGS;
import static org.thoughtcrime.securesms.connect.DcHelper.getContext;
import static org.thoughtcrime.securesms.service.IPCAddAccountsService.ACCOUNT_DATA;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class RegistrationActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

    private enum VerificationType {
        EMAIL,
        SERVER,
        PORT,
    }

    private final DynamicTheme dynamicTheme    = new DynamicTheme();

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private View providerLayout;
    private TextView providerHint;
    private TextView providerLink;
    private @Nullable DcProvider provider;

    private CheckBox encryptCheckbox;

    private Group advancedGroup;
    private ImageView advancedIcon;
    private ProgressDialog progressDialog;

    Spinner imapSecurity;
    Spinner smtpSecurity;
    Spinner authMethod;
    Spinner certCheck;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        dynamicTheme.onCreate(this);

        setContentView(R.layout.registration_activity);

        emailInput = findViewById(R.id.email_text);
        passwordInput = findViewById(R.id.password_text);

        providerLayout = findViewById(R.id.provider_layout);
        providerHint = findViewById(R.id.provider_hint);
        providerLink = findViewById(R.id.provider_link);
        providerLink.setOnClickListener(l -> onProviderLink());

        encryptCheckbox = findViewById(R.id.encrypt_checkbox);

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
        certCheck = findViewById(R.id.cert_check);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.login_header);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) { maybeCleanProviderInfo(); }
        });
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
            TextInputLayout passwordLayout = findViewById(R.id.password);
            passwordLayout.setPasswordVisibilityToggleEnabled(false);
            TextInputLayout smtpPasswordLayout = findViewById(R.id.smtp_password);
            smtpPasswordLayout.setPasswordVisibilityToggleEnabled(false);

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
            imapSecurity.setSelection(DcHelper.getInt(this, CONFIG_MAIL_SECURITY));
            TextInputEditText smtpLoginInput = findViewById(R.id.smtp_login_text);
            TextInputEditText smtpPasswordInput = findViewById(R.id.smtp_password_text);
            smtpLoginInput.setText(DcHelper.get(this, CONFIG_SEND_USER));
            smtpPasswordInput.setText(DcHelper.get(this, CONFIG_SEND_PASSWORD));
            smtpServerInput.setText(DcHelper.get(this, CONFIG_SEND_SERVER));
            smtpPortInput.setText(DcHelper.get(this, CONFIG_SEND_PORT));
            smtpSecurity.setSelection(DcHelper.getInt(this, CONFIG_SEND_SECURITY));

            int serverFlags = DcHelper.getInt(this, CONFIG_SERVER_FLAGS);
            int sel = 0;
            if((serverFlags&DcContext.DC_LP_AUTH_OAUTH2)!=0) sel = 1;
            authMethod.setSelection(sel);

            int certCheckFlags = DcHelper.getInt(this, "imap_certificate_checks");
            certCheck.setSelection(certCheckFlags);
            encryptCheckbox.setHeight(0);
            encryptCheckbox.setClickable(false);
            encryptCheckbox.setFocusable(false);
        } else if (getIntent() != null && getIntent().getBundleExtra(ACCOUNT_DATA) != null) {
          // Companion app might have sent account data
          Bundle b = getIntent().getBundleExtra(ACCOUNT_DATA);
          String emailAddress = b.getString(CONFIG_ADDRESS);
          String password = b.getString(CONFIG_MAIL_PASSWORD);
          if (!TextUtils.isEmpty(emailAddress) && !TextUtils.isEmpty(password)) {
            emailInput.setText(emailAddress);
            passwordInput.setText(password);
            onLogin();
          } else {
            String errorText = "Companion app auto-configuration failed.";
            errorText += TextUtils.isEmpty(emailAddress) ? " Missing emailAddress." : "";
            errorText += TextUtils.isEmpty(password) ? " Missing password." : "";
            Toast.makeText(this, errorText, Toast.LENGTH_LONG).show();
          }
        }

        registerForEvents();
    }

    private void registerForEvents() {
        DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this);
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
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.do_register) {
            // "log in" button clicked - even if oauth2DeclinedByUser is true,
            // we will ask for oauth2 to allow reverting the decision.
            checkOauth2start().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean oauth2started) {
                    if(!oauth2started) {
                        updateProviderInfo();
                        onLogin();
                    }
                }

                @Override
                public void onFailure(ExecutionException e) {
                    updateProviderInfo();
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
        DcHelper.getEventCenter(this).removeObservers(this);
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
                    if (!oauth2DeclinedByUser) {
                        checkOauth2start().addListener(new ListenableFuture.Listener<Boolean>() {
                            @Override
                            public void onSuccess(Boolean oauth2started) {
                                if (!oauth2started) {
                                    updateProviderInfo();
                                }
                            }

                            @Override
                            public void onFailure(ExecutionException e) {
                                updateProviderInfo();
                            }
                        });
                    } else {
                        updateProviderInfo();
                    }
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

    // this flag is set, if the user has declined oauth2 at some point;
    // if so, we won't bother em on focus changes again,
    // however, to allow reverting the decision, we will always ask on clicking the login button.
    private boolean oauth2DeclinedByUser = false;

    // this function checks if oauth2 is available for a given email address
    // and and asks the user if one wants to start oauth2.
    // the function returns the future "true" if oauth2 was started and "false" otherwise.
    private ListenableFuture<Boolean> checkOauth2start() {
        SettableFuture<Boolean> oauth2started = new SettableFuture<>();

        String email = emailInput.getText().toString();
        if (!TextUtils.isEmpty(email) ) {
            new PrecheckOauth2AsyncTask(this, email, oauth2started).execute();
        }
        else {
            oauth2started.set(false);
        }

        return oauth2started;
    }

    private static class PrecheckOauth2AsyncTask extends ProgressDialogAsyncTask<Void, Void, Void> {
        private final WeakReference<RegistrationActivity> activityWeakReference;
        private final String email;
        private final SettableFuture<Boolean> oauth2started;
        private final DcContext dcContext;
        private @NonNull String oauth2url = "";

        public PrecheckOauth2AsyncTask(RegistrationActivity activity, String email, SettableFuture<Boolean> oauth2started) {
            super(activity, null, activity.getString(R.string.login_oauth2_checking_addr, email));
            this.activityWeakReference = new WeakReference<>(activity);
            this.email = email;
            this.oauth2started = oauth2started;
            this.dcContext = DcHelper.getContext(activity);
            setCancelable(dialog -> {
                oauth2started.set(false);
            });
        }
        @Override
        protected Void doInBackground(Void... voids) {
            // the redirect-uri is also used as intent-filter in the manifest
            // and should be whitelisted by the supported oauth2 services
            String redirectUrl = "chat.delta:/"+BuildConfig.APPLICATION_ID+"/auth";
            oauth2url = dcContext.getOauth2Url(email, redirectUrl);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            RegistrationActivity activity = activityWeakReference.get();
            if (activity!=null && !TextUtils.isEmpty(oauth2url)) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.login_info_oauth2_title)
                        .setMessage(R.string.login_info_oauth2_text)
                        .setNegativeButton(R.string.cancel, (dialog, which)->{
                            activity.oauth2DeclinedByUser = true;
                            oauth2started.set(false);
                        })
                        .setPositiveButton(R.string.perm_continue, (dialog, which)-> {
                            // pass control to browser, we'll be back in business at (**)
                            activity.oauth2Requested = System.currentTimeMillis();
                            IntentUtils.showBrowserIntent(activity, oauth2url);
                            oauth2started.set(true);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                oauth2started.set(false);
            }
        }
    }

    private void updateProviderInfo() {
        provider = getContext(this).getProviderFromEmailWithDns(emailInput.getText().toString());
        if (provider!=null) {
            Resources res = getResources();
            providerHint.setText(provider.getBeforeLoginHint());
            switch (provider.getStatus()) {
                case DcProvider.DC_PROVIDER_STATUS_PREPARATION:
                    providerHint.setTextColor(res.getColor(R.color.provider_prep_fg));
                    providerLink.setTextColor(res.getColor(R.color.provider_prep_fg));
                    providerLayout.setBackgroundColor(res.getColor(R.color.provider_prep_bg));
                    providerLayout.setVisibility(View.VISIBLE);
                    break;

                case DcProvider.DC_PROVIDER_STATUS_BROKEN:
                    providerHint.setTextColor(res.getColor(R.color.provider_broken_fg));
                    providerLink.setTextColor(res.getColor(R.color.provider_broken_fg));
                    providerLayout.setBackgroundColor(getResources().getColor(R.color.provider_broken_bg));
                    providerLayout.setVisibility(View.VISIBLE);
                    break;

                default:
                    providerLayout.setVisibility(View.GONE);
                    break;
            }
        } else {
            providerLayout.setVisibility(View.GONE);
        }
    }

    private void maybeCleanProviderInfo() {
        if (provider!=null && providerLayout.getVisibility()==View.VISIBLE) {
            DcProvider newProvider = getContext(this).getProviderFromEmailWithDns(emailInput.getText().toString());
            if (newProvider == null
             || !newProvider.getOverviewPage().equals(provider.getOverviewPage())) {
                provider = null;
                providerLayout.setVisibility(View.GONE);
            }
        }
    }

    private void onProviderLink() {
        if (provider!=null) {
            String url = provider.getOverviewPage();
            if(!url.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_browser_installed, Toast.LENGTH_LONG).show();
                }
            } else {
                // this should normally not happen
                Toast.makeText(this, "ErrProviderWithoutUrl", Toast.LENGTH_LONG).show();
            }
        }
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

    private void verifyEmail(TextInputEditText view) {
        String error = getString(R.string.login_error_mail);
        String email = view.getText().toString();
        if (!DcHelper.getContext(this).mayBeValidAddr(email)) {
            view.setError(error);
        }
    }

    private void verifyServer(TextInputEditText view) {
        String error = getString(R.string.login_error_server);
        String server = view.getText().toString();
        if (!TextUtils.isEmpty(server) && !Patterns.DOMAIN_NAME.matcher(server).matches()
                && !Patterns.IP_ADDRESS.matcher(server).matches()
                && !Patterns.WEB_URL.matcher(server).matches()
                && !"localhost".equals(server)) {
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

        if (encryptCheckbox.isChecked()) {
            AccountManager accountManager = AccountManager.getInstance();

            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.one_moment));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Prevent the user from disabling the checkbox again, switching to unencrypted account is currently not implemented
            encryptCheckbox.setEnabled(false);
            Util.runOnBackground(() -> {
                DcHelper.getEventCenter(this).removeObservers(this);
                accountManager.switchToEncrypted(this);
                // Event center changed, register for events again
                registerForEvents();
                Util.runOnMain(this::continueLogin);
            });
        } else {
            continueLogin();
        }
    }

    private void continueLogin() {
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
        return DcHelper.getContext(this).mayBeValidAddr(email)
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

        DcHelper.getContext(this).setConfigInt(CONFIG_MAIL_SECURITY, imapSecurity.getSelectedItemPosition());
        DcHelper.getContext(this).setConfigInt(CONFIG_SEND_SECURITY, smtpSecurity.getSelectedItemPosition());

        int server_flags = 0;
        if(authMethod.getSelectedItemPosition()==1)   server_flags |= DcContext.DC_LP_AUTH_OAUTH2;
        DcHelper.getContext(this).setConfigInt(CONFIG_SERVER_FLAGS, server_flags);

        DcHelper.getContext(this).setConfigInt("smtp_certificate_checks", certCheck.getSelectedItemPosition());
        DcHelper.getContext(this).setConfigInt("imap_certificate_checks", certCheck.getSelectedItemPosition());

        // calling configure() results in
        // receiving multiple DC_EVENT_CONFIGURE_PROGRESS events
        DcHelper.getAccounts(this).stopIo();
        DcHelper.getEventCenter(this).captureNextError();
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
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId()==DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            long progress = event.getData1Int();
            if (progress==0/*error/aborted*/) {
                DcHelper.getAccounts(this).startIo(); // start-io is also needed on errors to make previous config work in case of changes
                DcHelper.getEventCenter(this).endCaptureNextError();
                progressDialog.dismiss();
                WelcomeActivity.maybeShowConfigurationError(this, event.getData2Str());
            }
            else if (progress<1000/*progress in permille*/) {
                int percent = (int)progress / 10;
                progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
            }
            else if (progress==1000/*done*/) {
                DcHelper.getAccounts(this).startIo();
                DcHelper.getEventCenter(this).endCaptureNextError();
                progressDialog.dismiss();
                Intent conversationList = new Intent(getApplicationContext(), ConversationListActivity.class);
                startActivity(conversationList);
                finish();
            }
        }
    }
}
