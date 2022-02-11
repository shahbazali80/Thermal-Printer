package com.example.thermalprinter;

import static android.graphics.Typeface.BOLD;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thermalprinter.models.FacetypeModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NewNoteActivity extends AppCompatActivity {

    Toolbar toolbar;
    EditText et_note;

    BottomNavigationView bottomNavigationView;

    BottomSheetDialog sizeDialog, formatDialog, fontFamilyDialog;

    int seekbarValue;
    String selectedTxt;

    List<FacetypeModel> list;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    List<String> BTDeviceList;

    boolean isBold = false, isItalic = false, isUnderLine = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        toolbar = findViewById(R.id.newNoteToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        et_note = findViewById(R.id.et_note);

        fontFamilyDialog = new BottomSheetDialog(this);
        sizeDialog = new BottomSheetDialog(this);
        formatDialog = new BottomSheetDialog(this);
        openFontDialog();
        openFontSizeDialog();
        openFontFormatDialog();

        list = new ArrayList<>();
        BTDeviceList = new ArrayList<>();

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Toolbar title clicked", Toast.LENGTH_SHORT).show();
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        fontFamilyDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        sizeDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        formatDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            {
                switch (item.getItemId()) {
                    case R.id.font:
                        fontFamilyDialog.show();
                        break;
                    case R.id.size:
                        sizeDialog.show();
                        break;
                    case R.id.format:
                        formatDialog.show();
                        break;
                    case R.id.doneNote:
                        SpannableString spannableString = new SpannableString(et_note.getText().toString());
                        StringBuffer buffer = new StringBuffer();
                        //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                        for (FacetypeModel model : list) {
                            buffer.append(model.getStart());
                            buffer.append(model.getEnd() + "\n");

                            StyleSpan boldSpan = new StyleSpan(BOLD);

                            spannableString.setSpan(boldSpan, model.getStart(), model.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        et_note.setText(spannableString);
                        Toast.makeText(getApplicationContext(), buffer, Toast.LENGTH_SHORT).show();

                        break;
                }
                return false;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.new_note_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_printerOut:
                try {
                    sendData();
                    //boldText();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error Occur During Sending Data\n"+e.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_findPrinter:
                findBT();
                return true;
            case R.id.menu_setting:
                Toast.makeText(getApplicationContext(), "Setting", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_about:
                StringBuffer buffer=new StringBuffer();
                for (String btDevice: BTDeviceList) {
                    buffer.append(btDevice);
                }

                Toast.makeText(getApplicationContext(), buffer, Toast.LENGTH_SHORT).show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void boldText() {
        int start = et_note.getSelectionStart();
        int end = et_note.getSelectionEnd();

        //StringBuffer buffer=new StringBuffer();
        //buffer.append(start+" "+end+"\n");

        list.add(new FacetypeModel(start, end, 1));

        SpannableString spannableString = new SpannableString(et_note.getText().toString());
        StringBuffer buffer = new StringBuffer();
        //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        for (FacetypeModel model : list) {
            buffer.append(model.getStart());
            buffer.append(model.getEnd() + "\n");

            StyleSpan boldSpan = new StyleSpan(BOLD);

            spannableString.setSpan(boldSpan, model.getStart(), model.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        et_note.setText(spannableString);

        /*SpannableString spannableString=new SpannableString(et_note.getText().toString());

        StyleSpan boldSpan=new StyleSpan(BOLD);

        spannableString.setSpan(boldSpan,start,end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        et_note.setText(spannableString);*/

        //et_note.setHighlightColor(ContextCompat.getColor(this, R.color.purple_200));
        Toast.makeText(getApplicationContext(), buffer, Toast.LENGTH_SHORT).show();
    }

    private void selectText() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        ClipData.Item item = clipData.getItemAt(0);
        selectedTxt = item.getText().toString();
    }

    private void openFontFormatDialog() {
        View view = getLayoutInflater().inflate(R.layout.custom_font_format_layout, null, false);

        ImageView closeFormatDailog = view.findViewById(R.id.fontFormatClose);
        RadioButton rb_bold = view.findViewById(R.id.rb_bold);
        RadioButton rb_italic = view.findViewById(R.id.rb_italic);
        RadioButton rb_underLine = view.findViewById(R.id.rb_underLine);
        RadioGroup rg_fontJustify = view.findViewById(R.id.rg_fontJustify);

        closeFormatDailog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                formatDialog.cancel();
            }
        });

        rb_bold.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View view) {
                if (isBold) {
                    isBold = false;
                    //et_note.setTypeface(et_note.getTypeface(), Typeface.NORMAL);
                    //et_note.setPaintFlags(et_note.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    rb_bold.setChecked(false);
                } else {
                    isBold = true;
                    if (isItalic)
                        et_note.setTypeface(et_note.getTypeface(), Typeface.ITALIC | Typeface.BOLD);
                    else
                        et_note.setTypeface(et_note.getTypeface(), BOLD);
                    rb_bold.setChecked(true);
                }
            }
        });

        rb_italic.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View view) {
                if (isItalic) {
                    isItalic = false;
                    //et_note.setTypeface(et_note.getTypeface(), Typeface.NORMAL);
                    //et_note.setPaintFlags(et_note.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    rb_italic.setChecked(false);
                } else {
                    isItalic = true;

                    if (isBold)
                        et_note.setTypeface(et_note.getTypeface(), Typeface.ITALIC | Typeface.BOLD);
                    else
                        et_note.setTypeface(et_note.getTypeface(), Typeface.ITALIC);

                    rb_italic.setChecked(true);
                }
            }
        });

        rb_underLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUnderLine) {
                    isUnderLine = false;
                    //et_note.setPaintFlags(et_note.getPaintFlags() | Paint.C);
                    rb_underLine.setChecked(false);
                } else {
                    isUnderLine = true;
                    et_note.setPaintFlags(et_note.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    rb_underLine.setChecked(true);
                }
            }
        });

        rg_fontJustify.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                switch (i) {
                    case R.id.rb_left:
                        et_note.setGravity(Gravity.LEFT);
                        return;
                    case R.id.rb_center:
                        et_note.setGravity(Gravity.CENTER);
                        return;
                    case R.id.rb_right:
                        et_note.setGravity(Gravity.RIGHT);
                        return;
                }
            }
        });
        formatDialog.setContentView(view);
    }

    private void openFontSizeDialog() {

        View view = getLayoutInflater().inflate(R.layout.custom_font_size_dialog_layout, null, false);

        ImageView close = view.findViewById(R.id.fontSizeClose);
        TextView fontSizeShow = view.findViewById(R.id.fontSizeTvShow);
        SeekBar seekBar = view.findViewById(R.id.fontSizeSeekbar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seekbarValue = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                et_note.setTextSize(seekbarValue);
                fontSizeShow.setText(seekbarValue + "");
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sizeDialog.cancel();
            }
        });

        sizeDialog.setContentView(view);
    }

    private void openFontDialog() {
        View view = getLayoutInflater().inflate(R.layout.custom_font_layout, null, false);

        TextView font_abril_fatface = view.findViewById(R.id.font_abril_fatface);
        TextView font_alegreya_sans_regular = view.findViewById(R.id.font_alegreya_sans_regular);
        TextView font_abeezee_italic = view.findViewById(R.id.font_abeezee_italic);
        TextView font_calligraffitti = view.findViewById(R.id.font_calligraffitti);
        TextView font_cookie_regular = view.findViewById(R.id.font_cookie_regular);
        TextView font_simonetta_italic = view.findViewById(R.id.font_simonetta_italic);
        ImageView closeDialog = view.findViewById(R.id.fontClose);

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fontFamilyDialog.cancel();
            }
        });

        font_abril_fatface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.abril_fatface);
                et_note.setTypeface(typeface);
                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        font_alegreya_sans_regular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.alegreya_sans_regular);
                et_note.setTypeface(typeface);

                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        font_abeezee_italic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.abeezee_italic);
                et_note.setTypeface(typeface);

                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        font_calligraffitti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.calligraffitti);
                et_note.setTypeface(typeface);

                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        font_cookie_regular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.cookie_regular);
                et_note.setTypeface(typeface);

                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        font_simonetta_italic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.simonetta_italic);
                et_note.setTypeface(typeface);

                font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0);
            }
        });

        fontFamilyDialog.setContentView(view);
    }

    @SuppressLint("MissingPermission")
    void findBT() {

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);

            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            BTDeviceList.clear();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    BTDeviceList.add(device.getName());
                }
            }

            openBTDeviceDialog();

        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error Occur During Find BT Device\n"+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openBTDeviceDialog() {
        final AlertDialog.Builder alert=new AlertDialog.Builder(NewNoteActivity.this);
        View view=getLayoutInflater().inflate(R.layout.custom_bt_devices_layout, null);

        final TextView tv_mybtName=view.findViewById(R.id.tv_mybtName);
        final Switch switch_btOn=view.findViewById(R.id.switch_btOn);
        final ListView list_device=view.findViewById(R.id.list_device);
        final ImageView closeBtDialog=view.findViewById(R.id.closeBtDialog);

        alert.setView(view);
        final AlertDialog alertDialog=alert.create();
        alertDialog.setCanceledOnTouchOutside(false);

        tv_mybtName.setText(mBluetoothAdapter.getName());
        switch_btOn.setChecked(true);

        switch_btOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    findBT();
                } else {
                    mBluetoothAdapter.disable();
                    tv_mybtName.setText("BT OFF");
                    BTDeviceList.clear();
                    list_device.setVisibility(View.GONE);
                }
            }
        });

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, BTDeviceList);
        list_device.setAdapter(adapter);

        closeBtDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        list_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {

                        if(adapter.getItem(position).trim().equals(device.getName())) {
                            try {
                                mmDevice = device;
                                openBT();
                                alertDialog.dismiss();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(NewNoteActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });

        alertDialog.show();
    }

    @SuppressLint("MissingPermission")
    void openBT() throws IOException {
        try {

            // Standard SerialPortService ID
            //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();

            Toast.makeText(getApplicationContext(), "Bluetooth Opened", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error Occur During Open BT Device\n"+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                Toast.makeText(getApplicationContext(), packetBytes+"", Toast.LENGTH_SHORT).show();
                                mmInputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendData() throws IOException {
        try {
            String msg = et_note.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

            Toast.makeText(getApplicationContext(), "Data Sent to Printer", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error Occur During Sending Data Method Calling\n"+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //if(mBluetoothAdapter.isEnabled())
            //mBluetoothAdapter.disable();
    }
}