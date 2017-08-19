package com.visualthreat.data.unit;

import android.content.Context;
import android.os.Environment;

import com.visualthreat.data.R;

import java.io.File;

/**
 * Created by USER on 1/9/2017.
 */

public class UnitFunction {
    /**
     * create folder save logfile
     * @param context
     */
    public static String CreateFolder(Context context) {
        String nameFoler = context.getString(R.string.app_name);
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + nameFoler);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        return nameFoler;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
