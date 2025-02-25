package org.thoughtcrime.securesms.util.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.thoughtcrime.securesms.R;

public class ProgressDialog extends AlertDialog {

    private boolean indeterminate;
    private String message;
    private TextView textView;
    private ProgressBar progressBar;

    public ProgressDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View dialogView = View.inflate(getContext(), R.layout.dialog_progress, null);
        setView(dialogView);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setMessage(CharSequence message) {
        this.message = message.toString();
        if (textView != null) {
            textView.setText(message);
        }
    }

    private boolean isButtonVisible(int which) {
        Button button = getButton(which);
        if (button==null) {
            return false;
        }
        return button.getVisibility()==View.VISIBLE;
    }

    @Override
    public void show() {
        super.show();

        if (isButtonVisible(Dialog.BUTTON_POSITIVE) || isButtonVisible(Dialog.BUTTON_NEGATIVE) || isButtonVisible(Dialog.BUTTON_NEUTRAL)) {
            findViewById(R.id.noButtonsSpacer).setVisibility(View.GONE);
        }

        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.text);
        setupProgressBar();
        setupTextView();
    }

    private void setupProgressBar() {
        if (progressBar != null) {
            progressBar.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(getContext(), R.color.delta_accent), PorterDuff.Mode.SRC_IN);
            progressBar.setIndeterminate(indeterminate);
        }
    }

    private void setupTextView() {
        if (textView != null && message != null && !message.isEmpty()) {
            textView.setText(message);
        }
    }

    private void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (progressBar != null) {
            progressBar.setIndeterminate(indeterminate);
        }
    }

    // Source: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ProgressDialog.java
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }

    // Source: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ProgressDialog.java
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    // Source: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ProgressDialog.java
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate,
                                      boolean cancelable, OnCancelListener cancelListener) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        if (cancelable) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel),
                    ((dialog1, which) -> cancelListener.onCancel(dialog)));
        }
        dialog.show();
        return dialog;
    }

}
