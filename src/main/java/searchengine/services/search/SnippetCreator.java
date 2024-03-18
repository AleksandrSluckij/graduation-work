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
  private static final String NO_LETTERS = "[^а-яё^А-ЯЁ^ ]";


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
    int prevPosition = -1;   // Изначально должно быть -1!!!!!!
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
    int startFragmentPosition;
    int endFragmentPosition;
    String item = getWholeWord(text, stemEntry);
    int startStemPosition = stemEntry.getValue();
    int endStemPosition = startStemPosition + item.length();
    int textLength = text.length();

    startFragmentPosition = prevPosition == -1 ?
          startStemPosition > SNIPPET_FRAG_HALF_SIZE ? startStemPosition - SNIPPET_FRAG_HALF_SIZE : 0
          :
          startStemPosition > prevPosition + SNIPPET_FRAG_HALF_SIZE ? startStemPosition - SNIPPET_FRAG_HALF_SIZE : prevPosition;

    endFragmentPosition = lastStem ?
            endStemPosition > textLength - SNIPPET_FRAG_HALF_SIZE ? textLength - 1 : endStemPosition + SNIPPET_FRAG_HALF_SIZE
            :
            endStemPosition;

    String fragment = text.substring(startFragmentPosition, endFragmentPosition);
    fragment = trimLeftTail(fragment, startFragmentPosition);

    if (lastStem && (endFragmentPosition != textLength - 1)) {
      fragment = trimRightTail(fragment, endFragmentPosition, textLength);
    }
    startStemPosition = fragment.indexOf(item);
    endStemPosition = startStemPosition + item.length();
    return fragment.substring(0, startStemPosition) + "<b>" + item + "</b>" + fragment.substring(endStemPosition);
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
