package com.amber.random.memoryleaksimulation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String sHideLiveLeakedActivity = "hide_live_leaked_activity";
    public static final String sHideLiveEmptyActivity = "hide_live_empty_activity";
    public static final String sHideDestroyedEmptyActivity = "hide_destroyed_empty_activity";
    public static final String sHideDestroyedLeakedActivity = "hide_destroyed_leaked_activity";
    private static List<ActivitiesTypeDrawInfo> mActivitiesTypeList = new ArrayList<ActivitiesTypeDrawInfo>() {
        {
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.LIVE_LEAKED_ACTIVITY, R.string.live_leaked_activity, R.color.colorLiveLeakedActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.LIVE_EMPTY_ACTIVITY, R.string.live_empty_activity, R.color.colorLiveEmptyActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.DESTROYED_EMPTY_ACTIVITY, R.string.destroyed_empty_activity, R.color.colorDestroyedEmptyActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.DESTROYED_LEAKED_ACTIVITY, R.string.destroyed_leaked_activity, R.color.colorDestroyedLeakedActivity));
        }
    };
    private static Map<Integer, MenuItemsInfo> mActivitiesMenuItemsInfo = new HashMap<Integer, MenuItemsInfo>() {
        {
            put(StatisticsDatabase.LIVE_LEAKED_ACTIVITY,
                    new MenuItemsInfo(R.id.show_live_leaked_activity_line, R.id.hide_live_leaked_activity_line, sHideLiveLeakedActivity));
            put(StatisticsDatabase.DESTROYED_LEAKED_ACTIVITY,
                    new MenuItemsInfo(R.id.show_destroyed_leaked_activity_line, R.id.hide_destroyed_leaked_activity_line, sHideDestroyedLeakedActivity));
            put(StatisticsDatabase.DESTROYED_EMPTY_ACTIVITY,
                    new MenuItemsInfo(R.id.show_destroyed_activity_line, R.id.hide_destroyed_activity_line, sHideDestroyedEmptyActivity));
            put(StatisticsDatabase.LIVE_EMPTY_ACTIVITY,
                    new MenuItemsInfo(R.id.show_live_activity_line, R.id.hide_live_activity_line, sHideLiveEmptyActivity));
        }
    };

    private LineChart mChart;
    private SharedPreferences mPreferences;

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, LeakingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(LeakingActivity.LEAK_STATE, R.id.next_leaked_activity == view.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StatisticsDatabase statisticsDatabase = StatisticsDatabase.getInstance();
        if (statisticsDatabase.activitiesSum() > 0) {
            getMenuInflater().inflate(R.menu.main_activity_menu, menu);
            for (Map.Entry<Integer, MenuItemsInfo> entry : mActivitiesMenuItemsInfo.entrySet()) {
                initializeMenuItems(menu, entry.getValue());
            }
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        for (Map.Entry<Integer, MenuItemsInfo> entry : mActivitiesMenuItemsInfo.entrySet()) {
            MenuItemsInfo menuItemsInfo = entry.getValue();
            boolean showMenuItem = id == menuItemsInfo.hideMenuIItemId;
            if (showMenuItem || id == menuItemsInfo.showMenuIItemId) {
                mPreferences.edit().
                        putBoolean(menuItemsInfo.prefShowMenuItemKey, showMenuItem).apply();
                changeLineVisibility(entry.getKey(), !showMenuItem);
                invalidateOptionsMenu();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        setContentView(R.layout.activity_main);
        Button nextEmptyActivity = findViewById(R.id.next_empty_activity);
        Button nextLeakedActivity = findViewById(R.id.next_leaked_activity);
        nextEmptyActivity.setOnClickListener(this);
        nextLeakedActivity.setOnClickListener(this);

        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        StatisticsDatabase statisticsDatabase = StatisticsDatabase.getInstance();
        if (statisticsDatabase.activitiesSum() > 0) {
            invalidateOptionsMenu();
            drawChart();
        }
    }

    private void drawChart() {
        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.GREEN);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        LineData data = mChart.getData();
        if (null == data) {
            data = new LineData();
            mChart.setData(data);
        }
        StatisticsDatabase statisticsDatabase = StatisticsDatabase.getInstance();
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        for (int index = 0; index < mActivitiesTypeList.size(); index++) {
            ILineDataSet set = data.getDataSetByIndex(index);
            ActivitiesTypeDrawInfo drawInfo = mActivitiesTypeList.get(index);
            List<Entry> entries = statisticsDatabase.getLinesData(drawInfo.activitiesType);
            MenuItemsInfo menuItemsInfo = mActivitiesMenuItemsInfo.get(drawInfo.activitiesType);
            boolean show = !mPreferences.getBoolean(menuItemsInfo.prefShowMenuItemKey, false);
            if (null == set) {
                LineDataSet lineDataSet = new LineDataSet(entries, getString(drawInfo.resourceLegendId));
                lineDataSet.setColors(getColorEx(drawInfo.resourceColorId));
                lineDataSet.setCircleColor(getColorEx(drawInfo.resourceColorId));
                data.addDataSet(lineDataSet);
                lineDataSet.setVisible(show);
            } else {
                set.addEntry(entries.get(entries.size() - 1));
                data.notifyDataChanged();
                set.setVisible(show);
            }
        }

        Legend legend = mChart.getLegend();
        legend.setWordWrapEnabled(true);
    }

    private int getColorEx(int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getResources().getColor(colorId, getTheme());
        else
            return ContextCompat.getColor(this, colorId);
    }

    private void initializeMenuItems(Menu menu, MenuItemsInfo menuItemsInfo) {
        boolean show = mPreferences.getBoolean(menuItemsInfo.prefShowMenuItemKey, false);
        MenuItem showMenuItem = menu.findItem(menuItemsInfo.showMenuIItemId);
        showMenuItem.setVisible(show);
        MenuItem hideMenuItem = menu.findItem(menuItemsInfo.hideMenuIItemId);
        hideMenuItem.setVisible(!show);
    }

    private void changeLineVisibility(int activitiesType, boolean visible) {
        LineData data = mChart.getData();
        if (null == data)
            return;

        for (int index = 0; index < mActivitiesTypeList.size(); index++) {
            if (activitiesType == mActivitiesTypeList.get(index).activitiesType) {
                ILineDataSet set = data.getDataSetByIndex(index);
                set.setVisible(visible);
                break;
            }
        }
        mChart.invalidate();
    }

    private static class ActivitiesTypeDrawInfo {
        public final int activitiesType;
        public final int resourceLegendId;
        public final int resourceColorId;

        public ActivitiesTypeDrawInfo(int activitiesType, int resourceLegendlId, int resourceColorId) {
            this.activitiesType = activitiesType;
            resourceLegendId = resourceLegendlId;
            this.resourceColorId = resourceColorId;
        }
    }

    private static class MenuItemsInfo {
        public final int showMenuIItemId;
        public final int hideMenuIItemId;
        public final String prefShowMenuItemKey;

        public MenuItemsInfo(int showMenuIItemId, int hideMenuIItemId, String prefShowMenuItemKey) {
            this.showMenuIItemId = showMenuIItemId;
            this.hideMenuIItemId = hideMenuIItemId;
            this.prefShowMenuItemKey = prefShowMenuItemKey;
        }
    }
}
