package com.example.a2048game;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.a2048game.logic.GameManager;
import com.example.a2048game.storage.ScoreManager;
import com.example.a2048game.ui.GameView;
import com.example.a2048game.R;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GameView gameView;
    private GameManager gameManager;
    private TextView tvScore;
    private TextView tvBest;
    private TextView tvMoves;
    private Button btnUndo;
    private ImageButton btnRestart;
    private ImageButton btnShowLast;
    private ScoreManager scoreManager;

    private static final String KEY_BOARD = "board_flat";
    private static final String KEY_SCORE = "score";
    private static final String KEY_MOVES = "moves";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate iniciado");

        try {
            setContentView(R.layout.activity_main);

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            // No usamos setSupportActionBar para evitar conflictos con el tema
            if (toolbar != null) {
                toolbar.setTitle(R.string.app_title);
            }

            tvScore = findViewById(R.id.tvScore);
            tvBest = findViewById(R.id.tvBest);
            tvMoves = findViewById(R.id.tvMoves);
            btnUndo = findViewById(R.id.btnUndo);
            btnRestart = findViewById(R.id.btnRestart);
            btnShowLast = findViewById(R.id.btnShowLast);
            gameView = findViewById(R.id.gameView);
        } catch (Exception e) {
            Log.e(TAG, "Error inflando layout o encontrando vistas", e);
            new AlertDialog.Builder(this)
                    .setTitle("Error al iniciar")
                    .setMessage("Se produjo un error al inflar la interfaz: " + e.toString())
                    .setPositiveButton("OK", null)
                    .show();
            // Stop further initialization to avoid more crashes
            return;
        }

        // Check for missing views early and avoid NPEs
        boolean anyNull = (tvScore == null || tvBest == null || tvMoves == null || btnUndo == null || btnRestart == null || gameView == null);
        if (anyNull) {
            Log.e(TAG, "Una o varias vistas requeridas son null. Revisa ids en activity_main.xml y score_panel.xml");
            // Mostrar diálogo para que el usuario sepa que hay un problema en recursos
            new AlertDialog.Builder(this)
                    .setTitle("Error de UI")
                    .setMessage("Algunas vistas no se encontraron en el layout. Revisa los recursos.\nLa app seguirá en modo degradado.")
                    .setPositiveButton("OK", null)
                    .show();
        }

        scoreManager = new ScoreManager(this);
        gameManager = new GameManager(new GameManager.OnScoreChangedListener() {
            @Override
            public void onScoreChanged(int newScore) {
                updateScoreViews(newScore);
            }
        }, new GameManager.OnGameOverListener() {
            @Override
            public void onGameOver() {
                MainActivity.this.onGameOver();
            }
        }, scoreManager.getBestScore());
        gameManager.setOnWinListener(new GameManager.OnWinListener() {
            @Override
            public void onWin() {
                MainActivity.this.onWin();
            }
        });
        if (gameView != null) gameView.setGameManager(gameManager);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BOARD)) {
            int[] flat = savedInstanceState.getIntArray(KEY_BOARD);
            int sc = savedInstanceState.getInt(KEY_SCORE, 0);
            int mv = savedInstanceState.getInt(KEY_MOVES, 0);
            gameManager.restoreFromFlattened(flat, sc, mv);
        } else {
            // Intentar restaurar partida persistida
            if (!restorePersistedGame()) {
                gameManager.newGame();
            }
        }
        updateScores();
        if (btnUndo != null) btnUndo.setEnabled(gameManager.canUndo());

        if (btnRestart != null) {
            btnRestart.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    Log.d(TAG, "Click reiniciar");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Reiniciar partida")
                            .setMessage("¿Estás seguro que quieres reiniciar la partida?")
                            .setPositiveButton("Sí", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    gameManager.newGame();
                                    updateScores();
                                    if (btnUndo != null) btnUndo.setEnabled(gameManager.canUndo());
                                    if (gameView != null) gameView.invalidate();
                                    // borrar estado guardado
                                    getSharedPreferences("2048_prefs", MODE_PRIVATE).edit().remove("saved_game").apply();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    Log.d(TAG, "Click undo");
                    if (gameManager.canUndo()) {
                        gameManager.undo();
                        updateScores();
                        if (gameView != null) gameView.invalidate();
                        btnUndo.setEnabled(gameManager.canUndo());
                    }
                }
            });
        }

        if (btnShowLast != null) {
            btnShowLast.setOnClickListener(v -> {
                try {
                    String json = getSharedPreferences("2048_prefs", MODE_PRIVATE).getString("saved_game", null);
                    if (json == null) {
                        new AlertDialog.Builder(MainActivity.this).setMessage("No hay última partida guardada").setPositiveButton("OK", null).show();
                        return;
                    }
                    JSONObject obj = new JSONObject(json);
                    int score = obj.optInt("score", 0);
                    int moves = obj.optInt("moves", 0);
                    JSONArray arr = obj.optJSONArray("board");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Score: ").append(score).append("\n");
                    sb.append("Moves: ").append(moves).append("\n\n");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            sb.append(arr.optInt(i, 0));
                            if ((i + 1) % 4 == 0) sb.append("\n"); else sb.append(" ");
                        }
                    }
                    new AlertDialog.Builder(MainActivity.this).setTitle("Última partida").setMessage(sb.toString()).setPositiveButton("OK", null).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando última partida", e);
                    new AlertDialog.Builder(MainActivity.this).setMessage("Error leyendo última partida").setPositiveButton("OK", null).show();
                }
            });
        }

        // Ya no mostramos el último crash automáticamente para evitar confusión.
    }

    private void updateScoreViews(int score, boolean animated) {
        if (tvScore != null) tvScore.setText(String.valueOf(score));
        if (tvMoves != null) tvMoves.setText(String.valueOf(gameManager.getMoves()));
        if (tvBest != null) tvBest.setText(String.valueOf(gameManager.getBestScore()));
        // animated reservado por si quieres animaciones del marcador
    }

    // Compatibilidad: método original usado por GameManager callbacks
    private void updateScoreViews(int score) {
        updateScoreViews(score, false);
    }

    private void updateScores() {
        if (tvScore != null) tvScore.setText(String.valueOf(gameManager.getScore()));
        if (tvBest != null) tvBest.setText(String.valueOf(gameManager.getBestScore()));
        if (tvMoves != null) tvMoves.setText(String.valueOf(gameManager.getMoves()));
        if (btnUndo != null) btnUndo.setEnabled(gameManager.canUndo());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putIntArray(KEY_BOARD, gameManager.getBoardFlattened());
            outState.putInt(KEY_SCORE, gameManager.getScore());
            outState.putInt(KEY_MOVES, gameManager.getMoves());
        } catch (Exception e) {
            Log.e(TAG, "Error guardando estado de la partida", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Guardar Best y snapshot de la partida
        try {
            scoreManager.saveBestScore(gameManager.getBestScore());
            persistCurrentGame();
        } catch (Exception e) {
            Log.e(TAG, "Error persistiendo partida", e);
        }
    }

    private void persistCurrentGame() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("score", gameManager.getScore());
            obj.put("moves", gameManager.getMoves());
            obj.put("best", gameManager.getBestScore());
            int[] flat = gameManager.getBoardFlattened();
            JSONArray arr = new JSONArray();
            for (int v : flat) arr.put(v);
            obj.put("board", arr);
            SharedPreferences prefs = getSharedPreferences("2048_prefs", MODE_PRIVATE);
            prefs.edit().putString("saved_game", obj.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "persistCurrentGame error", e);
        }
    }

    private boolean restorePersistedGame() {
        try {
            SharedPreferences prefs = getSharedPreferences("2048_prefs", MODE_PRIVATE);
            String json = prefs.getString("saved_game", null);
            if (json == null) return false;
            JSONObject obj = new JSONObject(json);
            int score = obj.optInt("score", 0);
            int moves = obj.optInt("moves", 0);
            JSONArray arr = obj.optJSONArray("board");
            if (arr == null || arr.length() != 16) return false;
            int[] flat = new int[16];
            for (int i = 0; i < 16; i++) flat[i] = arr.optInt(i, 0);
            gameManager.restoreFromFlattened(flat, score, moves);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "restorePersistedGame error", e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            scoreManager.saveBestScore(gameManager.getBestScore());
        } catch (Exception e) {
            Log.e(TAG, "Error guardando puntaje máximo", e);
        }
    }

    private void onGameOver() {
        new AlertDialog.Builder(this)
                .setTitle("Juego terminado")
                .setMessage("¡Fin del juego! Tu puntaje: " + gameManager.getScore())
                .setPositiveButton("OK", null)
                .show();
    }

    private void onWin() {
        new AlertDialog.Builder(this)
                .setTitle("¡Felicidades!")
                .setMessage("Has alcanzado el 2048. ¡Ganaste!")
                .setPositiveButton("OK", null)
                .show();
    }
}
