package com.iwedia.exampleip;

import android.app.AlertDialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.iwedia.dtv.audio.AudioTrack;
import com.iwedia.dtv.pvr.IPvrCallback;
import com.iwedia.dtv.pvr.PvrEventMediaAdd;
import com.iwedia.dtv.pvr.PvrEventMediaRemove;
import com.iwedia.dtv.pvr.PvrEventPlaybackJump;
import com.iwedia.dtv.pvr.PvrEventPlaybackPosition;
import com.iwedia.dtv.pvr.PvrEventPlaybackSpeed;
import com.iwedia.dtv.pvr.PvrEventPlaybackStart;
import com.iwedia.dtv.pvr.PvrEventPlaybackStop;
import com.iwedia.dtv.pvr.PvrEventRecordAdd;
import com.iwedia.dtv.pvr.PvrEventRecordConflict;
import com.iwedia.dtv.pvr.PvrEventRecordPosition;
import com.iwedia.dtv.pvr.PvrEventRecordRemove;
import com.iwedia.dtv.pvr.PvrEventRecordResourceIssue;
import com.iwedia.dtv.pvr.PvrEventRecordStart;
import com.iwedia.dtv.pvr.PvrEventRecordStop;
import com.iwedia.dtv.pvr.PvrEventTimeshiftJump;
import com.iwedia.dtv.pvr.PvrEventTimeshiftPosition;
import com.iwedia.dtv.pvr.PvrEventTimeshiftSpeed;
import com.iwedia.dtv.pvr.PvrEventTimeshiftStart;
import com.iwedia.dtv.pvr.PvrEventTimeshiftStop;
import com.iwedia.dtv.subtitle.SubtitleMode;
import com.iwedia.dtv.subtitle.SubtitleTrack;
import com.iwedia.dtv.subtitle.SubtitleType;
import com.iwedia.dtv.teletext.TeletextTrack;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.exampleip.dtv.ChannelInfo;
import com.iwedia.exampleip.dtv.IPService;
import com.iwedia.exampleip.dtv.PvrSpeedMode;
import com.iwedia.four.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends DTVActivity {
    public static final String TAG = "MainActivity";
    /** URI For VideoView. */
    public static final String TV_URI = "tv://";
    /** Channel Number/Name View Duration in Milliseconds. */
    public static final int CHANNEL_VIEW_DURATION = 5000;
    /** Views needed in activity. */
    private LinearLayout mChannelContainer = null;
    private TextView mChannelNumber = null;
    private TextView mChannelName = null;
    /** Handler for sending action messages to update UI. */
    private UiHandler mHandler = null;
    /** Subtitle and teletext views */
    private SurfaceView mSurfaceView;
    /**
     * PVR and Time shift stuff.
     */
    private View mPvrInfoContainer;
    /** PVR callback */
    private IPvrCallback mPvrCallback = new IPvrCallback() {
        @Override
        public void eventTimeshiftStop(PvrEventTimeshiftStop arg0) {
            Log.d(TAG, "eventTimeshiftStop");
            mDVBManager.getPvrManager().setTimeShftActive(false);
            try {
                mDVBManager.changeChannelByNumber(DTVActivity
                        .getLastWatchedChannelIndex());
            } catch (InternalException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void eventTimeshiftStart(PvrEventTimeshiftStart arg0) {
            Log.d(TAG, "eventTimeshiftStart");
            mDVBManager.getPvrManager().setTimeShftActive(true);
        }

        @Override
        public void eventTimeshiftSpeed(PvrEventTimeshiftSpeed arg0) {
            Log.d(TAG, "eventTimeshiftSpeed CHANGED " + arg0.getSpeed());
            mDVBManager.getPvrManager().setmPvrSpeedConst(arg0.getSpeed());
        }

        @Override
        public void eventTimeshiftPosition(PvrEventTimeshiftPosition arg0) {
            Log.d(TAG,
                    "TIME SHIFT PLAYBACK TIME POSITION CHANGED "
                            + arg0.getPlaybackTimePosition());
            Log.d(TAG,
                    "TIME SHIFT PLAYBACK SPACE POSITION CHANGED "
                            + arg0.getPlaybackSpacePosition());
            Log.d(TAG,
                    "TIME SHIFT RECORD SPACE POSITION CHANGED "
                            + arg0.getRecordSpacePosition());
            Log.d(TAG,
                    "TIME SHIFT RECORD TIME POSITION CHANGED "
                            + arg0.getRecordTimePosition());
            Log.d(TAG, "TIME SHIFT IS BEGIN " + arg0.isPlaybackBegin());
            Log.d(TAG, "TIME SHIFT IS END " + arg0.isPlaybackEnd());
            if (arg0.isPlaybackBegin()) {
                mDVBManager.getPvrManager().resetSpeedIndexes();
                mDVBManager.getPvrManager().setPvrSpeed(
                        PvrSpeedMode.PVR_SPEED_FORWARD_X1);
            } else if (arg0.isPlaybackEnd()) {
                mDVBManager.getPvrManager().resetSpeedIndexes();
                mDVBManager.getPvrManager().setPvrSpeed(
                        PvrSpeedMode.PVR_SPEED_FORWARD_X1);
            }
            /**
             * Calculate end time
             */
            // int timeshiftBufferSize = mDVBManager.getTimeShiftBufferSize();
            int endTime = 100;
            if (arg0.getRecordTimePosition() > endTime - 20) {
                endTime = arg0.getRecordTimePosition() + 20;
            }
            // int endTime = timeshiftBufferSize
            // * (arg0.getRecordTimePosition() - arg0
            // .getRecordTimePosition());
            mHandler.sendMessage(Message.obtain(mHandler,
                    UiHandler.REFRESH_TIMESHIFT_PLAYBACK,
                    new PvrTimeShiftPositionHolder(endTime, arg0)));
        }

        @Override
        public void eventTimeshiftJump(PvrEventTimeshiftJump arg0) {
        }

        @Override
        public void eventRecordStop(PvrEventRecordStop arg0) {
            Log.d(TAG, "\n\n\nRECORD STOPPED");
            mDVBManager.getPvrManager().setPvrActive(false);
        }

        @Override
        public void eventRecordStart(PvrEventRecordStart arg0) {
            Log.d(TAG, "\n\n\nRECORD STARTED");
            mDVBManager.getPvrManager().setPvrActive(true);
        }

        @Override
        public void eventRecordResourceIssue(PvrEventRecordResourceIssue arg0) {
        }

        @Override
        public void eventRecordRemove(PvrEventRecordRemove arg0) {
        }

        @Override
        public void eventRecordPosition(PvrEventRecordPosition arg0) {
            Log.d(TAG, "PVR TIME POSITION CHANGED " + arg0.getTimePosition());
            Log.d(TAG, "PVR SPACE POSITION CHANGED " + arg0.getSpacePosition());
            mHandler.sendMessage(Message.obtain(mHandler,
                    UiHandler.REFRESH_PVR_PLAYBACK, arg0));
        }

        @Override
        public void eventRecordConflict(PvrEventRecordConflict arg0) {
        }

        @Override
        public void eventRecordAdd(PvrEventRecordAdd arg0) {
        }

        @Override
        public void eventPlaybackStop(PvrEventPlaybackStop arg0) {
        }

        @Override
        public void eventPlaybackStart(PvrEventPlaybackStart arg0) {
        }

        @Override
        public void eventPlaybackSpeed(PvrEventPlaybackSpeed arg0) {
        }

        @Override
        public void eventPlaybackPosition(PvrEventPlaybackPosition arg0) {
        }

        @Override
        public void eventPlaybackJump(PvrEventPlaybackJump arg0) {
        }

        @Override
        public void eventMediaRemove(PvrEventMediaRemove arg0) {
        }

        @Override
        public void eventMediaAdd(PvrEventMediaAdd arg0) {
        }

        @Override
        public void eventDeviceError() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /** Initialize VideoView. */
        initializeVideoView();
        /** Initialize Channel Container. */
        initializeChannelContainer();
        /** Initialize subtitle and teletext views */
        initializeSubtitleAndTeletext();
        /** Initialize PVR container */
        initializePvrInfoContainer();
        /** Load default IP channel list */
        initIpChannels();
        /** Initialize Handler. */
        mHandler = new UiHandler(mChannelContainer, mPvrInfoContainer,
                mSurfaceView);
        /** Start DTV. */
        try {
            mDVBManager.startDTV(getLastWatchedChannelIndex());
        } catch (IllegalArgumentException e) {
            Toast.makeText(
                    this,
                    "Cant play service with index: "
                            + getLastWatchedChannelIndex(), Toast.LENGTH_SHORT)
                    .show();
        } catch (InternalException e) {
            /** Error with service connection. */
            finishActivity();
        }
        mDVBManager.getPvrManager().registerPvrCallback(mPvrCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /** Set Surface for subtitle and teletext */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    mDVBManager.getTeletextSubtitleAudioManager()
                            .initializeSubtitleAndTeletextDisplay(mSurfaceView,
                                    size.x, size.y);
                    refreshSurfaceView(mSurfaceView);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InternalException e) {
                    e.printStackTrace();
                }
            }
        }, 700);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mDVBManager.stopDTV();
        } catch (InternalException e) {
            e.printStackTrace();
        }
        sIpChannels = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /**
         * Subtitles automatic
         */
        MenuItem checkable = menu.findItem(R.id.menu_subtitles_automatic);
        checkable.setChecked(mDVBManager.getTeletextSubtitleAudioManager()
                .isSubtitleAutomatic());
        /**
         * Subtitles type
         */
        // SubtitleType type = mDVBManager.getTeletextSubtitleAudioManager()
        // .getSubtitleType();
        // checkable = menu.findItem(R.id.menu_subtitles_type_dvb);
        // checkable.setChecked(type == SubtitleType.DVB);
        // checkable = menu.findItem(R.id.menu_subtitles_type_ttxt);
        // checkable.setChecked(type == SubtitleType.TTX);
        /**
         * Subtitles mode
         */
        // SubtitleMode mode = mDVBManager.getTeletextSubtitleAudioManager()
        // .getSubtitleMode();
        // checkable = menu.findItem(R.id.menu_subtitles_mode_normal);
        // checkable.setChecked(mode == SubtitleMode.TRANSLATION);
        // checkable = menu.findItem(R.id.menu_subtitles_mode_hoh);
        // checkable.setChecked(mode == SubtitleMode.HEARING_IMPAIRED);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_scan_usb: {
                ArrayList<IPService> ipChannels = new ArrayList<IPService>();
                loadIPChannelsFromExternalStorage(ipChannels);
                sIpChannels = ipChannels;
                return true;
            }
            case R.id.menu_epg: {
                return true;
            }
            case R.id.menu_subtitles_automatic: {
                item.setChecked(!item.isChecked());
                mDVBManager.getTeletextSubtitleAudioManager()
                        .setSubtitleAutomatic(item.isChecked());
                return true;
            }
            // case R.id.menu_subtitles_type_dvb: {
            // if (item.isChecked())
            // mDVBManager.getTeletextSubtitleAudioManager()
            // .setSubtitleType(SubtitleType.DVB);
            // return true;
            // }
            // case R.id.menu_subtitles_type_ttxt: {
            // mDVBManager.getTeletextSubtitleAudioManager().setSubtitleType(
            // SubtitleType.TTX);
            // return true;
            // }
            case R.id.menu_subtitles_mode_normal: {
                mDVBManager.getTeletextSubtitleAudioManager().setSubtitleMode(
                        SubtitleMode.TRANSLATION);
                return true;
            }
            case R.id.menu_subtitles_mode_hoh: {
                mDVBManager.getTeletextSubtitleAudioManager().setSubtitleMode(
                        SubtitleMode.HEARING_IMPAIRED);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initialize IP
     */
    private void initIpChannels() {
        ContextWrapper contextWrapper = new ContextWrapper(this);
        String path = contextWrapper.getFilesDir() + "/"
                + DTVActivity.IP_CHANNELS;
        sIpChannels = new ArrayList<IPService>();
        DTVActivity.readFile(this, path, sIpChannels);
    }

    /**
     * Initialize VideoView and Set URI.
     * 
     * @return Instance of VideoView.
     */
    private void initializeVideoView() {
        VideoView videoView = ((VideoView) findViewById(R.id.videoView));
        videoView.setVideoURI(Uri.parse(TV_URI));
        videoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });
        videoView.start();
    }

    /**
     * Initialize LinearLayout and TextViews.
     */
    private void initializeChannelContainer() {
        mChannelContainer = (LinearLayout) findViewById(R.id.linearlayout_channel_container);
        mChannelContainer.setVisibility(View.GONE);
        mChannelNumber = (TextView) findViewById(R.id.textview_channel_number);
        mChannelName = (TextView) findViewById(R.id.textview_channel_name);
    }

    private void initializeSubtitleAndTeletext() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mSurfaceView.setVisibility(View.VISIBLE);
        mSurfaceView.setZOrderOnTop(true);
    }

    private void initializePvrInfoContainer() {
        mPvrInfoContainer = findViewById(R.id.pvr_info_container);
    }

    /**
     * Show Channel Name and Number of Current Channel on Channel Change.
     * 
     * @param channelInfo
     */
    private void showChannelInfo(ChannelInfo channelInfo) {
        if (channelInfo != null) {
            mChannelNumber.setText("" + channelInfo.getNumber());
            mChannelName.setText(channelInfo.getName());
            mChannelContainer.setVisibility(View.VISIBLE);
            mHandler.removeMessages(UiHandler.HIDE_CHANNEL_INFO_VIEW_MESSAGE);
            mHandler.sendEmptyMessageDelayed(
                    UiHandler.HIDE_CHANNEL_INFO_VIEW_MESSAGE,
                    CHANNEL_VIEW_DURATION);
        }
    }

    private void showPvrInfo() {
        mPvrInfoContainer.setVisibility(View.VISIBLE);
        mHandler.removeMessages(UiHandler.HIDE_PVR_INFO_MESSAGE);
        mHandler.sendEmptyMessageDelayed(UiHandler.HIDE_PVR_INFO_MESSAGE,
                CHANNEL_VIEW_DURATION);
    }

    /**
     * Listener For Keys.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "KEY PRESSED " + keyCode);
        /**
         * Disable non Teletext keys
         */
        if (mDVBManager.getTeletextSubtitleAudioManager().isTeletextActive()
                && !isTeletextKey(keyCode)) {
            return true;
        }
        if (mDVBManager.getPvrManager().isPvrActive() && !isPvrKey(keyCode)) {
            return true;
        }
        if (mDVBManager.getPvrManager().isTimeShftActive()
                && !isTimeShiftKey(keyCode)) {
            return true;
        }
        switch (keyCode) {
        /** Open Channel List. */
            case KeyEvent.KEYCODE_DPAD_CENTER: {
                new ChannelListDialog(this).show();
                return true;
            }
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
            case KeyEvent.KEYCODE_PROG_RED:
            case KeyEvent.KEYCODE_PROG_GREEN:
            case KeyEvent.KEYCODE_PROG_BLUE:
            case KeyEvent.KEYCODE_PROG_YELLOW: {
                if (mDVBManager.getTeletextSubtitleAudioManager()
                        .isTeletextActive()) {
                    mDVBManager.getTeletextSubtitleAudioManager()
                            .sendTeletextInputCommand(keyCode);
                    return true;
                }
            }
            /** TELETEXT KEY */
            case KeyEvent.KEYCODE_F5: {
                /** TTX is already active */
                if (mDVBManager.getTeletextSubtitleAudioManager()
                        .isTeletextActive()) {
                    try {
                        mDVBManager.getTeletextSubtitleAudioManager()
                                .changeTeletext(0);
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                }
                /** Show TTX */
                else {
                    /** Hide subtitles if they are active */
                    if (mDVBManager.getTeletextSubtitleAudioManager()
                            .isSubtitleActive()) {
                        try {
                            mDVBManager.getTeletextSubtitleAudioManager()
                                    .hideSubtitles();
                        } catch (InternalException e) {
                            e.printStackTrace();
                        }
                    }
                    int trackCount = mDVBManager
                            .getTeletextSubtitleAudioManager()
                            .getTeletextTrackCount();
                    if (trackCount > 0) {
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                this, android.R.layout.simple_list_item_1);
                        for (int i = 0; i < trackCount; i++) {
                            TeletextTrack track = mDVBManager
                                    .getTeletextSubtitleAudioManager()
                                    .getTeletextTrack(i);
                            String type = mDVBManager
                                    .getTeletextSubtitleAudioManager()
                                    .convertTeletextTrackTypeToHumanReadableFormat(
                                            track.getType());
                            arrayAdapter.add(track.getName() + " [" + type
                                    + "]");
                        }
                        createListDIalog("Select teletext track", arrayAdapter,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        try {
                                            if (mDVBManager
                                                    .getTeletextSubtitleAudioManager()
                                                    .changeTeletext(which)) {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Teletext started",
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                            } else {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Teletext is not available",
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                            }
                                        } catch (InternalException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(this, "No teletext available!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            /** SUBTITLES KEY. */
            case KeyEvent.KEYCODE_CAPTIONS: {
                /** Hide subtitles. */
                if (mDVBManager.getTeletextSubtitleAudioManager()
                        .isSubtitleActive()) {
                    try {
                        mDVBManager.getTeletextSubtitleAudioManager()
                                .hideSubtitles();
                        Toast.makeText(this, "Subtitle stopped",
                                Toast.LENGTH_SHORT).show();
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                }
                /** Show subtitles. */
                else {
                    int trackCount = mDVBManager
                            .getTeletextSubtitleAudioManager()
                            .getSubtitlesTrackCount();
                    if (trackCount > 0) {
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                this, android.R.layout.simple_list_item_1);
                        for (int i = 0; i < trackCount; i++) {
                            SubtitleTrack track = mDVBManager
                                    .getTeletextSubtitleAudioManager()
                                    .getSubtitleTrack(i);
                            arrayAdapter
                                    .add(track.getName()
                                            + " ["
                                            + mDVBManager
                                                    .getTeletextSubtitleAudioManager()
                                                    .convertSubtitleTrackModeToHumanReadableFormat(
                                                            track.getType())
                                            + "]");
                        }
                        createListDIalog("Select subtitle track", arrayAdapter,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        try {
                                            if (mDVBManager
                                                    .getTeletextSubtitleAudioManager()
                                                    .showSubtitles(which)) {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Subtitle started",
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                            } else {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Subtitle is not available",
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                            }
                                        } catch (InternalException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Subtitle is not available",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            /**
             * AUDIO LANGUAGES
             */
            case KeyEvent.KEYCODE_F6: {
                int trackCount = mDVBManager.getTeletextSubtitleAudioManager()
                        .getAudioLanguagesTrackCount();
                if (trackCount > 0) {
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                            this, android.R.layout.simple_list_item_1);
                    for (int i = 0; i < trackCount; i++) {
                        AudioTrack track = mDVBManager
                                .getTeletextSubtitleAudioManager()
                                .getAudioLanguage(i);
                        arrayAdapter.add(track.getName() + " "
                                + track.getLanguage() + "   ["
                                + track.getAudioDigitalType() + "]["
                                + track.getAudioChannleCfg() + "]");
                    }
                    createListDIalog("Select audio track", arrayAdapter,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    try {
                                        mDVBManager
                                                .getTeletextSubtitleAudioManager()
                                                .setAudioTrack(which);
                                    } catch (InternalException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(this, "Audio tracks are not available",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            /**
             * Change Channel Up (Using of KEYCODE_F4 is just workaround because
             * KeyEvent.KEYCODE_CHANNEL_UP is not mapped on remote control).
             */
            case KeyEvent.KEYCODE_F4:
            case KeyEvent.KEYCODE_CHANNEL_UP: {
                try {
                    showChannelInfo(mDVBManager.changeChannelUp());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InternalException e) {
                    e.printStackTrace();
                }
                return true;
            }
            /**
             * Change Channel Down (Using of KEYCODE_F3 is just workaround
             * because KeyEvent.KEYCODE_CHANNEL_DOWN is not mapped on remote
             * control).
             */
            case KeyEvent.KEYCODE_F3:
            case KeyEvent.KEYCODE_CHANNEL_DOWN: {
                try {
                    showChannelInfo(mDVBManager.changeChannelDown());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InternalException e) {
                    e.printStackTrace();
                }
                return true;
            }
            /**
             * SHOW INFORMATION SCREEN
             */
            case KeyEvent.KEYCODE_INFO: {
                if (mDVBManager.getPvrManager().isTimeShftActive()
                        || mDVBManager.getPvrManager().isPvrActive()) {
                    showPvrInfo();
                    return true;
                }
                showChannelInfo(mDVBManager.getChannelInfo(mDVBManager
                        .getCurrentChannelNumber()));
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: {
                if (mDVBManager.getPvrManager().isTimeShftActive()) {
                    if (mDVBManager.getPvrManager().getPvrSpeed() == PvrSpeedMode.PVR_SPEED_PAUSE) {
                        mDVBManager.getPvrManager().setPvrSpeed(
                                PvrSpeedMode.PVR_SPEED_FORWARD_X1);
                    } else {
                        mDVBManager.getPvrManager().setPvrSpeed(
                                PvrSpeedMode.PVR_SPEED_PAUSE);
                    }
                } else {
                    try {
                        mDVBManager.getPvrManager().startTimeShift();
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                }
                showPvrInfo();
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_STOP: {
                if (mDVBManager.getPvrManager().isTimeShftActive()) {
                    try {
                        mDVBManager.getPvrManager().stopTimeShift();
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                } else if (mDVBManager.getPvrManager().isPvrActive()) {
                    mDVBManager.getPvrManager().stopPvr();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
                if (mDVBManager.getPvrManager().isTimeShftActive()
                        || mDVBManager.getPvrManager().isPvrActive()) {
                    mDVBManager.getPvrManager().fastForward();
                    showPvrInfo();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_REWIND: {
                if (mDVBManager.getPvrManager().isTimeShftActive()
                        || mDVBManager.getPvrManager().isPvrActive()) {
                    mDVBManager.getPvrManager().rewind();
                    showPvrInfo();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_RECORD: {
                if (!mDVBManager.getPvrManager().isPvrActive()
                        && !mDVBManager.getPvrManager().isTimeShftActive()) {
                    try {
                        mDVBManager.getPvrManager().startOneTouchRecord();
                        showPvrInfo();
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
            default: {
                return super.onKeyDown(keyCode, event);
            }
        }
    }

    private static void refreshSurfaceView(SurfaceView surface) {
        if (surface.getVisibility() == View.VISIBLE) {
            Canvas canvas = surface.getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                surface.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Handler for sending action messages to update UI.
     */
    private static class UiHandler extends Handler {
        /** Message ID for Hiding Channel Number/Name View. */
        public static final int HIDE_CHANNEL_INFO_VIEW_MESSAGE = 0,
                HIDE_PVR_INFO_MESSAGE = 1, REFRESH_TIMESHIFT_PLAYBACK = 2,
                REFRESH_PVR_PLAYBACK = 3;
        private View mChannelContainer, mPvrContainer;
        /** PVR info container views */
        private TextView mPvrInfoPosition, mPvrInfoAvailablePosition;
        private ProgressBar mPvrProgressBar;
        private static final SimpleDateFormat sFormat = new SimpleDateFormat(
                "HH:mm:ss");
        private SurfaceView mSurface;

        public UiHandler(View channelContainer, View pvrContainer,
                SurfaceView surface) {
            mSurface = surface;
            mChannelContainer = channelContainer;
            mPvrContainer = pvrContainer;
            mPvrInfoPosition = (TextView) mPvrContainer
                    .findViewById(R.id.textViewPosition);
            mPvrInfoAvailablePosition = (TextView) mPvrContainer
                    .findViewById(R.id.textViewAvailablePosition);
            mPvrProgressBar = (ProgressBar) mPvrContainer
                    .findViewById(R.id.progressBar);
            mPvrProgressBar.setMax(100);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_CHANNEL_INFO_VIEW_MESSAGE: {
                    mChannelContainer.setVisibility(View.GONE);
                    refreshSurfaceView(mSurface);
                    break;
                }
                case HIDE_PVR_INFO_MESSAGE: {
                    mPvrContainer.setVisibility(View.GONE);
                    refreshSurfaceView(mSurface);
                    break;
                }
                case REFRESH_TIMESHIFT_PLAYBACK: {
                    PvrTimeShiftPositionHolder object = (PvrTimeShiftPositionHolder) msg.obj;
                    int numberOfSeconds = object.getPositionObject()
                            .getPlaybackTimePosition();
                    Date date = new Date(numberOfSeconds * 1000);
                    mPvrInfoPosition.setText(sFormat.format(date));
                    mPvrProgressBar.setProgress(numberOfSeconds);
                    numberOfSeconds = object.getPositionObject()
                            .getRecordTimePosition();
                    mPvrProgressBar.setSecondaryProgress(numberOfSeconds);
                    numberOfSeconds = object.getEndTime();
                    date = new Date(numberOfSeconds * 1000);
                    mPvrInfoAvailablePosition.setText(sFormat.format(date));
                    mPvrProgressBar.setMax(numberOfSeconds);
                    break;
                }
                case REFRESH_PVR_PLAYBACK: {
                    PvrEventRecordPosition object = (PvrEventRecordPosition) msg.obj;
                    int numberOfSeconds = object.getTimePosition();
                    Date date = new Date(numberOfSeconds * 1000);
                    mPvrInfoPosition.setText(sFormat.format(date));
                    mPvrProgressBar.setProgress(numberOfSeconds);
                    mPvrProgressBar.setSecondaryProgress(0);
                    numberOfSeconds = object.getSpacePosition();
                    date = new Date(numberOfSeconds * 1000);
                    mPvrInfoAvailablePosition.setText(sFormat.format(date));
                    mPvrProgressBar.setMax(numberOfSeconds);
                    break;
                }
            }
        }
    }

    private boolean isTeletextKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
            case KeyEvent.KEYCODE_PROG_RED:
            case KeyEvent.KEYCODE_PROG_GREEN:
            case KeyEvent.KEYCODE_PROG_BLUE:
            case KeyEvent.KEYCODE_PROG_YELLOW:
            case KeyEvent.KEYCODE_F5: {
                return true;
            }
            default:
                return false;
        }
    }

    private boolean isPvrKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_INFO:
            case KeyEvent.KEYCODE_MEDIA_STOP: {
                return true;
            }
            default:
                return false;
        }
    }

    private boolean isTimeShiftKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_INFO:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP: {
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Create alert dialog with entries
     * 
     * @param title
     * @param arrayAdapter
     * @param listClickListener
     */
    private void createListDIalog(String title,
            final ArrayAdapter<String> arrayAdapter,
            DialogInterface.OnClickListener listClickListener) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle(title);
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(arrayAdapter, listClickListener);
        builderSingle.show();
    }

    private class PvrTimeShiftPositionHolder {
        private int mEndTime;
        private PvrEventTimeshiftPosition mPositionObject;

        public PvrTimeShiftPositionHolder(int mEndTime,
                PvrEventTimeshiftPosition mPositionObject) {
            super();
            this.mEndTime = mEndTime;
            this.mPositionObject = mPositionObject;
        }

        public int getEndTime() {
            return mEndTime;
        }

        public PvrEventTimeshiftPosition getPositionObject() {
            return mPositionObject;
        }
    }
}
