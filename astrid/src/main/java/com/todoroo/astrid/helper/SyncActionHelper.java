/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.sync.IndeterminateProgressBarSyncResultCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * SyncActionHelper is a helper class for encapsulating UI actions
 * responsible for performing sync and prompting user to sign up for a new
 * sync service.
 *
 * In order to make this work you need to call register() and unregister() in
 * onResume and onPause, respectively.
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SyncActionHelper {

    public static final String PREF_LAST_AUTO_SYNC = "taskListLastAutoSync"; //$NON-NLS-1$

    public final SyncResultCallback syncResultCallback;

    private final SyncV2Service syncService;
    private final Activity activity;
    private final Preferences preferences;
    private final Fragment fragment;

    // --- boilerplate

    public SyncActionHelper(GtasksPreferenceService gtasksPreferenceService, SyncV2Service syncService, final FragmentActivity activity, Preferences preferences, Fragment fragment) {
        this.syncService = syncService;
        this.activity = activity;
        this.preferences = preferences;
        this.fragment = fragment;
        syncResultCallback = new IndeterminateProgressBarSyncResultCallback(gtasksPreferenceService, activity, new Runnable() {
                    @Override
                    public void run() {
                        activity.sendBroadcast(
                                new Intent(
                                        AstridApiConstants.BROADCAST_EVENT_REFRESH));
                    }
                });
    }

    // --- automatic sync logic

    public void initiateAutomaticSync() {
        long tasksPushedAt = preferences.getLong(PREF_LAST_AUTO_SYNC, 0);
        if (DateUtilities.now() - tasksPushedAt > TaskListFragment.AUTOSYNC_INTERVAL) {
            performSyncServiceV2Sync();
        }
    }

    // --- sync logic

    protected void performSyncServiceV2Sync() {
        boolean syncOccurred = syncService.synchronizeActiveTasks(syncResultCallback);
        if (syncOccurred) {
            preferences.setLong(PREF_LAST_AUTO_SYNC, DateUtilities.now());
        }
    }

    /**
     * Intent object with custom label returned by toString.
     *
     * @author joshuagross <joshua.gross@gmail.com>
     */
    protected static class IntentWithLabel extends Intent {
        private final String label;

        public IntentWithLabel(Intent in, String labelIn) {
            super(in);
            label = labelIn;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public void performSyncAction() {
        if (syncService.isActive()) {
            syncService.synchronizeActiveTasks(syncResultCallback);
        } else {
            String desiredCategory = activity.getString(R.string.SyP_label);

            // Get a list of all sync plugins and bring user to the prefs pane
            // for one of them
            Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            PackageManager pm = activity.getPackageManager();
            List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(
                    queryIntent, PackageManager.GET_META_DATA);
            ArrayList<Intent> syncIntents = new ArrayList<>();

            // Loop through a list of all packages (including plugins, addons)
            // that have a settings action: filter to sync actions
            for (ResolveInfo resolveInfo : resolveInfoList) {
                Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
                intent.setClassName(resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);

                String category = MetadataHelper.resolveActivityCategoryName(
                        resolveInfo, pm);

                if (resolveInfo.activityInfo.metaData != null) {
                    Bundle metadata = resolveInfo.activityInfo.metaData;
                    if (!metadata.getBoolean("syncAction")) //$NON-NLS-1$
                    {
                        continue;
                    }
                }

                if (category.equals(desiredCategory)) {
                    syncIntents.add(new IntentWithLabel(intent,
                            resolveInfo.activityInfo.loadLabel(pm).toString()));
                }
            }

            final Intent[] actions = syncIntents.toArray(new Intent[syncIntents.size()]);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface click, int which) {
                    fragment.startActivityForResult(actions[which], TaskListFragment.ACTIVITY_SETTINGS);
                }
            };
            if (actions.length == 1) {
                fragment.startActivityForResult(actions[0], TaskListFragment.ACTIVITY_SETTINGS);
            } else {
                showSyncOptionMenu(actions, listener);
            }
        }
    }

    /**
     * Show menu of sync options. This is shown when you're not logged into any
     * services, or logged into more than one.
     */
    private <TYPE> void showSyncOptionMenu(TYPE[] items, DialogInterface.OnClickListener listener) {
        if (items.length == 1) {
            listener.onClick(null, 0);
            return;
        }

        ArrayAdapter<TYPE> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, items);

        // show a menu of available options
        new AlertDialog.Builder(activity).setTitle(R.string.Sync_now_label).setAdapter(
                adapter, listener).show().setOwnerActivity(activity);
    }
}


