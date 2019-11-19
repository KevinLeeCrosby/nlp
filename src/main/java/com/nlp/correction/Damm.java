package com.nlp.correction;

import java.util.List;
import java.util.Scanner;

/**
 * @author Kevin Crosby.
 */
public class Damm implements Detector {
  public static final int BASE = 10;

  private static int[][] D = { // d(j, k)
      {0, 3, 1, 7, 5, 9, 8, 6, 4, 2},
      {7, 0, 9, 2, 1, 5, 4, 8, 6, 3},
      {4, 2, 0, 6, 8, 7, 1, 3, 5, 9},
      {1, 7, 5, 0, 9, 8, 3, 4, 2, 6},
      {6, 1, 2, 3, 0, 4, 5, 9, 7, 8},
      {3, 6, 7, 4, 2, 0, 9, 5, 8, 1},
      {5, 8, 6, 9, 7, 2, 0, 1, 3, 4},
      {8, 9, 4, 5, 3, 6, 2, 0, 1, 7},
      {9, 4, 3, 8, 6, 1, 7, 2, 0, 5},
      {2, 5, 8, 1, 4, 3, 6, 7, 9, 0}
  };

  private static Damm instance = new Damm();

  private Damm() {
  }

  public static Damm getInstance() {
    if (instance == null) {
      synchronized (Damm.class) {
        if (instance == null) {
          instance = new Damm();
        }
      }
    }
    return instance;
  }

  public int inv(int digit) {
    return digit;
  }

  @Override
  public int generate(long number) {
    List<Integer> digits = split(number);

    int c = 0; // interim digit
    for (int digit : digits) {
      c = D[c][digit];
    }
    return c;
  }

  @Override
  public boolean validate(long number) {
    List<Integer> digits = split(number);

    int c = 0; // interim digit
    for (int digit : digits) {
      c = D[c][digit];
    }
    return c == 0;
  }

  public static void main(String[] args) {
    Damm damm = Damm.getInstance();

    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    long number;
    long flag = 0;
    while (true) {
      System.out.format("Enter positive number (\"%d\" to stop):\t", flag);
      number = scanner.nextLong();
      if (number == flag) {
        break;
      }
      System.out.format("Entered: %d\n", number);
      List<Integer> split = damm.split(number);
      System.out.format("Split: %s\n", split);
      long joined = damm.join(split);
      System.out.format("Joined:  %d\n", joined);
      int check = damm.generate(number);
      System.out.format("Check:   %d\n", check);
      boolean isValid = damm.validate(number, check);
      System.out.format("Is valid?  %s\n", isValid);
      System.out.println();
    }
  }
}
