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
package com.iwedia.exampleip.adapters;

import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.iwedia.dtv.types.InternalException;
import com.iwedia.exampleip.dtv.DVBManager;
import com.iwedia.four.R;

import java.util.ArrayList;

/**
 * Adapter for Channel List GridView.
 */
public class ChannelListAdapter extends BaseAdapter {
    private final String TAG = "ChannelListAdapter";
    private LayoutInflater mLayoutInflater = null;
    private ArrayList<String> mChannelNames = null;
    private boolean inChannelLockedState = false;

    public ChannelListAdapter(Context context, ArrayList<String> channelNames) {
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        mChannelNames = channelNames;
    }

    @Override
    public int getCount() {
        return mChannelNames.size();
    }

    @Override
    public Object getItem(int position) throws IndexOutOfBoundsException {
        return mChannelNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChannelHolder lHolder = null;
        if (null == convertView) {
            convertView = mLayoutInflater.inflate(R.layout.channel_list_item,
                    null);
            lHolder = new ChannelHolder(convertView);
            convertView.setTag(lHolder);
        } else {
            lHolder = (ChannelHolder) convertView.getTag();
        }
        setChannelItemView(position, lHolder);
        return convertView;
    }

    /**
     * Populate grid view element with appropriate data.
     * 
     * @param position
     *        Grid view element position.
     * @param holder
     *        View holder instance for this element.
     */
    private void setChannelItemView(int position, ChannelHolder holder) {
        holder.getItemChannelName().setText(mChannelNames.get(position));
        holder.getItemChannelNumber().setText(String.valueOf(position + 1));
        if (inChannelLockedState) {
            holder.getCheckBoxLock().setVisibility(View.VISIBLE);
            try {
                boolean status = DVBManager.getInstance().getParentalManager()
                        .getChannelLockStatus(position);
                holder.getCheckBoxLock().setChecked(status);
            } catch (InternalException e) {
                e.printStackTrace();
            }
        } else {
            holder.getCheckBoxLock().setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Holder for GridView Elements.
     */
    private class ChannelHolder {
        private TextView mItemChannelNumber = null;
        private TextView mItemChannelName = null;
        private CheckBox mCheckBoxLock = null;

        protected ChannelHolder(View view) {
            mItemChannelNumber = (TextView) view
                    .findViewById(R.id.textview_channel_number);
            mItemChannelName = (TextView) view
                    .findViewById(R.id.textview_channel_name);
            mCheckBoxLock = (CheckBox) view.findViewById(R.id.check_box_locked);
        }

        protected TextView getItemChannelNumber() {
            return mItemChannelNumber;
        }

        protected TextView getItemChannelName() {
            return mItemChannelName;
        }

        protected CheckBox getCheckBoxLock() {
            return mCheckBoxLock;
        }
    }

    public boolean isInChannelLockedState() {
        return inChannelLockedState;
    }

    public void setInChannelLockedState(boolean inChannelLockedState) {
        this.inChannelLockedState = inChannelLockedState;
        notifyDataSetChanged();
    }
}
