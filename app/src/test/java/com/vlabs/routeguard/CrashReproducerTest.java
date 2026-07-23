package com.vlabs.routeguard;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.vlabs.routeguard.fragments.ReportDetailsBottomSheet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class CrashReproducerTest {

    @Test
    public void testReportDetailsCrash() {
        try {
            // Launch the fragment to see if it crashes during initialization
            FragmentScenario<ReportDetailsBottomSheet> scenario = FragmentScenario.launch(ReportDetailsBottomSheet.class);
            scenario.onFragment(fragment -> {
                System.out.println("Fragment launched successfully!");
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
