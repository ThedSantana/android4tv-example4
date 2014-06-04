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
package com.iwedia.exampleip.dtv;

import com.iwedia.dtv.dtvmanager.IDTVManager;
import com.iwedia.dtv.parental.dvb.IParentalCallbackDvb;
import com.iwedia.dtv.parental.dvb.IParentalControlDvb;
import com.iwedia.dtv.parental.dvb.ParentalLockAge;
import com.iwedia.dtv.types.InternalException;

/**
 * Class for handling parental rate and channel lock.
 */
public class ParentalManager {
    private IParentalControlDvb mParentalControl;
    private IParentalCallbackDvb mCallback;
    private static ParentalManager sInstance;

    protected static ParentalManager getInstance(IDTVManager dtvManager) {
        if (sInstance == null) {
            sInstance = new ParentalManager(dtvManager);
        }
        return sInstance;
    }

    protected static void destroyInstance() {
        sInstance = null;
    }

    private ParentalManager(IDTVManager dtvManager) {
        mParentalControl = dtvManager.getParentalControlDvb();
    }

    public void registerCallback(IParentalCallbackDvb callback) {
        mCallback = callback;
        mParentalControl.registerCallback(mCallback);
    }

    public void unregisterCallback() {
        try {
            mParentalControl.unregisterCallback(mCallback);
        } catch (IllegalArgumentException e) {
        }
    }

    public void setParentalRate(ParentalLockAge parentalRate) {
        mParentalControl.setParentalRate(parentalRate);
    }

    public int getParentalRate() {
        return mParentalControl.getParentalRate().ordinal();
    }

    public boolean checkPin(int pin) {
        return mParentalControl.checkPinCode(pin);
    }

    public void changePin(int newPin) {
        mParentalControl.setPinCode(newPin);
    }

    public void setChannelLock(int channelIndex, boolean locked)
            throws InternalException {
        mParentalControl
                .setChannelLock(
                        channelIndex
                                + (DVBManager.getInstance()
                                        .isIpAndSomeOtherTunerType() ? 1 : 0),
                        locked);
    }

    public boolean getChannelLockStatus(int channelIndex)
            throws InternalException {
        return mParentalControl
                .getChannelLock(channelIndex
                        + (DVBManager.getInstance().isIpAndSomeOtherTunerType() ? 1
                                : 0));
    }
}
