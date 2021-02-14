package com.example.myapplication;

/*
*   Basic currency converter app.
*   This class deals with the conversion from digital currencies to physical currencies
*   Application uses "https://github.com/patriques82/alphavantage4j" api to collect data
*/

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.patriques.AlphaVantageConnector;
import org.patriques.DigitalCurrencies;
import org.patriques.input.digitalcurrencies.Market;
import org.patriques.output.digitalcurrencies.Daily;
import org.patriques.output.digitalcurrencies.data.DigitalCurrencyData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CyrptoActivity extends AppCompatActivity {

    //  Variables
    String TAG = "CyrptoActivity";
    Context c = this;
    int numDigi = 0;
    int numPhys = 0;
    String curr1;
    String curr2;
    String graphOp = "";
    Boolean flagDaily = false;

    String apiKey = "API_Key insert";
    int timeout = 3000;
    String[] digiCurrencies;
    String[] physCurrencies;
    Daily response = null;

    GraphView graph;
    Button refresh;
    CheckBox c1;
    CheckBox c2;
    CheckBox c3;
    CheckBox c4;
    AutoCompleteTextView autocomplete;
    AutoCompleteTextView autocomplete2;

    AlphaVantageConnector apiConnector;
    DigitalCurrencies digitalCurrencies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cyroto);

        InputStream is2 = getResources().openRawResource(R.raw.digital_currency_list);
        InputStream is3 = getResources().openRawResource(R.raw.physical_currency_list);
        BufferedReader reader2 = new BufferedReader(
                new InputStreamReader(is2, Charset.forName("UTF-8")));
        BufferedReader reader3 = new BufferedReader(
                new InputStreamReader(is3, Charset.forName("UTF-8")));
        String lines = "";
        String pLines= "";

        try {
            while ((lines = reader2.readLine()) != null) {  // Tracing numbers of Digital Currencies
                numDigi++;
            }
            is2.close();
            reader2.close();

            while ((pLines = reader3.readLine()) != null) { // Tracing Numbers of Physical Currencies
                numPhys++;
            }
            is3.close();
            reader3.close();
        } catch (IOException e1) {
            Log.e(TAG, "Error" , e1);
            e1.printStackTrace();
        }
        digiCurrencies = new String[numDigi];
        for (int i = 0; i < digiCurrencies.length; i++) {
            digiCurrencies[i] = "";
        }
        physCurrencies = new String[numPhys];
        for (int i = 0; i < physCurrencies.length; i++) {
            physCurrencies[i] = "";
        }
        readPhysData();
        readDigiData();

        refresh = findViewById(R.id.refreshButton);
        c1 = findViewById(R.id.checkBox);
        c2 = findViewById(R.id.checkBox2);
        c3 = findViewById(R.id.checkBox3);
        c4 = findViewById(R.id.checkBox4);
        autocomplete = findViewById(R.id.autoCompleteTextView2);
        autocomplete2 = findViewById(R.id.autoCompleteTextView);
        graph = findViewById(R.id.graph);

        ArrayAdapter<String> digiAdapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, digiCurrencies);
        ArrayAdapter<String> physAdapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, physCurrencies);

        autocomplete.setThreshold(1);
        autocomplete.setAdapter(digiAdapter);
        autocomplete2.setThreshold(1);
        autocomplete2.setAdapter(physAdapter);

        apiConnector = new AlphaVantageConnector(apiKey, timeout);
        digitalCurrencies = new DigitalCurrencies(apiConnector);

        findViewById(R.id.button).setOnClickListener( v-> clearGraph());
        findViewById(R.id.button2).setOnClickListener( v-> physActivity());

        //  Refresh Button Listener
        refresh.setOnClickListener(v ->
        {
            try {
                if (!autocomplete.getText().toString().split(",")[0].equals(curr1) || !autocomplete2.getText().toString().equals(curr2))
                {
                    flagDaily = false;
                    clearGraph();
                }

                if(!autocomplete.getText().toString().isEmpty() && !autocomplete2.getText().toString().isEmpty()){
                    curr1 = autocomplete.getText().toString().split(",")[0];
                    curr2 = autocomplete2.getText().toString();

                    Log.d(TAG, " Curr1 : " + curr1 + "\tCurr2 : " + curr2);
                    exchange(digitalCurrencies, curr1, curr2);
                    fillGraph(digitalCurrencies, curr1, curr2);
                }
                else { Toast.makeText(getApplicationContext(),"Choose two Currencies!", Toast.LENGTH_SHORT).show(); }
            } catch (Exception e) {
                errorPopup(c, e);
            }
        });

        //  Only one checkBox is checked
        c1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    c2.setChecked(false);
                    c3.setChecked(false);
                    c4.setChecked(false);
                }
            }
        });
        c2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    c1.setChecked(false);
                    c3.setChecked(false);
                    c4.setChecked(false);
                }
            }
        });
        c3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    c4.setChecked(false);
                    c2.setChecked(false);
                    c1.setChecked(false);
                }
            }
        });
        c4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    c1.setChecked(false);
                    c2.setChecked(false);
                    c3.setChecked(false);
                }
            }
        });
    }

    public void exchange(DigitalCurrencies digitalExchange2, String curr1, String curr2)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DigitalCurrencyData> digitalData = null;
                try {
                    response = digitalExchange2.daily(curr1, Market.valueOf(curr2));
                    Map<String, String> metaData = response.getMetaData();
                    digitalData = response.getDigitalData();
                } catch (Exception e) { errorPopup(c, e); }

                List<DigitalCurrencyData> finalDigitalData = digitalData;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //  Result of Currency exchange rate
                            ((TextView) findViewById(R.id.textView2)).setText("");
                            ((TextView) findViewById(R.id.textView2)).append("Date:\t" + finalDigitalData.get(0).getDateTime().getDayOfMonth() + " "+ finalDigitalData.get(0).getDateTime().getMonth()
                                    + "\nClosing Value: " + finalDigitalData.get(0).getCloseA());

                        } catch (Exception e) { errorPopup(c, e); }
                    }
                });
            }
        }).start();
    }

    public void fillGraph(DigitalCurrencies foreignExchange2, String curr1, String curr2)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                ArrayList<DigitalCurrencyData> forexDataArrayList = new ArrayList<DigitalCurrencyData>();
                AtomicBoolean flag2 = new AtomicBoolean(false);

                try {
                    if(flagDaily == false) {
                        // DAILY = Last 100 exchange data from curr1 to curr2
                        response = foreignExchange2.daily(curr1, Market.valueOf(curr2));
                        flagDaily = true;
                    }
                    org.patriques.output.digitalcurrencies.Daily finalDaily1 = response;
                    response.getDigitalData().forEach(data -> {
                        if (data.getDateTime().getDayOfYear() > finalDaily1.getDigitalData().get(0).getDateTime().getDayOfYear()) {
                            flag2.set(true);
                        }
                        if (flag2.get() != true) {
                            forexDataArrayList.add(data);
                        }
                    });

                    series.clearReference(graph);
                    int monthNo = forexDataArrayList.get(0).getDateTime().getMonthValue();
                    int graphXSize = forexDataArrayList.get(0).getDateTime().getDayOfMonth();
                    //Log.d(TAG, graphXSize + " /-*");
                    String monthName = forexDataArrayList.get(0).getDateTime().getMonth().toString();
                    // --- Daily Graph Ops.
                    if (c1.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getCloseA()), true, graphXSize);
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c1.getText().toString());
                        series.setColor(Color.parseColor("#4B0082"));
                    }
                    else if (c2.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getOpenA()), true, graphXSize);
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c2.getText().toString());
                        series.setColor(Color.parseColor("#E9CFEC"));
                    }
                    else if (c3.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getHighA()), true, graphXSize);
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c3.getText().toString());
                        series.setColor(Color.parseColor("#B93B8F"));
                    }
                    else {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getLowA()), true, graphXSize);
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c4.getText().toString());
                        series.setColor(Color.parseColor("#990012"));
                    }

                    series.setOnDataPointTapListener(new OnDataPointTapListener() {
                        @Override
                        public void onTap(Series series, DataPointInterface dataPoint) {
                            Toast.makeText(c, "Series: On Data Point clicked: "+dataPoint, Toast.LENGTH_SHORT).show();
                        }
                    });
                    // styling series
                    series.setDrawDataPoints(true);
                    series.setDataPointsRadius(10);
                    series.setThickness(8);
                    graph.setTitle(monthName + graphOp + "Values!");
                    graph.getViewport().setXAxisBoundsManual(false);
                } catch (Exception e)
                {
                    errorPopup(c, e);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try
                        {
                            // Filling the DAILY Graph
                            graph.addSeries(series);
                        } catch (Exception e) {
                            errorPopup(c, e);
                        }
                    }
                });
            }
        }).start();
    }

    // Reading Currency names from digital_currency_list.csv
    private void readDigiData()
    {
        InputStream is = getResources().openRawResource(R.raw.digital_currency_list);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";

        int i = 0;
        boolean flag = false;

        try {
            while ((line = reader.readLine()) != null) {
                if (!flag) {
                    flag = true;
                    continue;
                }
                // Split the line into different tokens (using the comma as a separator).
                //String[] tokens = line.split(",");
                digiCurrencies[i] = line;    //tokens[0];
                i++;
            }
            Log.d(TAG, digiCurrencies.length + " digi");
            is.close();
            reader.close();
        } catch (IOException e1) {
            errorPopup(c, e1);
        }
    }

    // Reading Currency names from physical_currency_list.csv
    private void readPhysData() {
        InputStream is = getResources().openRawResource(R.raw.physical_currency_list);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";

        int i = 0;
        boolean flag = false;

        try {
            while ((line = reader.readLine()) != null) {
                if (!flag) {
                    flag = true;
                    continue;
                }
                // Split the line into different tokens (using the comma as a separator).
                String[] tokens = line.split(",");
                physCurrencies[i] = tokens[0];
                i++;
            }
            Log.d(TAG, physCurrencies.length + " phys");
            is.close();
            reader.close();
        } catch (IOException e1) {
            errorPopup(c, e1);
        }
    }

    private void physActivity() {
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        startActivity(switchActivityIntent);
    }

    private void clearGraph()
    {
        graph.removeAllSeries();
        graphOp = "";
        graph.setTitle(" ");
    }

    private void errorPopup(Context d, Exception e)
    {
        new AlertDialog.Builder(d)
                .setTitle("Error!")
                .setMessage(e.getMessage())
                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}