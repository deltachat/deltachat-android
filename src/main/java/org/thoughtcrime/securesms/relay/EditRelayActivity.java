package org.thoughtcrime.securesms.relay;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PROXY_ENABLED;
import static org.thoughtcrime.securesms.connect.DcHelper.getContext;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcProvider;
import com.google.android.material.textfield.TextInputEditText;

import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.LogViewActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.WelcomeActivity;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.proxy.ProxySettingsActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.EnteredCertificateChecks;
import chat.delta.rpc.types.EnteredLoginParam;
import chat.delta.rpc.types.Socket;

public class EditRelayActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

    private enum VerificationType {
        EMAIL,
        SERVER,
        PORT,
    }

    private static final String TAG = EditRelayActivity.class.getSimpleName();
    public final static String EXTRA_ADDR = "extra_addr";

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private View providerLayout;
    private TextView providerHint;
    private TextView providerLink;
    private @Nullable DcProvider provider;

    private Group advancedGroup;
    private ImageView advancedIcon;
    private ProgressDialog progressDialog;
    private boolean cancelled = false;

    Spinner imapSecurity;
    Spinner smtpSecurity;
    Spinner certCheck;

    private SwitchCompat proxySwitch;

    Rpc rpc;
    int accId;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        rpc = DcHelper.getRpc(this);
        accId = DcHelper.getContext(this).getAccountId();

        setContentView(R.layout.activity_edittransport);

        // add padding to avoid content hidden behind system bars
        ViewUtil.applyWindowInsets(findViewById(R.id.content_container));


        emailInput = findViewById(R.id.email_text);
        passwordInput = findViewById(R.id.password_text);

        providerLayout = findViewById(R.id.provider_layout);
        providerHint = findViewById(R.id.provider_hint);
        providerLink = findViewById(R.id.provider_link);
        providerLink.setOnClickListener(l -> onProviderLink());

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
        certCheck = findViewById(R.id.cert_check);

        proxySwitch = findViewById(R.id.proxy_settings);
        proxySwitch.setOnClickListener(l -> {
            proxySwitch.setChecked(!proxySwitch.isChecked()); // revert toggle
            startActivity(new Intent(this, ProxySettingsActivity.class));
        });

        String addr = getIntent().getStringExtra(EXTRA_ADDR);
        EnteredLoginParam config = null;
        try {
            List<EnteredLoginParam> relays = rpc.listTransports(accId);
            for (EnteredLoginParam relay : relays) {
                if (addr != null && addr.equals(relay.addr)) {
                    config = relay;
                    break;
                }
            }
            if (config == null && !relays.isEmpty()) {
                Log.e(TAG, "Error got unknown address: " + addr);
                finish();
                return;
            };
        } catch (RpcException e) {
            Log.e(TAG, "Error calling Rpc.listTransports()", e);
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(
              config != null? R.string.edit_transport : R.string.manual_account_setup_option
            );
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

        if (config != null) emailInput.setEnabled(false);
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

        boolean expandAdvanced = false;
        int intVal;

        intVal = DcHelper.getInt(this, CONFIG_PROXY_ENABLED);
        proxySwitch.setChecked(intVal == 1);
        expandAdvanced = expandAdvanced || intVal == 1;

        if (config != null) { // configured
            emailInput.setText(config.addr);
            if(!TextUtils.isEmpty(config.addr)) {
                emailInput.setSelection(config.addr.length(), config.addr.length());
            }
            passwordInput.setText(config.password);

            TextInputEditText imapLoginInput = findViewById(R.id.imap_login_text);
            imapLoginInput.setText(config.imapUser);
            expandAdvanced = expandAdvanced || !TextUtils.isEmpty(config.imapUser);

            imapServerInput.setText(config.imapServer);
            expandAdvanced = expandAdvanced || !TextUtils.isEmpty(config.imapServer);

            if (config.imapPort != null) imapPortInput.setText(config.imapPort.toString());
            expandAdvanced = expandAdvanced || config.imapPort != null;

            intVal = socketSecurityToInt(config.imapSecurity);
            imapSecurity.setSelection(ViewUtil.checkBounds(intVal, imapSecurity));
            expandAdvanced = expandAdvanced || intVal != 0;

            TextInputEditText smtpLoginInput = findViewById(R.id.smtp_login_text);
            smtpLoginInput.setText(config.smtpUser);
            expandAdvanced = expandAdvanced || !TextUtils.isEmpty(config.smtpUser);

            TextInputEditText smtpPasswordInput = findViewById(R.id.smtp_password_text);
            smtpPasswordInput.setText(config.smtpPassword);
            expandAdvanced = expandAdvanced || !TextUtils.isEmpty(config.smtpPassword);

            smtpServerInput.setText(config.smtpServer);
            expandAdvanced = expandAdvanced || !TextUtils.isEmpty(config.smtpServer);

            if (config.smtpPort != null) smtpPortInput.setText(config.smtpPort.toString());
            expandAdvanced = expandAdvanced || config.smtpPort != null;

            intVal = socketSecurityToInt(config.smtpSecurity);
            smtpSecurity.setSelection(ViewUtil.checkBounds(intVal, smtpSecurity));
            expandAdvanced = expandAdvanced || intVal != 0;

            intVal = certificateChecksToInt(config.certificateChecks);
            certCheck.setSelection(ViewUtil.checkBounds(intVal, certCheck));
            expandAdvanced = expandAdvanced || intVal != 0;
        }

        if (expandAdvanced) { onAdvancedSettings(); }
        registerForEvents();
    }

    private void registerForEvents() {
        DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        proxySwitch.setChecked(DcHelper.getInt(this, CONFIG_PROXY_ENABLED) == 1);
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
            updateProviderInfo();
            onLogin();
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
                    updateProviderInfo();
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

    private void updateProviderInfo() {
        Util.runOnBackground(() -> {
            provider = getContext(this).getProviderFromEmailWithDns(emailInput.getText().toString());
            Util.runOnMain(() -> {
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
            });
        });
    }

    private void maybeCleanProviderInfo() {
        if (provider!=null && providerLayout.getVisibility()==View.VISIBLE) {
            provider = null;
            providerLayout.setVisibility(View.GONE);
        }
    }

    private void onProviderLink() {
        if (provider!=null) {
            String url = provider.getOverviewPage();
            if(!url.isEmpty()) {
                IntentUtils.showInBrowser(this, url);
            } else {
                // this should normally not happen
                Toast.makeText(this, "ErrProviderWithoutUrl", Toast.LENGTH_LONG).show();
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

        cancelled = false;
        setupConfig();

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.one_moment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialog, which) -> {
          cancelled = true;
          DcHelper.getContext(this).stopOngoingProcess();
        });
        progressDialog.show();
    }

    private boolean verifyRequiredFields() {
        String email = emailInput.getText().toString();
        return DcHelper.getContext(this).mayBeValidAddr(email)
                && !passwordInput.getText().toString().isEmpty();
    }

    private EnteredCertificateChecks certificateChecksFromInt(int position) {
        switch (position) {
            case 0:
                return EnteredCertificateChecks.automatic;
            case 1:
                return EnteredCertificateChecks.strict;
            case 2:
                return EnteredCertificateChecks.acceptInvalidCertificates;
        }
        throw new IllegalArgumentException("Invalid certificate position: " + position);
    }

    private int certificateChecksToInt(EnteredCertificateChecks check) {
        if (check == null) return 0;

        switch (check) {
            case strict:
                return 1;
            case acceptInvalidCertificates:
                return 2;
            case automatic:
            default:
                return 0;
        }
    }

    public static Socket socketSecurityFromInt(int position) {
        switch (position) {
        case 0:
            return Socket.automatic;
        case 1:
            return Socket.ssl;
        case 2:
            return Socket.starttls;
        case 3:
            return Socket.plain;
        }
        throw new IllegalArgumentException("Invalid socketSecurity position: " + position);
    }

    public static int socketSecurityToInt(Socket security) {
        if (security == null) return 0;

        switch (security) {
            case ssl:
                return 1;
            case starttls:
                return 2;
            case plain:
                return 3;
            case automatic:
            default:
                return 0;
        }
    }

    private void setupConfig() {
        DcHelper.getEventCenter(this).captureNextError();

        EnteredLoginParam param = new EnteredLoginParam();
        param.addr = getParam(R.id.email_text, true);
        param.password = getParam(R.id.password_text, false);
        param.imapServer = getParam(R.id.imap_server_text, true);
        param.imapPort = Util.objectToInt(getParam(R.id.imap_port_text, true));
        param.imapSecurity = socketSecurityFromInt(imapSecurity.getSelectedItemPosition());
        param.imapUser = getParam(R.id.imap_login_text, false);
        param.smtpServer = getParam(R.id.smtp_server_text, true);
        param.smtpPort = Util.objectToInt(getParam(R.id.smtp_port_text, true));
        param.smtpSecurity = socketSecurityFromInt(smtpSecurity.getSelectedItemPosition());
        param.smtpUser = getParam(R.id.smtp_login_text, false);
        param.smtpPassword = getParam(R.id.smtp_password_text, false);
        param.certificateChecks = certificateChecksFromInt(certCheck.getSelectedItemPosition());

        new Thread(() -> {
            try {
                rpc.addOrUpdateTransport(accId, param);
                DcHelper.getEventCenter(this).endCaptureNextError();
                progressDialog.dismiss();
                Intent conversationList = new Intent(getApplicationContext(), ConversationListActivity.class);
                startActivity(conversationList);
                finish();
            } catch (RpcException e) {
                DcHelper.getEventCenter(this).endCaptureNextError();
                if (!cancelled) {
                  Util.runOnMain(() -> {
                      progressDialog.dismiss();
                      WelcomeActivity.maybeShowConfigurationError(this, e.getMessage());
                  });
                }
            }
        }).start();
    }

    private String getParam(@IdRes int viewId, boolean doTrim) {
        TextInputEditText view = findViewById(viewId);
        String value = view.getText().toString();
        if(doTrim) {
            value = value.trim();
        }
        return value.isEmpty()? null : value;
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId()==DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            long progress = event.getData1Int(); // progress in permille
            int percent = (int)progress / 10;
            progressDialog.setMessage(getResources().getString(R.string.one_moment)+String.format(" %d%%", percent));
        }
    }
}
