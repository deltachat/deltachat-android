package org.thoughtcrime.securesms.util.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    @Override
    public void show() {
        super.show();
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
        dialog.show();
        return dialog;
    }

}
