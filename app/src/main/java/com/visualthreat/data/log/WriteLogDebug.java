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
 * Created by USER on 2/4/2017.
 */

public class WriteLogDebug {
    private static File file;
    private static final String TAG = "WriteLogDebug";
    public static WriteLogDebug writeLogDebug;
    /**
     * init file log
     */
    public WriteLogDebug(Context context) {
        if(writeLogDebug == null) {
            String nameFoler = UnitFunction.CreateFolder(context);
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss");
            String date = format.format(new Date());
            file = new File(Environment.getExternalStorageDirectory() + "/" + nameFoler + "/" + date + "_debug.txt");
            Log.d(TAG, "path log: " + file.getPath());
            if (!file.exists()) {
                try {
                    Log.d("File created ", "File created ");
                    file.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            writeLogDebug = this;
        }
    }

    public static WriteLogDebug GetLogDebug() {
        return writeLogDebug;
    }

    /**
     * add text to file
     * @param data data
     * @return string data
     */
    public void addLog(String data) {
        if (file == null)
            return;
        String text = data;
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ;
    }

    public void insertLog(String data) {
        if (file == null)
            return;
        try {
            FileWriter writer = new FileWriter(file,true);
            writer.append(data);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
