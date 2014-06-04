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

import com.iwedia.exampleip.dtv.DVBManager;

/**
 * Class for checking PIN code.
 */
public class CheckPinDialog extends AlertDialog {
    private EditText mEditText;
    private final PinCheckedCallback mCallback;

    public interface PinCheckedCallback {
        public void pinChecked(boolean pinOk);
    }

    public CheckPinDialog(final Context context, PinCheckedCallback callback) {
        super(context);
        mCallback = callback;
        setTitle("Enter Pin code");
        mEditText = new EditText(context);
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(4);
        mEditText.setFilters(new InputFilter[] { maxLengthFilter });
        mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        setView(mEditText);
        setButton(BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (mCallback != null) {
                        mCallback.pinChecked(DVBManager
                                .getInstance()
                                .getParentalManager()
                                .checkPin(
                                        Integer.valueOf(mEditText.getText()
                                                .toString())));
                    }
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mCallback != null) {
            mCallback.pinChecked(false);
        }
    }
}
