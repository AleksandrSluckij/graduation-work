package searchengine.services.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class SnippetCreator {

  private static final int SNIPPET_FRAG_HALF_SIZE = 50;
  private static final String DOTS = "...";
  private static final String ABSENT_STEM = "...>>NOT FOUND<<...";
  private static final String NO_LETTERS = "[^а-я^А-Я^ ]";


  public static String getExactlySnippet (String text, String query) {
    int start = text.indexOf(query);
    int end = start + query.length();
    int textLength = text.length();
    if (start > -1) {
      start = start > SNIPPET_FRAG_HALF_SIZE ? start - SNIPPET_FRAG_HALF_SIZE : 0;
      end = end > textLength - SNIPPET_FRAG_HALF_SIZE ? textLength - 1 : end + SNIPPET_FRAG_HALF_SIZE;
      String part = text.substring(start, end);
      //trim tails!!!
      part = trimRightTail(trimLeftTail(part, start), end, textLength);
      //
      start = part.indexOf(query);
      end = start + query.length();
      return part.substring(0, start) + "<b>" + query + "</b>" + part.substring(end);
    }
    return null;
  }

  private static String trimLeftTail (String part, int start) {
    if (start > 0) {
      String partCorrected = part.replaceAll(NO_LETTERS, " ");
      int leftTailIndex = partCorrected.indexOf(" ") + 1;
      while (partCorrected.substring(leftTailIndex, leftTailIndex + 1).matches(" ")) {
        leftTailIndex++;
      }
      part = DOTS + part.substring(leftTailIndex);
    }
    return part;
  }

  private static String trimRightTail (String part, int end, int textLength) {
    if (end < textLength - 1) {
      String partCorrected = part.replaceAll(NO_LETTERS, " ");
      int rightTailIndex = partCorrected.lastIndexOf(" ");
      while (partCorrected.substring(rightTailIndex - 1, rightTailIndex).matches(" ")) {
        rightTailIndex--;
      }
      part = part.substring(0, rightTailIndex) + DOTS;
    }
    return part;
  }

  public static String getApproxSnippet(String text, String query) {
    String result = "";
    int prevPosition = 0;
    List<String> queryStems = getQueryStems(query);
    Map<String, Integer> queryStemsMap = new HashMap<>();
    String textInLowerCase = text.toLowerCase();
    for (String stem : queryStems) {
      int position = textInLowerCase.indexOf(stem);
      if (position > -1) {
        queryStemsMap.put(stem, position);
      }
    }
    List<Entry<String, Integer>> queryStemsListSorted = new ArrayList<>(queryStemsMap.entrySet());
    queryStemsListSorted.sort(Entry.comparingByValue());
    int stemsCount = queryStemsListSorted.size();
    for (int i = 0; i < stemsCount; i++) {
      Entry<String, Integer> stemEntry = queryStemsListSorted.get(i);
      result = result.concat(getSnippetFragment(text, stemEntry, prevPosition, stemsCount - i < 2));
      prevPosition = stemEntry.getValue() + stemEntry.getKey().length();
    }
    return result;
  }

  private static String getSnippetFragment(String text, Entry<String, Integer> stemEntry, int prevPosition,
      boolean lastStem) {
    int leftPos;
    int rightPos;
    String item = getWholeWord(text, stemEntry);
    int start = stemEntry.getValue();
    int end = start + item.length();
    int textLength = text.length();
    if (start > -1) {
      if (prevPosition == -1) {
        leftPos = start > SNIPPET_FRAG_HALF_SIZE ? start - SNIPPET_FRAG_HALF_SIZE : 0;
      } else {
        leftPos = start > prevPosition + SNIPPET_FRAG_HALF_SIZE ? start - SNIPPET_FRAG_HALF_SIZE : prevPosition;
      }
      if (lastStem) {
        rightPos = end > textLength - SNIPPET_FRAG_HALF_SIZE ? textLength - 1 : end + SNIPPET_FRAG_HALF_SIZE;
      } else {
        rightPos = end;
      }
      String part = text.substring(leftPos, rightPos);
      if (lastStem && (rightPos != textLength - 1)) {
        part = trimRightTail(trimLeftTail(part, leftPos), rightPos, textLength);
      } else {
        part = trimLeftTail(part, leftPos);
      }
      start = part.indexOf(item);
      end = start + item.length();
      return part.substring(0, start) + "<b>" + item + "</b>" + part.substring(end);
    } else {
      return ABSENT_STEM;
    }
  }

  private static String getWholeWord(String text, Entry<String, Integer> stemEntry) {
    int start = stemEntry.getValue();
    int end = start + stemEntry.getKey().length();
    while (!(text.substring(end, end + 1).replaceAll(NO_LETTERS, " ").matches(" "))) {
      end++;
    }
    return text.substring(start, end);
  }

  private static List<String> getQueryStems(String query) {
    String queryCorrected = query.replaceAll(NO_LETTERS, " ");
    queryCorrected = queryCorrected.replaceAll("[\\s]+", " ");
    return Stream.of(queryCorrected.trim().split(" "))
        .filter(s -> s.length() > 2)
        .map(StemmerPorterRu::stem)
        .toList();
  }
}
