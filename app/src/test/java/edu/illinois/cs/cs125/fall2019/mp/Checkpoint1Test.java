package edu.illinois.cs.cs125.fall2019.mp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadow.api.Shadow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

import edu.illinois.cs.cs125.fall2019.mp.shadows.MockedWrapperInstantiator;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowGoogleMap;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowLocalBroadcastManager;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowMarker;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowSupportMapFragment;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.robolectricsecurity.PowerMockSecurity;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*", "com.google.android.*", "edu.illinois.cs.cs125.fall2019.mp.shadows.*"})
@PrepareForTest({WebApi.class, FirebaseAuth.class})
@Trusted
public class Checkpoint1Test {

    @Rule
    public PowerMockRule mockStaticClasses = new PowerMockRule();

    @Before
    public void setup() {
        PowerMockSecurity.secureMockMethodCache();
        FirebaseMocker.mock();
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        WebApiMocker.interceptHttp();
    }

    @After
    public void teardown() {
        WebApiMocker.reset();
        ShadowLocalBroadcastManager.reset();
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testAreaDivision() {
        // Test a square area one mile on each side
        AreaDivider divider = new AreaDivider(40.098143, -88.257649, 40.083690, -88.276347, 40);
        Assert.assertEquals("Incorrect X cell count for square area", 40, divider.getXCells());
        Assert.assertEquals("Incorrect Y cell count for square area", 40, divider.getYCells());
        LatLng position = new LatLng(40.083902, -88.276162);
        Assert.assertEquals("Incorrect X coordinate for point in southwestern cell", 0, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point in southwestern cell", 0, divider.getYCoordinate(position));
        position = new LatLng(40.083734, -88.266903);
        Assert.assertEquals("Incorrect X coordinate for point near south border", 20, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point near south border", 0, divider.getYCoordinate(position));
        position = new LatLng(40.098041, -88.257809);
        Assert.assertEquals("Incorrect X coordinate for point in northeastern cell", 39, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point in northeastern cell", 39, divider.getYCoordinate(position));
        position = new LatLng(40.090763, -88.276310);
        Assert.assertEquals("Incorrect X coordinate for point near west border", 0, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point near west border", 19, divider.getYCoordinate(position));
        position = new LatLng(40.091436, -88.257672);
        Assert.assertEquals("Incorrect X coordinate for point near east border", 39, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point near east border", 21, divider.getYCoordinate(position));
        checkBoundsEqual("Incorrect cell bounds for corner cell", new LatLngBounds(
                new LatLng(40.08369, -88.276347), new LatLng(40.084051325, -88.27587955)),
                divider.getCellBounds(0, 0));
        checkBoundsEqual("Incorrect cell bounds for internal cell", new LatLngBounds(
                new LatLng(40.085496625, -88.2707376), new LatLng(40.08585795, -88.27027015)),
                divider.getCellBounds(12, 5));

        // Test the same area with a different cell size
        divider = new AreaDivider(40.098143, -88.257649, 40.083690, -88.276347, 100);
        Assert.assertEquals(16, divider.getXCells());
        Assert.assertEquals(16, divider.getYCells());
        position = new LatLng(40.098113, -88.265344);
        Assert.assertEquals("Incorrect X coordinate for point near north border", 9, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point near north border", 15, divider.getYCoordinate(position));

        // Test the main quad
        divider = new AreaDivider(40.108986, -88.226489, 40.106274, -88.227814, 50);
        Assert.assertEquals("Incorrect X cell count for the quad", 3, divider.getXCells());
        Assert.assertEquals("Incorrect Y cell count for the quad", 6, divider.getYCells());
        position = new LatLng(40.107793, -88.227130);
        Assert.assertEquals("Incorrect X coordinate for point on the quad", 1, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point on the quad", 3, divider.getYCoordinate(position));
        checkBoundsEqual("Incorrect cell bounds on the quad", new LatLngBounds(
                new LatLng(40.108082, -88.22693067), new LatLng(40.108534, -88.226489)),
                divider.getCellBounds(2, 4));

        // Test a small one-cell area
        divider = new AreaDivider(40.107854, -88.224972, 40.107817, -88.225130, 80);
        Assert.assertEquals("Incorrect X cell count for small area", 1, divider.getXCells());
        Assert.assertEquals("Incorrect Y cell count for small area", 1, divider.getYCells());
        position = new LatLng(40.107839, -88.225066);
        Assert.assertEquals("Incorrect X coordinate for point in small area", 0, divider.getXCoordinate(position));
        Assert.assertEquals("Incorrect Y coordinate for point in small area", 0, divider.getYCoordinate(position));
        checkBoundsEqual("Incorrect cell bounds for small area", new LatLngBounds(
                new LatLng(40.107817, -88.225130), new LatLng(40.107854, -88.224972)),
                divider.getCellBounds(0, 0));

        // Randomized tests (loaded from a JSON file)
        JsonArray tests;
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/areadivider.json"))) {
            tests = new JsonParser().parse(scanner.nextLine()).getAsJsonArray();
        }
        for (JsonElement t : tests) {
            JsonObject test = t.getAsJsonObject();
            JsonObject answer = test.getAsJsonObject("answer");

            // Test cell counts
            divider = new AreaDivider(test.get("north").getAsDouble(), test.get("east").getAsDouble(),
                    test.get("south").getAsDouble(), test.get("west").getAsDouble(), test.get("size").getAsInt());
            Assert.assertEquals("Incorrect X cell count", answer.get("width").getAsInt(), divider.getXCells());
            Assert.assertEquals("Incorrect Y cell count", answer.get("height").getAsInt(), divider.getYCells());

            for (JsonElement st : test.getAsJsonArray("subtests")) {
                JsonObject subtest = st.getAsJsonObject();
                JsonObject subanswer = subtest.getAsJsonObject("answer");

                // Test location to cell coordinate translation
                LatLng subtestPoint = new LatLng(subtest.get("lat").getAsDouble(), subtest.get("lng").getAsDouble());
                Assert.assertEquals("Incorrect cell X coordinate",
                        subanswer.get("x").getAsInt(), divider.getXCoordinate(subtestPoint));
                Assert.assertEquals("Incorrect cell Y coordinate",
                        subanswer.get("y").getAsInt(), divider.getYCoordinate(subtestPoint));

                // Test cell boundaries
                LatLngBounds subtestBounds = divider.getCellBounds(subtest.get("x").getAsInt(), subtest.get("y").getAsInt());
                LatLngBounds answerBounds = new LatLngBounds(
                        new LatLng(subanswer.get("south").getAsDouble(), subanswer.get("west").getAsDouble()),
                        new LatLng(subanswer.get("north").getAsDouble(), subanswer.get("east").getAsDouble()));
                checkBoundsEqual("Incorrect cell bounds", answerBounds, subtestBounds);
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    public void testAreaGrid() {
        // Test a small one-cell area (should only render outer border)
        for (int i = 0; i < 3; i++) {
            final double north = 40.107854;
            final double east = -88.224972;
            final double south = 40.107817;
            final double west = -88.225130;
            AreaDivider divider = new AreaDivider(north, east, south, west, 80 + i * 20);
            GoogleMap map = MockedWrapperInstantiator.create(GoogleMap.class);
            ShadowGoogleMap shadowMap = Shadow.extract(map);
            divider.renderGrid(map);
            Assert.assertEquals("One-cell areas should have four lines in their grid",
                    4, shadowMap.getPolylines().size());
            checkBorderLines(shadowMap, north, east, south, west);
        }

        // Test a 3 x 2 area
        double north = 40.105402;
        double east = -88.257836;
        double south = 40.101920;
        double west = -88.267111;
        AreaDivider divider = new AreaDivider(north, east, south, west, 270);
        GoogleMap map = MockedWrapperInstantiator.create(GoogleMap.class);
        ShadowGoogleMap shadowMap = Shadow.extract(map);
        divider.renderGrid(map);
        Assert.assertEquals("A 3x2 area should have 7 lines in the grid (4 border, 3 internal)",
                7, shadowMap.getPolylines().size());
        checkBorderLines(shadowMap, north, east, south, west);
        Assert.assertNotEquals("Missing/misplaced west vertical internal line", 0, shadowMap
                .getPolylinesConnecting(new LatLng(north, -88.2640193), new LatLng(south, -88.2640193)).size());
        Assert.assertNotEquals("Missing/misplaced east vertical internal line", 0, shadowMap
                .getPolylinesConnecting(new LatLng(north, -88.26092767), new LatLng(south, -88.26092767)).size());
        Assert.assertNotEquals("Missing/misplaced horizontal internal line", 0, shadowMap
                .getPolylinesConnecting(new LatLng(40.103661, west), new LatLng(40.103661, east)).size());

        // Randomized tests (loaded from JSON)
        JsonArray tests;
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/areagrid.json"))) {
            tests = new JsonParser().parse(scanner.nextLine()).getAsJsonArray();
        }
        for (JsonElement t : tests) {
            JsonObject test = t.getAsJsonObject();
            north = test.get("north").getAsDouble();
            east = test.get("east").getAsDouble();
            south = test.get("south").getAsDouble();
            west = test.get("west").getAsDouble();
            divider = new AreaDivider(north, east, south, west, test.get("size").getAsInt());
            map.clear();
            divider.renderGrid(map);
            checkBorderLines(shadowMap, north, east, south, west);
            JsonArray answer = test.getAsJsonArray("answer");
            for (JsonElement l : answer) {
                JsonObject line = l.getAsJsonObject();
                LatLng onePoint = new LatLng(line.get("lat1").getAsDouble(), line.get("lng1").getAsDouble());
                LatLng otherPoint = new LatLng(line.get("lat2").getAsDouble(), line.get("lng2").getAsDouble());
                Assert.assertNotEquals("Incorrect/missing internal line", 0, shadowMap
                        .getPolylinesConnecting(onePoint, otherPoint).size());
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testCreateGameButton() {
        // Start the activity
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        Assert.assertNull("MainActivity should no longer immediately launch another activity", intent);
        WebApiMocker.process((path, method, body, callback, errorListener) -> {
            // Checkpoint 2 compatibility
            if (path.equals("/games")) {
                callback.onResponse(SampleData.createGamesResponse());
            } else {
                errorListener.onErrorResponse(new VolleyError("Endpoint not available during Checkpoint 1 testing."));
            }
        });

        // Find the button
        Button createGameButton = activity.findViewById(IdLookup.require("createGame"));
        Assert.assertNotNull("MainActivity should have a Create Game button", createGameButton);
        Assert.assertEquals("The Create Game button has the wrong text",
                "CREATE GAME", createGameButton.getText().toString().toUpperCase());
        Assert.assertEquals("The Create Game button should be visible",
                View.VISIBLE, createGameButton.getVisibility());

        // Press the button
        createGameButton.performClick();
        intent = Shadows.shadowOf(activity).getNextStartedActivity();
        Assert.assertNotNull("Pressing Create Game should launch the game setup activity", intent);
        Assert.assertEquals("Create Game should launch NewGameActivity",
                new ComponentName(activity, NewGameActivity.class), intent.getComponent());
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    @SuppressWarnings("ConstantConditions")
    public void testSetupUI() {
        // Get IDs
        @IdRes int rIdTargetModeOption = IdLookup.require("targetModeOption");
        @IdRes int rIdTargetSettings = IdLookup.require("targetSettings");
        @IdRes int rIdAreaModeOption = IdLookup.require("areaModeOption");
        @IdRes int rIdAreaSettings = IdLookup.require("areaSettings");
        @IdRes int rIdProximityThreshold = IdLookup.require("proximityThreshold");
        @IdRes int rIdCellSize = IdLookup.require("cellSize");
        @IdRes int rIdTargetsMap = IdLookup.request("targetsMap"); // For Checkpoint 3 compatibility

        // Start the activity
        NewGameActivity activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        SupportMapFragment areaMap = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.areaSizeMap);
        Shadow.<ShadowSupportMapFragment>extract(areaMap).notifyMapReady();
        SupportMapFragment targetMap = null;
        if (rIdTargetsMap != 0) {
            targetMap = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(rIdTargetsMap);
            Shadow.<ShadowSupportMapFragment>extract(targetMap).notifyMapReady();
        }

        // Check initial UI
        RadioButton targetModeOption = activity.findViewById(rIdTargetModeOption);
        Assert.assertFalse("Neither game mode should be selected initially", targetModeOption.isChecked());
        RadioButton areaModeOption = activity.findViewById(rIdAreaModeOption);
        Assert.assertFalse("Neither game mode should be selected initially", areaModeOption.isChecked());
        ViewGroup targetSettings = activity.findViewById(rIdTargetSettings);
        Assert.assertEquals("The targetSettings group should be gone initially",
                View.GONE, targetSettings.getVisibility());
        ViewGroup areaSettings = activity.findViewById(rIdAreaSettings);
        Assert.assertEquals("The areaSettings group should be gone initially",
                View.GONE, areaSettings.getVisibility());
        TextView proximityThreshold = activity.findViewById(rIdProximityThreshold);
        Assert.assertNotEquals("The proximity threshold box should accept only numbers",
                0, proximityThreshold.getInputType() & InputType.TYPE_CLASS_NUMBER);
        TextView cellSize = activity.findViewById(rIdCellSize);
        Assert.assertNotEquals("The cell size box should accept only numbers",
                0, cellSize.getInputType() & InputType.TYPE_CLASS_NUMBER);
        Assert.assertTrue("The proximity threshold box should be inside targetSettings",
                inside(proximityThreshold, targetSettings));
        if (targetMap != null) {
            // Checkpoint 3 only
            Assert.assertTrue("The targets map should be inside targetSettings",
                    inside(targetMap.getView(), targetSettings));
        }
        Assert.assertTrue("The cell size box should be inside areaSettings",
                inside(cellSize, areaSettings));
        Assert.assertTrue("The area size map should be inside areaSettings",
                inside(areaMap.getView(), areaSettings));
        Assert.assertEquals("areaSettings and targetSettings should be siblings",
                areaSettings.getParent(), targetSettings.getParent());

        // Push the radio buttons
        targetModeOption.setChecked(true);
        Assert.assertEquals("targetSettings should be visible when target mode is selected",
                View.VISIBLE, targetSettings.getVisibility());
        Assert.assertEquals("areaSettings should be gone when target mode is selected",
                View.GONE, areaSettings.getVisibility());
        areaModeOption.setChecked(true);
        Assert.assertEquals("areaSettings should be visible when area mode is selected",
                View.VISIBLE, areaSettings.getVisibility());
        Assert.assertEquals("targetSettings should be gone when area mode is selected",
                View.GONE, targetSettings.getVisibility());
        targetModeOption.setChecked(true);
        Assert.assertEquals("targetSettings should be visible when target mode is selected",
                View.VISIBLE, targetSettings.getVisibility());
        Assert.assertEquals("areaSettings should be gone when target mode is selected",
                View.GONE, areaSettings.getVisibility());
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    public void testSetupAction() {
        // Get IDs
        @IdRes int rIdTargetModeOption = IdLookup.require("targetModeOption");
        @IdRes int rIdAreaModeOption = IdLookup.require("areaModeOption");
        @IdRes int rIdProximityThreshold = IdLookup.require("proximityThreshold");
        @IdRes int rIdCellSize = IdLookup.require("cellSize");
        @IdRes int rIdTargetsMap = IdLookup.request("targetsMap"); // For Checkpoint 3 compatibility
        @IdRes int rIdCreateGame = IdLookup.require("createGame");

        // Start the activity
        NewGameActivity activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        SupportMapFragment areaMap = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.areaSizeMap);
        Shadow.<ShadowSupportMapFragment>extract(areaMap).notifyMapReady();
        SupportMapFragment targetMap = null;
        if (rIdTargetsMap != 0) {
            targetMap = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(rIdTargetsMap);
            Shadow.<ShadowSupportMapFragment>extract(targetMap).notifyMapReady();
        }
        Button createGame = activity.findViewById(rIdCreateGame);

        // Test validation
        createGame.performClick();
        ensureNothing("Trying to create a game without specifying the game mode should do nothing", activity);
        RadioButton targetModeOption = activity.findViewById(rIdTargetModeOption);
        targetModeOption.setChecked(true);
        TextView proximityThreshold = activity.findViewById(rIdProximityThreshold);
        proximityThreshold.setText("");
        createGame.performClick();
        ensureNothing("Trying to create a target mode game without specifying the proximity threshold should do nothing", activity);
        RadioButton areaModeOption = activity.findViewById(rIdAreaModeOption);
        areaModeOption.setChecked(true);
        TextView cellSize = activity.findViewById(rIdCellSize);
        cellSize.setText("");
        createGame.performClick();
        ensureNothing("Trying to create an area mode game without specifying the cell size should do nothing", activity);

        // Create an area mode game
        int randomCellSize = 60 + (int) RandomHelper.randomPlusMinusRange(30);
        cellSize.setText(Integer.toString(randomCellSize));
        LatLngBounds bounds = RandomHelper.randomBounds();
        Shadow.<ShadowGoogleMap>extract(Shadow.<ShadowSupportMapFragment>extract(areaMap).getMap())
                .setVisibleRegion(bounds);
        boolean[] gotRequestHolder = new boolean[] {false};
        createGame.performClick();
        WebApiMocker.process((path, method, body, callback, errorListener) -> {
            // Checkpoint 3 uses a web request instead of an intent
            gotRequestHolder[0] = true;
            Assert.assertEquals("/games/create", path);
            Assert.assertEquals(Request.Method.POST, method);
            JsonObject config = body.getAsJsonObject();
            Assert.assertEquals("The mode should be 'area' for an area mode game",
                    "area", config.get("mode").getAsString());
            Assert.assertEquals("Incorrect cell size in request",
                    randomCellSize, config.get("cellSize").getAsInt());
            Assert.assertEquals("Incorrect north boundary in request",
                    bounds.northeast.latitude, config.get("areaNorth").getAsDouble(), 1e-7);
            Assert.assertEquals("Incorrect east boundary in request",
                    bounds.northeast.longitude, config.get("areaEast").getAsDouble(), 1e-7);
            Assert.assertEquals("Incorrect south boundary in request",
                    bounds.southwest.latitude, config.get("areaSouth").getAsDouble(), 1e-7);
            Assert.assertEquals("Incorrect west boundary in request",
                    bounds.southwest.longitude, config.get("areaWest").getAsDouble(), 1e-7);
            errorListener.onErrorResponse(new VolleyError("This activity will be reused."));
            Assert.assertFalse("NewGameActivity should not finish until the create-game request completes",
                    activity.isFinishing());
        });
        if (!gotRequestHolder[0]) {
            Intent intent = ensureGameIntent(activity);
            Assert.assertEquals("The mode should be 'area' for an area mode game",
                    "area", intent.getStringExtra("mode"));
            Assert.assertEquals("Incorrect cell size in intent",
                    randomCellSize, intent.getIntExtra("cellSize", 0));
            Assert.assertEquals("Incorrect north boundary in intent",
                    bounds.northeast.latitude, intent.getDoubleExtra("areaNorth", 0), 1e-7);
            Assert.assertEquals("Incorrect east boundary in intent",
                    bounds.northeast.longitude, intent.getDoubleExtra("areaEast", 0), 1e-7);
            Assert.assertEquals("Incorrect south boundary in intent",
                    bounds.southwest.latitude, intent.getDoubleExtra("areaSouth", 0), 1e-7);
            Assert.assertEquals("Incorrect west boundary in intent",
                    bounds.southwest.longitude, intent.getDoubleExtra("areaWest", 0), 1e-7);
            Assert.assertTrue("NewGameActivity should finish() after launching GameActivity",
                    activity.isFinishing());
        }

        // Create a target mode game
        gotRequestHolder[0] = false;
        targetModeOption.setChecked(true);
        int randomProximityThreshold = 15 + (int) RandomHelper.randomPlusMinusRange(10);
        proximityThreshold.setText(Integer.toString(randomProximityThreshold));
        if (targetMap != null) {
            // Checkpoint 3 has a map to place targets on
            Shadow.<ShadowGoogleMap>extract(Shadow.<ShadowSupportMapFragment>extract(targetMap).getMap())
                    .longPress(new LatLng(RandomHelper.randomLat(), RandomHelper.randomLng()));
        }
        createGame.performClick();
        WebApiMocker.process((path, method, body, callback, errorListener) -> {
            // Checkpoint 3 uses a web request instead of an intent
            gotRequestHolder[0] = true;
            Assert.assertEquals("/games/create", path);
            Assert.assertEquals(Request.Method.POST, method);
            JsonObject config = body.getAsJsonObject();
            Assert.assertEquals("The mode should be 'target' for a target mode game",
                    "target", config.get("mode").getAsString());
            Assert.assertEquals("Incorrect proximity threshold in request",
                    randomProximityThreshold, config.get("proximityThreshold").getAsInt());
            JsonObject response = new JsonObject();
            response.addProperty("game", RandomHelper.randomId());
            callback.onResponse(response);
            ensureGameIntent(activity);
            Assert.assertTrue("NewGameActivity should finish() after the create-game request completes",
                    activity.isFinishing());
        });
        if (!gotRequestHolder[0]) {
            Intent intent = ensureGameIntent(activity);
            Assert.assertEquals("The mode should be 'target' for an area mode game",
                    "target", intent.getStringExtra("mode"));
            Assert.assertEquals("Incorrect proximity threshold in intent",
                    randomProximityThreshold, intent.getIntExtra("proximityThreshold", 0));
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testAreaModeGameplay() {
        // Start the activity with a 10x8 area
        Intent intent = new Intent();
        String gameId = RandomHelper.randomId();
        intent.putExtra("game", gameId);
        intent.putExtra("mode", "area");
        intent.putExtra("areaNorth", 40.104144);
        intent.putExtra("areaEast", -88.224515);
        intent.putExtra("areaSouth", 40.100573);
        intent.putExtra("areaWest", -88.230183);
        intent.putExtra("cellSize", 50);
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection(); // Checkpoint 4 compatibility
        GameActivityLauncher launcher = new GameActivityLauncher(intent);
        ShadowGoogleMap map = Shadow.extract(launcher.getMap());

        // Check initial map
        if (webSocketControl.isConnected()) {
            // Checkpoint 4 compatibility
            webSocketControl.sendData(createS4AreaGame(gameId, 50));
        }
        Assert.assertEquals("No polygons should be on the map initially", 0, map.getPolygons().size());
        Assert.assertEquals("Grid wasn't rendered on the map correctly", 20, map.getPolylines().size());

        // Visit a cell
        launcher.sendLocationUpdate(new LatLng(40.101326, -88.229540)); // (1, 1)
        Assert.assertNotEquals("Capturing a cell should create a polygon", 0, map.getPolygons().size());
        int decorativePolygons = map.getPolygons().size() - 1; // To allow extra head indication
        LatLngBounds firstCaptureBounds = new LatLngBounds(new LatLng(40.101019375, -88.2296162),
                new LatLng(40.10146575, -88.2290494));
        Polygon polygon = map.getPolygonFilling(firstCaptureBounds);
        Assert.assertNotNull("The polygon didn't cover the captured cell", polygon);
        Assert.assertEquals("The polygon should be green", Color.GREEN & 0xFFFFFF, polygon.getFillColor() & 0xFFFFFF);
        Assert.assertNotEquals("The polygon is invisible (100% transparent)", 0, polygon.getFillColor() >> 24);

        // Visit an adjacent cell
        launcher.sendLocationUpdate(new LatLng(40.100701, -88.229402)); // (1, 0)
        Assert.assertEquals("Capturing another cell should create another polygon",
                decorativePolygons + 2, map.getPolygons().size());
        Assert.assertNotNull("Capturing another cell shouldn't affect the previous polygon",
                map.getPolygonFilling(firstCaptureBounds));
        Assert.assertNotNull("The new polygon didn't cover the newly captured cell", map.getPolygonFilling(new LatLngBounds(
                new LatLng(40.101019375, -88.2296162), new LatLng(40.10146575, -88.2290494))));

        // Meander around the outside of the area
        launcher.sendLocationUpdate(new LatLng(40.100139, -88.229335)); // (1, <0)
        Assert.assertEquals("Going past the south boundary shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.100457, -88.230357)); // (-1, -1)
        Assert.assertEquals("Going southwest outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.102621, -88.230451)); // (-1, 4)
        Assert.assertEquals("Going past the west boundary shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.103812, -88.233710)); // farther west
        Assert.assertEquals("Going far outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.104270, -88.230421)); // (-1, 8)
        Assert.assertEquals("Going northwest outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.104297, -88.227827)); // (4, 8)
        Assert.assertEquals("Going past the north boundary shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.104843, -88.227186)); // (5, 9)
        Assert.assertEquals("Going outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.104449, -88.224126)); // (10, 9)
        Assert.assertEquals("Going northeast outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.101687, -88.224401)); // (10, 2)
        Assert.assertEquals("Going past the east boundary shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.100427, -88.224406)); // (10, -1)
        Assert.assertEquals("Going southeast of the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.099040, -88.220298)); // far southeast
        Assert.assertEquals("Going far outside the area shouldn't capture any cells",
                decorativePolygons + 2, map.getPolygons().size());

        // Walk across the area without going near the captured cells
        for (double longitude = -88.224848; longitude > -88.230159; longitude -= 0.0002) {
            launcher.sendLocationUpdate(new LatLng(40.10259 + RandomHelper.randomPlusMinusRange(0.00001), longitude));
            Assert.assertEquals("It should not be possible to capture cells not adjacent to the previous capture",
                    decorativePolygons + 2, map.getPolygons().size());
        }

        // Revisit the previously captured cells
        launcher.sendLocationUpdate(new LatLng(40.100755, -88.229531)); // (1, 0)
        Assert.assertEquals("Revisiting the last captured cell should do nothing",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.101326, -88.229540)); // (1, 1)
        Assert.assertEquals("Revisiting a previously captured cell should do nothing",
                decorativePolygons + 2, map.getPolygons().size());
        polygon = map.getPolygonFilling(firstCaptureBounds);
        Assert.assertNotNull("Ineffective walking shouldn't affect existing cell polygons", polygon);

        // Visit cells adjacent to the start of the path but not the end
        launcher.sendLocationUpdate(new LatLng(40.101786, -88.229392)); // (1, 2)
        Assert.assertEquals("Visiting a cell not adjacent to the most recent capture should do nothing",
                decorativePolygons + 2, map.getPolygons().size());
        launcher.sendLocationUpdate(new LatLng(40.101154, -88.230020)); // (0, 1)
        Assert.assertEquals("Visiting a cell only diagonally adjacent to the most recent capture should do nothing",
                decorativePolygons + 2, map.getPolygons().size());

        // Capture two more cells
        launcher.sendLocationUpdate(new LatLng(40.100724, -88.228973)); // (2, 0)
        Assert.assertEquals("Visiting a cell adjacent to the most recent capture should capture it",
                decorativePolygons + 3, map.getPolygons().size());
        Assert.assertNotNull("Capturing another cell should add a polygon on it", map.getPolygonFilling(new LatLngBounds(
                new LatLng(40.100573, -88.2290494), new LatLng(40.101019375, -88.2284826))));
        launcher.sendLocationUpdate(new LatLng(40.101211, -88.228816)); // (2, 1)
        Assert.assertEquals("Visiting a cell adjacent to the most recent capture should capture it",
                decorativePolygons + 4, map.getPolygons().size());
        Assert.assertNotNull("Capturing another cell should add a polygon on it", map.getPolygonFilling(new LatLngBounds(
                new LatLng(40.101019375, -88.2290494), new LatLng(40.10146575, -88.2284826))));

        // Capture a run of cells going all the way east
        for (double longitude = -88.228815; longitude < -88.224864; longitude += 0.0002) {
            launcher.sendLocationUpdate(new LatLng(40.10121 + RandomHelper.randomPlusMinusRange(0.000001), longitude));
        }
        Assert.assertEquals("Capturing additional cells should create additional polygons",
                decorativePolygons + 11, map.getPolygons().size());

        // Capture a run of cells going all the way north
        for (double latitude = 40.1013; latitude < 40.104079; latitude += 0.00003) {
            launcher.sendLocationUpdate(new LatLng(latitude, -88.224864 + RandomHelper.randomPlusMinusRange(0.00001)));
        }
        Assert.assertEquals("Capturing additional cells should create additional polygons",
                decorativePolygons + 17, map.getPolygons().size());

        // Capture a run of cells going all the way west
        for (double longitude = -88.224864; longitude > -88.230054; longitude -= 0.0002) {
            launcher.sendLocationUpdate(new LatLng(40.10408 + RandomHelper.randomPlusMinusRange(0.00001), longitude));
        }
        Assert.assertEquals("Capturing additional cells should create additional polygons",
                decorativePolygons + 26, map.getPolygons().size());

        // Capture a run of cells going all the way south
        for (double latitude = 40.10408; latitude > 40.10063; latitude -= 0.00003) {
            launcher.sendLocationUpdate(new LatLng(latitude, -88.23004 + RandomHelper.randomPlusMinusRange(0.00001)));
        }
        Assert.assertEquals("Capturing additional cells should create additional polygons",
                decorativePolygons + 33, map.getPolygons().size());

        // Run over the whole area now that the snake is stuck
        for (double latitude = 40.10063; latitude < 40.10408; latitude += 0.00015) {
            for (double longitude = -88.23004; longitude < -88.22486; longitude += 0.0009) {
                launcher.sendLocationUpdate(new LatLng(latitude, longitude));
                Assert.assertEquals("It should not be possible to capture more cells when stuck",
                        decorativePolygons + 33, map.getPolygons().size());
            }
        }

        // Make sure it respects the cell size setting
        intent.putExtra("cellSize", 85);
        webSocketControl = WebSocketMocker.expectConnection();
        launcher = new GameActivityLauncher(intent);
        map = Shadow.extract(launcher.getMap());
        if (webSocketControl.isConnected()) {
            webSocketControl.sendData(createS4AreaGame(gameId, 85));
        }
        Assert.assertEquals("Grid was incorrect with a different cell size", 13, map.getPolylines().size());
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testProximityThreshold() throws IllegalAccessException, InvocationTargetException {
        // Test at several different thresholds
        for (int proximityThreshold = 50; proximityThreshold >= 20; proximityThreshold -= 10) {
            // Start the activity
            Intent intent = new Intent();
            String gameId = RandomHelper.randomId();
            intent.putExtra("game", gameId);
            intent.putExtra("mode", "target");
            intent.putExtra("proximityThreshold", proximityThreshold);
            WebSocketMocker webSocketControl = WebSocketMocker.expectConnection(); // Checkpoint 4 compatibility
            GameActivityLauncher launcher = new GameActivityLauncher(intent);
            ShadowGoogleMap map = Shadow.extract(launcher.getMap());

            // Check initial markers
            if (webSocketControl.isConnected()) {
                // Checkpoint 4 compatibility mode
                webSocketControl.sendData(createS4TargetGame(gameId, proximityThreshold));
                Assert.assertEquals("Incorrect target count", 2, map.getMarkers().size());
            } else {
                // Working on Checkpoint 1
                try {
                    Class<?> defaultTargetsClass = Class.forName("edu.illinois.cs.cs125.fall2019.mp.DefaultTargets");
                    Method getPositionsMethod = defaultTargetsClass.getMethod("getPositions", Context.class);
                    LatLng[] positions = (LatLng[]) getPositionsMethod.invoke(null, ApplicationProvider.getApplicationContext());
                    Assert.assertEquals("Incorrect target count", positions.length, map.getMarkers().size());
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    Assert.fail("DefaultTargets was removed but no websocket connection was attempted");
                }
            }
            Assert.assertEquals("No targets should be claimed (green) yet",
                    0, map.getMarkersWithColor(BitmapDescriptorFactory.HUE_GREEN).size());

            // Approach the Armory
            double latitude = 40.104364;
            double longitude = -88.235512;
            LatLng armoryPos = new LatLng(40.104323, -88.231939);
            while (longitude < armoryPos.longitude) {
                LatLng position = new LatLng(latitude, longitude);
                launcher.sendLocationUpdate(position);
                Marker marker = map.getMarkerAt(armoryPos);
                ShadowMarker shadowMarker = Shadow.extract(marker);
                Assert.assertNotNull("Missing marker at the Armory target", marker);
                if (LatLngUtils.distance(position, armoryPos) < proximityThreshold) {
                    // Should be captured
                    Assert.assertEquals("The Armory should have been claimed at this distance and proximity threshold",
                            BitmapDescriptorFactory.HUE_GREEN, shadowMarker.getHue(), 1e-3);
                    break;
                } else {
                    // Shouldn't be captured yet
                    Assert.assertNotEquals("The Armory should not have been claimed yet at this distance and proximity threshold",
                            BitmapDescriptorFactory.HUE_GREEN, shadowMarker.getHue(), 1e-3);
                }
                longitude += 0.0002;
            }
            Assert.assertEquals("Only the Armory should have been claimed", 1, map.getMarkers().stream()
                    .filter(m -> Math.abs(Shadow.<ShadowMarker>extract(m).getHue() - BitmapDescriptorFactory.HUE_GREEN) < 1e-3).count());

            // Approach Vet-Med
            latitude = 40.090689;
            longitude = -88.217342;
            LatLng vetMedPos = new LatLng(40.092733, -88.220400);
            while (latitude < vetMedPos.latitude) {
                LatLng position = new LatLng(latitude, longitude);
                launcher.sendLocationUpdate(position);
                Marker marker = map.getMarkerAt(vetMedPos);
                ShadowMarker shadowMarker = Shadow.extract(marker);
                Assert.assertNotNull("Missing marker at the Vet-Med target", marker);
                if (LatLngUtils.distance(position, vetMedPos) < proximityThreshold) {
                    // Should be captured
                    Assert.assertEquals("Vet-Med should have been claimed at this distance and proximity threshold",
                            BitmapDescriptorFactory.HUE_GREEN, shadowMarker.getHue(), 1e-3);
                    break;
                } else {
                    // Shouldn't be captured yet
                    Assert.assertNotEquals("Vet-Med should not have been claimed yet at this distance and proximity threshold",
                            BitmapDescriptorFactory.HUE_GREEN, shadowMarker.getHue(), 1e-3);
                }
                latitude += 0.000136;
                longitude -= 0.0002;
            }
            Assert.assertEquals("Only the Armory and Vet-Med should have been claimed",
                    2, map.getMarkersWithColor(BitmapDescriptorFactory.HUE_GREEN).size());
        }
    }

    private void checkBoundsEqual(String message, LatLngBounds expected, LatLngBounds actual) {
        Assert.assertEquals(message + ": Incorrect north boundary",
                expected.northeast.latitude, actual.northeast.latitude, 1e-7);
        Assert.assertEquals(message + ": Incorrect east boundary",
                expected.northeast.longitude, actual.northeast.longitude, 1e-7);
        Assert.assertEquals(message + ": Incorrect south boundary",
                expected.southwest.latitude, actual.southwest.latitude, 1e-7);
        Assert.assertEquals(message + ": Incorrect west boundary",
                expected.southwest.longitude, actual.southwest.longitude, 1e-7);
    }

    private void checkBorderLines(ShadowGoogleMap map, double north, double east, double south, double west) {
        Assert.assertTrue("A polyline is invisible (zero width)",
                map.getPolylines().stream().noneMatch(p -> p.getWidth() < 1e-5));
        Assert.assertNotEquals("Missing/incorrect north border", 0, map
                .getPolylinesConnecting(new LatLng(north, east), new LatLng(north, west)).size());
        Assert.assertNotEquals("Missing/incorrect east border", 0, map
                .getPolylinesConnecting(new LatLng(north, east), new LatLng(south, east)).size());
        Assert.assertNotEquals("Missing/incorrect south border", 0, map
                .getPolylinesConnecting(new LatLng(south, east), new LatLng(south, west)).size());
        Assert.assertNotEquals("Missing/incorrect west border", 0, map
                .getPolylinesConnecting(new LatLng(north, west), new LatLng(south, west)).size());
    }

    private boolean inside(View control, ViewParent container) {
        if (control.getParent() == null) {
            return false;
        } else if (control.getParent() == container) {
            return true;
        } else if (control.getParent() instanceof View) {
            return inside((View) control.getParent(), container);
        } else {
            return false;
        }
    }

    private void ensureNothing(String message, Activity activity) {
        WebApiMocker.process((path, method, body, callback, errorListener) -> Assert.fail(message));
        Assert.assertNull(message, Shadows.shadowOf(activity).getNextStartedActivity());
        Assert.assertFalse(message, activity.isFinishing());
    }

    private Intent ensureGameIntent(Activity activity) {
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        Assert.assertNotNull("Creating a game should start the game activity", intent);
        Assert.assertEquals("Creating a game should launch GameActivity",
                new ComponentName(activity, GameActivity.class), intent.getComponent());
        Assert.assertNotNull("The intent should contain the game setup", intent.getExtras());
        return intent;
    }

    private JsonObject createS4Game(String id, String mode) {
        JsonObject fullUpdate = JsonHelper.game(id, SampleData.USER_EMAIL, GameStateID.RUNNING, mode,
                JsonHelper.player(SampleData.USER_EMAIL, TeamID.TEAM_GREEN, PlayerStateID.PLAYING));
        fullUpdate.addProperty("type", "full");
        return fullUpdate;
    }

    private JsonObject createS4AreaGame(String id, int cellSize) {
        JsonObject fullUpdate = createS4Game(id, "area");
        fullUpdate.addProperty("cellSize", cellSize);
        fullUpdate.addProperty("areaNorth", 40.104144);
        fullUpdate.addProperty("areaEast", -88.224515);
        fullUpdate.addProperty("areaSouth", 40.100573);
        fullUpdate.addProperty("areaWest", -88.230183);
        return fullUpdate;
    }

    private JsonObject createS4TargetGame(String id, int proximityThreshold) {
        JsonObject fullUpdate = createS4Game(id, "target");
        fullUpdate.addProperty("proximityThreshold", proximityThreshold);
        fullUpdate.add("targets", JsonHelper.arrayOf(
                JsonHelper.target("Armory", 40.104323, -88.231939, TeamID.OBSERVER),
                JsonHelper.target("VetMed", 40.092733, -88.220400, TeamID.OBSERVER)));
        return fullUpdate;
    }

}
