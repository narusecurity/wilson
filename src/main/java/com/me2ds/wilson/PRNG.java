package com.me2ds.wilson;

import java.util.Random;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class PRNG {
    private static Random rand = new Random();

    public static void setSeed(long seed) {
        rand.setSeed(seed);
    }

    public static int getInt(int ceiling) {
        return rand.nextInt(ceiling);
    }

    public static int getInt(int floor, int ceiling) {
        return rand.nextInt(ceiling - floor) + floor;
    }

    public static boolean TF() {
        return getInt(10) % 2 == 0;
    }
}
