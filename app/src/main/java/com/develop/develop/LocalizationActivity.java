package com.develop.develop;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.library.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocalizationActivity extends AppCompatActivity implements  LocationListener , View.OnClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private MapView mMapView;
    private IMapController mapController;

    Geocoder geocoder;
    private EditText addressEditText;
    private Button searchButton;

    private LocationManager locationManager;
    private Marker currentMarker;

    private MyLocationNewOverlay mMyLocationNewOverlay;

    private SensorManager sensorManager;

    private Marker marker;
    private Sensor lightSensor;

    private SensorEventListener lightSensorListener;

    private boolean centered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.localization_activity);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        // Inicializar mapa
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();
        mapController.setZoom(15);

        // Obtener ubicación actual
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            updateCurrentMarker(location);
        }

        lightSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float lightValue = event.values[0];
                if (lightValue < 500) {
                    mMapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                } else {
                    mMapView.getOverlayManager().getTilesOverlay().setColorFilter(null);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // Inicializar el objeto MapController a partir del MapView
        mapController = mMapView.getController();
        mapController.setZoom(15);

// Inicializar el objeto Geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

// Obtener referencias a los elementos del layout
        addressEditText = findViewById(R.id.address_input);
        searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener((View.OnClickListener) this);

        mMapView.setClickable(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);

        mMapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(final GeoPoint p) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMarkerAtLocation(p);
                    }
                });
                return false;
            }
        }));


    }

    private void addMarkerAtLocation(GeoPoint p) {
        if (marker != null) {
            mMapView.getOverlays().remove(marker);
        }

        try {
            List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                marker = new Marker(mMapView);
                marker.setPosition(p);
                marker.setTitle(addressLine);
                mMapView.getOverlayManager().add(marker);
                mMapView.invalidate();

                // Obtener ubicación actual del usuario
                Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (currentLocation != null) {
                    // Calcular distancia entre la ubicación actual y el marcador creado
                    Location markerLocation = new Location("");
                    markerLocation.setLatitude(p.getLatitude());
                    markerLocation.setLongitude(p.getLongitude());
                    float distance = currentLocation.distanceTo(markerLocation);
                    distance = calculateDistance(distance);

                    // Mostrar la distancia en un Toast
                    Toast.makeText(this, "Distancia al marcador: " + String.format("%.2f",distance) + " Kilometros", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private float calculateDistance(float distance) {
        return distance / 1000; // Convertir a kilómetros
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
            if (mMyLocationNewOverlay == null) {
                mMyLocationNewOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);
                mMapView.getOverlays().add(mMyLocationNewOverlay);
                mMapView.getController().animateTo(point);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == searchButton) {
            // Obtener la dirección ingresada por el usuario
            String addressString = addressEditText.getText().toString();

            try {
                // Buscar la dirección usando el objeto Geocoder
                List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();

                    // Crear un objeto GeoPoint
                    GeoPoint point = new GeoPoint(latitude, longitude);

                    // Mover la cámara del mapa para que se centre en el punto
                    mapController.animateTo(point);

                    // Crear un marcador en el punto
                    if (marker != null) {
                        mMapView.getOverlays().remove(marker);
                    }
                    marker = new Marker(mMapView);
                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mMapView.getOverlays().add(marker);
                    mapController.setCenter(point);

                } else {
                    Toast.makeText(this, "No se encontró la dirección ingresada", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void updateCurrentMarker(Location location) {
        if (currentMarker == null) {
            currentMarker = new Marker(mMapView);
            currentMarker.setIcon(getResources().getDrawable(R.drawable.location33));
            mMapView.getOverlays().add(currentMarker);
        }
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        currentMarker.setPosition(geoPoint);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapController.setCenter(geoPoint);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                }
            }
        }
    }


    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "El proveedor de ubicación está deshabilitado.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

        // Inicializar mapa y marcador
        if (mMapView != null) {
            mMapView.setTileSource(TileSourceFactory.MAPNIK);
            mMapView.setBuiltInZoomControls(true);
            mMapView.getController().setZoom(15);
            mMapView.getController().setCenter(new GeoPoint(0, 0)); // Punto inicial, se actualizará con la ubicación del usuario
            currentMarker = new Marker(mMapView);
            currentMarker.setIcon(getResources().getDrawable(R.drawable.location_button_background));
            mMapView.getOverlays().add(currentMarker);
        }

        // Registrar listener de sensor de luminosidad
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();

        // Desregistrar listener de sensor de luminosidad
        sensorManager.unregisterListener(lightSensorListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
