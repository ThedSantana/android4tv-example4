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

import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.iwedia.dtv.audio.AudioTrack;
import com.iwedia.dtv.audio.IAudioControl;
import com.iwedia.dtv.display.IDisplayControl;
import com.iwedia.dtv.display.SurfaceBundle;
import com.iwedia.dtv.subtitle.ISubtitleControl;
import com.iwedia.dtv.subtitle.SubtitleMode;
import com.iwedia.dtv.subtitle.SubtitleTrack;
import com.iwedia.dtv.subtitle.SubtitleType;
import com.iwedia.dtv.teletext.ITeletextControl;
import com.iwedia.dtv.teletext.TeletextTrack;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.dtv.types.UserControl;

public class TeletextSubtitleAudioManager {
    public static final int TELETEXT_BACKGROUND_TRANSPARENT = 0,
            TELETEXT_BACKGROUND_OPAQUE = 255;
    private ITeletextControl mTeletextControl;
    private ISubtitleControl mSubtitleControl;
    private IAudioControl mAudioControl;
    private IDisplayControl mDisplayControl;
    private boolean subtitleActive = false, teletextActive = false;
    private static TeletextSubtitleAudioManager instance;
    private SurfaceView mSurfaceView = null;
    private int mScreenWidth = 0, mScreenHeight = 0;

    /**
     * Teletext state stuff
     */
    public enum TeletextMode {
        FULL, HALF, TRANSPARENT, OFF
    }

    private TeletextMode mTeletextMode = TeletextMode.OFF;

    protected static TeletextSubtitleAudioManager getInstance(
            ITeletextControl mTeletextControl,
            ISubtitleControl mSubtitleControl, IAudioControl mAudioControl,
            IDisplayControl mDisplayControl) {
        if (instance == null) {
            instance = new TeletextSubtitleAudioManager(mTeletextControl,
                    mSubtitleControl, mAudioControl, mDisplayControl);
        }
        return instance;
    }

    private TeletextSubtitleAudioManager(ITeletextControl mTeletextControl,
            ISubtitleControl mSubtitleControl, IAudioControl mAudioControl,
            IDisplayControl mDisplayControl) {
        this.mTeletextControl = mTeletextControl;
        this.mSubtitleControl = mSubtitleControl;
        this.mAudioControl = mAudioControl;
        this.mDisplayControl = mDisplayControl;
    }

    public void initializeSubtitleAndTeletextDisplay(SurfaceView surfaceView,
            int screenWidth, int screenHeight) throws IllegalArgumentException,
            InternalException {
        mSurfaceView = surfaceView;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        SurfaceBundle surfaceBundle = new SurfaceBundle();
        surfaceBundle.setSurface(surfaceView.getHolder().getSurface());
        mDisplayControl.setVideoLayerSurface(1, surfaceBundle);
    }

    /**
     * Shows teletext dialog and send command to middleware to start drawing
     * 
     * @throws InternalException
     */
    public boolean changeTeletext(int trackIndex) throws InternalException {
        if (mTeletextMode == TeletextMode.FULL) {
            RelativeLayout.LayoutParams params = (LayoutParams) mSurfaceView
                    .getLayoutParams();
            params.width = mScreenWidth / 2;
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            mSurfaceView.setLayoutParams(params);
            mDisplayControl.scaleWindow(0, 0, mScreenWidth / 2, mScreenHeight);
            mTeletextMode = TeletextMode.HALF;
        } else if (mTeletextMode == TeletextMode.HALF) {
            RelativeLayout.LayoutParams params = (LayoutParams) mSurfaceView
                    .getLayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurfaceView.setLayoutParams(params);
            mTeletextMode = TeletextMode.TRANSPARENT;
            mDisplayControl.scaleWindow(0, 0, mScreenWidth, mScreenHeight);
            mTeletextControl
                    .setTeletextBgAlpha(TELETEXT_BACKGROUND_TRANSPARENT);
        } else if (mTeletextMode == TeletextMode.OFF) {
            RelativeLayout.LayoutParams params = (LayoutParams) mSurfaceView
                    .getLayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurfaceView.setLayoutParams(params);
            mTeletextMode = TeletextMode.FULL;
            mTeletextControl.setTeletextBgAlpha(TELETEXT_BACKGROUND_OPAQUE);
            mTeletextControl.setCurrentTeletextTrack(DVBManager.getInstance()
                    .getCurrentLiveRoute(), trackIndex);
            if (mTeletextControl.getCurrentTeletextTrackIndex(DVBManager
                    .getInstance().getCurrentLiveRoute()) >= 0) {
                teletextActive = true;
            }
        } else if (mTeletextMode == TeletextMode.TRANSPARENT) {
            mTeletextControl.deselectCurrentTeletextTrack(DVBManager
                    .getInstance().getCurrentLiveRoute());
            if (mTeletextControl.getCurrentTeletextTrackIndex(DVBManager
                    .getInstance().getCurrentLiveRoute()) < 0) {
                teletextActive = false;
                mTeletextMode = TeletextMode.OFF;
            }
        }
        return teletextActive;
    }

    /**
     * Hide teletext
     * 
     * @throws InternalException
     */
    public void hideTeletext() throws InternalException {
        mTeletextControl.deselectCurrentTeletextTrack(DVBManager.getInstance()
                .getCurrentLiveRoute());
        if (mTeletextControl.getCurrentTeletextTrackIndex(DVBManager
                .getInstance().getCurrentLiveRoute()) < 0) {
            mDisplayControl.scaleWindow(0, 0, mScreenWidth, mScreenHeight);
            teletextActive = false;
            mTeletextMode = TeletextMode.OFF;
        }
    }

    public TeletextTrack getTeletextTrack(int index) {
        return mTeletextControl.getTeletextTrack(DVBManager.getInstance()
                .getCurrentLiveRoute(), index);
    }

    public void sendTeletextInputCommand(int keyCode) {
        mTeletextControl.sendInputControl(DVBManager.getInstance()
                .getCurrentLiveRoute(), UserControl.PRESSED, keyCode);
    }

    /**
     * Get teletext track count.
     * 
     * @return Number of teletext tracks.
     */
    public int getTeletextTrackCount() {
        return mTeletextControl.getTeletextTrackCount(DVBManager.getInstance()
                .getCurrentLiveRoute());
    }

    public String convertTeletextTrackTypeToHumanReadableFormat(int type) {
        switch (type) {
            case 1: {
                return "TTXT NORMAL";
            }
            case 2: {
                return "TTXT SUB";
            }
            case 3: {
                return "TTXT INFO";
            }
            case 4: {
                return "TTXT PROGRAM SCHEDULE";
            }
            case 5: {
                return "TTXT SUB HOH";
            }
            default:
                return "UNKNOWN";
        }
    }

    public String convertSubtitleTrackModeToHumanReadableFormat(int modeIndex) {
        SubtitleMode mode = SubtitleMode.getFromValue(modeIndex);
        if (mode == SubtitleMode.TRANSLATION) {
            return "NORMAL";
        } else if (mode == SubtitleMode.HEARING_IMPAIRED) {
            return "HOH";
        }
        return "";
    }

    public boolean showSubtitles(int trackIndex) throws InternalException {
        mSubtitleControl.setCurrentSubtitleTrack(DVBManager.getInstance()
                .getCurrentLiveRoute(), trackIndex);
        if (mSubtitleControl.getCurrentSubtitleTrackIndex(DVBManager
                .getInstance().getCurrentLiveRoute()) >= 0) {
            subtitleActive = true;
        }
        return subtitleActive;
    }

    public void hideSubtitles() throws InternalException {
        mSubtitleControl.deselectCurrentSubtitleTrack(DVBManager.getInstance()
                .getCurrentLiveRoute());
        if (mSubtitleControl.getCurrentSubtitleTrackIndex(DVBManager
                .getInstance().getCurrentLiveRoute()) < 0) {
            subtitleActive = false;
        }
    }

    public SubtitleTrack getSubtitleTrack(int index) {
        return mSubtitleControl.getSubtitleTrack(DVBManager.getInstance()
                .getCurrentLiveRoute(), index);
    }

    /**
     * Get subtitle track count.
     * 
     * @return Number of subtitle tracks.
     */
    public int getSubtitlesTrackCount() {
        return mSubtitleControl.getSubtitleTrackCount(DVBManager.getInstance()
                .getCurrentLiveRoute());
    }

    public boolean isSubtitleAutomatic() {
        return mSubtitleControl.isAutomaticSubtitleDisplayEnabled();
    }

    public void setSubtitleAutomatic(boolean automatic) {
        mSubtitleControl.enableAutomaticSubtitleDisplay(automatic);
    }

    public SubtitleType getSubtitleType() {
        return mSubtitleControl.getSubtitleType();
    }

    public void setSubtitleType(SubtitleType type) {
        mSubtitleControl.setSubtitleType(type);
    }

    public SubtitleMode getSubtitleMode() {
        return mSubtitleControl.getSubtitleMode();
    }

    public void setSubtitleMode(SubtitleMode mode) {
        mSubtitleControl.setSubtitleMode(mode);
    }

    public int getAudioLanguagesTrackCount() {
        return mAudioControl.getAudioTrackCount(DVBManager.getInstance()
                .getCurrentLiveRoute());
    }

    public AudioTrack getAudioLanguage(int index) {
        return mAudioControl.getAudioTrack(DVBManager.getInstance()
                .getCurrentLiveRoute(), index);
    }

    public void setAudioTrack(int index) throws InternalException {
        mAudioControl.setCurrentAudioTrack(DVBManager.getInstance()
                .getCurrentLiveRoute(), index);
    }

    public boolean isSubtitleActive() {
        if (mSubtitleControl.getCurrentSubtitleTrackIndex(DVBManager
                .getInstance().getCurrentLiveRoute()) < 0) {
            subtitleActive = false;
        } else {
            subtitleActive = true;
        }
        return subtitleActive;
    }

    public boolean isTeletextActive() {
        if (mTeletextControl.getCurrentTeletextTrackIndex(DVBManager
                .getInstance().getCurrentLiveRoute()) < 0) {
            teletextActive = false;
            mTeletextMode = TeletextMode.OFF;
        } else {
            teletextActive = true;
        }
        return teletextActive;
    }

    public TeletextMode getmTeletextMode() {
        return mTeletextMode;
    }
}
