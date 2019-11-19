package com.nlp.correction;

import com.google.common.collect.Table;

import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.IntStream;

import static java.lang.Math.floorMod;
import static java.lang.Math.max;

/**
 * @author Kevin Crosby.
 */
public class Hamming implements Corrector {
  private static int[][] D = d(); // d(j, k)

  private static int[] INV = addInv(MODULUS); // inv(j)

  private static int[] ONES = ones(MODULUS);
  private static int[] WEIGHTS = weights(MODULUS);

  private static int[][] d() {
    int[] multInv = new int[MODULUS];
    for (int i = 1; i < MODULUS; ++i) {
      multInv[i] = multInv(i, MODULUS);
    }
    int[][] table = new int[MODULUS][MODULUS];
    for (int r = 1; r < MODULUS; ++r) {
      int i = multInv[r];
      for (int c = r; c < MODULUS; ++c) {
        int j = multInv[c];
        int k = i * j % MODULUS;
        table[r][j] = k;
        table[c][i] = k;
      }
    }
    return table;
  }

  private static int[] addInv(int base) {
    int[] addInv = new int[base];
    for (int i = 1; i < base; ++i) {
      addInv[i] = base - i;
    }
    return addInv;
  }

  private static int[] ones(int base) {
    return IntStream.generate(() -> 1)
        .limit(base)
        .toArray();
  }

  private static int[] weights(int base) {
    return IntStream.range(0, base)
        .toArray();
  }

  // modular inverse for modulus, returns 0 if undefined
  private static int multInv(int a, int b) {
    int[] v = new int[]{b, a, 0, 1}; // r, newr, t, newt
    int quotient;

    while (v[1] != 0) {
      quotient = v[0] / v[1];
      System.arraycopy(new int[]{v[1], v[0] - quotient * v[1], v[3], v[2] - quotient * v[3]}, 0, v, 0, v.length);
    }
    if (v[0] > 1) {
      return 0; // not invertible
    }
    return floorMod(v[2], b);
  }

  private static Hamming instance = new Hamming();

  private Hamming() {
  }

  public static Hamming getInstance() {
    if (instance == null) {
      synchronized (Hamming.class) {
        if (instance == null) {
          instance = new Hamming();
        }
      }
    }
    return instance;
  }

  public int inv(int digit) {
    return INV[digit];
  }

  @Override
  public int sum0(Table<Integer, Integer, Integer> matrix) { // TODO input Matrix class
    return sum(ONES, matrix, ONES);
  }

  @Override
  public int sum1(Table<Integer, Integer, Integer> matrix) { // TODO input Matrix class
    return sum(ONES, matrix, WEIGHTS);
  }

  @Override
  public int sumM(Table<Integer, Integer, Integer> matrix) { // TODO input Matrix class
    return sum(WEIGHTS, matrix, ONES);
  }

  @Override
  public int[] generate(long number) {
    Table<Integer, Integer, Integer> matrix = matricize(number);
    int c1 = inv(sum1(matrix));
    matrix.put(row(1), column(1), c1);
    int cM = inv(sumM(matrix));
    matrix.put(row(MODULUS), column(MODULUS), cM);
    int c0 = inv(sum0(matrix));
    matrix.put(row(0), column(0), c0);
    return new int[]{c0, c1, cM};
  }

  @Override
  public boolean validate(long number, int[] checks) {
    Table<Integer, Integer, Integer> matrix = matricize(number, checks);
    int error = sum0(matrix);
    int c1 = sum1(matrix);
    int cM = sumM(matrix);
    if (error == 0 && c1 == 0 && cM == 0) {
      return true;
    }
    if (error == 0) {
      return false; // a double error
    }
    int index = D[error][cM] * MODULUS + D[error][c1];
    if (index > max(MODULUS, matrix.size())) { // TODO replace with Matrix.size(), which is max(MODULUS, array.length) - 3
      System.out.println("doCheck: position: " + index + ", error: " + error + ", check1: " + c1 + ", checkM: " + cM);
      return false;
    }
    return false;
  }

  @Override
  public long correct(long number, int[] checks) {
    Table<Integer, Integer, Integer> matrix = matricize(number, checks);
    int error = sum0(matrix);
    int c1 = sum1(matrix);
    int cM = sumM(matrix);
    if (error == 0 && c1 == 0 && cM == 0) {
      return number;
    }
    if (error == 0) {
      return -1; // a double error
    }
    int index = D[error][cM] * MODULUS + D[error][c1];
    if (index > max(MODULUS, matrix.size())) {
      System.out.println("doCheck: position: " + index + ", error: " + error + ", check1: " + c1 + ", checkM: " + cM);
      return -1;
    }
    int row = row(index);
    int column = column(index);
    int typo = matrix.contains(row, column) ? matrix.get(row, column) : 0;
    int correction = (typo - error + MODULUS) % MODULUS;
    matrix.put(row, column, correction);

    System.out.format("Position %d corrected from %d to %d\n", index, typo, correction);
    return linearize(matrix);
  }

  public static void main(String[] args) {
    Hamming hamming = Hamming.getInstance();

    // single error check
    //Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    //long number;
    //long flag = 0;
    //while (true) {
    //  System.out.format("Enter positive number (\"%d\" to stop):\t", flag);
    //  number = scanner.nextLong();
    //  if (number == flag) {
    //    break;
    //  }
    //  System.out.format("Entered: %d\n", number);
    //  List<Integer> split = hamming.split(number);
    //  //System.out.format("Split: %s\n", split);
    //  int length = split.size();
    //  int[] checks = hamming.generate(number);
    //  System.out.format("Checks:  %s\n", Arrays.toString(checks));
    //  {
    //    boolean isValid = hamming.validate(number, checks);
    //    System.out.format("Is valid?  %s\n", isValid);
    //    long correction = hamming.correct(number, checks);
    //    System.out.format("Correction: %s\n", correction);
    //    System.out.println();
    //  }
    //  for (int i = 0; i < length; ++i) {
    //    for (int d = 1; d < TEN; ++d) {
    //      List<Integer> digits = Lists.newArrayList(split);
    //      digits.set(i, (digits.get(i) + d) % TEN);
    //      long typo = hamming.join(digits);
    //      System.out.format("Typo:   %d\n", typo);
    //      boolean isValid = hamming.validate(typo, checks);
    //      System.out.format("Is valid?  %s\n", isValid);
    //      long correction = hamming.correct(typo, checks);
    //      System.out.format("Correction: %s\n", correction);
    //      assert correction == number : String.format("%d ≠ %d!", number, correction);
    //      System.out.println();
    //    }
    //  }
    //  System.out.println();
    //  System.out.println();
    //}

    // double error check
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
      int[] checks = hamming.generate(number);
      System.out.format("Checks:  %s\n", Arrays.toString(checks));
      {
        boolean isValid = hamming.validate(number, checks);
        System.out.format("Is valid?  %s\n", isValid);
        long correction = hamming.correct(number, checks);
        System.out.format("Correction: %s\n", correction);
        System.out.println();
      }
      System.out.print("Repeat positive number (with typo):\t");
      long typo = scanner.nextLong();
      System.out.format("Typo:   %d\n", typo);
      boolean isValid = hamming.validate(typo, checks);
      System.out.format("Is valid?  %s\n", isValid);
      long correction = hamming.correct(typo, checks);
      System.out.format("Correction: %s\n", correction);
      //assert correction == number : String.format("%d ≠ %d!", number, correction);
      System.out.println();
      System.out.println();
      System.out.println();
    }
  }
}
