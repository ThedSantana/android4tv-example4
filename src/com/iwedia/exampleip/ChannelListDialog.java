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
import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;

import com.iwedia.custom.EnterPinDialog;
import com.iwedia.custom.EnterPinDialog.PinCheckedCallback;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.exampleip.adapters.ChannelListAdapter;
import com.iwedia.exampleip.dtv.DVBManager;
import com.iwedia.four.R;

/**
 * Channel List Activity.
 */
public class ChannelListDialog extends Dialog implements OnItemClickListener {
    public static final String TAG = "ChannelListActivity";
    private GridView mChannelList;
    private Activity mActivity;

    public ChannelListDialog(Activity activity, int width, int height) {
        super(activity, R.style.DialogTransparent);
        mActivity = activity;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        setContentView(R.layout.channel_list_activity);
        getWindow().getAttributes().width = width;
        getWindow().getAttributes().height = height;
        /** Initialize GridView. */
        initializeChannelList(activity);
    }

    /**
     * Initialize GridView (Channel List) and set click listener to it.
     * 
     * @throws RemoteException
     *         If connection error happens.
     */
    private void initializeChannelList(Context context) {
        mChannelList = (GridView) findViewById(R.id.gridview_channellist);
        mChannelList.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.channel_lock, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.menu_channel_lock) {
            if (((ChannelListAdapter) mChannelList.getAdapter())
                    .isInChannelLockedState()) {
                ((ChannelListAdapter) mChannelList.getAdapter())
                        .setInChannelLockedState(false);
            } else {
                EnterPinDialog dialog = new EnterPinDialog(mActivity,
                        new PinCheckedCallback() {
                            @Override
                            public void pinChecked(boolean pinOk) {
                                ((ChannelListAdapter) mChannelList.getAdapter())
                                        .setInChannelLockedState(true);
                            }
                        });
                dialog.show();
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ((ChannelListAdapter) mChannelList.getAdapter())
                .setInChannelLockedState(false);
    }

    @Override
    public void show() {
        super.show();
        mChannelList.setAdapter(new ChannelListAdapter(mActivity, DVBManager
                .getInstance().getChannelNames()));
        try {
            mChannelList.setSelection(DVBManager.getInstance()
                    .getCurrentChannelNumber());
        } catch (InternalException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (((ChannelListAdapter) parent.getAdapter()).isInChannelLockedState()) {
            CheckBox checkBox = (CheckBox) v
                    .findViewById(R.id.check_box_locked);
            try {
                DVBManager.getInstance().getParentalManager()
                        .setChannelLock(position, !checkBox.isChecked());
            } catch (InternalException e) {
                e.printStackTrace();
            }
            checkBox.setChecked(!checkBox.isChecked());
            v.invalidate();
        } else {
            try {
                DVBManager.getInstance().changeChannelByNumber(position);
                cancel();
            } catch (InternalException e) {
                e.printStackTrace();
            }
        }
    }
}
