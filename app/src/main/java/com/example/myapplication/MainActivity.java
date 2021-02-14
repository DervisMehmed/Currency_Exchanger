package com.example.myapplication;

/*
 *   Basic currency converter app.
 *   This class deals with the conversion between physical currencies
 *   Application uses "https://github.com/patriques82/alphavantage4j" api to collect data
 */

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import org.patriques.ForeignExchange;
import org.patriques.input.timeseries.OutputSize;
import org.patriques.output.AlphaVantageException;
import org.patriques.output.exchange.CurrencyExchange;
import org.patriques.output.exchange.Daily;
import org.patriques.output.exchange.data.CurrencyExchangeData;
import org.patriques.output.exchange.data.ForexData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {

    String TAG = "MainActivity";
    Context c = this;
    int n = 0;

    String apiKey = "API_Key insert";
    int timeout = 3000;
    DecimalFormat numberFormat;
    String curr1;
    String curr2;
    AlphaVantageConnector apiConnector;
    ForeignExchange foreignExchange;
    String[] currencies;
    String graphOp = "";
    GraphView graph;
    Daily daily = null;
    Boolean flagDaily = false;

    Button refresh;
    CheckBox c1;
    CheckBox c2;
    CheckBox c3;
    CheckBox c4;
    AutoCompleteTextView autocomplete;
    AutoCompleteTextView autocomplete2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InputStream is2 = getResources().openRawResource(R.raw.physical_currency_list);
        BufferedReader reader2 = new BufferedReader(
                new InputStreamReader(is2, Charset.forName("UTF-8")));
        String lines = "";

        try {
            while ((lines = reader2.readLine()) != null) {
                n++;
            }
            is2.close();
            reader2.close();
        } catch (IOException e1) {
            Log.e(TAG, "Error" + lines, e1);
            e1.printStackTrace();
        }
        currencies = new String[n];
        for (int i = 0; i < currencies.length; i++) {
            currencies[i] = "";
        }
        readData();

        findViewById(R.id.textView2).setVerticalScrollBarEnabled(true);

        numberFormat = new DecimalFormat("#.00");

        c1 = findViewById(R.id.checkBox);
        c2 = findViewById(R.id.checkBox2);
        c3 = findViewById(R.id.checkBox3);
        c4 = findViewById(R.id.checkBox4);
        refresh = findViewById(R.id.refreshButton);
        autocomplete = findViewById(R.id.autoCompleteTextView2);
        autocomplete2 = findViewById(R.id.autoCompleteTextView);
        graph = findViewById(R.id.graph);

        c1.setChecked(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, currencies);

        autocomplete.setThreshold(1);
        autocomplete.setAdapter(adapter);
        autocomplete2.setThreshold(1);
        autocomplete2.setAdapter(adapter);

        apiConnector = new AlphaVantageConnector(apiKey, timeout);
        foreignExchange = new ForeignExchange(apiConnector);

        //  Swap Button
        findViewById(R.id.swapButton).setOnClickListener(v -> {
            autocomplete.setText(curr2);
            autocomplete2.setText(curr1);

            if (!autocomplete.getText().toString().equals(curr1) || !autocomplete2.getText().toString().equals(curr2))
            {
                flagDaily = false;
                clearGraph();
            }

            String temp = curr1;
            curr1 = curr2;
            curr2 = temp;
            exchange(foreignExchange, curr1, curr2);
            fillGraph(foreignExchange, curr1, curr2);
        });

        //  Refresh Button Listener
        refresh.setOnClickListener(v ->
        {
            try {
                if (!autocomplete.getText().toString().equals(curr1) || !autocomplete2.getText().toString().equals(curr2))
                {
                    flagDaily = false;
                    clearGraph();
                }

                if(!autocomplete.getText().toString().isEmpty() && !autocomplete2.getText().toString().isEmpty()){
                    curr1 = autocomplete.getText().toString();
                    curr2 = autocomplete2.getText().toString();

                    exchange(foreignExchange, curr1, curr2);
                    fillGraph(foreignExchange, curr1, curr2);
                }
                else { Toast.makeText(getApplicationContext(),"Choose two Currencies!", Toast.LENGTH_SHORT).show(); }
            } catch (Exception e) {
                errorPopup(c, e);
            }
        });

        //  Clear Button Listener
        findViewById(R.id.button).setOnClickListener( v-> clearGraph());

        findViewById(R.id.button2).setOnClickListener( v->
        {
            cyrptoActivity();
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

    private void cyrptoActivity() {
        Intent switchActivityIntent = new Intent(this, CyrptoActivity.class);
        startActivity(switchActivityIntent);
    }

    public void fillGraph(ForeignExchange foreignExchange2, String curr1, String curr2)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                ArrayList<ForexData> forexDataArrayList = new ArrayList<ForexData>();
                AtomicBoolean flag2 = new AtomicBoolean(false);

                try {
                    if(flagDaily == false) {
                        // DAILY = Last 100 exchange data from curr1 to curr2
                        daily = foreignExchange2.daily(curr1, curr2, OutputSize.COMPACT);
                        flagDaily = true;
                    }
                    Daily finalDaily1 = daily;
                    daily.getForexData().forEach(data -> {
                        if (data.getDateTime().getDayOfYear() > finalDaily1.getForexData().get(0).getDateTime().getDayOfYear()) {
                            flag2.set(true);
                        }
                        if (flag2.get() != true) {
                            forexDataArrayList.add(data);
                        }
                    });

                    series.clearReference(graph);
                    int monthNo = forexDataArrayList.get(0).getDateTime().getMonthValue();
                    String monthName = forexDataArrayList.get(0).getDateTime().getMonth().toString();
                    // --- Daily Graph Ops.
                    if (c1.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getClose()), false, forexDataArrayList.size());
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c1.getText().toString());
                        series.setColor(Color.parseColor("#4B0082"));
                    }
                    else if (c2.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getOpen()), false, forexDataArrayList.size());
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c2.getText().toString());
                        series.setColor(Color.parseColor("#E9CFEC"));
                    }
                    else if (c3.isChecked()) {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getHigh()), false, forexDataArrayList.size());
                                //Log.d(TAG, "day of mounth\t:\t" + forexDataArrayList.get(i).getDateTime().getMonth() + " . " + forexDataArrayList.get(i).getDateTime().getDayOfMonth());
                            }
                        }
                        graphOp = graphOp.concat(c3.getText().toString());
                        series.setColor(Color.parseColor("#B93B8F"));
                    }
                    else {
                        for (int i = forexDataArrayList.size() - 1; i > 0; i--) {
                            if (forexDataArrayList.get(i).getDateTime().getMonthValue() == monthNo) {
                                series.appendData(new DataPoint(forexDataArrayList.get(i).getDateTime().getDayOfMonth(), forexDataArrayList.get(i).getLow()), false, forexDataArrayList.size());
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

    public void exchange(ForeignExchange foreignExchange2, String curr1, String curr2)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CurrencyExchange currencyExchange;
                CurrencyExchangeData currencyExchangeData = null;

                try {
                    // Current Exchange rate
                    currencyExchange = foreignExchange2.currencyExchangeRate(curr1, curr2);
                    currencyExchangeData = currencyExchange.getData();

                } catch (AlphaVantageException e) {
                    errorPopup(c, e);
                }

                CurrencyExchangeData finalCurrencyExchangeData = currencyExchangeData;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //  Result of Currency exchange rate
                            ((TextView) findViewById(R.id.textView2)).setText("");
                            ((TextView) findViewById(R.id.textView2)).append("Current Value:\t" + finalCurrencyExchangeData.getFromCurrencyCode()
                                    + "\t-\t" + finalCurrencyExchangeData.getToCurrencyCode()
                                    + " :\t" + numberFormat.format(finalCurrencyExchangeData.getExchangeRate()));

                        } catch (Exception e) {
                            errorPopup(c, e);
                        }
                    }
                });
            }
        }).start();
    }

    private void clearGraph()
    {
        graph.removeAllSeries();
        graphOp = "";
        graph.setTitle(" ");
    }

    // Reading Currency names from physical_currency_list.csv
    private void readData() {
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
                currencies[i] = tokens[0];
                i++;
            }
            Log.d(TAG, currencies.length + "");
            is.close();
            reader.close();
        } catch (IOException e1) {
            errorPopup(c, e1);
        }
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
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}