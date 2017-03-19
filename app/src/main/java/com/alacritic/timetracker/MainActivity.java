package com.alacritic.timetracker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.alacritic.timetracker.util.FragmentHelper;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        StrictMode.enableDefaults();

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            showCheckInCheckOutFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            showAddTimeRecordFragment();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCheckInCheckOutFragment() {
        CheckInCheckoutFragment fragment = new CheckInCheckoutFragment();
        FragmentHelper.showFragment(this, fragment, false);
    }

    private void showAddTimeRecordFragment() {
        EditTimeRecordFragment fragment = EditTimeRecordFragment.newInstance(null);
        FragmentHelper.showFragment(this, fragment);
    }
}
