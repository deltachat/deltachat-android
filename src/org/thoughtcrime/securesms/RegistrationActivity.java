package org.thoughtcrime.securesms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;

import org.thoughtcrime.securesms.permissions.Permissions;

/**
 * The register account activity.  Prompts ths user for their registration information
 * and begins the account registration process.
 *
 * @author Moxie Marlinspike
 * @author Daniel Böhrs
 */
public class RegistrationActivity extends BaseActionBarActivity {

    private enum VerificationType {
        EMAIL,
        SERVER,
        PORT,
    }

    public static final String RE_REGISTRATION_EXTRA = "TO_BE_REMOVED"; // TODO remove

    public static final String CHALLENGE_EVENT = "TO_BE_REMOVED"; // TODO remove

    public static final String CHALLENGE_EXTRA = "TO_BE_REMOVED"; // TODO remove

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Group advancedGroup;
    private ImageView advancedIcon;
    private ProgressDialog progressDialog;
    private TextView subHeaderText;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.registration_activity);

        initializeResources();
        initializePermissions();
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
        subHeaderText = findViewById(R.id.sub_header);
        CircularProgressButton loginButton = findViewById(R.id.register_button);
        TextView advancedTextView = findViewById(R.id.advanced_text);
        TextInputEditText imapServerInput = findViewById(R.id.imap_server_text);
        TextInputEditText imapPortInput = findViewById(R.id.imap_port_text);
        TextInputEditText smtpServerInput = findViewById(R.id.smtp_server_text);
        TextInputEditText smtpPortInput = findViewById(R.id.smtp_port_text);

        emailInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.EMAIL));
        imapServerInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.SERVER));
        imapPortInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.PORT));
        smtpServerInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.SERVER));
        smtpPortInput.setOnFocusChangeListener((view, focused) -> focusListener(view, focused, VerificationType.PORT));
        loginButton.setOnClickListener(l -> onLogin());
        advancedTextView.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setOnClickListener(l -> onAdvancedSettings());
        advancedIcon.setRotation(45);
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
        String error = "Please enter a valid email address";
        String email = view.getText().toString();
        if (!matchesEmailPattern(email)) {
            view.setError(error);
        }
        if (!TextUtils.isEmpty(email) && isGmail(email)) {
            subHeaderText.setText("For GMail Accounts you need to create an App-Password if you have 2FA enabled. If this setting is not available, you need to enable Less Secure Apps");
        } else {
            subHeaderText.setText("For known email providers additional settings are setup automatically. Sometimes IMAP needs to be enabled in the web frontend. Consult your email provider or friends for help.");
        }
    }

    private boolean matchesEmailPattern(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isGmail(String email) {
        return email != null && (email.contains("@gmail.") || email.contains("@googlemail."));
    }

    private void verifyServer(TextInputEditText view) {
        String error = "Please enter a valid server / IP address";
        String server = view.getText().toString();
        if (!TextUtils.isEmpty(server) && !Patterns.DOMAIN_NAME.matcher(server).matches()
                && !Patterns.IP_ADDRESS.matcher(server).matches()
                && !Patterns.WEB_URL.matcher(server).matches()) {
            view.setError(error);
        }
    }

    private void verifyPort(TextInputEditText view) {
        String error = "Please enter a valid port (1-65535)";
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

    @SuppressLint("InlinedApi")
    private void initializePermissions() {
        Permissions.with(RegistrationActivity.this)
                .request(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withRationaleDialog("Delta Chat needs access to your contacts and media in order to connect with friends and send files",
                        R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
                .execute();
    }

    private void onLogin() {
        if (!verifyRequiredFields()) {
            Toast.makeText(this, "Please enter a valid email address and a password", Toast.LENGTH_LONG).show();
            return;
        }
        setupConfig();

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading ...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> stopLoginProcess());
        progressDialog.show();

        // TODO start configuration process via bindings
    }

    private boolean verifyRequiredFields() {
        String email = emailInput.getText().toString();
        return !email.isEmpty() && matchesEmailPattern(email)
                && !passwordInput.getText().toString().isEmpty();
    }

    private void setupConfig() {
        setConfig(R.id.email_text, "addr");
        setConfig(R.id.password_text, "mail_pw");
        setConfig(R.id.imap_server_text, "mail_server");
        setConfig(R.id.imap_port_text, "mail_port");
        setConfig(R.id.imap_login_text, "mail_user");
        setConfig(R.id.smtp_server_text, "send_server");
        setConfig(R.id.smtp_port_text, "send_port");
        setConfig(R.id.smtp_login_text, "send_user");
        setConfig(R.id.smtp_password_text, "send_pw");
    }

    private void setConfig(@IdRes int viewId, String configTarget) {
        TextInputEditText view = findViewById(viewId);
        String value = view.getText().toString().trim();
        if (!value.isEmpty()) {
            //JavaBindings.setConfig(configTarget, value); TODO set configuration value via bindings
        }
    }

    private void stopLoginProcess() {
        //JavaBindings.stopLoginProcess(); TODO stop configuration process via bindings
    }

    //@Override
    public void didReceivedNotification(int id, Object... args) {
        // TODO react to notifications sent via NotificationCenter
    }


}
