package com.nlp.spelling;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicLongMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static com.nlp.math.LogUtils.logSumExp;
import static java.lang.Math.exp;

/**
 * @author Kevin Crosby.
 */
public abstract class SpellCorrect {
  protected static final String FREQUENCY_DICTIONARY = "/com/nlp/spelling/frequency_dictionary_en_82_765.txt";

  protected static final Joiner JOINER = Joiner.on(" ");

  protected SpellCorrect() {
  }

  protected static int editDistance(String s1, String s2) {
    return DamerauLevenshtein.distance(s1, s2);
  }

  protected abstract double logProbability(String word);

  protected Map<String, Double> logProbabilities(Collection<String> words) {
    ImmutableMap.Builder<String, Double> builder = ImmutableMap.<String, Double>builder()
        .orderEntriesByValue(Comparator.reverseOrder());
    builder.putAll(Maps.toMap(words, this::logProbability));

    return builder.build();
  }

  protected abstract double sentenceLogProbability(String sentence);

  @SuppressWarnings("ConstantConditions")
  private Map<String, Double> sentenceProbabilities(Collection<String> sentences) {
    ImmutableMap.Builder<String, Double> builder = ImmutableMap.<String, Double>builder()
        .orderEntriesByValue(Comparator.reverseOrder());
    Map<String, Double> map = Maps.toMap(sentences, this::sentenceLogProbability);
    final double denominator = logSumExp(map.values());
    builder.putAll(Maps.transformValues(map, logProbability -> exp(logProbability - denominator)));
    return builder.build();
  }

  public abstract String correct(String word);

  public abstract Set<String> candidates(String word);

  public Map<String, Double> process(String sentence) {
    return sentenceProbabilities(expand(sentence));
  }

  // expand sentence into all possible corrections
  protected Collection<String> expand(String sentence) {
    sentence = sentence.toLowerCase();
    List<List<String>> candidates = Lists.newArrayList();
    StringTokenizer tokenizer = new StringTokenizer(sentence);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      List<String> candidate = Lists.newArrayList(candidates(token));
      // TODO add thresholds based on 1) number of candidates per word, 2) probability, and/or 3) number of paths??
      candidates.add(candidate);
    }
    return cartesian(candidates);
  }

  // cartesian product of string concatenations
  protected static List<String> cartesian(List<List<String>> candidates) {
    return Lists.cartesianProduct(candidates).stream()
        .map(JOINER::join)
        .collect(Collectors.toList());
  }

  protected static boolean isInitial(String string) {
    return string.length() == 1 && string.charAt(0) != 'a' && string.charAt(0) != 'i';
  }

  protected static AtomicLongMap<String> load(String resource) {
    AtomicLongMap<String> counter = AtomicLongMap.create();
    load(resource, counter);
    return counter;
  }

  private static void load(String resource, AtomicLongMap<String> counter) {
    InputStream inputStream = SpellCorrect.class.getResourceAsStream(resource);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      while ((line = br.readLine()) != null) {
        Scanner scanner = new Scanner(line).useDelimiter("\\s");
        String string = scanner.next().toLowerCase().trim();
        if (!isInitial(string)) {
          long frequency = scanner.hasNextLong() ? scanner.nextLong() : 1;
          counter.addAndGet(string, frequency);
        }
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
