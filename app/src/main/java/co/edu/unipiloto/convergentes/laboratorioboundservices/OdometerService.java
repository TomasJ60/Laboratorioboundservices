package co.edu.unipiloto.convergentes.laboratorioboundservices;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.Provider;
import java.util.Random;


public class OdometerService extends Service {

    private final IBinder binder = new OdometerBinder();
    private LocationListener listener;
    private LocationManager locManager;
    private static double distanceInMeters;
    private static Location lastLocation = null;
    private int precision = 1;  // Precisión en metros por defecto
    private int updateTime = 1000;  // Tiempo de actualización en milisegundos por defecto

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLocationListener();
        startLocationUpdates();
    }

    private void initLocationListener() {
        listener = location -> {
            if (lastLocation == null) {
                lastLocation = location;
            }
            distanceInMeters += location.distanceTo(lastLocation);
            lastLocation = location;
        };
    }

    private void startLocationUpdates() {
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            String provider = locManager.getBestProvider(new Criteria(), true);
            if (provider != null) {
                locManager.requestLocationUpdates(provider, updateTime, precision, listener);
            }
        }

    }

    public void updateSettings(int newPrecision, int newUpdateTime) {
        this.precision = newPrecision;
        this.updateTime = newUpdateTime;
        // Reiniciar las actualizaciones de ubicación con la nueva configuración
        locManager.removeUpdates(listener);
        startLocationUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locManager != null && listener != null &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            locManager.removeUpdates(listener);
        }
    }

    public double getDistance() {
        return distanceInMeters;
    }
}