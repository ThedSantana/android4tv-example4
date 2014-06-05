package com.iwedia.exampleip.callbacks;

import com.iwedia.dtv.epg.IEpgCallback;
import com.iwedia.exampleip.dtv.DVBManager;

public class EPGCallBack implements IEpgCallback {
    private static final String TAG = "EPGCallBack";
    private DVBManager mDVBManager = null;
    public static EPGCallBack sInstance = null;

    public static EPGCallBack getInstance() {
        if (sInstance == null) {
            sInstance = new EPGCallBack();
        }
        return sInstance;
    }

    private EPGCallBack() {
        mDVBManager = DVBManager.getInstance();
    }

    @Override
    public void pfAcquisitionFinished(int arg0, int arg1) {
        if (mDVBManager != null) {
            mDVBManager.updateNowNext();
        }
    }

    @Override
    public void pfEventChanged(int arg0, int arg1) {
        if (mDVBManager != null) {
            mDVBManager.updateNowNext();
        }
    }

    @Override
    public void scAcquisitionFinished(int arg0, int arg1) {
    }

    @Override
    public void scEventChanged(int arg0, int arg1) {
    }
}
