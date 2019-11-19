package com.nlp.correction;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

import java.util.LinkedList;

/**
 * @author Kevin Crosby.
 */
public interface Corrector extends Converter {
  int TEN = 10;
  int MODULUS = 11;

  int[] generate(long number);

  default int[] convert(String string) {
    return new StringBuilder(string).reverse().chars()
        .map(c -> c == 'X' ? 10 : Character.getNumericValue(c))
        .toArray();
  }

  // checks stored as catenated string of c11, c1, c0
  default boolean validate(long number, String string) {
    return validate(number, convert(string));
  }

  boolean validate(long number, int[] checks);

  // checks stored as catenated string of c11, c1, c0
  default long correct(long number, String string) {
    return correct(number, convert(string));
  }

  long correct(long number, int[] checks);

  default int index(int row, int column) {
    return row * MODULUS + column;
  }

  default int row(int index) {
    return index / MODULUS;
  }

  default int column(int index) {
    return index % MODULUS;
  }

  default Table<Integer, Integer, Integer> matricize(long number) { // TODO output Matrix class
    Table<Integer, Integer, Integer> matrix = TreeBasedTable.create();
    int i = 2; // i.e. skip 0 and 1
    while (number > 0) {
      int digit = (int) (number % TEN);
      number /= TEN;
      matrix.put(row(i), column(i), digit);
      i += i == (MODULUS - 1) ? 2 : 1; // i.e. skip MODULUS
    }
    return matrix;
  }

  default Table<Integer, Integer, Integer> matricize(long number, int[] checks) { // TODO output Matrix class
    Table<Integer, Integer, Integer> matrix = matricize(number);
    int i = 0;
    for (int check : checks) {
      matrix.put(row(i), column(i), check);
      i = i == 0 ? 1 : i * MODULUS;
    }
    return matrix;
  }

  default long linearize(Table<Integer, Integer, Integer> matrix) { // TODO input Matrix class
    LinkedList<Integer> digits = Lists.newLinkedList();
    for (Cell<Integer, Integer, Integer> cell : matrix.cellSet()) {
      Integer r = cell.getRowKey();
      Integer c = cell.getColumnKey();
      Integer digit = cell.getValue();
      if (r != null && c != null && digit != null) {
        int i = index(r, c);
        if (i >= 2 && i != MODULUS) { // i.e. skip 0, 1, and 11
          digits.push(digit);
        }
      }
    }
    long number = 0;
    for (int digit : digits) {
      number = number * TEN + digit;
    }
    return number;
  }

  default int sum(int[] left, Table<Integer, Integer, Integer> matrix, int[] right) { // TODO input Matrix class
    int sum = 0;
    for (Cell<Integer, Integer, Integer> cell : matrix.cellSet()) {
      Integer r = cell.getRowKey();
      Integer c = cell.getColumnKey();
      Integer digit = cell.getValue();
      if (r != null && c != null && digit != null) {
        sum += left[r] * digit * right[c];
      }
    }
    return sum % MODULUS;
  }

  int sum0(Table<Integer, Integer, Integer> matrix); // TODO input Matrix class

  int sum1(Table<Integer, Integer, Integer> matrix); // TODO input Matrix class

  int sumM(Table<Integer, Integer, Integer> matrix); // TODO input Matrix class
}
