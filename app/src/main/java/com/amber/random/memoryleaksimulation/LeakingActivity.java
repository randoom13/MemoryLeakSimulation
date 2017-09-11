package com.amber.random.memoryleaksimulation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class LeakingActivity extends AppCompatActivity {
    public static String LEAK_STATE = "leaking_state";
    private boolean mLeaking = false;

    public boolean isLeaking() {
        return mLeaking;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (null != intent && intent.getBooleanExtra(LEAK_STATE, false)) {
            mLeaking = true;
            new FreezeThread().start();
        }
        StatisticsDatabase.getInstance().addActivity(this);
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    private class FreezeThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.currentThread().sleep(60000);
            } catch (InterruptedException ex) {
                Log.e(getClass().getSimpleName(),
                        String.format("Thread %d was interrupted!", Thread.currentThread().getId()));
            }
        }
    }
}
