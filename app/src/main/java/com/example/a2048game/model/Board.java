package com.example.a2048game.model;

public class Board implements Cloneable {
    public static final int SIZE = 4;
    private Tile[][] tiles;

    public Board() {
        tiles = new Tile[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                tiles[r][c] = new Tile();
            }
        }
    }

    public Tile getTile(int r, int c) {
        return tiles[r][c];
    }

    public void setTile(int r, int c, Tile t) {
        tiles[r][c] = t;
    }

    public int getValue(int r, int c) {
        return tiles[r][c].getValue();
    }

    public void setValue(int r, int c, int value) {
        tiles[r][c].setValue(value);
    }

    public Board clone() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                b.setValue(r, c, this.getValue(r, c));
            }
        }
        return b;
    }

    public boolean isFull() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (getValue(r, c) == 0) return false;
            }
        }
        return true;
    }
}

