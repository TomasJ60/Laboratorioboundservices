package co.edu.unipiloto.convergentes.laboratorioboundservices;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.content.ComponentName;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Provider;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_REQUEST_CODE = 698;
    private OdometerService odometer;
    private boolean bound = false;
    private TextView distanceView, locationView;
    private EditText precisionInput, updateTimeInput;
    private final Handler handler = new Handler();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private String currentAddress = "";
    private String currentLocality = "";
    private int precision;
    private int updateTime;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) iBinder;
            odometer = odometerBinder.getOdometer();
            bound = true;
            Log.d("MainActivity", "Servicio de odómetro vinculado correctamente");
            applySettings(); // Aplica configuraciones cuando el servicio está conectado
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceView = findViewById(R.id.distance_view);
        locationView = findViewById(R.id.location_view);
        precisionInput = findViewById(R.id.precision_input);
        updateTimeInput = findViewById(R.id.update_time_input);
        Button btLocation = findViewById(R.id.bt_location);
        Button btApplySettings = findViewById(R.id.bt_apply_settings);
        Button btGenerateReport = findViewById(R.id.bt_generate_report);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        });

        btApplySettings.setOnClickListener(v -> {
            Log.d("MainActivity", "Botón 'Aplicar Configuración' presionado");
            if (bound) {
                applySettings();
            } else {
                Log.e("MainActivity", "OdometerService no está vinculado, no se pueden aplicar configuraciones.");
            }
        });

        btGenerateReport.setOnClickListener(v -> {
            Log.d("MainActivity", "Botón 'Generar Reporte' presionado");
            if (bound) {
                generateReport();
            } else {
                Log.e("MainActivity", "OdometerService no está vinculado, no se puede generar reporte.");
            }
        });

        displayDistance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (!bound) {
                // Si no está vinculado, vincula el servicio.
                Intent intent = new Intent(this, OdometerService.class);
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
                Log.d("MainActivity", "Intentando vincular al servicio de odómetro");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes continuar con la acción.
                Log.d("MainActivity", "Permiso concedido");
            } else {
                // Permiso denegado, muestra un mensaje al usuario.
                Log.d("MainActivity", "Permiso denegado");
                Toast.makeText(this, "Permiso necesario para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayDistance() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (bound && odometer != null) {
                    double distance = odometer.getDistance();
                    String distanceStr = String.format(Locale.getDefault(), "%1$,.2f metros", distance);
                    distanceView.setText(distanceStr);
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                Location location = task.getResult();
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (!addresses.isEmpty()) {
                            currentLocality = addresses.get(0).getSubLocality();
                            currentAddress = addresses.get(0).getAddressLine(0);
                            locationView.setText("Localidad: " + currentLocality + "\nDirección: " + currentAddress);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void applySettings() {
        try {
            precision = Integer.parseInt(precisionInput.getText().toString());
            updateTime = Integer.parseInt(updateTimeInput.getText().toString()) * 1000; // Convertir a milisegundos

            if (bound && odometer != null) {
                odometer.updateSettings(precision, updateTime);

                // Log para mostrar información de configuración
                Log.d("MainActivity", "Configuración aplicada - Precisión: " + precision + ", Tiempo de actualización: " + updateTime / 1000 + " segundos");

                // Mostrar el toast de configuración exitosa
                Toast.makeText(this, "Configuraciones aplicadas: Precisión = " + precision + ", Tiempo de actualización = " + updateTime / 1000 + " segundos", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingrese valores válidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateReport() {
        // Verifica si el servicio está vinculado correctamente
        if (odometer == null) {
            Log.e("MainActivity", "OdometerService no está vinculado");
            Toast.makeText(this, "Servicio no disponible, por favor espere...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifica permisos de escritura en almacenamiento externo
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return;
        }

        // Crea el directorio donde se guardará el reporte
        File directory = new File(Environment.getExternalStorageDirectory(), "Resultados Odómetro");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Crea el archivo para el reporte
        File file = new File(directory, "Reporte_Odómetro.csv");

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.append("Dirección,Localidad,Precisión (m),Tiempo de actualización (s),Distancia (m)\n");
            String reportLine = String.format("%s,%s,%d,%d,%.2f\n",
                    currentAddress, currentLocality, precision, updateTime / 1000, odometer.getDistance());
            writer.append(reportLine);
            writer.flush();

            // Muestra el Toast con la ruta del archivo
            Toast.makeText(this, "Reporte generado exitosamente en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "Reporte generado exitosamente en: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("MainActivity", "Error al generar el reporte", e);
            Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show();
        }
    }

}




