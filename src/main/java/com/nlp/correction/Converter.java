package com.nlp.correction;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Kevin Crosby.
 */
public interface Converter {
  int TEN = 10;

  /**
   * Split number into digits using radix conversion formula.
   *
   * @param number Number to split.
   * @return List of digits comprising number.
   */
  default LinkedList<Integer> split(long number) {
    LinkedList<Integer> digits = Lists.newLinkedList();
    while (number > 0) {
      digits.push((int)(number % TEN));
      number /= TEN;
    }
    return digits;
  }

  /**
   * Join digits into number using Horner's method.
   *
   * @param digits Digits to join.
   * @return Number composed of digits.
   */
  default long join(Collection<Integer> digits) {
    long number = 0;
    for (int digit : digits) {
      number = number * TEN + digit;
    }
    return number;
  }
}
