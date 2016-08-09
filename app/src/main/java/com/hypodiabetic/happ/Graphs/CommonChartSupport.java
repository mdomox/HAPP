package com.hypodiabetic.happ.Graphs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.hypodiabetic.happ.Constants;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import io.realm.Realm;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by Tim on 16/02/2016.
 */
public class CommonChartSupport {

    public Double yIOBMax = 16D;
    public Double yIOBMin = 0D;
    public Double yCOBMax = 80D;
    public Double yCOBMin = 0D;

    public double  end_time         = (new Date().getTime() + (60000 * 130));                       //Added 120 mins to this time for future values
    public Date start_time          = new Date(new Date().getTime() - ((60000 * 60 * 24)));         //24 Hours ago
    public Context context;
    public SharedPreferences prefs;
    public double highMark;
    public double lowMark;
    public double defaultMinY;
    public double defaultMaxY;
    public boolean doMgdl;
    final int pointSize;
    public int axisTextSize;
    final int previewAxisTextSize;
    final int hoursPreviewStep;

    public double endHour;
    public final int numValues =(60/5)*24;
    public Viewport viewport;

    public Double maxBasal;                                                                         //Added Max user bolus

    public Realm realm;

    public CommonChartSupport(Context context, Realm realm){
        this.realm          = realm;
        this.context        = context;
        this.prefs          = PreferenceManager.getDefaultSharedPreferences(context);
        this.highMark       = Double.parseDouble(prefs.getString("highValue", "170"));
        this.lowMark        = Double.parseDouble(prefs.getString("lowValue", "70"));
        this.doMgdl         = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);
        this.maxBasal       = Double.parseDouble(prefs.getString("max_basal", "4"));
        defaultMinY         = unitized(40);
        defaultMaxY         = unitized(250);
        pointSize           = isXLargeTablet() ? 5 : 3;
        axisTextSize        = isXLargeTablet() ? 20 : Axis.DEFAULT_TEXT_SIZE_SP;
        previewAxisTextSize = isXLargeTablet() ? 12 : 5;
        hoursPreviewStep    = isXLargeTablet() ? 2 : 1;
    }

    public Line minShowLine() {
        List<PointValue> minShowValues = new ArrayList<PointValue>();
        minShowValues.add(new PointValue((float) start_time.getTime(), (float) defaultMinY));
        minShowValues.add(new PointValue((float) end_time, (float) defaultMinY));
        Line minShowLine = new Line(minShowValues);
        minShowLine.setHasPoints(false);
        minShowLine.setHasLines(false);
        return minShowLine;
    }

    public Axis xAxis() {
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        double start_hour_block = today.getTime().getTime();
        double timeNow = new Date().getTime() + (60000 * 120);                                      //2 Hours into the future
        for(int l=0; l<=24; l++) {
            if ((start_hour_block + (60000 * 60 * (l))) <  timeNow) {
                if((start_hour_block + (60000 * 60 * (l + 1))) >=  timeNow) {
                    endHour = start_hour_block + (60000 * 60 * (l));
                    l=25;
                }
            }
        }
        for(int l=0; l<=26; l++) {                                                                  //2 Hours into the future
            double timestamp = (endHour - (60000 * 60 * l));
            xAxisValues.add(new AxisValue((long)(timestamp), (timeFormat.format(timestamp)).toCharArray()));
        }
        xAxis.setValues(xAxisValues);
        xAxis.setHasLines(true);
        xAxis.setTextSize(axisTextSize);
        return xAxis;
    }

    public Axis iobPastyAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 8; j += 1) {
            //axisValues.add(new AxisValue((float)fitIOB2COBRange(j)));
            AxisValue value = new AxisValue(j*10);
            value.setLabel(String.valueOf(j*2) + "u");
            axisValues.add(value);
        }
        yAxis.setTextColor(ChartUtils.COLOR_BLUE);
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }
    public Axis cobPastyAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 8; j += 1) {
            AxisValue value = new AxisValue(j*10);
            value.setLabel(String.valueOf(j*10) + "g");
            axisValues.add(value);
        }
        yAxis.setTextColor(ChartUtils.COLOR_ORANGE);
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        return yAxis;
    }

    public double fitIOB2COBRange(double value){                                                    //Converts a IOB value to the COB Chart Range
        Double yBgMax = yCOBMax;
        Double yBgMin = yCOBMin;

        Double percent = (value - yIOBMin) / (yIOBMax - yIOBMin);
        return percent * (yBgMax - yBgMin) + yBgMin;
    }

    private boolean isXLargeTablet() {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public SimpleDateFormat hourFormat() {
        return new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH" : "h a");
    }

    public double unitized(double value) {
        if(doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }
}
