package com.nlp.normalize;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper tool to convert decimal, cardinal, and ordinal numbers into words.
 *
 * @author Kevin Crosby.
 */
public final class Numerics {
  private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
  private static final Pattern IS_DECIMAL = Pattern.compile("^(\\$)?(-?\\d+)\\.(\\d+)$");
  private static final Pattern IS_CARDINAL = Pattern.compile("^(\\$)?(-?\\d+)$");
  private static final Pattern IS_ORDINAL = Pattern.compile("^(-?\\d+(?:st|nd|rd|th))$");
  private static final Splitter SPLITTER = Splitter.onPattern("\\s+").trimResults().omitEmptyStrings();
  private static final Joiner JOINER = Joiner.on(" ");

  private static final Set<Long> PRETEENS = ImmutableSet.of(11L, 12L, 13L);
  private static final long BILLION = 1_000_000_000;

  private static final BiMap<Long, String> CARDINALS = ImmutableBiMap.<Long, String>builder()
      .put(0L, "zero").put(1L, "one").put(2L, "two").put(3L, "three").put(4L, "four").put(5L, "five")
      .put(6L, "six").put(7L, "seven").put(8L, "eight").put(9L, "nine").put(10L, "ten")
      .put(11L, "eleven").put(12L, "twelve").put(13L, "thirteen").put(14L, "fourteen").put(15L, "fifteen")
      .put(16L, "sixteen").put(17L, "seventeen").put(18L, "eighteen").put(19L, "nineteen").put(20L, "twenty")
      .put(30L, "thirty").put(40L, "forty").put(50L, "fifty")
      .put(60L, "sixty").put(70L, "seventy").put(80L, "eighty").put(90L, "ninety").put(100L, "hundred")
      .put(1_000L, "thousand").put(1_000_000L, "million")
      .build();

  private static final BiMap<String, String> ORDINALS = ImmutableBiMap.<String, String>builder()
      .put("0th", "zeroth").put("1st", "first").put("2nd", "second").put("3rd", "third").put("4th", "fourth").put("5th", "fifth")
      .put("6th", "sixth").put("7th", "seventh").put("8th", "eighth").put("9th", "ninth").put("10th", "tenth")
      .put("11th", "eleventh").put("12th", "twelfth").put("13th", "thirteenth").put("14th", "fourteenth").put("15th", "fifteenth")
      .put("16th", "sixteenth").put("17th", "seventeenth").put("18th", "eighteenth").put("19th", "nineteenth").put("20th", "twentieth")
      .put("30th", "thirtieth").put("40th", "fortieth").put("50th", "fiftieth")
      .put("60th", "sixtieth").put("70th", "seventieth").put("80th", "eightieth").put("90th", "ninetieth").put("100th", "hundredth")
      .put("1000th", "thousandth").put("1000000th", "millionth")
      .build();

  private Numerics() {
  }

  private static void toWords(long i, List<String> words) {
    if (i < 0) {
      words.add("negative");
      toWords(-i, words);
    } else if (i <= 20) {
      words.add(CARDINALS.get(i));
    } else if (i < 100) {
      words.add(CARDINALS.get(i / 10 * 10));
      long mod = i % 10;
      if (mod != 0) {
        toWords(mod, words);
      }
    } else if (i < 1_000) {
      toWords(i / 100, words);
      words.add(CARDINALS.get(100L));
      long mod = i % 100;
      if (mod != 0) {
        toWords(mod, words);
      }
    } else if (i < 1_000_000) {
      toWords(i / 1_000, words);
      words.add(CARDINALS.get(1_000L));
      long mod = i % 1_000;
      if (mod != 0) {
        toWords(mod, words);
      }
    } else if (i < 1_000_000_000L) {
      toWords(i / 1_000_000, words);
      words.add(CARDINALS.get(1_000_000L));
      long mod = i % 1_000_000;
      if (mod != 0) {
        toWords(mod, words);
      }
    } else {
      words.add(Long.toString(i));
    }
  }

  private static void toDigits(long i, List<String> words) {
    long n = i;
    if (i < 0) {
      words.add("negative");
      n = -n;
    }
    Deque<String> stack = Queues.newArrayDeque();
    while (n > 0) {
      stack.push(CARDINALS.get(n % 10));
      n /= 10;
    }
    words.addAll(stack);
  }

  private static void decimal(long w, long p, List<String> words) {
    decimal(false, w, p, words);
  }

  private static void decimal(boolean money, long w, long f, List<String> words) {
    toWords(w, words);
    if (money) {
      words.add("dollars");
      words.add("and");
    } else {
      words.add("point");
    }
    toWords(f, words);
    if (money) {
      words.add("cents");
    }
  }

  private static void cardinal(long cardinal, List<String> words) {
    cardinal(false, cardinal, words);
  }

  private static void cardinal(boolean money, long cardinal, List<String> words) {
    if (CARDINALS.containsKey(cardinal)) {
      words.add(CARDINALS.get(cardinal));
    } else if (cardinal < 1000 || cardinal % 1000 == 0) {
      toWords(cardinal, words);
    } else {
      toDigits(cardinal, words);
    }
    if (money) {
      words.add("dollars");
    }
  }

  private static void ordinal(String ordinal, List<String> words) {
    if (ORDINALS.containsKey(ordinal)) {
      words.add(ORDINALS.get(ordinal));
    } else {
      long number = Long.parseLong(ordinal.substring(0, ordinal.length() - 2)); // remove st, nd, rd, th
      if (number >= BILLION) {
        words.add(ordinal);
        return;
      }
      long i = number % 100;
      long card;
      if (i <= 20 || i % 10 == 0) {
        card = number / 100 * 100;
      } else {
        i = number % 10;
        card = number / 10 * 10;
      }
      String ord = String.format("%d%s", i, ending(i));
      if (card > 0) {
        toWords(card, words);
      }
      ordinal(ord, words);
    }
  }

  private static String ending(long i) {
    if (i < 0) {
      return ending(-i);
    }
    long hundreths = i % 100;
    if (PRETEENS.contains(hundreths)) {
      return "th";
    } else {
      int ones = (int) (i % 10);
      switch (ones) {
        case 1:
          return "st";
        case 2:
          return "nd";
        case 3:
          return "rd";
        default:
          return "th";
      }
    }
  }

  /**
   * Number to string.
   *
   * @param string Sentence possibly containing cardinal or cardinal numbers.
   * @return Normalized string.
   */
  public static String toString(final String string) {
    List<String> tokens = SPLITTER.splitToList(string);
    List<String> words = Lists.newArrayList();

    for (String token : tokens) {
      Matcher matcher = HAS_DIGIT.matcher(token);
      boolean match = matcher.find();
      if (match) {
        matcher = IS_DECIMAL.matcher(token);
        match = matcher.find();
        if (match) {
          boolean money = Optional.ofNullable(matcher.group(1)).isPresent();
          long whole = Long.parseLong(matcher.group(2));
          long fractional = Long.parseLong(matcher.group(3));
          decimal(money, whole, fractional, words);
        }
        if (!match) {
          matcher = IS_CARDINAL.matcher(token);
          match = matcher.find();
          if (match) {
            boolean money = Optional.ofNullable(matcher.group(1)).isPresent();
            long cardinal = Long.parseLong(matcher.group(2));
            cardinal(money, cardinal, words);
          }
        }
        if (!match) {
          matcher = IS_ORDINAL.matcher(token);
          match = matcher.find();
          if (match) {
            String ordinal = matcher.group(1);
            ordinal(ordinal, words);
          }
        }
      }
      if (!match) {
        words.add(token);
      }
    }

    return JOINER.join(words);
  }

  //public String toNumeric(String string) {
  // TODO add "oh"
  //  return "";
  //}

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    String sentence;
    String flag = "xxx";
    do {
      System.out.format("Enter sentence (\"%s\" to stop):\t", flag);
      sentence = scanner.next();
      if (!sentence.equals(flag)) {
        System.out.println(Numerics.toString(sentence));
      }
    } while (!sentence.equals(flag));
  }
}
