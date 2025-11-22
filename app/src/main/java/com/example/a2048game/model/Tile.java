package com.example.a2048game.model;

public class Tile {
    private int value;

    public Tile(int value) {
        this.value = value;
    }

    public Tile() {
        this(0);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isEmpty() {
        return value == 0;
    }
}

