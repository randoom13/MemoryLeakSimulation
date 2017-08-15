package com.amber.random.memoryleaksimulation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LineChart mChart;
    private SharedPreferences mPreferences;
    private List<ActivitiesTypeDrawInfo> mActivitiesTypeList = new ArrayList<ActivitiesTypeDrawInfo>() {
        {
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.LIVE_LEAKED_ACTIVITY, R.string.live_leaked_activity, R.color.colorLiveLeakedActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.LIVE_EMPTY_ACTIVITY, R.string.live_empty_activity, R.color.colorLiveEmptyActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.DESTROYED_EMPTY_ACTIVITY, R.string.destroyed_empty_activity, R.color.colorDestroyedEmptyActivity));
            add(new ActivitiesTypeDrawInfo(StatisticsDatabase.DESTROYED_LEAKED_ACTIVITY, R.string.destroyed_leaked_activity, R.color.colorDestroyedLeakedActivity));
        }
    };

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, LeakingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(LeakingActivity.LEAK_STATE, R.id.next_leaked_activity == view.getId());
        startActivity(intent);
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
        StatisticsDatabase collector = StatisticsDatabase.getInstance();
        if (collector.activitiesCount() > 0) {
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
        StatisticsDatabase collector = StatisticsDatabase.getInstance();
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        for (int index = 0; index < mActivitiesTypeList.size(); index++) {
            ILineDataSet set = data.getDataSetByIndex(index);
            ActivitiesTypeDrawInfo drawInfo = mActivitiesTypeList.get(index);
            List<Entry> entries = collector.getLinesData(drawInfo.ActivitiesType);
            if (null == set) {
                LineDataSet lineDataSet = new LineDataSet(entries, getString(drawInfo.ResourceLabelId));
                lineDataSet.setColors(getColorEx(drawInfo.ResourceColorId));
                lineDataSet.setCircleColor(getColorEx(drawInfo.ResourceColorId));
                data.addDataSet(lineDataSet);
            } else {
                set.addEntry(entries.get(entries.size() - 1));
                data.notifyDataChanged();
            }
        }
        Legend legend = mChart.getLegend();
        if (null != legend)
        legend.setWordWrapEnabled(true);

    }

    private int getColorEx(int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getResources().getColor(colorId, getTheme());
        else
            return ContextCompat.getColor(this, colorId);
    }

    private static class ActivitiesTypeDrawInfo {
        public final int ActivitiesType;
        public final int ResourceLabelId;
        public final int ResourceColorId;

        public ActivitiesTypeDrawInfo(int activitiesType, int resourceLabelId, int resourceColorId) {
            ActivitiesType = activitiesType;
            ResourceLabelId = resourceLabelId;
            ResourceColorId = resourceColorId;
        }
    }
}
