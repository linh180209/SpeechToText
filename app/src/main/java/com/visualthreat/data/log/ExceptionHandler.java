package com.visualthreat.data.log;

import android.app.Activity;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.visualthreat.data.unit.UnitFunction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by USER on 2/4/2017.
 */

public class ExceptionHandler implements
        Thread.UncaughtExceptionHandler {
    private static String TAG = "ExceptionHandler";
    private final Activity myContext;
    private final String LINE_SEPARATOR = "\n";

    public ExceptionHandler(Activity context) {
        myContext = context;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);

        try {
            try {
                String nameFoler = UnitFunction.CreateFolder(myContext);
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss");
                String date = format.format(new Date());
                File file = new File(Environment.getExternalStorageDirectory() + "/" + nameFoler + "/" + date + "_crash.txt");
                byte[] data = String.valueOf(errorReport).getBytes();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (Exception ex) {
                Log.d(TAG, "uncaughtException: " + ex);
            }
        } catch (Exception e) {
            Log.d(TAG, "uncaughtException: " + e);
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

}
