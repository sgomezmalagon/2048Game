package com.example.a2048game;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class App2048 extends Application {
    private static final String TAG = "GlobalCrash";
    private static final String CRASH_FILE = "last_crash.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "Uncaught exception en hilo " + t.getName(), e);
            try (FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), CRASH_FILE), false);
                 PrintWriter pw = new PrintWriter(fos)) {
                pw.println("Thread: " + t.getName());
                e.printStackTrace(pw);
                pw.flush();
            } catch (Exception io) {
                Log.e(TAG, "Error guardando stacktrace", io);
            }
        });
    }

    public String readLastCrash() {
        File f = new File(getFilesDir(), CRASH_FILE);
        if (!f.exists()) return null;
        try (FileInputStream fis = new FileInputStream(f)) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[512];
            int n;
            while ((n = fis.read(buf)) > 0) sb.append(new String(buf, 0, n));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
