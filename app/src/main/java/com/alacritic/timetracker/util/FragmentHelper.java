package com.alacritic.timetracker.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.alacritic.timetracker.R;

public class FragmentHelper {

    public static void showFragment(FragmentActivity activity, Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    public static void showFragment(FragmentActivity activity, Fragment fragment) {
        showFragment(activity, fragment, true);
    }
}
