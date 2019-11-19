package com.nlp.correction;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Scanner;

/**
 * @author Kevin Crosby.
 */
public class Verhoeff implements Detector {
  public static final int BASE = 10;
  public static final int EIGHT = 8;

  private static int[][] D = { // d(j, k)
      {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
      {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
      {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
      {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
      {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
      {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
      {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
      {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
      {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
      {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
  };
  private static int[] INV = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9}; // inv(j)
  private static int[][] P = p(); // p(pos, num)

  /**
   * "a single permutation (1 5 8 9 4 2 7 0)(3 6) applied iteratively; i.e. p(i+j,n) = p(i, p(j,n))"
   */
  private static int[][] p() {
    int[][] p = new int[EIGHT][];
    p[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // identity
    p[1] = new int[]{1, 5, 7, 6, 2, 8, 3, 0, 9, 4}; // "magic"
    for (int i = 2; i < EIGHT; ++i) {
      p[i] = new int[BASE];
      for (int j = 0; j < BASE; ++j) {
        p[i][j] = p[i - 1][p[1][j]];
      }
    }
    return p;
  }

  private static Verhoeff instance = new Verhoeff();

  private Verhoeff() {
  }

  public static Verhoeff getInstance() {
    if (instance == null) {
      synchronized (Verhoeff.class) {
        if (instance == null) {
          instance = new Verhoeff();
        }
      }
    }
    return instance;
  }

  public int inv(int digit) {
    return INV[digit];
  }

  @Override
  public int generate(long number) {
    List<Integer> digits = split(number);

    int c = 0;
    int i = 0;
    for (int digit : Lists.reverse(digits)) {
      c = D[c][P[++i % EIGHT][digit]];
    }
    return inv(c);
  }

  @Override
  public boolean validate(long number) {
    List<Integer> digits = split(number);

    int c = 0;
    int i = 0;
    for (int digit : Lists.reverse(digits)) {
      c = D[c][P[i++ % EIGHT][digit]];
    }
    return c == 0;
  }

  public static void main(String[] args) {
    Verhoeff verhoeff = Verhoeff.getInstance();

    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    long number;
    long flag = 0;
    while (true) {
      System.out.format("Enter positive number (\"%d\" to stop):\t", flag);
      number = scanner.nextLong();
      if (number == flag) break;
      System.out.format("Entered: %d\n", number);
      List<Integer> split = verhoeff.split(number);
      System.out.format("Split: %s\n", split);
      long joined = verhoeff.join(split);
      System.out.format("Joined:  %d\n", joined);
      int check = verhoeff.generate(number);
      System.out.format("Check:   %d\n", check);
      boolean isValid = verhoeff.validate(number, check);
      System.out.format("Is valid?  %s\n", isValid);
      System.out.println();
    }
  }
}
