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

import android.util.Log;

import com.iwedia.dtv.epg.EpgEvent;
import com.iwedia.dtv.types.InternalException;

import java.util.Date;

/**
 * Class for Holding Channel Name and Number.
 */
public class ChannelInfo {
    private static final String TAG = "ChannelInfo";
    private int mNumber = 0;
    private String mName = "";
    private String mEPGNow = "";
    private String mEPGNext = "";
    private Date mStartTime = null;
    private Date mEndTime = null;
    private String mParental = "";

    public ChannelInfo(int channelNumber, String channelName, EpgEvent now,
            EpgEvent next) {
        mNumber = channelNumber;
        mName = channelName;
        if (now != null) {
            mEPGNow = now.getName();
            mStartTime = now.getStartTime().getCalendar().getTime();
            mEndTime = now.getEndTime().getCalendar().getTime();
            mParental = getParentalRating(now.getParentalRate());
        }
        if (next != null) {
            mEPGNext = next.getName();
        }
    }

    /**
     * Get Parental Rating by Index.
     */
    private String getParentalRating(int rate) {
        if (rate >= 4 && rate <= 18) {
            return "" + rate;
        }
        return "";
    }

    public int getProgressPercentPassed() {
        int returnValue = -1;
        Date currentDate = DVBManager.getInstance().getCurrentTimeDate()
                .getCalendar().getTime();
        if (mStartTime != null && mEndTime != null && currentDate != null) {
            long startTime = mStartTime.getTime();
            long endTime = mEndTime.getTime();
            long currentTime = currentDate.getTime();
            if (currentTime < startTime || currentTime > endTime) {
                return -1;
            }
            returnValue = (int) ((((currentTime - startTime) * 100) / (endTime - startTime)));
            if (returnValue > 100) {
                return 100;
            } else if (returnValue < 0) {
                return 0;
            }
        }
        return returnValue;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getName() {
        return mName;
    }

    public String getEPGNow() {
        return mEPGNow;
    }

    public String getEPGNext() {
        return mEPGNext;
    }

    public Date getStartTime() {
        return mStartTime;
    }

    public Date getEndTime() {
        return mEndTime;
    }

    public String getParental() {
        return mParental;
    }
}
