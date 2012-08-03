/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.test.activity;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.util.Log;

public class ActivityTestMain extends Activity {
    static final String TAG = "ActivityTest";

    ActivityManager mAm;

    private void addThumbnail(LinearLayout container, Bitmap bm,
            final ActivityManager.RecentTaskInfo task,
            final ActivityManager.TaskThumbnails thumbs, final int subIndex) {
        ImageView iv = new ImageView(this);
        if (bm != null) {
            iv.setImageBitmap(bm);
        }
        iv.setBackgroundResource(android.R.drawable.gallery_thumb);
        int w = getResources().getDimensionPixelSize(android.R.dimen.thumbnail_width);
        int h = getResources().getDimensionPixelSize(android.R.dimen.thumbnail_height);
        container.addView(iv, new LinearLayout.LayoutParams(w, h));

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (task.id >= 0 && thumbs != null) {
                    if (subIndex < (thumbs.numSubThumbbails-1)) {
                        mAm.removeSubTask(task.id, subIndex+1);
                    }
                    mAm.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME);
                } else {
                    try {
                        startActivity(task.baseIntent);
                    } catch (ActivityNotFoundException e) {
                        Log.w("foo", "Unable to start task: " + e);
                    }
                }
                buildUi();
            }
        });
        iv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (task.id >= 0 && thumbs != null) {
                    if (subIndex < 0) {
                        mAm.removeTask(task.id, ActivityManager.REMOVE_TASK_KILL_PROCESS);
                    } else {
                        mAm.removeSubTask(task.id, subIndex);
                    }
                    buildUi();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAm = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Animate!").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ActivityTestMain.this,
                        R.style.SlowDialog);
                builder.setTitle("This is a title");
                builder.show();
                return true;
            }
        });
        menu.add("Bind!").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ActivityTestMain.this, SingleUserService.class);
                ServiceConnection conn = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.i(TAG, "Service connected " + name + " " + service);
                    }
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.i(TAG, "Service disconnected " + name);
                    }
                };
                bindService(intent, conn, Context.BIND_AUTO_CREATE);
                return true;
            }
        });
        menu.add("Start!").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ActivityTestMain.this, SingleUserService.class);
                startService(intent);
                return true;
            }
        });
        menu.add("Send!").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ActivityTestMain.this, UserTarget.class);
                sendBroadcastToUser(intent, 1);
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        buildUi();
    }

    private View scrollWrap(View view) {
        ScrollView scroller = new ScrollView(this);
        scroller.addView(view, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        return scroller;
    }

    private void buildUi() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);

        List<ActivityManager.RecentTaskInfo> recents = mAm.getRecentTasks(10,
                ActivityManager.RECENT_WITH_EXCLUDED);
        if (recents != null) {
            for (int i=0; i<recents.size(); i++) {
                ActivityManager.RecentTaskInfo r = recents.get(i);
                ActivityManager.TaskThumbnails tt = mAm.getTaskThumbnails(r.persistentId);
                TextView tv = new TextView(this);
                tv.setText(r.baseIntent.getComponent().flattenToShortString());
                top.addView(tv, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                addThumbnail(item, tt != null ? tt.mainThumbnail : null, r, tt, -1);
                for (int j=0; j<tt.numSubThumbbails; j++) {
                    addThumbnail(item, tt.getSubThumbnail(j), r, tt, j);
                }
                top.addView(item, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }

        setContentView(scrollWrap(top));
    }
}
