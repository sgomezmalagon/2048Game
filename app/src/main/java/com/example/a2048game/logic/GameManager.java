package com.example.a2048game.logic;

import com.example.a2048game.model.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager {

    private Board board;
    private int score;
    private int bestScore;
    private int moves; // number of moves used
    private Random random = new Random();
    private OnScoreChangedListener scoreListener;
    private OnGameOverListener gameOverListener;
    private OnWinListener winListener;

    // undo state
    private Board prevBoard = null;
    private int prevScore = 0;
    private int prevMoves = 0;

    // internal tracking for merges
    private int lastMergedValue = 0;

    public interface OnScoreChangedListener {
        void onScoreChanged(int newScore);
    }

    public interface OnGameOverListener {
        void onGameOver();
    }

    public interface OnWinListener {
        void onWin();
    }

    public GameManager(OnScoreChangedListener scoreListener, OnGameOverListener gameOverListener, int bestScore) {
        this.scoreListener = scoreListener;
        this.gameOverListener = gameOverListener;
        this.bestScore = bestScore;
        newGame();
    }

    public void setOnWinListener(OnWinListener l) { this.winListener = l; }

    private Board copyBoard(Board source) {
        if (source == null) return null;
        Board b = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                b.setValue(r, c, source.getValue(r, c));
            }
        }
        return b;
    }

    public void newGame() {
        try {
            board = new Board();
            score = 0;
            moves = 0;
            prevBoard = null;
            prevScore = 0;
            prevMoves = 0;
            lastMergedValue = 0;
            spawnRandomTile();
            spawnRandomTile();
            notifyScore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Board getBoard() {
        return board;
    }

    public int getScore() {
        return score;
    }

    public int getMoves() { return moves; }

    public int getBestScore() {
        return Math.max(bestScore, score);
    }

    private void notifyScore() {
        if (scoreListener != null) scoreListener.onScoreChanged(score);
    }

    private void spawnRandomTile() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.getValue(r, c) == 0) empty.add(new int[]{r, c});
            }
        }
        if (empty.isEmpty()) return;
        int[] pos = empty.get(random.nextInt(empty.size()));
        int val = random.nextDouble() < 0.9 ? 2 : 4;
        board.setValue(pos[0], pos[1], val);
    }

    public boolean move(Direction dir) {
        if (board == null) return false;
        boolean moved = false;
        Board before = copyBoard(board);
        int beforeScore = score;
        int beforeMoves = moves;
        lastMergedValue = 0;
        try {
            switch (dir) {
                case LEFT:
                    for (int r = 0; r < Board.SIZE; r++) {
                        int[] line = new int[Board.SIZE];
                        for (int c = 0; c < Board.SIZE; c++) line[c] = board.getValue(r, c);
                        int[] merged = mergeLine(line);
                        for (int c = 0; c < Board.SIZE; c++) if (board.getValue(r, c) != merged[c]) { board.setValue(r, c, merged[c]); moved = true; }
                    }
                    break;
                case RIGHT:
                    for (int r = 0; r < Board.SIZE; r++) {
                        int[] line = new int[Board.SIZE];
                        for (int c = 0; c < Board.SIZE; c++) line[c] = board.getValue(r, Board.SIZE - 1 - c);
                        int[] merged = mergeLine(line);
                        for (int c = 0; c < Board.SIZE; c++) if (board.getValue(r, Board.SIZE - 1 - c) != merged[c]) { board.setValue(r, Board.SIZE - 1 - c, merged[c]); moved = true; }
                    }
                    break;
                case UP:
                    for (int c = 0; c < Board.SIZE; c++) {
                        int[] line = new int[Board.SIZE];
                        for (int r = 0; r < Board.SIZE; r++) line[r] = board.getValue(r, c);
                        int[] merged = mergeLine(line);
                        for (int r = 0; r < Board.SIZE; r++) if (board.getValue(r, c) != merged[r]) { board.setValue(r, c, merged[r]); moved = true; }
                    }
                    break;
                case DOWN:
                    for (int c = 0; c < Board.SIZE; c++) {
                        int[] line = new int[Board.SIZE];
                        for (int r = 0; r < Board.SIZE; r++) line[r] = board.getValue(Board.SIZE - 1 - r, c);
                        int[] merged = mergeLine(line);
                        for (int r = 0; r < Board.SIZE; r++) if (board.getValue(Board.SIZE - 1 - r, c) != merged[r]) { board.setValue(Board.SIZE - 1 - r, c, merged[r]); moved = true; }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // abort move on error
        }
        if (moved) {
            prevBoard = copyBoard(before);
            prevScore = beforeScore;
            prevMoves = beforeMoves;
            moves++;
            spawnRandomTile();
            notifyScore();
            if (lastMergedValue >= 2048 && winListener != null) winListener.onWin();
            if (isGameOver() && gameOverListener != null) gameOverListener.onGameOver();
        }
        return moved;
    }

    private int[] mergeLine(int[] oldLine) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < oldLine.length; i++) if (oldLine[i] != 0) list.add(oldLine[i]);
        List<Integer> merged = new ArrayList<>();
        int i = 0;
        while (i < list.size()) {
            if (i + 1 < list.size() && list.get(i).equals(list.get(i + 1))) {
                int newVal = list.get(i) * 2;
                merged.add(newVal);
                score += newVal;
                // track largest merged value in this move
                if (newVal > lastMergedValue) lastMergedValue = newVal;
                i += 2;
            } else {
                merged.add(list.get(i));
                i += 1;
            }
        }
        while (merged.size() < Board.SIZE) merged.add(0);
        int[] res = new int[Board.SIZE];
        for (int j = 0; j < Board.SIZE; j++) res[j] = merged.get(j);
        return res;
    }

    public boolean isGameOver() {
        if (!board.isFull()) return false;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                int v = board.getValue(r, c);
                if ((r < Board.SIZE - 1 && board.getValue(r + 1, c) == v) || (c < Board.SIZE - 1 && board.getValue(r, c + 1) == v))
                    return false;
            }
        }
        return true;
    }

    // undo support
    public boolean canUndo() {
        return prevBoard != null;
    }

    public void undo() {
        if (prevBoard != null) {
            board = copyBoard(prevBoard);
            score = prevScore;
            moves = prevMoves;
            prevBoard = null;
            prevScore = 0;
            prevMoves = 0;
            notifyScore();
        }
    }

    // Serialization helpers for save/restore
    public int[] getBoardFlattened() {
        int[] flat = new int[Board.SIZE * Board.SIZE];
        int idx = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                flat[idx++] = board.getValue(r, c);
            }
        }
        return flat;
    }

    public void restoreFromFlattened(int[] flat, int restoredScore, int restoredMoves) {
        if (flat == null || flat.length != Board.SIZE * Board.SIZE) return;
        Board b = new Board();
        int idx = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                b.setValue(r, c, flat[idx++]);
            }
        }
        this.board = b;
        this.score = restoredScore;
        // clear undo
        this.prevBoard = null;
        this.prevScore = 0;
        this.prevMoves = 0;
        this.moves = restoredMoves;
        notifyScore();
    }
}
