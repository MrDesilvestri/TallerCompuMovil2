package com.develop.develop;


import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {

    private ImageButton camaraButton;
    private ImageButton contactsButton;
    private ImageButton localizationButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        camaraButton = findViewById(R.id.Camara_BTN);
        contactsButton = findViewById(R.id.Contacts_BTN);
        localizationButton = findViewById(R.id.Map_BTN);

        camaraButton.setOnClickListener(v -> {
            Intent camaraIntent = new Intent(MainActivity.this, CamaraActivity.class);
            startActivity(camaraIntent);
        });

        contactsButton.setOnClickListener(v -> {
            Intent contactsIntent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(contactsIntent);
        });

        localizationButton.setOnClickListener(v -> {
            Intent localizationIntent = new Intent(MainActivity.this, LocalizationActivity.class);
            startActivity(localizationIntent);
        });
    }
}

