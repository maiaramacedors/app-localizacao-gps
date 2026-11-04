package com.example.localizacao;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private TextView txtStatus;
    private TextView txtLatitude;
    private TextView txtLongitude;
    private TextView txtPrecisao;
    private EditText edtDestino;

    private boolean rastreando = false;
    private Location ultimaLocalizacao;

    // Mostra a janela de permissao e recebe a resposta do usuario.
    private final ActivityResultLauncher<String[]> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (temPermissao()) {
                    iniciar();
                } else {
                    txtStatus.setText("Permissão negada");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        txtLatitude = findViewById(R.id.txtLatitude);
        txtLongitude = findViewById(R.id.txtLongitude);
        txtPrecisao = findViewById(R.id.txtPrecisao);
        edtDestino = findViewById(R.id.edtDestino);
        Button btnRota = findViewById(R.id.btnRota);
        btnRota.setOnClickListener(v -> verRota());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Pede uma atualizacao a cada 5 segundos, com a maior precisao possivel.
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        // O sistema chama este metodo sempre que ha uma nova localizacao.
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    ultimaLocalizacao = location;
                    txtLatitude.setText(String.format(Locale.getDefault(), "Latitude: %.6f", location.getLatitude()));
                    txtLongitude.setText(String.format(Locale.getDefault(), "Longitude: %.6f", location.getLongitude()));
                    txtPrecisao.setText(String.format(Locale.getDefault(), "Precisão: %.0f m", location.getAccuracy()));
                    txtStatus.setText("Localização encontrada. Digite o destino.");
                }
            }
        };

        // Busca a localizacao assim que o app abre.
        if (temPermissao()) {
            iniciar();
        } else {
            pedirPermissao.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rastreando && temPermissao()) {
            comecarAtualizacoes();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Para de receber localizacoes para economizar bateria.
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private boolean temPermissao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void iniciar() {
        rastreando = true;
        txtStatus.setText("Buscando localização...");
        comecarAtualizacoes();
    }

    // A permissao ja foi conferida antes de este metodo ser chamado.
    @SuppressLint("MissingPermission")
    private void comecarAtualizacoes() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Abre o Google Maps com a rota da posicao atual ate o destino digitado.
    private void verRota() {
        String destino = edtDestino.getText().toString().trim();

        if (ultimaLocalizacao == null) {
            Toast.makeText(this, "Aguarde a localização", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destino.isEmpty()) {
            Toast.makeText(this, "Digite um destino", Toast.LENGTH_SHORT).show();
            return;
        }

        String endereco = "https://www.google.com/maps/dir/?api=1&origin="
                + ultimaLocalizacao.getLatitude() + "," + ultimaLocalizacao.getLongitude()
                + "&destination=" + Uri.encode(destino);

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(endereco)));
    }
}