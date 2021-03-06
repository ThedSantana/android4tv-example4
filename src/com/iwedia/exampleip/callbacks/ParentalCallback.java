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
package com.iwedia.exampleip.callbacks;

import android.content.Context;
import android.util.Log;

import com.iwedia.dtv.parental.dvb.IParentalCallbackDvb;
import com.iwedia.dtv.parental.dvb.ParentalAgeEvent;
import com.iwedia.exampleip.dtv.DVBManager;

/**
 * Parental control callback.
 */
public class ParentalCallback implements IParentalCallbackDvb {
    private DVBManager mDvbManager = null;
    private static ParentalCallback sInstance;

    public static ParentalCallback getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParentalCallback(context);
        }
        return sInstance;
    }

    private ParentalCallback(final Context context) {
        mDvbManager = DVBManager.getInstance();
    }

    @Override
    public void ageLocked(ParentalAgeEvent arg0) {
        Log.d("ParentalCallback", "AGE LOCKED CALLBACK HAPPENED, FOR AGE: "
                + arg0.getAge() + ", IS LOCKED: " + arg0.isLocked());
        mDvbManager.updateAgeLocked(arg0.isLocked());
    }

    @Override
    public void channelLocked(int arg0, boolean arg1) {
        Log.d("ParentalCallback",
                "CHANNEL LOCKED CALLBACK HAPPENED, FOR CHANNEL: " + arg0 + " "
                        + arg1);
        mDvbManager.updateChannelLocked(arg1);
    }
}
