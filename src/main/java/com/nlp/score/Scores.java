package com.nlp.score;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Takes in a confusion matrix and computes additional scores on the matrix.
 * The keys are the correct classification answers (gold) and the values are the actual answers' counts (system).
 *
 * @author Kevin Crosby.
 */
public class Scores {
  private final Map<String, Map<String, Long>> matrix;
  private final Map<String, Metrics> metricsMap;

  public Scores(final Map<String, Map<String, Long>> matrix) {
    this.matrix = matrix;
    metricsMap = Maps.newConcurrentMap();
  }

  private class Metrics {
    private final String category;
    private final long tp, tn, fp, fn;

    private Metrics(final String category) {
      this.category = category;
      long tp = 0, tn = 0, fn = 0, tfp = 0; // tp + fp
      if (matrix.containsKey(category)) {
        for (final Entry<String, Long> entry : matrix.get(category).entrySet()) {
          if (category.equals(entry.getKey())) {
            tp += entry.getValue();
          } else {
            fn += entry.getValue();
          }
        }
        for (final Map<String, Long> values : matrix.values()) {
          if (values.containsKey(category)) {
            tfp += values.get(category);
          } else {
            tn++;
          }
        }
      }
      this.tp = tp;
      this.tn = tn;
      this.fp = tfp - tp;
      this.fn = fn;
    }

    private String category() {
      return category;
    }

    private long tp() { // hits
      return tp;
    }

    private long tn() { // correct rejection
      return tn;
    }

    private long fp() { // spurious (Type I error)
      return fp;
    }

    private long fn() { // missing (Type II error)
      return fn;
    }

    private double precision() {
      long n = tp;
      long d = tp + fp;
      return d > 0 ? (double) n / d : 0d;
    }

    private double recall() {
      long n = tp;
      long d = tp + fn;
      return d > 0 ? (double) n / d : 0d;
    }

    private double f() {
      double p = precision();
      double r = recall();
      double n = 2 * p * r;
      double d = p + r;
      return d > 0 ? n / d : 0d;
    }

    private double accuracy() {
      long n = tp + tn;
      long d = n + fp + fn;
      return d > 0 ? (double) n / d : 0d;
    }
  }

  private Metrics compute(final String category) {
    return metricsMap.computeIfAbsent(category, Metrics::new);
  }

  /**
   * Compute true positives (hits) for category.
   *
   * @param category Category of interest.
   * @return True positive count for category.
   */
  public long tp(final String category) {
    return compute(category).tp();
  }

  /**
   * Compute True positives (hits) for system.
   *
   * @return True positive count for system.
   */
  public long tp() {
    return matrix.entrySet().stream().mapToLong(e -> tp(e.getKey())).sum();
  }

  /**
   * Compute true negatives (correct rejections) for category.
   *
   * @param category Category of interest.
   * @return True negative count for category.
   */
  public long tn(final String category) {
    return compute(category).tn();
  }

  /**
   * Compute true negatives (correct rejections) for system.
   *
   * @return True negative count for system.
   */
  public long tn() {
    return matrix.keySet().stream().mapToLong(this::tn).sum();
  }

  /**
   * Compute false positives (type I errors) for category.
   *
   * @param category Category of interest.
   * @return False positive count for category.
   */
  public long fp(final String category) {
    return compute(category).fp();
  }

  /**
   * Compute false positives (type I errors) for system.
   *
   * @return False positive count for system.
   */
  public long fp() {
    return matrix.keySet().stream().mapToLong(this::fp).sum();
  }

  /**
   * Compute false negatives (type II errors) for category.
   *
   * @param category Category of interest.
   * @return False negative count for category.
   */
  public long fn(final String category) {
    return compute(category).fn();
  }

  /**
   * Compute false negatives (type II errors) for system.
   *
   * @return False negative count for system.
   */
  public long fn() {
    return matrix.keySet().stream().mapToLong(this::fn).sum();
  }

  /**
   * Compute precision for category.
   *
   * @param category Category of interest.
   * @return Precision for category.
   */
  public double getPrecision(final String category) {
    return compute(category).precision();
  }

  /**
   * Compute macro averaged precision of the system.
   *
   * @return macro averaged precision of the system.
   */
  public double getPrecision() { // macro
    return matrix.keySet().stream().mapToDouble(this::getPrecision).sum() / matrix.size();
  }

  /**
   * Compute micro averaged precision of the system.
   *
   * @return micro averaged precision of the system.
   */
  public double getPrecisionMicro() { // micro
    long n = tp();
    long d = tp() + fp();
    return d > 0 ? (double) n / d : 0d;
  }

  /**
   * Compute recall for category.
   *
   * @param category Category of interest.
   * @return Recall for category.
   */
  public double getRecall(final String category) {
    return compute(category).recall();
  }

  /**
   * Compute macro averaged recall of the system.
   *
   * @return macro averaged recall of the system.
   */
  public double getRecall() { // macro
    return matrix.keySet().stream().mapToDouble(this::getRecall).sum() / matrix.size();
  }

  /**
   * Compute micro averaged recall of the system.
   *
   * @return micro averaged recall of the system.
   */
  public double getRecallMicro() { // micro
    long n = tp();
    long d = tp() + fn();
    return d > 0 ? (double) n / d : 0d;
  }

  /**
   * Compute F1 measure for category.
   *
   * @param category Category of interest.
   * @return F1 measure for category.
   */
  public double getF1Measure(final String category) {
    return compute(category).f();
  }

  /**
   * Compute macro averaged F1 measure of the system.
   *
   * @return macro averaged F1 measure of the system.
   */
  public double getF1Measure() { // macro
    double p = getPrecision();
    double r = getRecall();
    double n = 2 * p * r;
    double d = p + r;
    return d > 0 ? n / d : 0d;
  }

  /**
   * Compute micro averaged F1 measure of the system.
   *
   * @return micro averaged F1 measure of the system.
   */
  public double getF1MeasureMicro() { // micro
    double p = getPrecisionMicro();
    double r = getRecallMicro();
    double n = 2 * p * r;
    double d = p + r;
    return d > 0 ? n / d : 0d;
  }

  /**
   * Compute accuracy for category.
   *
   * @param category Category of interest.
   * @return Accuracy for category.
   */
  public double getAccuracy(final String category) {
    return compute(category).accuracy();
  }

  /**
   * Compute macro averaged accuracy of the system.
   *
   * @return macro averaged accuracy of the system.
   */
  public double getAccuracy() { // macro
    return matrix.keySet().stream().mapToDouble(this::getAccuracy).sum() / matrix.size();
  }

  /**
   * Compute micro averaged accuracy of the system.
   *
   * @return micro averaged accuracy of the system.
   */
  public double getAccuracyMicro() { // micro
    long n = tp() + tn();
    long d = n + fp() + fn();
    return d > 0 ? (double) n / d : 0d;
  }

  /**
   * Show the scores on the screen.
   */
  public void showScores() {
    generateScores(System.out);
  }

  /**
   * Write the scores to a file.
   */
  public void writeScores(final File file) {
    try {
      if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
        System.err.format("Cannot create directory for file \"%s\"\n", file);
        System.exit(1);
      }
      OutputStream os = new FileOutputStream(file);
      generateScores(os);
      os.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateScores(final OutputStream os) {
    PrintWriter out = new PrintWriter(os, true);
    // show header
    String divider = new String(new char[90]).replace("\0", "-");
    out.println(divider);
    out.format("%30s,%6s,%6s,%6s,%6s,%6s,%6s,%6s\n", "CATEGORY", "C", "I", "S", "M", "P", "R", "F");
    out.format("%30s,%6s,%6s,%6s,%6s,%6s,%6s,%6s\n", "", "(tp)", "(tn)", "(fp)", "(fn)", "(prec)", "(rec)", "(f1)");
    out.println(divider);
    for (String category : getCategories()) {
      out.format("%30s,%6d,%6d,%6d,%6d,%.4f,%.4f,%.4f\n", category,
          tp(category), tn(category), fp(category), fn(category),
          getPrecision(category), getRecall(category), getF1Measure(category));
    }
    out.println(divider);
    out.format("%30s,%6s,%6s,%6s,%6s,%.4f,%.4f,%.4f\n", "(MICRO)",
        "N/A", "N/A", "N/A", "N/A",
        getPrecisionMicro(), getRecallMicro(), getF1MeasureMicro());
    out.format("%30s,%6d,%6d,%6d,%6d,%.4f,%.4f,%.4f\n", "(MACRO)",
        tp(), tn(), fp(), fn(),
        getPrecision(), getRecall(), getF1Measure());
  }

  /**
   * Show the confusion matrix on the screen.
   */
  public void showMatrix() {
    generateMatrix(System.out);
  }

  /**
   * Write the confusion matrix to a file.
   */
  public void writeMatrix(final File file) {
    try {
      if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
        System.err.format("Cannot create directory for file \"%s\"", file);
        System.exit(1);
      }
      OutputStream os = new FileOutputStream(file);
      generateMatrix(os);
      os.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateMatrix(final OutputStream os) {
    List<String> categories = getCategories();
    int n = categories.size();
    Map<String, Integer> indices = IntStream.range(0, n).boxed() // inverse mapping from category to index
        .collect(Collectors.toMap(categories::get, i -> i));

    // create 2D array and plug in values based on category, category pairs
    long[][] grid = new long[n][n];
    for (Entry<String, Map<String, Long>> row : matrix.entrySet()) {
      String gold = row.getKey();
      int r = indices.get(gold);
      for (Entry<String, Long> column : row.getValue().entrySet()) {
        String system = column.getKey();
        int c = indices.get(system);
        long count = column.getValue();
        grid[r][c] = count;
      }
    }

    // display to screen
    PrintWriter out = new PrintWriter(os, true);
    out.print(',');
    out.println(Joiner.on(',').join(categories));
    for (int r = 0; r < n; ++r) {
      String gold = categories.get(r);
      out.format("%30s,", gold);
      for (int c = 0; c < n; ++c) {
        if (grid[r][c] != 0) {
          out.format("%6d,", grid[r][c]);
        } else {
          out.format("%6s,", '.');
        }
      }
      out.println();
    }
  }

  /**
   * Get the categories represented in the system.
   *
   * @return List of unique categories in the system.
   */
  public List<String> getCategories() { // get list of unique categories
    Set<String> rows = matrix.keySet(); // category rows (gold)
    Set<String> columns = matrix.values().stream() // category columns (system)
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    return Sets.union(rows, columns).stream() // all known categories
        .sorted()
        .collect(Collectors.toList());
  }

  public Map<String, Map<String, Long>> getMatrix() {
    return matrix;
  }
}
