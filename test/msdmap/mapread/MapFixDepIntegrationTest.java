package msdmap.mapread;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests that run the real build/mapFixDep.jar (built by
 * scripts/build-mapfixdep.sh) via ProcessBuilder and inspect the labels of the
 * resulting output file, replacing the old scripts/run-sample.sh manual workflow.
 */
public class MapFixDepIntegrationTest {

  private static final String JAR = "build/mapFixDep.jar";

  private static JSONObject runMapFixDep(String in, String out, String voxel, String label) throws Exception {
    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    ProcessBuilder pb = new ProcessBuilder(
        javaBin, "-jar", JAR,
        "-in", in, "-out", out,
        "-voxel", voxel, voxel, voxel,
        "-label", label);
    // DAInternals writes its debug noise to real stderr and only the final JSON to real
    // stdout (mirroring RcsbDpUtility, which always keeps them as two separate files) -
    // merging the streams here would interleave that noise ahead of the JSON.
    Process process = pb.start();
    String stdout = new String(process.getInputStream().readAllBytes());
    String stderr = new String(process.getErrorStream().readAllBytes());
    int exit = process.waitFor();
    assertEquals(0, exit, "mapFixDep.jar exited non-zero.\nstdout:\n" + stdout + "\nstderr:\n" + stderr);
    return new JSONObject(stdout);
  }

  private static MapHeader readLabels(String path) throws Exception {
    MapHeader header = new MapHeader();
    FileInputStream in = header.open(path);
    header.read(in);
    header.close(in);
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

  private static void assertReportLabelIsCurrentOnly(JSONObject report, String expectedCurrentLabel) {
    JSONObject headerLong = report.getJSONObject("output_header_long");
    assertEquals(expectedCurrentLabel, headerLong.getString("label").trim());
    JSONObject headerOut = report.getJSONObject("output_header");
    assertEquals(expectedCurrentLabel, headerOut.getString("Map title").trim());
  }

  @Test
  void relionUploadKeepsDepositorLabelAfterSystemLabel(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("relion.converted.map").toString();
    JSONObject report = runMapFixDep("sample-data/D_1292121466_em-volume-upload_P1.map.V1", out,
        "1.2194290161132812", "D_1292121466");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1292121466"), header.getLabel(0).trim());
    assertTrue(header.getLabel(1).trim().startsWith("Relion"));

    // "label"/"Map title" carry only the current label - the depositor's Relion line
    // must not leak into them, only into "label_block".
    assertReportLabelIsCurrentOnly(report, MapHeader.makeSystemLabel("D_1292121466"));
    assertTrue(report.getJSONObject("output_header_long").getString("label_block").contains("Relion"));
  }

  @Test
  void chimeraxUploadKeepsDepositorLabelAfterSystemLabel(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("chimerax.converted.map").toString();
    JSONObject report = runMapFixDep("sample-data/D_1292153729_em-volume-upload_P1.map.V1", out,
        "1.384615421295166", "D_1292153729");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1292153729"), header.getLabel(0).trim());
    assertTrue(header.getLabel(1).trim().startsWith("ChimeraX"));

    assertReportLabelIsCurrentOnly(report, MapHeader.makeSystemLabel("D_1292153729"));
    assertTrue(report.getJSONObject("output_header_long").getString("label_block").contains("ChimeraX"));
  }

  @Test
  void blankUploadProducesSystemLabelOnly(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("blank.converted.map").toString();
    runMapFixDep("sample-data/D_1292111988_em-volume-upload_P1.map.V1", out,
        "1.2799999713897705", "D_1292111988");

    MapHeader header = readLabels(out);
    assertEquals(1, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1292111988"), header.getLabel(0).trim());
  }

  @Test
  void alreadySystemLabelledInputDemotesOldIdToHistory(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("relabel.converted.map").toString();
    JSONObject report = runMapFixDep("sample-data/D_1292121466_em-volume-upload_P1.map.V2", out,
        "1.2194000482559204", "EMD-112358");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-112358"), header.getLabel(0).trim());
    assertDemotedForm(header.getLabel(1).trim(), "D_1292121466");

    // The demoted D_1292121466 changelog entry must not leak into "label"/"Map title",
    // only into "label_block" - this is exactly the scenario Ezra's manual test hit.
    assertReportLabelIsCurrentOnly(report, MapHeader.makeSystemLabel("EMD-112358"));
    String labelBlock = report.getJSONObject("output_header_long").getString("label_block");
    assertTrue(labelBlock.contains("D_1292121466"),
        "expected label_block to still contain the demoted history entry but was <" + labelBlock + ">");
  }

  @Test
  void overflowFixtureFillsNineLinesAndAnnotatesLineTen(@TempDir Path tempDir) throws Exception {
    File input = tempDir.resolve("overflow-input.map").toFile();
    String[] depositorLines = new String[10];
    for (int i = 0; i < 10; i++) depositorLines[i] = "Depositor line " + (i + 1);
    TestFixtures.writeFixture(input, depositorLines);

    String out = tempDir.resolve("overflow.converted.map").toString();
    runMapFixDep(input.getPath(), out, "1.0", "D_9999999999");

    MapHeader header = readLabels(out);
    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_9999999999"), header.getLabel(0).trim());
    // Natural in-order shifting keeps the first depositor line safe at line 2, right
    // after the system label - no special reservation needed.
    for (int i = 0; i < 8; i++) {
      assertEquals("Depositor line " + (i + 1), header.getLabel(1 + i).trim());
    }
    assertTrue(header.getLabel(9).trim().contains("[TRUNCATED: 1 more line(s) removed]"));
    // "Depositor line 10" is the one line fully dropped - it appears nowhere.
  }

  @Test
  void replacementFixtureDemotesOldSystemLabelWithoutLosingDepositorLine(@TempDir Path tempDir) throws Exception {
    File input = tempDir.resolve("replacement-input.map").toFile();
    TestFixtures.writeFixture(input,
        MapHeader.makeSystemLabel("D_9999999999"),
        "ChimeraX 0.1 Wed Jan 21 12:53:36 2026");

    String out = tempDir.resolve("replacement.converted.map").toString();
    runMapFixDep(input.getPath(), out, "1.0", "EMD-999999");

    MapHeader header = readLabels(out);
    assertEquals(3, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-999999"), header.getLabel(0).trim());
    assertDemotedForm(header.getLabel(1).trim(), "D_9999999999");
    assertEquals("ChimeraX 0.1 Wed Jan 21 12:53:36 2026", header.getLabel(2).trim());
  }

  @Test
  void evictionTallyAccumulatesAcrossMultipleRealRuns(@TempDir Path tempDir) throws Exception {
    File input = tempDir.resolve("eviction-input.map").toFile();
    String[] depositorLines = new String[8];
    for (int i = 0; i < 8; i++) depositorLines[i] = "Depositor line " + (i + 1);
    TestFixtures.writeFixture(input, depositorLines);

    String out1 = tempDir.resolve("round1.map").toString();
    runMapFixDep(input.getPath(), out1, "1.0", "D_1"); // no history yet

    String out2 = tempDir.resolve("round2.map").toString();
    runMapFixDep(out1, out2, "1.0", "D_2"); // fits exactly (10), no eviction

    String out3 = tempDir.resolve("round3.map").toString();
    runMapFixDep(out2, out3, "1.0", "D_3"); // evicts D_1 -> D_2 tally=1

    String out4 = tempDir.resolve("round4.map").toString();
    runMapFixDep(out3, out4, "1.0", "D_4"); // evicts D_2(tally=1) -> D_3 tally=2

    MapHeader header = readLabels(out4);
    assertEquals(10, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_4"), header.getLabel(0).trim());
    assertDemotedFormWithTally(header.getLabel(1).trim(), "D_3", 2);
    for (int i = 0; i < 8; i++) {
      assertEquals("Depositor line " + (i + 1), header.getLabel(2 + i).trim());
    }
  }
}
