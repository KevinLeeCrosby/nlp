package com.nlp.correction;

import java.util.List;
import java.util.Scanner;

/**
 * @author Kevin Crosby.
 */
public class Mod97 implements Detector {
  public static final int BASE = 97;

  private static Mod97 instance = new Mod97();

  private Mod97() {
  }

  public static Mod97 getInstance() {
    if (instance == null) {
      synchronized (Mod97.class) {
        if (instance == null) {
          instance = new Mod97();
        }
      }
    }
    return instance;
  }

  @Override
  public int generate(long number) {
    List<Integer> digits = split(number);

    int c = 0; // interim digit
    int weight = TEN;
    for (int digit : digits) {
      c = (c + weight * digit) % BASE;
      weight = (weight * TEN) % BASE;
    }
    return c == 0 ? 0 : BASE - c;
  }

  @Override
  public boolean validate(long number, int check) {
    List<Integer> digits = split(number);

    int c = check; // interim digit
    int weight = TEN;
    for (int digit : digits) {
      c = (c + weight * digit) % BASE;
      weight = (weight * TEN) % BASE;
    }
    return c == 0;
  }

  @Override
  public boolean validate(long number) { // TODO determine how to combine validate methods
    List<Integer> digits = split(number);

    int c = 0; // interim digit
    int weight = ONE;
    for (int digit : digits) {
      c = (c + weight * digit) % BASE;
      weight = (weight * TEN) % BASE;
    }
    return c == 0;
  }

  public static void main(String[] args) {
    Mod97 mod97 = Mod97.getInstance();

    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    long number;
    long flag = 0;
    while (true) {
      System.out.format("Enter positive number (\"%d\" to stop):\t", flag);
      number = scanner.nextLong();
      if (number == flag) break;
      System.out.format("Entered: %d\n", number);
      List<Integer> split = mod97.split(number);
      System.out.format("Split: %s\n", split);
      long joined = mod97.join(split);
      System.out.format("Joined:  %d\n", joined);
      int check = mod97.generate(number);
      System.out.format("Check:   %d\n", check);
      boolean isValid = mod97.validate(number, check);
      System.out.format("Is valid?  %s\n", isValid);
      System.out.println();
    }
  }
}
