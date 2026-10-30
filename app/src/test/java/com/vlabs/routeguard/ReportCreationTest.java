package com.vlabs.routeguard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.vlabs.routeguard.models.ReportStatus;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReportDraft;
import com.vlabs.routeguard.utils.FirestoreFields;
import com.vlabs.routeguard.utils.ReportDefaults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osmdroid.util.GeoPoint;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28, packageName = "com.vlabs.routeguard")
public class ReportCreationTest {

    @Test
    public void testDraftValidation_ValidFields() {
        RoadReportDraft draft = new RoadReportDraft();
        draft.setLocation(new GeoPoint(37.422, -122.084));
        draft.setCategory(ReportType.ACCIDENT);
        draft.setDescription("Test road accident");

        assertNotNull("Location should not be null", draft.getLocation());
        assertEquals(ReportType.ACCIDENT, draft.getCategory());
        assertEquals("Test road accident", draft.getDescription());
    }

    @Test
    public void testDraftValidation_MissingLocation() {
        RoadReportDraft draft = new RoadReportDraft();
        draft.setCategory(ReportType.POLICE_CHECKING);

        assertNull("Location should be null", draft.getLocation());
        assertEquals(ReportType.POLICE_CHECKING, draft.getCategory());
    }

    @Test
    public void testDraftValidation_MissingCategory() {
        RoadReportDraft draft = new RoadReportDraft();
        draft.setLocation(new GeoPoint(19.0760, 72.8777));

        assertNotNull("Location should not be null", draft.getLocation());
        assertNull("Category should be null", draft.getCategory());
    }

    @Test
    public void testDraft_AllReportTypesSupported() {
        RoadReportDraft draft = new RoadReportDraft();
        for (ReportType type : ReportType.values()) {
            draft.setCategory(type);
            assertEquals(type, draft.getCategory());
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }

    @Test
    public void testReportDataPayload_ContainsBothReporterIdAndUserId() {
        String testReporterId = "auth_user_12345";
        String testReportId = "rep_98765";

        Map<String, Object> reportData = new HashMap<>();
        reportData.put(FirestoreFields.REPORT_ID, testReportId);
        reportData.put(FirestoreFields.TYPE, ReportType.ROAD_CONSTRUCTION.name());
        reportData.put(FirestoreFields.LAT, 19.0760);
        reportData.put(FirestoreFields.LON, 72.8777);
        reportData.put(FirestoreFields.REPORTER_ID, testReporterId);
        reportData.put("userId", testReporterId);
        reportData.put(FirestoreFields.STATUS, ReportStatus.ACTIVE.name());
        reportData.put(FirestoreFields.RELIABILITY, ReportDefaults.INITIAL_RELIABILITY);

        assertEquals("reporterId must match", testReporterId, reportData.get(FirestoreFields.REPORTER_ID));
        assertEquals("userId must match reporterId for security rule compatibility", testReporterId, reportData.get("userId"));
        assertEquals("reportId must be present", testReportId, reportData.get(FirestoreFields.REPORT_ID));
        assertEquals("ReportStatus must be ACTIVE", ReportStatus.ACTIVE.name(), reportData.get(FirestoreFields.STATUS));
        assertEquals("Type must be ROAD_CONSTRUCTION", ReportType.ROAD_CONSTRUCTION.name(), reportData.get(FirestoreFields.TYPE));
        assertTrue("lat must be numeric", reportData.get(FirestoreFields.LAT) instanceof Double);
        assertTrue("lon must be numeric", reportData.get(FirestoreFields.LON) instanceof Double);
    }

    @Test
    public void testLocalReportJsonSerialization() {
        com.vlabs.routeguard.models.RoadReport report = new com.vlabs.routeguard.models.RoadReport(
                "rep_local_animal_01",
                ReportType.ANIMAL_ON_ROAD,
                37.4220,
                -122.0840,
                "Animal spotted on road",
                System.currentTimeMillis(),
                "usr_kuppaabhi2",
                System.currentTimeMillis() + 7200000L
        );
        report.setConfidenceScore(80);

        org.json.JSONObject json = com.vlabs.routeguard.managers.LocalReportManager.reportToJson(report);
        assertEquals("rep_local_animal_01", json.optString("reportId"));
        assertEquals("ANIMAL_ON_ROAD", json.optString("type"));
        assertEquals(37.4220, json.optDouble("latitude"), 0.0001);
        assertEquals(-122.0840, json.optDouble("longitude"), 0.0001);
        assertEquals("usr_kuppaabhi2", json.optString("reporterId"));
        assertEquals("Animal spotted on road", json.optString("description"));

        com.vlabs.routeguard.models.RoadReport restored =
                com.vlabs.routeguard.managers.LocalReportManager.reportFromJson(json);
        assertNotNull(restored);
        assertEquals(report.getReportId(), restored.getReportId());
        assertEquals(ReportType.ANIMAL_ON_ROAD, restored.getType());
        assertEquals(report.getLatitude(), restored.getLatitude(), 0.0001);
        assertEquals(report.getLongitude(), restored.getLongitude(), 0.0001);
        assertEquals(report.getReporterId(), restored.getReporterId());
        assertEquals(report.getDescription(), restored.getDescription());
        assertEquals(80, restored.getConfidenceScore());
    }
}
