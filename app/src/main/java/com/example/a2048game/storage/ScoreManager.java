package com.example.a2048game.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class ScoreManager {
    private static final String PREFS = "2048_prefs";
    private static final String KEY_BEST = "best";
    private SharedPreferences prefs;

    public ScoreManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getBestScore() {
        return prefs.getInt(KEY_BEST, 0);
    }

    public void saveBestScore(int best) {
        prefs.edit().putInt(KEY_BEST, best).apply();
    }
}

