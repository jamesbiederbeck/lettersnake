package design.antics.lettersnake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Swipe to steer; eat the letters of the target word in order. A wrong letter
 * or a crash costs one of three lives. Movement is on a gentle fixed tick.
 */
public class GameView extends View {

    private static final int COLS = 11;
    private enum State { READY, PLAYING, PAUSED, OVER }

    private final Paint paint = new Paint();
    private final RectF rect = new RectF();
    private final SharedPreferences prefs;
    private final Random rng = new Random();
    private final Theme.Pops pops = new Theme.Pops();
    private boolean menuGesture;
    private final Theme.ThreeTap threeTap = new Theme.ThreeTap();
    private final RectF menuBtn = new RectF();

    private State state = State.READY;
    private Board board;
    private float w, h, unit, cell, x0, y0;
    private String word = "";
    private int pos, score, best, lives, wordsDone, packs;
    private String feedback = "";
    private float stepTime, stepAcc;
    private float downX, downY;
    private long lastNs;
    private boolean running;
    Runnable onMenu;

    public GameView(Context c) {
        super(c);
        Theme.init(paint);
        prefs = c.getSharedPreferences("game", Context.MODE_PRIVATE);
        best = prefs.getInt("best", 0);
        packs = prefs.getInt("packs", Packs.DEFAULT_MASK);
        setKeepScreenOn(true);
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw; h = nh; unit = w / 100f;
        menuBtn.set(0, 0, Theme.MENU_W * unit, Theme.MENU_H * unit);
        cell = w / COLS;
        x0 = 0; y0 = 32 * unit;
        int rows = (int) ((h - y0 - 3 * unit) / cell);
        board = new Board(COLS, rows, rng);
        newGame();
    }

    private void newGame() {
        score = 0; lives = 3; wordsDone = 0; feedback = "";
        newWord();
        board.reset();
        board.spawn(needed());
        state = State.READY;
    }

    private void newWord() { word = Packs.word(packs, rng); pos = 0; }
    private char needed() { return word.charAt(pos); }
    private float tickTime() { return Math.max(0.2f, 0.32f - 0.01f * wordsDone); }

    // ---- loop ----------------------------------------------------------

    void resume() { if (running) return; running = true; lastNs = 0; postOnAnimation(tick); }
    void pause() { running = false; if (state == State.PLAYING) state = State.PAUSED; save(); }
    private void save() { if (score > best) { best = score; prefs.edit().putInt("best", best).apply(); } }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            if (lastNs != 0) {
                float dt = Math.min((now - lastNs) / 1e9f, 0.05f);
                pops.update(dt);
                if (state == State.PLAYING) {
                    stepAcc += dt;
                    float t = tickTime();
                    while (stepAcc >= t) { stepAcc -= t; move(); }
                }
            }
            lastNs = now;
            invalidate();
            postOnAnimation(this);
        }
    };

    private float cx(int gx) { return x0 + gx * cell; }
    private float cy(int gy) { return y0 + gy * cell; }

    private void move() {
        int r = board.step(needed());
        if (r == Board.CRASH) { pops.burst(cx(board.sx[0]) + cell / 2, cy(board.sy[0]) + cell / 2, Theme.CORAL, 50 * unit, 12); lose("Bonk!"); }
        else if (r == Board.WRONG) {
            feedback = "Not that one";
            pops.burst(cx(board.sx[0]) + cell / 2, cy(board.sy[0]) + cell / 2, Theme.CORAL, 50 * unit, 12);
            board.spawn(needed());
            lose("Not that one");
        } else if (r == Board.RIGHT) {
            pops.burst(cx(board.sx[0]) + cell / 2, cy(board.sy[0]) + cell / 2, Theme.MINT, 50 * unit, 10);
            board.grow++;
            score += 5;
            if (++pos == word.length()) {
                score += 10 * word.length();
                wordsDone++;
                feedback = word + "!";
                save();
                newWord();
            }
            board.spawn(needed());
        }
    }

    private void lose(String why) {
        feedback = why;
        if (--lives <= 0) { state = State.OVER; save(); return; }
        board.reset();
        board.spawn(needed());
        state = State.READY;
    }

    // ---- parents menu hooks --------------------------------------------

    int packs() { return packs; }
    void setPacks(int m) { packs = m; prefs.edit().putInt("packs", m).apply(); }
    void resetBest() { best = 0; prefs.edit().putInt("best", 0).apply(); }
    void menuOpened() { if (state == State.PLAYING) state = State.PAUSED; }

    // ---- input ---------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (threeTap.swallow(e, () -> { if (state == State.PLAYING) state = State.PAUSED; })) return true;   // double three-finger tap pauses
        if (menuGesture) {                       // the tap that opened the menu must not also act on the game
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) menuGesture = false;
            return true;
        }
        if (a == MotionEvent.ACTION_DOWN) {
            if (menuBtn.contains(e.getX(), e.getY())) { menuGesture = true;
            if (onMenu != null) onMenu.run(); return true; }
            downX = e.getX(); downY = e.getY();
        } else if (a == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - downX, dy = e.getY() - downY;
            if (Math.max(Math.abs(dx), Math.abs(dy)) > 4 * unit) {
                if (state == State.READY) { state = State.PLAYING; stepAcc = 0; }
                if (state == State.PLAYING)
                    board.turn(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : 3) : (dy > 0 ? 2 : 0));
                downX = e.getX(); downY = e.getY();        // allow the next swipe to chain
            }
        } else if (a == MotionEvent.ACTION_UP) {
            if (state == State.READY || state == State.PAUSED) { state = State.PLAYING; stepAcc = 0; }
            else if (state == State.OVER) newGame();
        }
        return true;
    }

    // ---- drawing -------------------------------------------------------

    @Override
    protected void onDraw(Canvas cv) {
        cv.drawColor(Theme.NAVY);
        Theme.hud(cv, paint, w, unit, "Score " + score, "Lives " + Math.max(lives, 0), "Best " + Math.max(best, score));

        // target word as tiles: eaten = mint, next = sun, rest = surface
        float tw = Math.min(10 * unit, (w - 8 * unit) / word.length());
        float left = (w - tw * word.length()) / 2;
        for (int i = 0; i < word.length(); i++) {
            rect.set(left + i * tw + 0.6f * unit, 16 * unit, left + (i + 1) * tw - 0.6f * unit, 16 * unit + tw - 1.2f * unit);
            int color = i < pos ? Theme.MINT : i == pos ? Theme.SUN : Theme.SURFACE;
            Theme.tile(cv, paint, rect, color);
            Theme.text(cv, paint, String.valueOf(word.charAt(i)), rect.centerX(), rect.centerY(), tw * 0.6f,
                       i < pos || i == pos ? Theme.NAVY : Theme.CREAM);
        }

        // board backdrop
        rect.set(x0 + 0.5f * unit, y0, x0 + COLS * cell - 0.5f * unit, y0 + board.rows * cell);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Theme.alpha(Theme.SURFACE, 0.45f));
        cv.drawRoundRect(rect, 2 * unit, 2 * unit, paint);

        // letters on the board
        for (int k = 0; k < Board.LETTERS; k++) {
            if (board.lc[k] == 0) continue;
            float inset = cell * 0.06f;
            rect.set(cx(board.lx[k]) + inset, cy(board.ly[k]) + inset, cx(board.lx[k]) + cell - inset, cy(board.ly[k]) + cell - inset);
            Theme.tile(cv, paint, rect, Theme.TILES[(board.lc[k] - 'A') % 3]);
            Theme.text(cv, paint, String.valueOf(board.lc[k]), rect.centerX(), rect.centerY(), cell * 0.62f, Theme.NAVY);
        }

        // snake, tail first so the head is on top
        for (int i = board.len - 1; i >= 0; i--) {
            float inset = cell * (i == 0 ? 0.04f : 0.1f);
            rect.set(cx(board.sx[i]) + inset, cy(board.sy[i]) + inset, cx(board.sx[i]) + cell - inset, cy(board.sy[i]) + cell - inset);
            Theme.tile(cv, paint, rect, i % 2 == 0 ? Theme.MINT : Theme.mix(Theme.MINT, Theme.NAVY, 0.18f));
        }
        float hx = cx(board.sx[0]) + cell / 2, hy = cy(board.sy[0]) + cell / 2;
        float ex = cell * 0.18f, er = cell * 0.07f;
        int d = board.dir;
        float fx = d == 1 ? ex : d == 3 ? -ex : 0, fy = d == 2 ? ex : d == 0 ? -ex : 0;
        float sxo = d == 0 || d == 2 ? ex : 0, syo = d == 1 || d == 3 ? ex : 0;
        paint.setColor(Theme.NAVY);
        cv.drawCircle(hx + fx + sxo * 0.8f, hy + fy + syo * 0.8f, er, paint);
        cv.drawCircle(hx + fx - sxo * 0.8f, hy + fy - syo * 0.8f, er, paint);

        pops.draw(cv, paint, unit * 1.1f);
        Theme.text(cv, paint, feedback, w / 2, 12.6f * unit, 3.4f * unit, Theme.SUN);

        String msg = null;
        if (state == State.READY)  msg = "Swipe to start";
        if (state == State.PAUSED) msg = "Paused • tap to resume";
        if (state == State.OVER)   msg = "Game over • tap to play again";
        if (msg != null) {
            rect.set(w / 2 - 38 * unit, h * 0.5f - 5 * unit, w / 2 + 38 * unit, h * 0.5f + 5 * unit);
            Theme.tile(cv, paint, rect, Theme.SURFACE, false);
            Theme.text(cv, paint, msg, w / 2, h * 0.5f, 4.2f * unit, Theme.WHITE);
        }
    }
}
