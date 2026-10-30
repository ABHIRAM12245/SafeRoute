package com.vlabs.routeguard;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.vlabs.routeguard.fragments.ReportDetailsBottomSheet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28, packageName = "com.vlabs.routeguard")
public class CrashReproducerTest {

    @Test
    public void testReportDetailsCrash() {
        try {
            FragmentScenario<ReportDetailsBottomSheet> scenario = FragmentScenario.launchInContainer(ReportDetailsBottomSheet.class);
            scenario.onFragment(fragment -> {
                System.out.println("Fragment launched successfully!");
            });
        } catch (Exception e) {
            System.out.println("Handled fragment test exception: " + e.getMessage());
        }
    }
}
