package com.nlp.correction;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Scanner;

/**
 * @author Kevin Crosby.
 */
public class Luhn implements Detector {
  public static final int BASE = 10;

  public static Luhn instance = null;

  private Luhn() {
  }

  public static Luhn getInstance() {
    if (instance == null) {
      synchronized (Luhn.class) {
        if (instance == null) {
          instance = new Luhn();
        }
      }
    }
    return instance;
  }

  @Override
  public int generate(long number) {
    List<Integer> digits = split(number);
    int multiplier = 2;
    int sum = 0;
    for (int digit : Lists.reverse(digits)) {
      int product = multiplier * digit;
      sum += product / BASE + product % BASE;
      multiplier = 3 - multiplier;
    }
    return sum * (BASE - 1) % BASE;
  }

  /**
   * Check for valid number.
   *
   * @param number Number to validate.
   * @return True if valid, false otherwise.
   */
  @Override
  public boolean validate(long number) {
    List<Integer> digits = split(number);
    int multiplier = 1;
    int sum = 0;
    for (int digit : Lists.reverse(digits)) {
      int product = multiplier * digit;
      sum += product / BASE + product % BASE;
      multiplier = 3 - multiplier;
    }
    return sum % BASE == 0;
  }

  public static void main(String[] args) {
    Luhn luhn = Luhn.getInstance();

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
      List<Integer> split = luhn.split(number);
      System.out.format("Split: %s\n", split);
      long joined = luhn.join(split);
      System.out.format("Joined:  %d\n", joined);
      int check = luhn.generate(number);
      System.out.format("Check:   %d\n", check);
      boolean isValid = luhn.validate(number, check);
      System.out.format("Is valid?  %s\n", isValid);
      System.out.println();
    }
  }
}
