package org.thoughtcrime.securesms;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
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
import com.dd.CircularProgressButton;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.permissions.Permissions;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_ADDRESS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_MAIL_USER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PASSWORD;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_PORT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_SERVER;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SEND_USER;

/**
 * The register account activity.  Prompts ths user for their registration information
 * and begins the account registration process.
 *
 * @author Moxie Marlinspike
 * @author Daniel Böhrs
 */
public class RegistrationActivity extends BaseActionBarActivity implements DcEventCenter.DcEventDelegate {

    private enum VerificationType {
        EMAIL,
        SERVER,
        PORT,
    }

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Group advancedGroup;
    private ImageView advancedIcon;
    private ProgressDialog progressDialog;
    private boolean gmailDialogShown;

    MenuItem loginMenuItem;
    Spinner imapSecurity;
    Spinner smtpSecurity;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.registration_activity);

        initializeResources();
        DcHelper.getContext(this).eventCenter.addObserver(this, DcContext.DC_EVENT_CONFIGURE_PROGRESS);
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
        DcHelper.getContext(this).eventCenter.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void initializeResources() {
        emailInput = findViewById(R.id.email_text);
        passwordInput = findViewById(R.id.password_text);
        advancedGroup = findViewById(R.id.advanced_group);
        advancedIcon = findViewById(R.id.advanced_icon);
//        CircularProgressButton loginButton = findViewById(R.id.register_button);
        TextView advancedTextView = findViewById(R.id.advanced_text);
        TextInputEditText imapServerInput = findViewById(R.id.imap_server_text);
        TextInputEditText imapPortInput = findViewById(R.id.imap_port_text);
        TextInputEditText smtpServerInput = findViewById(R.id.smtp_server_text);
        TextInputEditText smtpPortInput = findViewById(R.id.smtp_port_text);

        imapSecurity = findViewById(R.id.imap_security);
        smtpSecurity = findViewById(R.id.smtp_security);

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
//        loginButton.setOnClickListener(l -> onLogin());
        advancedTextView.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setRotation(45);
        boolean isConfigured = DcHelper.isConfigured(getApplicationContext());
        if (isConfigured) {
            TextInputEditText imapLoginInput = findViewById(R.id.imap_login_text);
            emailInput.setText(DcHelper.get(this, CONFIG_ADDRESS));
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
        }
    }

    private void focusListener(View view, boolean focused, VerificationType type) {
        if (!focused) {
            TextInputEditText inputEditText = (TextInputEditText) view;
            switch (type) {
                case EMAIL:
                    verifyEmail(inputEditText);
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

    private void verifyEmail(TextInputEditText view) {
        String error = getString(R.string.login_error_mail);
        String email = view.getText().toString();
        if (!matchesEmailPattern(email)) {
            view.setError(error);
        }
        if (!TextUtils.isEmpty(email) && isGmail(email) && !gmailDialogShown) {
            gmailDialogShown = true;
            new AlertDialog.Builder(this)
                .setMessage(R.string.login_info_gmail_text)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    private boolean matchesEmailPattern(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isGmail(String email) {
        return email != null && (email.toLowerCase().contains("@gmail.") || email.toLowerCase().contains("@googlemail."));
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
