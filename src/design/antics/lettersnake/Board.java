package design.antics.lettersnake;

import java.util.Random;

/**
 * Snake grid logic with no Android dependencies, so it can be tested on the
 * JVM. Directions: 0 up, 1 right, 2 down, 3 left.
 */
final class Board {
    static final int NONE = 0, RIGHT = 1, WRONG = 2, CRASH = 3;
    static final int LETTERS = 4;                       // 1 needed + 3 decoys on the board
    private static final int[] DX = {0, 1, 0, -1}, DY = {-1, 0, 1, 0};

    final int cols, rows;
    final int[] sx, sy;
    int len, dir, pending, grow;
    final int[] lx = new int[LETTERS], ly = new int[LETTERS];
    final char[] lc = new char[LETTERS];
    private final Random rng;

    Board(int cols, int rows, Random rng) {
        this.cols = cols; this.rows = rows; this.rng = rng;
        sx = new int[cols * rows]; sy = new int[cols * rows];
        reset();
    }

    void reset() {
        len = 3; dir = pending = 1; grow = 0;
        for (int i = 0; i < len; i++) { sx[i] = cols / 3 - i; sy[i] = rows / 2; }
    }

    /** Queue a turn; a direct reversal into the neck is ignored. */
    void turn(int d) { if (d != ((dir + 2) & 3)) pending = d; }

    boolean occupied(int x, int y) {
        for (int i = 0; i < len; i++) if (sx[i] == x && sy[i] == y) return true;
        for (int k = 0; k < LETTERS; k++) if (lc[k] != 0 && lx[k] == x && ly[k] == y) return true;
        return false;
    }

    /** Advance one cell. Returns NONE, RIGHT (ate the needed letter), WRONG or CRASH. */
    int step(char needed) {
        dir = pending;
        int nx = sx[0] + DX[dir], ny = sy[0] + DY[dir];
        if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) return CRASH;
        int limit = grow > 0 ? len : len - 1;           // the tail moves away unless growing
        for (int i = 0; i < limit; i++) if (sx[i] == nx && sy[i] == ny) return CRASH;
        int newLen = len;
        if (grow > 0) { newLen++; grow--; }
        for (int i = newLen - 1; i > 0; i--) { sx[i] = sx[i - 1]; sy[i] = sy[i - 1]; }
        sx[0] = nx; sy[0] = ny; len = newLen;
        for (int k = 0; k < LETTERS; k++)
            if (lc[k] != 0 && lx[k] == nx && ly[k] == ny) return lc[k] == needed ? RIGHT : WRONG;
        return NONE;
    }

    /** Scatter the needed letter and three distinct decoys on free cells. */
    void spawn(char needed) {
        for (int k = 0; k < LETTERS; k++) lc[k] = 0;
        for (int k = 0; k < LETTERS; k++) {
            char c = needed;
            if (k > 0) {
                boolean dup;
                do {
                    c = (char) ('A' + rng.nextInt(26));
                    dup = false;
                    for (int j = 0; j < k; j++) if (lc[j] == c) dup = true;
                } while (dup);
            }
            int x, y;
            do { x = rng.nextInt(cols); y = rng.nextInt(rows); } while (occupied(x, y));
            lx[k] = x; ly[k] = y; lc[k] = c;
        }
    }
}
