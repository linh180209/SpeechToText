package com.visualthreat.data.log;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.visualthreat.data.unit.UnitFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by USER on 1/2/2017.
 */

public class WriteLog {
    private File file;
    private BufferedWriter fileWriter;
    private static final String TAG = "WriteLog";
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss");
    /**
     * init file log
     */
    public WriteLog(Context context) {
        String nameFoler = UnitFunction.CreateFolder(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss");
        String date = format.format(new Date());
        file = new File(Environment.getExternalStorageDirectory() + "/" + nameFoler + "/" + date + ".txt");
        Log.d(TAG, "path log: " + file.getPath());
        if (!file.exists()) {
            try {
                file.createNewFile();
                Log.d("File created ", "File created ");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        try {
            fileWriter = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            fileWriter = null;
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * add time in data log
     * @param data data log
     * @return new data log
     */
    public String GeneralLog(String data){
        String date = format.format(new Date());
        String text = date + ": " + data;
        return text;
    }

    /**
     * add text to file
     * @param data data
     * @return string data
     */
    public void addLog(final String data) {
        if (fileWriter == null) {
            return;
        }
        try {
            fileWriter.write(data);
            fileWriter.newLine();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return;
    }

    public void close() {
        if(fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException ignored) {
            }
        }
    }
}
