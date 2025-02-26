package org.thoughtcrime.securesms.components;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.CustomDefaultPreference.CustomDefaultPreferenceDialogFragmentCompat.CustomPreferenceValidator;
import org.thoughtcrime.securesms.util.Prefs;

public class CustomDefaultPreference extends DialogPreference {

  private static final String TAG = CustomDefaultPreference.class.getSimpleName();

  private final int    inputType;
  private final String customPreference;
  private final String customToggle;

  private final CustomPreferenceValidator validator;
  private String                    defaultValue;

  public CustomDefaultPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    int[]      attributeNames = new int[]{android.R.attr.inputType, R.attr.custom_pref_toggle};
    try (TypedArray attributes = context.obtainStyledAttributes(attrs, attributeNames)) {
      this.inputType = attributes.getInt(0, 0);
      this.customPreference = getKey();
      this.customToggle = attributes.getString(1);
      this.validator = new CustomDefaultPreferenceDialogFragmentCompat.NullValidator();
    }

    setPersistent(false);
    setDialogLayoutResource(R.layout.custom_default_preference_dialog);
  }

  public CustomDefaultPreference setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    this.setSummary(getSummary());
    return this;
  }

  @Override
  public String getSummary() {
    if (isCustom()) {
      return getContext().getString(R.string.pref_using_custom,
                                    getPrettyPrintValue(getCustomValue()));
    } else {
      return getContext().getString(R.string.pref_using_default,
                                    getPrettyPrintValue(getDefaultValue()));
    }
  }

  private String getPrettyPrintValue(String value) {
    if (TextUtils.isEmpty(value)) return getContext().getString(R.string.none);
    else                          return value;
  }

  private boolean isCustom() {
    return Prefs.getBooleanPreference(getContext(), customToggle, false);
  }

  private void setCustom(boolean custom) {
    Prefs.setBooleanPreference(getContext(), customToggle, custom);
  }

  private String getCustomValue() {
    return Prefs.getStringPreference(getContext(), customPreference, "");
  }

  private void setCustomValue(String value) {
    Prefs.setStringPreference(getContext(), customPreference, value);
  }

  private String getDefaultValue() {
    return defaultValue;
  }


  public static class CustomDefaultPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String INPUT_TYPE = "input_type";

    private Spinner  spinner;
    private EditText customText;
    private TextView defaultLabel;

    public static CustomDefaultPreferenceDialogFragmentCompat newInstance(String key) {
      CustomDefaultPreferenceDialogFragmentCompat fragment = new CustomDefaultPreferenceDialogFragmentCompat();
      Bundle b = new Bundle(1);
      b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key);
      fragment.setArguments(b);
      return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
      Log.w(TAG, "onBindDialogView");
      super.onBindDialogView(view);

      CustomDefaultPreference preference = (CustomDefaultPreference)getPreference();

      this.spinner      = (Spinner) view.findViewById(R.id.default_or_custom);
      this.defaultLabel = (TextView) view.findViewById(R.id.default_label);
      this.customText   = (EditText) view.findViewById(R.id.custom_edit);

      this.customText.setInputType(preference.inputType);
      this.customText.addTextChangedListener(new TextValidator());
      this.customText.setText(preference.getCustomValue());
      this.spinner.setOnItemSelectedListener(new SelectionLister());
      this.defaultLabel.setText(preference.getPrettyPrintValue(preference.defaultValue));
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle instanceState) {
      Dialog dialog = super.onCreateDialog(instanceState);

      CustomDefaultPreference preference = (CustomDefaultPreference)getPreference();

      if (preference.isCustom()) spinner.setSelection(1, true);
      else                       spinner.setSelection(0, true);

      return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
      CustomDefaultPreference preference = (CustomDefaultPreference)getPreference();

      if (positiveResult) {
        if (spinner != null)    preference.setCustom(spinner.getSelectedItemPosition() == 1);
        if (customText != null) preference.setCustomValue(customText.getText().toString());

        preference.setSummary(preference.getSummary());
      }
    }

    interface CustomPreferenceValidator {
      public boolean isValid(String value);
    }

    private static class NullValidator implements CustomPreferenceValidator {
      @Override
      public boolean isValid(String value) {
        return true;
      }
    }

    private class TextValidator implements TextWatcher {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        CustomDefaultPreference preference = (CustomDefaultPreference)getPreference();

        if (spinner.getSelectedItemPosition() == 1) {
          Button positiveButton = ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
          positiveButton.setEnabled(preference.validator.isValid(s.toString()));
        }
      }
    }

    private class SelectionLister implements AdapterView.OnItemSelectedListener {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        CustomDefaultPreference preference = (CustomDefaultPreference)getPreference();
        Button positiveButton = ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);

        defaultLabel.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        customText.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        positiveButton.setEnabled(position == 0 || preference.validator.isValid(customText.getText().toString()));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        defaultLabel.setVisibility(View.VISIBLE);
        customText.setVisibility(View.GONE);
      }
    }

  }



}
