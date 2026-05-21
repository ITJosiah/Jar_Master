package com.example.aling_jar.utils;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aling_jar.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapPickerActivity extends AppCompatActivity {

    private static final String TAG = "MapPickerActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private TextView tvCurrentAddress;
    private MaterialButton btnConfirmLocation;
    private EditText etSearchBox;
    private FloatingActionButton fabMyLocation;

    private FusedLocationProviderClient fusedLocationClient;
    private String currentSelectedAddress = "";
    
    // To avoid spamming Nominatim when map is dragging
    private final Handler geocodeHandler = new Handler(Looper.getMainLooper());
    private Runnable geocodeRunnable;
    private static final long GEOCODE_DELAY_MS = 800; // Wait 800ms after scrolling stops

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required by osmdroid: Load configuration
        Configuration.getInstance().setUserAgentValue("AlingJarApp/1.0 (" + getPackageName() + ")");
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        // Ensure tiles are cached in internal storage to avoid permission issues on Android 10+
        Configuration.getInstance().setOsmdroidBasePath(getFilesDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        setContentView(R.layout.activity_map_picker);

        initViews();
        setupMap();
        setupListeners();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkPermissionsAndGetLocation();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        etSearchBox = findViewById(R.id.etSearchBox);
        fabMyLocation = findViewById(R.id.fabMyLocation);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);

        // Default coordinate (Manila)
        GeoPoint startPoint = new GeoPoint(14.5995, 120.9842);
        mapView.getController().setCenter(startPoint);
        
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                // User is dragging map
                tvCurrentAddress.setText("Getting address...");
                btnConfirmLocation.setEnabled(false);
                
                // Cancel pending reverse geocodes
                if (geocodeRunnable != null) {
                    geocodeHandler.removeCallbacks(geocodeRunnable);
                }
                
                geocodeRunnable = () -> {
                    GeoPoint center = (GeoPoint) mapView.getMapCenter();
                    reverseGeocode(center.getLatitude(), center.getLongitude());
                };
                
                geocodeHandler.postDelayed(geocodeRunnable, GEOCODE_DELAY_MS);
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });
    }

    private void setupListeners() {
        fabMyLocation.setOnClickListener(v -> checkPermissionsAndGetLocation());

        btnConfirmLocation.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("address", currentSelectedAddress);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        etSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchBox.getText().toString().trim();
                if (!query.isEmpty()) {
                    forwardGeocode(query);
                }
                return true;
            }
            return false;
        });
    }

    private void checkPermissionsAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    GeoPoint myPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(myPoint);
                    reverseGeocode(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(this, "Could not determine current location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────
    // Nominatim Reverse Geocoding (Lat/Lng -> Address)
    // ─────────────────────────────────────────────
    private void reverseGeocode(double lat, double lon) {
        new Thread(() -> {
            try {
                // Nominatim reverse geocode endpoint
                String urlStr = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat + "&lon=" + lon;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "AlingJarApp");
                conn.setRequestMethod("GET");
                
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                final String address = json.optString("display_name", "Unknown location");

                runOnUiThread(() -> {
                    tvCurrentAddress.setText(address);
                    currentSelectedAddress = address;
                    btnConfirmLocation.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Reverse Geocode Error", e);
                runOnUiThread(() -> {
                    tvCurrentAddress.setText("Unable to parse address");
                    btnConfirmLocation.setEnabled(false);
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────
    // Nominatim Forward Geocoding (Text -> Lat/Lng)
    // ─────────────────────────────────────────────
    private void forwardGeocode(String query) {
        tvCurrentAddress.setText("Searching...");
        btnConfirmLocation.setEnabled(false);

        new Thread(() -> {
            try {
                // Nominatim search endpoint
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=" + encodedQuery;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "AlingJarApp");
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject firstResult = jsonArray.getJSONObject(0);
                    double lat = firstResult.getDouble("lat");
                    double lon = firstResult.getDouble("lon");
                    final String addressName = firstResult.optString("display_name", query);

                    runOnUiThread(() -> {
                        GeoPoint resultPoint = new GeoPoint(lat, lon);
                        mapView.getController().animateTo(resultPoint);
                        tvCurrentAddress.setText(addressName);
                        currentSelectedAddress = addressName;
                        btnConfirmLocation.setEnabled(true);
                    });
                } else {
                    runOnUiThread(() -> {
                        tvCurrentAddress.setText("Location not found");
                        Toast.makeText(MapPickerActivity.this, "No results found for that address", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Forward Geocode Error", e);
                runOnUiThread(() -> tvCurrentAddress.setText("Search failed"));
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
