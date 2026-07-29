package msdmap.mapread;

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

  private static void runMapFixDep(String in, String out, String voxel, String label) throws Exception {
    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    ProcessBuilder pb = new ProcessBuilder(
        javaBin, "-jar", JAR,
        "-in", in, "-out", out,
        "-voxel", voxel, voxel, voxel,
        "-label", label);
    pb.redirectErrorStream(true);
    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes());
    int exit = process.waitFor();
    assertEquals(0, exit, "mapFixDep.jar exited non-zero. Output:\n" + output);
  }

  private static MapHeader readLabels(String path) throws Exception {
    MapHeader header = new MapHeader();
    FileInputStream in = header.open(path);
    header.read(in);
    header.close(in);
    return header;
  }

  @Test
  void relionUploadKeepsDepositorLabelAfterSystemLabel(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("relion.converted.map").toString();
    runMapFixDep("sample-data/D_1292121466_em-volume-upload_P1.map.V1", out,
        "1.2194290161132812", "D_1292121466");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1292121466"), header.getLabel(0).trim());
    assertTrue(header.getLabel(1).trim().startsWith("Relion"));
  }

  @Test
  void chimeraxUploadKeepsDepositorLabelAfterSystemLabel(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("chimerax.converted.map").toString();
    runMapFixDep("sample-data/D_1292153729_em-volume-upload_P1.map.V1", out,
        "1.384615421295166", "D_1292153729");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("D_1292153729"), header.getLabel(0).trim());
    assertTrue(header.getLabel(1).trim().startsWith("ChimeraX"));
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
  void alreadySystemLabelledInputIsReplacedNotDuplicated(@TempDir Path tempDir) throws Exception {
    String out = tempDir.resolve("relabel.converted.map").toString();
    runMapFixDep("sample-data/D_1292121466_em-volume-upload_P1.map.V2", out,
        "1.2194000482559204", "EMD-112358");

    MapHeader header = readLabels(out);
    assertEquals(1, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-112358"), header.getLabel(0).trim());
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
  void replacementFixtureSwapsSystemLabelWithoutLosingDepositorLine(@TempDir Path tempDir) throws Exception {
    File input = tempDir.resolve("replacement-input.map").toFile();
    TestFixtures.writeFixture(input,
        MapHeader.makeSystemLabel("D_9999999999"),
        "ChimeraX 0.1 Wed Jan 21 12:53:36 2026");

    String out = tempDir.resolve("replacement.converted.map").toString();
    runMapFixDep(input.getPath(), out, "1.0", "EMD-999999");

    MapHeader header = readLabels(out);
    assertEquals(2, header.getNLabels());
    assertEquals(MapHeader.makeSystemLabel("EMD-999999"), header.getLabel(0).trim());
    assertEquals("ChimeraX 0.1 Wed Jan 21 12:53:36 2026", header.getLabel(1).trim());
  }
}
