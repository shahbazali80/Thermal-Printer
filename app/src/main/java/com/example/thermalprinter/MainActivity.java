package com.example.thermalprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView tv_wlcm_lines;
    ImageView newNoteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_wlcm_lines=findViewById(R.id.tv_wlcm_lines);
        newNoteBtn=findViewById(R.id.newNoteBtn);

        tv_wlcm_lines.setText("Bluetooth Notes Printer helps to take prints via thermal, lets get started with your first note " +
                "by clicking on 'New Note' button bottom right corner");

        newNoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),NewNoteActivity.class));
            }
        });
    }
}