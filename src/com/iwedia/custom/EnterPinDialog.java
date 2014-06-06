/*
 * Copyright (C) 2014 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.iwedia.custom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.iwedia.exampleip.dtv.DVBManager;

/**
 * Class for checking PIN code.
 */
public class EnterPinDialog extends AlertDialog {
    public static final int PIN_INVALID = -1;
    private static final int NUMBER_OF_PIN_DIGITS = 4;
    private EditText mEditText;
    private PinCheckedCallback mCallbackPinChecked;
    private PinEnteredCallback mCallbackPinEntered;

    public interface PinCheckedCallback {
        public void pinChecked(boolean pinOk);
    }

    public interface PinEnteredCallback {
        public void pinEntered(int pin);
    }

    public EnterPinDialog(final Context context, PinEnteredCallback callback) {
        super(context);
        mCallbackPinEntered = callback;
        init(context);
    }

    public EnterPinDialog(final Context context, PinCheckedCallback callback) {
        super(context);
        mCallbackPinChecked = callback;
        init(context);
    }

    private void init(final Context context) {
        setTitle("Enter Pin code");
        setButton(BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mEditText.getText().length() == NUMBER_OF_PIN_DIGITS) {
                    int pin = PIN_INVALID;
                    try {
                        pin = Integer.valueOf(mEditText.getText().toString());
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (mCallbackPinChecked != null) {
                        boolean ok = DVBManager.getInstance()
                                .getParentalManager().checkPin(pin);
                        if (!ok) {
                            Toast.makeText(context, "Wrong pin entered",
                                    Toast.LENGTH_SHORT).show();
                        }
                        mCallbackPinChecked.pinChecked(ok);
                    }
                    if (mCallbackPinEntered != null) {
                        mCallbackPinEntered.pinEntered(pin);
                    }
                    dialog.dismiss();
                }
            }
        });
        mEditText = new EditText(context);
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(4);
        mEditText.setFilters(new InputFilter[] { maxLengthFilter });
        mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        setView(mEditText);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mCallbackPinChecked != null) {
            mCallbackPinChecked.pinChecked(false);
        }
    }
}
