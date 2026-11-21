package design.antics.lettersnake;

import java.util.Random;

/**
 * Word packs for Letter Snake, ordered roughly by difficulty. Parents toggle
 * them; each round's target word comes from a random enabled pack. Same shape as
 * the problem packs in the maths games (NAMES / EXAMPLES / masks) so the shared
 * ParentsMenu works unchanged.
 */
final class Packs {
    static final String[] NAMES = {
        "Alphabet in order", "Short words (c-a-t)", "Sight words", "Four-letter words", "Five-letter words"};
    static final String[] EXAMPLES = {
        "D-E-F-G-H", "cat, dog, sun", "the, and, you, was", "frog, fish, moon", "apple, house, tiger"};
    static final String[][] WORDS = {
        null,   // pack 0 is generated: a run of consecutive letters
        {"CAT", "DOG", "SUN", "PIG", "HEN", "BUS", "CUP", "MAP", "RED", "TOP", "PEN", "BED",
         "SIT", "HAT", "RUN", "FOX", "HOP", "NET", "JAM", "MUD", "BAT", "LOG", "WEB", "ZIP"},
        {"THE", "AND", "YOU", "WAS", "ARE", "FOR", "NOT", "BUT", "ALL", "CAN", "HER", "HAD",
         "ONE", "OUR", "OUT", "DAY", "GET", "HAS", "HIM", "HIS", "HOW", "SEE", "TWO", "WHO"},
        {"FROG", "FISH", "JUMP", "TREE", "PLAY", "BOOK", "MOON", "RAIN", "STAR", "SHIP", "DUCK",
         "CAKE", "BIRD", "MILK", "BLUE", "SNOW", "KITE", "BEAR", "LION", "SAND"},
        {"APPLE", "GREEN", "HOUSE", "WATER", "HAPPY", "PLANT", "SMILE", "TIGER", "HORSE", "RIVER",
         "CLOUD", "BREAD", "LEMON", "SNAKE", "TRAIN", "MOUSE"}};
    static final int COUNT = NAMES.length;
    static final int ALL = (1 << COUNT) - 1;
    static final int DEFAULT_MASK = 0b00011;       // alphabet + short words

    static String word(int mask, Random r) {
        if ((mask & ALL) == 0) mask = DEFAULT_MASK;
        int p;
        do { p = r.nextInt(COUNT); } while ((mask & (1 << p)) == 0);
        if (p == 0) {                              // five consecutive letters, A..Z
            int start = r.nextInt(22);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append((char) ('A' + start + i));
            return sb.toString();
        }
        return WORDS[p][r.nextInt(WORDS[p].length)];
    }

    private Packs() {}
}
