package com.nlp.normalize;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Normalize duplicate, adjacent, non-numeric unigrams and bigrams in strings and lowercase.
 *
 * @author Kevin Crosby.
 */
public final class Normalizer {
  private Normalizer() {
  }

  private static final Set<String> CARDINALS = ImmutableSet.of(
      "oh", "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
      "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
      "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
      "hundred", "thousand", "million", "billion", "trillion"
  );

  /**
   * Normalize string.
   *
   * @param string String to normalize.
   * @return Normalized string.
   */
  public static String normalize(final String string) {
    String[] split = string.toLowerCase().split("\\s+");
    List<String> tokens = Lists.newArrayList(split[0]);

    boolean append;
    boolean remove;
    int j = 1;
    for (int i = 1; i < split.length; ++i) {
      append = false;
      remove = false;
      String token = split[i];
      if (CARDINALS.contains(token)) {
        append = true; // let cardinals through always
      } else {
        if (!token.equals(tokens.get(j - 1))) {
          append = true; // let non-duplicate adjacent unigrams through, unless overridden by bigrams below
        }
        if (j > 2 && !CARDINALS.contains(tokens.get(j - 1)) && token.equals(tokens.get(j - 2)) && tokens.get(j - 1).equals(tokens.get(j - 3))) {
          append = false; // reject duplicate adjacent non-numeric bigrams
          remove = true;  // ... in fact remove the last unigram
        }
      }
      if (append) {
        tokens.add(j++, token);
      } else if (remove) {
        tokens.remove(--j);
      }
    }
    return Joiner.on(" ").join(tokens);
  }

  public static void main(String[] args) {
    //String utterance = "agent customer customer customer service service customer customer service service service agent agent";
    //String utterance = "five potato five potato tomato two tomato two agent agent agent four four foo bar bar bar foo foo bar forty sixty forty sixty";

    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    String utterance;
    String flag = "xxx";
    do {
      System.out.format("Enter utterance (\"%s\" to stop):\t", flag);
      utterance = scanner.next();
      if (!utterance.equals(flag)) {
        System.out.format("\"%s\"\n   => \"%s\"\n", utterance, normalize(utterance));
      }
    } while (!utterance.equals(flag));
  }
}
