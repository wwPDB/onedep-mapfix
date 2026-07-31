package msdmap.mapread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapHeaderTest {

  private static MapHeader withLabels(String... lines) throws Exception {
    MapHeader header = new MapHeader();
    header.label = new String[10];
    for (int i = 0; i < lines.length; i++) header.label[i] = lines[i];
    header.setWord(1, new int[]{56}, new String[]{String.valueOf(lines.length)});
    return header;
  }

  private static void assertDemotedForm(String actual, String expectedId) {
    String pattern = "^::::\\d{14}::::" + java.util.regex.Pattern.quote(expectedId) + "::::$";
    assertTrue(actual.matches(pattern),
        "expected a demoted history entry for <" + expectedId + "> but was <" + actual + ">");
  }

  private static void assertDemotedFormWithTally(String actual, String expectedId, int expectedTally) {
    String pattern = "^::::\\d{14}::::" + java.util.regex.Pattern.quote(expectedId)
        + ":::: \\[TRUNCATED: " + expectedTally + " more line\\(s\\) removed\\]$";
    assertTrue(actual.matches(pattern),
        "expected a demoted history entry for <" + expectedId + "> with tally " + expectedTally
        + " but was <" + actual + ">");
  }

  @Test
  void normalPrependShiftsDepositorLinesDown() throws Exception {
    MapHeader header = withLabels("Relion reconstruction metadata", "Second depositor line");
    header.changeLabel(MapHeader.makeSystemLabel("D_1234567890"));

    assertEquals(3, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1234567890"), header.getLabel(0).trim());
    assertEquals("Relion reconstruction metadata", header.getLabel(1).trim());
    assertEquals("Second depositor line", header.getLabel(2).trim());
  }

  @Test
  void overflowFillsNineLinesAndAnnotatesLineTen() throws Exception {
    String[] depositor = new String[10];
    for (int i = 0; i < 10; i++) depositor[i] = "Depositor line " + (i + 1);
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1234567890"));

    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1234567890"), header.getLabel(0).trim());
    // Natural in-order shifting keeps the first depositor line safe at line 2, right
    // after the system label - no special reservation needed.
    for (int i = 0; i < 8; i++) {
      assertEquals("Depositor line " + (i + 1), header.getLabel(1 + i).trim());
    }
    String lineTen = header.getLabel(9).trim();
    assertTrue(lineTen.startsWith("Depositor line 9"));
    assertTrue(lineTen.contains("[TRUNCATED: 1 more line(s) removed]"));
    // "Depositor line 10" is the one line fully dropped - it appears nowhere.
  }

  @Test
  void overflowEllipsisTruncatesLongLineTenContent() throws Exception {
    String[] depositor = new String[10];
    for (int i = 0; i < 10; i++) depositor[i] = "Depositor line " + (i + 1);
    depositor[8] = "This depositor line nine is intentionally very long so it must be "
        + "truncated with an ellipsis before the warning suffix is appended to it";
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1234567890"));

    String lineTen = header.getLabel(9).trim();
    assertTrue(lineTen.length() <= 80);
    assertTrue(lineTen.startsWith("This depositor line nine"));
    assertTrue(lineTen.contains("..."));
    assertTrue(lineTen.endsWith("[TRUNCATED: 1 more line(s) removed]"));
  }

  @Test
  void systemLabelIsDemotedToHistory() throws Exception {
    MapHeader header = withLabels(
        MapHeader.makeSystemLabel("D_1234567890"),
        "ChimeraX 0.1 Wed Jan 21 12:53:36 2026");

    header.changeLabel(MapHeader.makeSystemLabel("EMD-112358"));

    assertEquals(3, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-112358"), header.getLabel(0).trim());
    assertDemotedForm(header.getLabel(1).trim(), "D_1234567890");
    assertEquals("ChimeraX 0.1 Wed Jan 21 12:53:36 2026", header.getLabel(2).trim());
  }

  @Test
  void relabelingWithSameIdIsNoOpAndAddsNoHistoryEntry() throws Exception {
    MapHeader header = withLabels("Relion reconstruction metadata");

    header.changeLabel(MapHeader.makeSystemLabel("D_100"));
    header.changeLabel(MapHeader.makeSystemLabel("D_100"));

    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_100"), header.getLabel(0).trim());
    assertEquals("Relion reconstruction metadata", header.getLabel(1).trim());
  }

  @Test
  void multipleRelabelingRoundsBuildHistoryNewestFirst() throws Exception {
    MapHeader header = withLabels("Relion reconstruction metadata", "Second depositor line");

    header.changeLabel(MapHeader.makeSystemLabel("D_100"));
    header.changeLabel(MapHeader.makeSystemLabel("EMD-50"));
    header.changeLabel(MapHeader.makeSystemLabel("D_200"));
    header.changeLabel(MapHeader.makeSystemLabel("EMD-99"));

    assertEquals(6, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-99"), header.getLabel(0).trim());
    assertDemotedForm(header.getLabel(1).trim(), "D_200");
    assertDemotedForm(header.getLabel(2).trim(), "EMD-50");
    assertDemotedForm(header.getLabel(3).trim(), "D_100");
    assertEquals("Relion reconstruction metadata", header.getLabel(4).trim());
    assertEquals("Second depositor line", header.getLabel(5).trim());
  }

  @Test
  void historyEvictsOldestEntryFirstWhileProtectingCurrentAndDepositorLines() throws Exception {
    String[] depositor = new String[8];
    for (int i = 0; i < 8; i++) depositor[i] = "Depositor line " + (i + 1);
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1")); // no history yet
    header.changeLabel(MapHeader.makeSystemLabel("D_2")); // demotes D_1 - fits exactly (10)
    header.changeLabel(MapHeader.makeSystemLabel("D_3")); // demotes D_2 - D_1's entry is evicted

    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_3"), header.getLabel(0).trim());
    assertDemotedFormWithTally(header.getLabel(1).trim(), "D_2", 1);
    // demoted(D_1) was evicted (oldest, nearest the depositor content) - its loss is
    // reflected in the "1" tally now carried by D_2's surviving entry, not silently.
    for (int i = 0; i < 8; i++) {
      assertEquals("Depositor line " + (i + 1), header.getLabel(2 + i).trim());
    }
  }

  @Test
  void evictionTallyAccumulatesAcrossMultipleRounds() throws Exception {
    String[] depositor = new String[8];
    for (int i = 0; i < 8; i++) depositor[i] = "Depositor line " + (i + 1);
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1")); // no history yet
    header.changeLabel(MapHeader.makeSystemLabel("D_2")); // fits exactly (10), no eviction
    header.changeLabel(MapHeader.makeSystemLabel("D_3")); // evicts D_1 -> D_2 tally=1
    assertDemotedFormWithTally(header.getLabel(1).trim(), "D_2", 1);

    header.changeLabel(MapHeader.makeSystemLabel("D_4")); // evicts D_2(tally=1) -> D_3 tally=2
    assertDemotedFormWithTally(header.getLabel(1).trim(), "D_3", 2);

    header.changeLabel(MapHeader.makeSystemLabel("D_5")); // evicts D_3(tally=2) -> D_4 tally=3
    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_5"), header.getLabel(0).trim());
    assertDemotedFormWithTally(header.getLabel(1).trim(), "D_4", 3);
    // The tally correctly reflects all 3 evicted IDs (D_1, D_2, D_3) across the map's
    // full history, not just the single most recent eviction.
    for (int i = 0; i < 8; i++) {
      assertEquals("Depositor line " + (i + 1), header.getLabel(2 + i).trim());
    }
  }

  @Test
  void evictionTallyEllipsisTruncatesLongIdWhenCombinedLengthExceeds80() throws Exception {
    String[] depositor = new String[8];
    for (int i = 0; i < 8; i++) depositor[i] = "Depositor line " + (i + 1);
    MapHeader header = withLabels(depositor);

    String longId = "D_999999999999999999"; // 20 chars - long enough to force truncation
    header.changeLabel(MapHeader.makeSystemLabel("D_1"));
    header.changeLabel(MapHeader.makeSystemLabel(longId));
    header.changeLabel(MapHeader.makeSystemLabel("D_3")); // evicts D_1 -> longId survives, tallied

    String lineTwo = header.getLabel(1).trim();
    assertTrue(lineTwo.length() <= 80);
    assertTrue(lineTwo.contains("..."));
    assertTrue(lineTwo.endsWith("[TRUNCATED: 1 more line(s) removed]"));
    assertTrue(lineTwo.matches("^::::\\d{14}::::D_9+\\.\\.\\.:::: \\[TRUNCATED: 1 more line\\(s\\) removed\\]$"),
        "expected an ellipsis-truncated, tallied entry but was <" + lineTwo + ">");
  }

  @Test
  void formatLabelTruncatesOverlongNewLabelTo80Chars() throws Exception {
    MapHeader header = withLabels();
    String longText = "X".repeat(200);

    header.changeLabel(longText);

    assertEquals(1, header.getNLabels());
    assertEquals(80, header.getLabel(0).length());
    assertEquals("X".repeat(80), header.getLabel(0));
  }
}
