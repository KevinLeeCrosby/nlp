package com.nlp.spelling;

import static java.lang.Math.min;

/**
 * Damerau-Levenshtein Edit Distance
 *
 * Adapted from online NLP course by Dan Jurafsky and Christopher Manning.
 *
 * @author Kevin Crosby.
 */
public class DamerauLevenshtein {
  private DamerauLevenshtein() {
  }

  public static int distance(String x, String w) {
    // switch for min sized lattice slices
    if (x.length() > w.length()) {
      return distance(w, x);
    }

    // compute small array cases
    if (x.length() == 0) {
      return w.length();
    }
    if (x.length() == 1) {
      char cn = x.charAt(0);
      for (char cm : w.toCharArray()) {
        if (cm == cn) {
          return w.length() - 1; // one match
        }
      }
      return w.length(); // one subst, other deletes
    }

    // x.length >= w.length > 1
    int m = w.length() + 1; // > n
    int n = x.length() + 1; // > 2

    int[] antipenultimate = new int[n];
    int[] penultimate = new int[n];
    int[] ultimate = new int[n];

    // i = 0: first slice is just inserts
    for (int j = 0; j < n; ++j) {
      penultimate[j] = j;  // j inserts down first column of lattice
    }

    // i = 1: second slice no transpose
    ultimate[0] = 1; // insert i[0]
    char ci = w.charAt(0);
    for (int j = 1; j < n; ++j) {
      int jm1 = j - 1;
      ultimate[j] = min(ci == x.charAt(jm1)
              ? penultimate[jm1] // match
              : 1 + penultimate[jm1], // subst
          1 + min(penultimate[j], // delete
              ultimate[jm1])); // insert
    }

    char cj0 = x.charAt(0);

    // i > 1: transpose after first element
    for (int i = 2; i < m; ++i) {
      char cim1 = ci;
      ci = w.charAt(i - 1);

      // rotate slices
      int[] tmpSlice = antipenultimate;
      antipenultimate = penultimate;
      penultimate = ultimate;
      ultimate = tmpSlice;

      ultimate[0] = i; // i deletes across first row of lattice

      // j = 1: no transpose here
      ultimate[1] = min(ci == cj0
              ? penultimate[0] // match
              : 1 + penultimate[0], // subst
          1 + min(penultimate[1], // delete
              ultimate[0])); // insert

      // j > 1: transpose
      char cj = cj0;
      for (int j = 2; j < n; ++j) {
        int jm1 = j - 1;
        char cjm1 = cj;
        cj = x.charAt(jm1);
        ultimate[j] = min(ci == cj
                ? penultimate[jm1] // match
                : 1 + penultimate[jm1], // subst
            1 + min(penultimate[j], // delete
                ultimate[jm1])); // insert
        if (ci == cjm1 && cj == cim1) {
          ultimate[j] = min(ultimate[j], 1 + antipenultimate[j - 2]);
        }
      }
    }
    return ultimate[ultimate.length - 1];
  }
}
