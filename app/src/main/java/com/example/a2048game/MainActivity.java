package com.example.a2048game;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.a2048game.logic.GameManager;
import com.example.a2048game.storage.ScoreManager;
import com.example.a2048game.ui.GameView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GameView gameView;
    private GameManager gameManager;
    private TextView tvScore;
    private TextView tvBest;
    private TextView tvMoves;
    private Button btnUndo;
    private Button btnRestart;
    private ScoreManager scoreManager;

    private static final String KEY_BOARD = "board_flat";
    private static final String KEY_SCORE = "score";
    private static final String KEY_MOVES = "moves";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate iniciado");
        Toast.makeText(this, "MainActivity onCreate", Toast.LENGTH_SHORT).show();

        tvScore = findViewById(R.id.tvScore);
        tvBest = findViewById(R.id.tvBest);
        tvMoves = findViewById(R.id.tvMoves);
        btnUndo = findViewById(R.id.btnUndo);
        btnRestart = findViewById(R.id.btnRestart);
        gameView = findViewById(R.id.gameView);

        if (tvScore == null || tvBest == null || tvMoves == null || btnUndo == null || btnRestart == null || gameView == null) {
            Log.e(TAG, "Una vista requerida es null. Revisa ids en activity_main.xml");
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
        gameView.setGameManager(gameManager);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BOARD)) {
            int[] flat = savedInstanceState.getIntArray(KEY_BOARD);
            int sc = savedInstanceState.getInt(KEY_SCORE, 0);
            int mv = savedInstanceState.getInt(KEY_MOVES, 0);
            gameManager.restoreFromFlattened(flat, sc, mv);
        } else {
            gameManager.newGame();
        }
        updateScores();
        btnUndo.setEnabled(gameManager.canUndo());

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
                                btnUndo.setEnabled(gameManager.canUndo());
                                gameView.invalidate();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        btnUndo.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                Log.d(TAG, "Click undo");
                if (gameManager.canUndo()) {
                    gameManager.undo();
                    updateScores();
                    gameView.invalidate();
                    btnUndo.setEnabled(gameManager.canUndo());
                }
            }
        });
    }

    private void updateScoreViews(int score) {
        if (tvScore == null) return;
        tvScore.setText(String.valueOf(score));
        int best = Math.max(scoreManager.getBestScore(), score);
        if (tvBest != null) tvBest.setText(String.valueOf(best));
        if (tvMoves != null) tvMoves.setText(String.valueOf(gameManager.getMoves()));
    }

    private void onGameOver() {
        Log.d(TAG, "Game Over");
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("No hay más movimientos disponibles")
                .setPositiveButton("Reiniciar", (d, w) -> {
                    gameManager.newGame();
                    updateScores();
                    btnUndo.setEnabled(gameManager.canUndo());
                    gameView.invalidate();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void onWin() {
        Log.d(TAG, "Victoria alcanzada");
        new AlertDialog.Builder(this)
                .setTitle("¡Has ganado!")
                .setMessage("Has conseguido 2048. ¿Quieres seguir jugando?")
                .setPositiveButton("Seguir", (d, w) -> {
                    // continuar sin reiniciar
                })
                .setNegativeButton("Reiniciar", (d, w) -> {
                    gameManager.newGame();
                    updateScores();
                    btnUndo.setEnabled(gameManager.canUndo());
                    gameView.invalidate();
                })
                .show();
    }

    private void updateScores() {
        if (tvScore != null) tvScore.setText(String.valueOf(gameManager.getScore()));
        int best = Math.max(scoreManager.getBestScore(), gameManager.getScore());
        if (tvBest != null) tvBest.setText(String.valueOf(best));
        if (tvMoves != null) tvMoves.setText(String.valueOf(gameManager.getMoves()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putIntArray(KEY_BOARD, gameManager.getBoardFlattened());
            outState.putInt(KEY_SCORE, gameManager.getScore());
            outState.putInt(KEY_MOVES, gameManager.getMoves());
            Log.d(TAG, "Estado guardado");
        } catch (Exception e) {
            Log.e(TAG, "Error guardando estado", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scoreManager.saveBestScore(gameManager.getBestScore());
        Log.d(TAG, "Best score guardado: " + gameManager.getBestScore());
    }
}
