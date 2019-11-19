package com.nlp.spelling;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicLongMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.log;

/**
 * Adapted from {@see <a href="https://github.com/wolfgarbe/SymSpellCompound">Wolf Garbe's SymSpellCompound</a>}.
 *
 * @author Kevin Crosby.
 */
public class SmartSpeller extends SpellCorrect {
  private static final Pattern PASS_THROUGH = Pattern.compile("[$\\d]");

  private static final int EDIT_DISTANCE_MAX = 2;
  private static final int DEFAULT_SENTENCE_COUNT = 12;

  private static final int CORPUS_MULTIPLIER = 1;
  private static final int TRAINING_MULTIPLIER = 100_000;
  private static final int COMMON_MULTIPLIER = 1;

  private static SmartSpeller instance = null;

  private final int sentenceCount;

  private final Set<String> vocabulary;

  private final AtomicLongMap<String> words;
  private final int maxlength;
  private final Table<String, String, Integer> dictionary; // rows are suggestions, columns are dictionary words, int is edit distance
  private final Comparator<Entry<String, Integer>> suggestionComparator;

  private SmartSpeller() {
    super();
    sentenceCount = DEFAULT_SENTENCE_COUNT;

    Stopwatch stopwatch = Stopwatch.createUnstarted();
    stopwatch.reset();
    stopwatch.start();
    System.out.println("Loading words ...");
    words = load(FREQUENCY_DICTIONARY);
    stopwatch.stop();
    vocabulary = words.asMap().keySet();
    maxlength = maxlength(vocabulary);
    System.out.printf("Loading %d words: %d milliseconds\n", vocabulary.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    System.out.println("Constructing edit dictionary ...");
    stopwatch.reset();
    stopwatch.start();
    dictionary = dictionary(words);
    stopwatch.stop();
    System.out.printf("Constructing edit dictionary: %d milliseconds\n", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    suggestionComparator = suggestionComparator(words);
    System.out.println("Finished initializing spell corrector");
  }

  public static SmartSpeller getInstance() {
    if (instance == null) {
      synchronized (SmartSpeller.class) {
        if (instance == null) {
          instance = new SmartSpeller();
        }
      }
    }
    return instance;
  }

  private int maxlength(Set<String> vocabulary) {
    return vocabulary.stream()
        .mapToInt(String::length)
        .max()
        .orElse(0);
  }

  private Table<String, String, Integer> dictionary(AtomicLongMap<String> words) {
    ImmutableTable.Builder<String, String, Integer> builder = ImmutableTable.builder();
    for (String word : words.asMap().keySet()) {
      if (word.length() == 1) {
        builder.put("", word, 1);
      } else {
        for (int d = 1; d <= EDIT_DISTANCE_MAX; ++d) {
          for (String edit : edits(word, d)) {
            builder.put(edit, word, d);
          }
        }
      }
    }
    return builder.build();
  }

  private Comparator<Entry<String, Integer>> suggestionComparator(AtomicLongMap<String> words) {
    return Entry.<String, Integer>comparingByValue()
        .thenComparing(x -> words.get(x.getKey()), Comparator.reverseOrder());
  }

  @Override
  protected double logProbability(String word) {
    return log(words.get(word) + 1) - log(words.sum() + vocabulary.size());
  }

  @Override
  protected double sentenceLogProbability(String sentence) {
    double logProbability = 0;
    for (String word : parseWords(sentence)) {
      if (!PASS_THROUGH.matcher(word).find()) {
        logProbability += logProbability(word);
      }
    }
    return logProbability;
  }

  //create a non-unique wordlist from sample text
  private List<String> parseWords(String text) {
    List<String> words = Lists.newArrayList();
    StringTokenizer tokenizer = new StringTokenizer(text);
    while (tokenizer.hasMoreTokens()) {
      String word = tokenizer.nextToken();
      words.add(word);
    }
    return words;
  }

  @Override
  public String correct(String word) {
    return candidates(word).stream()
        .findFirst()
        .orElse(word);
  }

  @Override
  public Set<String> candidates(String input) {
    if (PASS_THROUGH.matcher(input).find()) {
      return ImmutableSet.of(input);
    }
    return lookup(input).keySet();
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  protected Collection<String> expand(String sentence) {
    sentence = sentence.toLowerCase();
    List<List<String>> candidates = Lists.newArrayList();
    for (String token : parseWords(sentence)) {
      LinkedListMultimap<Integer, String> multimap = Multimaps.invertFrom(Multimaps.forMap(lookup(token)), LinkedListMultimap.create());
      for (int d = 0; d <= EDIT_DISTANCE_MAX; ++d) {
        if (multimap.containsKey(d)) {
          candidates.add(multimap.get(d));
          break;
        }
      }
    }

    //build candidates
    long sentenceCount = 1;
    List<List<String>> choices = candidates.stream()
        .map(c -> Lists.newArrayList(c.get(0)))
        .collect(Collectors.toList());
    long[] horizonCounts = candidates.stream()
        .map(c -> c.size() > 1 ? c.get(1) : "")
        .mapToLong(words::get)
        .toArray();
    Queue<Integer> pq = new PriorityQueue<>(Comparator.<Integer>comparingLong(i -> horizonCounts[i]).reversed());
    IntStream.range(0, candidates.size()).forEach(pq::add);
    while (sentenceCount < this.sentenceCount) {
      int argmax = pq.remove(); // will add back later after horizon counts are updated
      if (horizonCounts[argmax] == 0) {
        break;
      }
      List<String> candidate = candidates.get(argmax);
      List<String> choice = choices.get(argmax);
      int index = choice.size();
      choice.add(candidate.get(index++));
      horizonCounts[argmax] = index < candidate.size() ? words.get(candidate.get(index)) : 0;
      pq.add(argmax); // refreshes priority queue
      sentenceCount = choices.stream()
          .mapToLong(List::size)
          .reduce(1L, Math::multiplyExact);
    }

    return cartesian(choices);
  }

  //inexpensive and language independent: only deletes, no transposes + replaces + inserts
  private static Set<String> edits(String word, int d) {
    Set<String> deletes = Sets.newLinkedHashSet();
    int n = word.length();
    if (n > d) {
      int limit = (1 << n);
      for (int mask = (1 << (n - d)) - 1; mask < limit; mask = hakmem175(mask)) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; ++i) {
          if ((mask & (1 << i)) != 0) {
            sb.append(word.charAt(i));
          }
        }
        deletes.add(sb.toString());
      }
    }
    return deletes;
  }

  // compute the lexicographically next bit permutation
  private static int hakmem175(int v) {
    int t = v | (v - 1);
    return (t + 1) | ((~t & -~t) - 1) >> (Integer.numberOfTrailingZeros(v) + 1);
  }

  private Map<String, Integer> lookup(String input) {
    //save some time
    if (input.length() - EDIT_DISTANCE_MAX > maxlength) {
      return ImmutableMap.of();
    }
    Queue<String> candidates = Queues.newArrayDeque();
    Set<String> hashset1 = Sets.newHashSet();

    Map<String, Integer> suggestions = Maps.newHashMap();
    Set<String> hashset2 = Sets.newHashSet();

    //add original term
    candidates.add(input);

    while (!candidates.isEmpty()) {
      String candidate = candidates.remove();

      //if count>0 then candidate entry is correct dictionary term, not only delete item
      if (dictionary.containsColumn(candidate) && hashset2.add(candidate)) { // i.e. if count>0, then candidate is a column of dictionary
        int distance = input.length() - candidate.length();
        suggestions.put(candidate, distance);
      }

      //read candidate entry from dictionary
      //iterate through suggestions (to other correct dictionary items) of delete item and add them to suggestion list
      for (String suggestion : dictionary.row(candidate).keySet()) { // i.e. dictionary words only
        //save some time
        //skipping double items early: different deletes of the input term can lead to the same suggestion
        if (hashset2.add(suggestion)) {
          //True Damerau-Levenshtein Edit Distance: adjust distance, if both distances>0
          //We allow simultaneous edits (deletes) of editDistanceMax on on both the dictionary and the input term.
          //For replaces and adjacent transposes the resulting edit distance stays <= editDistanceMax.
          //For inserts and deletes the resulting edit distance might exceed editDistanceMax.
          //To prevent suggestions of a higher edit distance, we need to calculate the resulting edit distance, if there are simultaneous edits on both sides.
          //Example: (bank==bnak and bank==bink, but bank!=kanb and bank!=xban and bank!=baxn for editDistanceMaxe=1)
          //Two deletes on each side of a pair makes them all equal, but the first two pairs have edit distance=1, the others edit distance=2.
          int distance = 0;
          if (!suggestion.equals(input)) {
            if (suggestion.length() == candidate.length()) {
              distance = input.length() - candidate.length();
            } else if (input.length() == candidate.length()) {
              distance = suggestion.length() - candidate.length();
            } else {
              //common prefixes and suffixes are ignored, because this speeds up the Damerau-Levenshtein-Distance calculation without changing it.
              int ii = 0;
              int jj = 0;
              while (ii < suggestion.length() && ii < input.length() && suggestion.charAt(ii) == input.charAt(ii)) {
                ii++;
              }
              while (jj < suggestion.length() - ii && jj < input.length() - ii && suggestion.charAt(suggestion.length() - jj - 1) == input.charAt(input.length() - jj - 1)) {
                jj++;
              }
              if (ii > 0 || jj > 0) {
                distance = editDistance(suggestion.substring(ii, suggestion.length() - jj), input.substring(ii, input.length() - jj)); // FIXED by Kevin
              } else {
                distance = editDistance(suggestion, input);
              }
            }
          }

          if (distance <= EDIT_DISTANCE_MAX) {
            suggestions.put(suggestion, distance);
          }
        }
      }//end foreach

      //add edits
      //derive edits (deletes) from candidate (input) and add them to candidates list
      //this is a recursive process until the maximum edit distance has been reached
      if (input.length() - candidate.length() < EDIT_DISTANCE_MAX) {
        //noinspection ConstantConditions
        Set<String> edits = hashset2.contains(candidate)
            ? Maps.filterValues(dictionary.column(candidate), d -> d == 1).keySet()
            : edits(candidate, 1);
        for (String delete : edits) {
          if (hashset1.add(delete)) {
            candidates.add(delete);
          }
        }
      }
    }//end while

    // return original word if no other corrections offered ...
    if (suggestions.isEmpty()) {
      return ImmutableMap.of(input, 0);
    }

    //sort by ascending edit distance, then by descending word frequency
    return suggestions.entrySet().stream()
        .sorted(suggestionComparator)
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  public static void main(String[] args) {
    SmartSpeller spell = SmartSpeller.getInstance();

    //Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    //String word;
    //String flag = "xxx";
    //do {
    //  System.out.format("Enter word (\"%s\" to stop):\t", flag);
    //  word = scanner.next();
    //  for (Entry<String, Integer> entry : spell.candidates(word).entrySet()) {
    //    String term = entry.getKey();
    //    int distance = entry.getValue();
    //    long count = words.get(term);
    //    System.out.format("%s %d %d\n", term, distance, count);
    //  }
    //} while (!word.equals(flag));

    Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
    String sentence;
    String flag = "xxx";
    while (true) {
      System.out.format("Enter sentence (\"%s\" to stop):\t", flag);
      sentence = scanner.next().toLowerCase();
      if (sentence.equals(flag)) {
        break;
      }
      Map<String, Double> corrections = spell.process(sentence);
      for (Entry<String, Double> entry : corrections.entrySet()) {
        System.out.format("%20s : %.15f\n", entry.getKey(), entry.getValue());
      }
    }
  }
}
