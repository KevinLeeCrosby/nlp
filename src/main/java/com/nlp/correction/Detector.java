package com.nlp.correction;

/**
 * @author Kevin Crosby.
 */
public interface Detector extends Converter {
  int ONE = 1;

  int generate(long number);

  default boolean validate(long number, int check) {
    return validate(number * TEN + check);
  }

  boolean validate(long number);
}
