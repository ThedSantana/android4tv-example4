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
package com.iwedia.exampleip;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.iwedia.dtv.types.InternalException;
import com.iwedia.exampleip.dtv.ChannelInfo;
import com.iwedia.exampleip.dtv.DVBManager;
import com.iwedia.exampleip.dtv.DVBManager.DVBStatus;
import com.iwedia.exampleip.dtv.IPService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Parent class off all activities. This class contains connection to dtv
 * service through dtv manager object.
 */
public abstract class DTVActivity extends Activity {
    public static final String TAG = "DTV_EXAMPLE_IP";
    private static final String LAST_WATCHED_CHANNEL_INDEX = "last_watched";
    public static final String EXTERNAL_MEDIA_PATH = "/mnt/media/";
    public static final String IP_CHANNELS = "ip_service_list.txt";
    private static final int MESSAGE_UPDATE_NOW_NEXT = 1,
            MESSAGE_AGE_LOCKED = 2, MESSAGE_CHANNEL_LOCKED = 3;
    private static DTVActivity instance;
    /** DTV manager instance. */
    protected DVBManager mDVBManager = null;
    /** List of IP channels */
    public static ArrayList<IPService> sIpChannels = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        /** Set Full Screen Application. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        Window window = getWindow();
        if (window != null) {
            window.setFormat(PixelFormat.RGBA_8888);
            window.addFlags(WindowManager.LayoutParams.FLAG_DITHER);
            window.getDecorView().getBackground().setDither(true);
        }
        /** Creates dtv manager object and connects it to service. */
        mDVBManager = DVBManager.getInstance();
        mDVBManager.registerEpgCallback();
        mDVBManager.setDVBStatus(mDvbStatusCallBack);
        initializeIpChannels();
    }

    public static SharedPreferences getSharedPreferences() {
        return instance.getSharedPreferences(TAG, MODE_PRIVATE);
    }

    public static void setLastWatchedChannelIndex(int index) {
        getSharedPreferences().edit().putInt(LAST_WATCHED_CHANNEL_INDEX, index)
                .commit();
    }

    public static int getLastWatchedChannelIndex() {
        return getSharedPreferences().getInt(LAST_WATCHED_CHANNEL_INDEX, 0);
    }

    public void finishActivity() {
        Toast.makeText(this,
                "Error with DTV service connection, closing application...",
                Toast.LENGTH_LONG).show();
        super.finish();
    }

    /**
     * Initialize IP
     */
    private void initializeIpChannels() {
        copyFile(IP_CHANNELS);
    }

    /**
     * Copy configuration file.
     */
    private void copyFile(String filename) {
        ContextWrapper contextWrapper = new ContextWrapper(this);
        String file = contextWrapper.getFilesDir().getPath() + "/" + filename;
        File fl = new File(file);
        if (!fl.exists())
            copyAssetToData(fl);
    }

    /**
     * Copy configuration file from assets to data folder.
     * 
     * @param strFilename
     */
    private void copyAssetToData(File file) {
        /** Open your local db as the input stream */
        try {
            InputStream myInput = getAssets().open(file.getName());
            String outFileName = file.getPath();
            /** Open the empty db as the output stream */
            OutputStream myOutput = new FileOutputStream(outFileName);
            /** transfer bytes from the inputfile to the outputfile */
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            /** Close the streams */
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the configuration file with built-in application which will be
     * displayed in Content list.
     */
    public static void readFile(Context ctx, String filePath,
            ArrayList<IPService> arrayList) {
        File file = new File(filePath);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            String[] separated = new String[2];
            while ((line = br.readLine()) != null) {
                separated = line.split("#");
                arrayList.add(new IPService(separated[0], separated[1]));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        br = null;
    }

    /**
     * Load list of IP channels from external storage.
     * 
     * @param ipChannels
     *        List to populate with IP channels.
     */
    public void loadIPChannelsFromExternalStorage(
            ArrayList<IPService> ipChannels) {
        ArrayList<File> ipServiceListFiles = new ArrayList<File>();
        File[] storages = new File(EXTERNAL_MEDIA_PATH).listFiles();
        if (storages != null) {
            /**
             * Loop through storages
             */
            for (File storage : storages) {
                File[] foundIpFiles = storage.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.getName().equalsIgnoreCase(IP_CHANNELS)) {
                            return true;
                        }
                        return false;
                    }
                });
                /**
                 * Files with given name are found in this array
                 */
                if (foundIpFiles != null) {
                    for (File ip : foundIpFiles) {
                        ipServiceListFiles.add(ip);
                    }
                }
            }
            /**
             * Loop through found files and add it to IP service list
             */
            for (File ipFile : ipServiceListFiles) {
                readFile(this, ipFile.getPath(), ipChannels);
            }
            /**
             * No files found
             */
            if (ipServiceListFiles.size() == 0) {
                Toast.makeText(this,
                        "No files found with name: " + IP_CHANNELS,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Show Channel Name and Number of Current Channel on Channel Change.
     * 
     * @param channelInfo
     */
    public abstract void showChannelInfo();

    /**
     * Sets Channel Name and Number of Current Channel on Channel Change.
     * 
     * @param channelInfo
     */
    public abstract void setChannelInfo(ChannelInfo channelInfo);

    /**
     * Show/hide age locked info layout.
     * 
     * @param locked
     *        Is event age locked.
     */
    public abstract void showAgeLockedInfo(boolean locked);

    /**
     * Show/hide channel locked info layout.
     * 
     * @param locked
     *        Is channel locked.
     */
    public abstract void showChannelLockedInfo(boolean locked);

    /** Handler */
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_NOW_NEXT: {
                    try {
                        setChannelInfo(mDVBManager.getChannelInfo(
                                mDVBManager.getCurrentChannelNumber(), false));
                    } catch (InternalException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case MESSAGE_AGE_LOCKED: {
                    showAgeLockedInfo((Boolean) msg.obj);
                    break;
                }
                case MESSAGE_CHANNEL_LOCKED: {
                    showChannelLockedInfo((Boolean) msg.obj);
                    break;
                }
                default:
                    break;
            }
        };
    };
    /**
     * DVB CallBack.
     */
    private DVBStatus mDvbStatusCallBack = new DVBStatus() {
        @Override
        public void updateNowNext() {
            Message.obtain(mHandler, MESSAGE_UPDATE_NOW_NEXT).sendToTarget();
        }

        @Override
        public void ageLocked(boolean locked) {
            Message.obtain(mHandler, MESSAGE_AGE_LOCKED, locked).sendToTarget();
        }

        @Override
        public void channelLocked(boolean locked) {
            Message.obtain(mHandler, MESSAGE_CHANNEL_LOCKED, locked)
                    .sendToTarget();
        }
    };
}
