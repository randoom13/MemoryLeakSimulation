package com.amber.random.memoryleaksimulation;


import com.github.mikephil.charting.data.Entry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsDatabase {
    public static final int LIVE_LEAKED_ACTIVITY = 2;
    public static final int LIVE_EMPTY_ACTIVITY = 0;
    public static final int DESTROYED_EMPTY_ACTIVITY = 1;
    public static final int DESTROYED_LEAKED_ACTIVITY = 3;

    private static StatisticsDatabase sInternalInstance = new StatisticsDatabase();
    private static int sBytesPerKB = 1024;
    private List<Long> mMemoryUsageHistory = new ArrayList<>();
    private Map<Integer, List<Entry>> mDatas = new HashMap<>();

    private List<ActivityInfo> mActivitiesList = new ArrayList<>();

    private StatisticsDatabase() {
    }

    public static StatisticsDatabase getInstance() {
        return sInternalInstance;
    }

    public void addActivity(LeakingActivity activity) {
        mActivitiesList.add(new ActivityInfo(activity));
        addItem(LIVE_LEAKED_ACTIVITY);
        addItem(LIVE_EMPTY_ACTIVITY);
        addItem(DESTROYED_EMPTY_ACTIVITY);
        addItem(DESTROYED_LEAKED_ACTIVITY);
    }

    private void addItem(int activityType) {
        List<Entry> entries = mDatas.get(activityType);
        if (null == entries) {
            entries = new ArrayList<>();
            mDatas.put(activityType, entries);
        }
        entries.add(new Entry(entries.size(), getActivitiesCount(activityType)));
    }

    private void addUsedMemoryHistory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInKB = (runtime.totalMemory() - runtime.freeMemory()) / sBytesPerKB;
        mMemoryUsageHistory.add(usedMemInKB);
    }

    public List<Entry> getLinesData(int activityType) {
        return Collections.unmodifiableList(mDatas.get(activityType));
    }


    public int getActivitiesCount(int activityType) {
        int count = 0;
        boolean isDestroyed = (activityType & 1) == 1;
        boolean isLeaking = (activityType & 2) == 2;
        for (ActivityInfo activityInfo : mActivitiesList)
            if (activityInfo.isLive() != isDestroyed && activityInfo.isLeaking == isLeaking)
                count++;

        return count;
    }

    public int activitiesSum() {
        return mActivitiesList.size();
    }

    private static class ActivityInfo {
        public final boolean isLeaking;
        private final WeakReference<LeakingActivity> mLeakingActivityWR;

        public ActivityInfo(LeakingActivity activity) {
            isLeaking = activity.isLeaking();
            mLeakingActivityWR = new WeakReference<LeakingActivity>(activity);
        }

        public boolean isLive() {
            return null != mLeakingActivityWR.get();
        }
    }
}
