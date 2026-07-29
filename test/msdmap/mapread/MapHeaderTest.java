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
  void overflowReservesOriginalLineAndAnnotatesLineNine() throws Exception {
    String[] depositor = new String[10];
    for (int i = 0; i < 10; i++) depositor[i] = "Depositor line " + (i + 1);
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1234567890"));

    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1234567890"), header.getLabel(0).trim());
    for (int i = 0; i < 7; i++) {
      assertEquals("Depositor line " + (i + 2), header.getLabel(1 + i).trim());
    }
    String lineNine = header.getLabel(8).trim();
    assertTrue(lineNine.startsWith("Depositor line 9"));
    assertTrue(lineNine.contains("[TRUNCATED: 1 more line(s) removed]"));
    assertEquals("Depositor line 1", header.getLabel(9).trim());
  }

  @Test
  void overflowEllipsisTruncatesLongLineNineContent() throws Exception {
    String[] depositor = new String[10];
    for (int i = 0; i < 10; i++) depositor[i] = "Depositor line " + (i + 1);
    depositor[8] = "This depositor line nine is intentionally very long so it must be "
        + "truncated with an ellipsis before the warning suffix is appended to it";
    MapHeader header = withLabels(depositor);

    header.changeLabel(MapHeader.makeSystemLabel("D_1234567890"));

    String lineNine = header.getLabel(8).trim();
    assertTrue(lineNine.length() <= 80);
    assertTrue(lineNine.startsWith("This depositor line nine"));
    assertTrue(lineNine.contains("..."));
    assertTrue(lineNine.endsWith("[TRUNCATED: 1 more line(s) removed]"));
  }

  @Test
  void systemLabelIsReplacedNotShifted() throws Exception {
    MapHeader header = withLabels(
        MapHeader.makeSystemLabel("D_1234567890"),
        "ChimeraX 0.1 Wed Jan 21 12:53:36 2026");

    header.changeLabel(MapHeader.makeSystemLabel("EMD-112358"));

    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-112358"), header.getLabel(0).trim());
    assertEquals("ChimeraX 0.1 Wed Jan 21 12:53:36 2026", header.getLabel(1).trim());
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
