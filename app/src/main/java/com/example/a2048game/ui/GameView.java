package com.example.a2048game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.a2048game.logic.Direction;
import com.example.a2048game.logic.GameManager;
import com.example.a2048game.model.Board;

public class GameView extends View {

    private static final String TAG = "GameView";
    private GameManager gameManager;
    private Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GestureDetector gestureDetector;

    // Drag state
    private boolean dragging = false;
    private int dragValue = 0;
    private int dragFromRow = -1;
    private int dragFromCol = -1;
    private float dragX;
    private float dragY;

    // Grid metrics cached
    private int gridLeft;
    private int gridTop;
    private int cellSize;
    private int gridSize;
    private int padding;
    private int gap;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        bgPaint.setColor(Color.parseColor("#bbada0"));
        cellPaint.setColor(Color.parseColor("#cdc1b4"));
        tilePaint.setColor(Color.LTGRAY);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics()));
        highlightPaint.setColor(Color.parseColor("#80ffffff"));

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 100;
            private static final int SWIPE_THRESHOLD_VELOCITY = 100;
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (gameManager == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (Math.abs(dx) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        doMove(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                        return true;
                    }
                } else if (Math.abs(dy) > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    doMove(dy > 0 ? Direction.DOWN : Direction.UP);
                    return true;
                }
                return false;
            }
        });
    }

    private void computeGridMetrics() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            gridLeft = gridTop = 0;
            cellSize = 0;
            gridSize = 0;
            gap = 0;
            return; // tamaño no listo
        }
        int size = Math.min(w, h);
        padding = size / 40;
        int available = size - padding * 2;
        if (available <= 0) available = size; // fallback
        gap = Math.max(8, available / 80);
        cellSize = (available - gap * (Board.SIZE + 1)) / Board.SIZE;
        if (cellSize <= 0) {
            gap = 8;
            cellSize = (available - gap * (Board.SIZE + 1)) / Board.SIZE;
        }
        if (cellSize <= 0) cellSize = size / (Board.SIZE + 2); // último fallback
        gridSize = cellSize * Board.SIZE + gap * (Board.SIZE + 1);
        gridLeft = (w - gridSize) / 2;
        gridTop = (h - gridSize) / 2;
    }

    private void doMove(Direction dir) {
        if (gameManager == null) return;
        boolean moved = false;
        try {
            moved = gameManager.move(dir);
        } catch (Exception e) {
            Log.e(TAG, "Error ejecutando movimiento", e);
        }
        if (moved) invalidate();
    }

    public void setGameManager(GameManager gm) {
        this.gameManager = gm;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
            computeGridMetrics();
            if (cellSize <= 0) {
                Log.w(TAG, "cellSize inválido, omitiendo draw hasta próximo layout");
                return;
            }
            RectF outer = new RectF(gridLeft, gridTop, gridLeft + gridSize, gridTop + gridSize);
            canvas.drawRoundRect(outer, padding, padding, bgPaint);
            if (gameManager == null) return;
            Board b = gameManager.getBoard();
            float textSize = cellSize * 0.42f;
            textPaint.setTextSize(textSize);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textOffset = (fm.descent + fm.ascent) / 2f;
            float corner = cellSize * 0.12f;
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    int x = gridLeft + gap + c * (cellSize + gap);
                    int y = gridTop + gap + r * (cellSize + gap);
                    int value = b.getValue(r, c);
                    RectF cellRect = new RectF(x, y, x + cellSize, y + cellSize);
                    canvas.drawRoundRect(cellRect, corner, corner, cellPaint);
                    if (dragging && r == dragFromRow && c == dragFromCol) {
                        canvas.drawRoundRect(cellRect, corner, corner, highlightPaint);
                        continue;
                    }
                    if (value != 0) {
                        tilePaint.setColor(getColorForValue(value));
                        canvas.drawRoundRect(cellRect, corner, corner, tilePaint);
                        textPaint.setColor(getTextColorForValue(value));
                        canvas.drawText(String.valueOf(value), cellRect.centerX(), cellRect.centerY() - textOffset, textPaint);
                    }
                }
            }
            if (dragging && dragValue != 0) {
                RectF dragRect = new RectF(dragX - cellSize / 2f, dragY - cellSize / 2f, dragX + cellSize / 2f, dragY + cellSize / 2f);
                tilePaint.setColor(getColorForValue(dragValue));
                canvas.drawRoundRect(dragRect, corner, corner, tilePaint);
                textPaint.setColor(getTextColorForValue(dragValue));
                canvas.drawText(String.valueOf(dragValue), dragRect.centerX(), dragRect.centerY() - textOffset, textPaint);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onDraw", e);
        }
    }

    private int getColorForValue(int v) {
        switch (v) {
            case 2: return Color.parseColor("#eee4da");
            case 4: return Color.parseColor("#ede0c8");
            case 8: return Color.parseColor("#f2b179");
            case 16: return Color.parseColor("#f59563");
            case 32: return Color.parseColor("#f67c5f");
            case 64: return Color.parseColor("#f65e3b");
            case 128: return Color.parseColor("#edcf72");
            case 256: return Color.parseColor("#edcc61");
            case 512: return Color.parseColor("#edc850");
            case 1024: return Color.parseColor("#edc53f");
            case 2048: return Color.parseColor("#edc22e");
            default: return Color.parseColor("#d6d6d6");
        }
    }

    private int getTextColorForValue(int v) {
        return (v == 2 || v == 4) ? Color.parseColor("#776e65") : Color.parseColor("#f9f6f2");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameManager == null) return false;
        computeGridMetrics();
        if (cellSize <= 0) return false;
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            if (x >= gridLeft && x <= gridLeft + gridSize && y >= gridTop && y <= gridTop + gridSize) {
                int col = (int) ((x - gridLeft - gap) / (cellSize + gap));
                int row = (int) ((y - gridTop - gap) / (cellSize + gap));
                if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                    int val = gameManager.getBoard().getValue(row, col);
                    if (val != 0) {
                        dragging = true;
                        dragValue = val;
                        dragFromRow = row;
                        dragFromCol = col;
                        dragX = x;
                        dragY = y;
                        invalidate();
                        return true;
                    }
                }
            }
        }
        if (dragging) {
            if (action == MotionEvent.ACTION_MOVE) {
                dragX = x;
                dragY = y;
                invalidate();
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float startCenterX = gridLeft + gap + dragFromCol * (cellSize + gap) + cellSize / 2f;
                float startCenterY = gridTop + gap + dragFromRow * (cellSize + gap) + cellSize / 2f;
                float dx = x - startCenterX;
                float dy = y - startCenterY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);
                float threshold = Math.max(20f, cellSize / 3f);
                if (absDx > threshold || absDy > threshold) {
                    if (absDx > absDy) doMove(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                    else doMove(dy > 0 ? Direction.DOWN : Direction.UP);
                }
                dragging = false;
                dragValue = 0;
                dragFromRow = -1;
                dragFromCol = -1;
                invalidate();
                return true;
            }
        }
        gestureDetector.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
