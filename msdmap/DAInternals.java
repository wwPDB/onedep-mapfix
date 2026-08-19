package msdmap;

/* =============================================
Author  Eduardo Sanz
Date    13 March 2014
What    Interface for the D&A project
*/

import java.io.*;
import msdmap.mapread.MapHeader;
import msdmap.mapread.MapRead;
import msdmap.mapread.MapWrite;
import msdmap.Version;
import java.lang.RuntimeException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.text.Normalizer;
import org.json.JSONObject;

public class DAInternals {
  
    static List<String> errors = new ArrayList<String>();
    static List<String> warnings = new ArrayList<String>();
    static Map<String, String> headerIn = new HashMap<String, String>();
    static Map<String, String> headerOut = new HashMap<String, String>();
    static Map<String, String> headerOutLong = new HashMap<String, String>();
    static int[] histogramInValues = null;
    static float[] histogramInCategories = null;
    static int[] histogramOutValues = null;
    static float[] histogramOutCategories = null;
    static PrintStream originalOut = System.out;
    static int NBINS = 128;

    public static void setup(){
        System.setOut(System.err);
    }

    public static void convert (GetArgs getArgs, String programName) throws Exception{
        // Redirect all stdout to stderr.
        // MapFix libraries print a lot of things to the stdout.
        // Stdout will be a JSON structure
        
        FileInputStream buff = null;
        MapHeader mapHeader = new MapHeader();

        try {
            buff = new FileInputStream(getArgs.fin);

            if (buff == null) {
                errors.add("Unable to read the input map.");
                PrintJsonAndExit(-1);
            }

            if(mapHeader.read(buff) == false){
                errors.add("Problems reading the input header.");
                PrintJsonAndExit(-1);
            }

            StoreHeader(headerIn, mapHeader);
            MapRead mapRead = new MapRead(buff);
            mapRead.setEndian(mapHeader.getEndian());
            mapRead.setMapMode(mapHeader.getMapMode());

            int nCol = mapHeader.getNoFast();
            int nRow = mapHeader.getNoMedium();
            int nSec = mapHeader.getNoSlow();

            boolean noError = mapRead.mapReadBig(nCol, nRow, nSec);

            // cannot manage lost bytes in map
            if (noError == false) {
                errors.add("Incomplete reading of the input map.");
                PrintJsonAndExit(-1);
            }

            mapRead.calcInfo(nCol, nRow, nSec, mapHeader.headerSizeInByte);
            histogramInValues = mapRead.getDistribution(nCol, nRow, nSec, mapHeader.headerSizeInByte, NBINS);
            float min = mapRead.getMin();
            float max = mapRead.getMax();
            float step = (max - min) / (float) NBINS;
            histogramInCategories = new float[NBINS];
            for (int i =0; i < NBINS ;++i){
                histogramInCategories[i] = min + ((float) i * step);
            }

            // These are all the functions invoked for mapBigFix.jar "-all" option
            mapHeader.setMin(min);
            mapHeader.setMax(max);
            mapHeader.setMean(mapRead.getMean());
            mapHeader.setRMS(mapRead.getRMS());
            mapHeader.fixSpaceGroup();
            mapHeader.fixMapType();
            if (getArgs.fixLabel){
                mapHeader.changeLabel(MapHeader.makeSystemLabel(getArgs.text));
            }
            mapHeader.autoFixCellAngle();

            // This needs to happen before warning about grid sampling
            if (getArgs.fixGridSampling){
                String[] values = new String[3];
                values[0] = "" + getArgs.sx;
                values[1] = "" + getArgs.sy;
                values[2] = "" + getArgs.sz;
                int word[] = {8, 9, 10};
                mapHeader.setWord(3, word, values);
            }

            // When space group is 1 (single particle):
            //   * remove symmetry records
            //   * fix gridx, gridy, gridz
            if (mapHeader.getSpaceGroupNumber() == 1){
                boolean mismatchSampling = false;
                int word[] = new int[1];
                String value[] = new String[1];

                // Remove extra symmetry records
                if ( mapHeader.getNumberSymOps() != 0 ){
                    word[0] = 24;
                    value[0] = "0"; 
                    mapHeader.setWord(1, word, value);
                    warnings.add("Symmetry records have been deleted.");
                }

                // Automatically fix the discrepancies between number of col, row, and
                // sections (NoFast, NoMedium, NoSlow) and grid sampling
                // (GridX, GridY, GridZ)
                int newGridX = getGridSampling(mapHeader.getAxis1(), mapHeader);
                if (newGridX != mapHeader.getGridX()){
                    mismatchSampling = true;
                    if (programName.equals("mapFixDep.jar")){
                        word[0] = 8;
                        value[0] = "" + newGridX; 
                        mapHeader.setWord(1, word, value);
                    }
                }
                int newGridY = getGridSampling(mapHeader.getAxis2(), mapHeader);
                if (newGridY != mapHeader.getGridY()){
                    mismatchSampling = true;
                    if (programName.equals("mapFixDep.jar")){
                        word[0] = 9;
                        value[0] = "" + newGridY; 
                        mapHeader.setWord(1, word, value);
                    }
                }
                int newGridZ = getGridSampling(mapHeader.getAxis3(), mapHeader);
                if (newGridZ != mapHeader.getGridZ()){
                    mismatchSampling = true;
                    if (programName.equals("mapFixDep.jar")){
                        word[0] = 10;
                        value[0] = "" + newGridZ; 
                        mapHeader.setWord(1, word, value);
                    }
                }

                if(mismatchSampling){
                    if (programName.equals("mapFixDep.jar")){
                        warnings.add("Grid sampling on x, y and z has been automatically changed to be identical than the number of columns, rows and sections. It is recommended that you download the map and verify the map is correct. Click on 'Communication' in the navigation menu to inform curators about map conversion problems.");
                    }
                    if (programName.equals("mapFixAnot.jar")){
                        warnings.add("Grid sampling on x, y and z is different than the number of columns, rows and sections.");
                    }
                }
            }

            // Pixel size is preserved even after grid sampling is changed
            // Potential problem: this modify the cell dimensions.
            mapHeader.fixVoxel(getArgs.pixelX, getArgs.pixelY, getArgs.pixelZ);

            // This need to happen after call to fixVoxel function
            // Pixel size is not preserved
            if (getArgs.fixCell) mapHeader.fixCell(getArgs.cellX,getArgs.cellY,getArgs.cellZ);

            if (getArgs.fixGridStart) mapHeader.fixGridO(getArgs.ix,getArgs.iy,getArgs.iz);

            // If the origin encoded in words 50 to 52 (MRC format) are not zero, set words 5-7 to the nearest integer.
            float smallNum = 0.00001F;
            float originalX = mapHeader.getOriginXYZ(0);
            float originalY = mapHeader.getOriginXYZ(1);
            float originalZ = mapHeader.getOriginXYZ(2);
            if ( Math.abs(originalX) > smallNum || Math.abs(originalY) > smallNum || Math.abs(originalZ) > smallNum ){
                mapHeader.fixR2G();
                int newX = mapHeader.getFastOffset();
                int newY = mapHeader.getMediumOffset();
                int newZ = mapHeader.getSlowOffset();
                if ( Math.abs(originalX - newX) > smallNum || Math.abs(originalY -newY) > smallNum || Math.abs(originalZ - newZ) > smallNum ){
                    String message = "Map origin has been automatically shifted:<br/><ul>"; 
                    message += "<li>X: from " + originalX  + " to " + newX + " pixel units</li>";
                    message += "<li>Y: from " + originalY  + " to " + newY + " pixel units</li>";
                    message += "<li>Z: from " + originalZ  + " to " + newZ + " pixel units</li>";
                    message += "</ul>In order to avoid loss of precision in map position upon conversion to EMDataBank format, MRC origin position values (in Angstrom) should be cleanly divisible by the map pixel sizes (in Angstrom/pixel).<br/>";
                    message += "It is recommended that you download and inspect the converted map. Click on 'Communication' in the navigation menu to inform curators about map conversion problems.";
                    warnings.add(message);
                }
            }

            // Wipes out the MRC origin words 50-52
            mapHeader.setOriginZero();

            MapWrite mapWrite = new MapWrite();
            mapWrite.setEndian(mapHeader.getEndian());
            FileOutputStream out = mapWrite.open(getArgs.fout);
            mapWrite.write(out,mapHeader,mapHeader.headerSizeInByte,buff, mapHeader.getMapMode());

            mapWrite.close(out);
            mapRead.kill();
            mapHeader.close(buff);
            mapHeader.kill();
        
            // Read the newly created map
            buff = new FileInputStream(getArgs.fout);

            if (buff == null) {
                errors.add("Unable to read the output map.");
                PrintJsonAndExit(-1);
            }

            mapHeader = new MapHeader();
            if(mapHeader.read(buff) == false){
                errors.add("Problems reading the output header.");
                PrintJsonAndExit(-1);
            }

            StoreHeader(headerOut, mapHeader);
            StoreHeaderLong(headerOutLong, mapHeader);

            mapRead = new MapRead(buff);
            mapRead.setEndian(mapHeader.getEndian());
            mapRead.setMapMode(mapHeader.getMapMode());

            nCol = mapHeader.getNoFast();
            nRow = mapHeader.getNoMedium();
            nSec = mapHeader.getNoSlow();

            noError = mapRead.mapReadBig(nCol, nRow, nSec);

            // cannot manage lost bytes in map
            if (noError == false) {
                errors.add("Incomplete reading of the output map.");
                PrintJsonAndExit(-1);
            }

            mapRead.calcInfo(nCol, nRow, nSec, mapHeader.headerSizeInByte);
            histogramOutValues = mapRead.getDistribution(nCol, nRow, nSec, mapHeader.headerSizeInByte, NBINS);
            min = mapRead.getMin();
            max = mapRead.getMax();
            step = (max - min) / (float) NBINS;
            histogramOutCategories =  new float[NBINS];
            for (int i =0; i < NBINS ;++i){
                histogramOutCategories[i] = min + ((float) i * step);
            }

            // Detect histograms with a 'V' shape. These often results from storing values as unsigned bytes
            // Divide the histogram in 5 portions and make sure that the sum of the values in the middle portion is bigger than the /*other first and*/ last portions.
            long sumFirstPortion = 0;
            long sumMiddlePortion = 0;
            long sumLastPortion = 0;
            int numPortions = 5;
            int blockSize = NBINS/numPortions;
            /*for (int i = 0; i < blockSize; ++i){
                sumFirstPortion += histogramOutValues[i];
            }
            */
            int startMiddlePortion = blockSize * (numPortions / 2);
            int endMiddlePortion = startMiddlePortion + blockSize;
            for (int i = startMiddlePortion; i < endMiddlePortion; ++i){
                sumMiddlePortion += histogramOutValues[i];
            }
            for (int i = histogramOutValues.length - blockSize; i < histogramOutValues.length; ++i){
                sumLastPortion += histogramOutValues[i];
            }
            if (/*sumFirstPortion > sumMiddlePortion ||*/ sumLastPortion > sumMiddlePortion){
                warnings.add("Abnormal type of histogram. Check that the values in the uploaded map were not stored as unsigned bytes.");
            }

            mapRead.kill();
            mapHeader.close(buff);
            mapHeader.kill();
        }
        catch (FileNotFoundException e){
            System.err.println(e.getMessage());
            errors.add("Could not open file " + getArgs.fin);
            PrintJsonAndExit(-1);
        }
        catch (Exception e){
            System.err.println(e.getMessage());
            errors.add("Sorry there was an unknown problem.");
            for (StackTraceElement ste : e.getStackTrace()) {
                System.err.println(ste);
            }
            PrintJsonAndExit(-1);
        }

        PrintJsonAndExit(0);
    }

    static void StoreHeaderLong(Map<String, String>store, MapHeader header) throws Exception{
        if(header.getEndian() == 1){
            store.put("endian_type", "big");
        }
        else{
            store.put("endian_type", "little");
        }

        String label = new String();
        int nLabel = header.getNLabels();
        for (int i = 0; i < nLabel; ++i){
            label += header.getLabel(i).replaceAll("\\p{Cntrl}", "");
            if( i < nLabel -1 ){
                label += " ";
            }
        }
        // "label" is the current label only, matching what actually gets written to
        // _em_map.label downstream - the full block (current + changelog + depositor
        // content) is kept separately in "label_block" so nothing is lost, but callers
        // that just want the current value don't have to parse it back out of a blob.
        String currentLabel = nLabel > 0 ? header.getLabel(0).replaceAll("\\p{Cntrl}", "") : "";
        store.put("label", currentLabel);
        store.put("label_block", label);

        int nCol =  header.getNoFast();
        int nRow =  header.getNoMedium();
        int nSec =  header.getNoSlow();
        store.put("dimensions_col", "" + nCol );
        store.put("dimensions_row", "" + nRow );
        store.put("dimensions_sec", "" + nSec );

        long size = 1024;
        String mode = new String();
        switch (header.getMapMode()){
            case 0: mode = "Image stored as signed byte";
		size += (long) nCol * (long) nRow *  (long)nSec;
                    break;
            case 1: mode = "Image stored as signed integer (2 bytes)";
                    size += (long) nCol * (long) nRow * (long) nSec * 2;
                    break;
            case 2: mode = "Image stored as floating point number (4 bytes)";
                    size += (long) nCol * (long) nRow * (long) nSec * 4;
                    break;
            default: errors.add("Map mode not supported!");
                    PrintJsonAndExit(-1);
        }
        store.put("data_type", mode);

        size += header.getSymOpsBytes();
        store.put("size_kb", "" + String.valueOf(size));


        String[]  XYZmap = {"", "X", "Y", "Z"};
        store.put("axis_order_fast", XYZmap[header.getAxis1()]);
        store.put("axis_order_medium", XYZmap[header.getAxis2()]);
        store.put("axis_order_slow", XYZmap[header.getAxis3()]);

        
        int gridX = header.getGridX();
        int gridY = header.getGridY();
        int gridZ = header.getGridZ();
        store.put("spacing_x", "" + gridX );
        store.put("spacing_y", "" + gridY );
        store.put("spacing_z", "" + gridZ );

        float cellX = header.getCellA();
        float cellY = header.getCellB();
        float cellZ = header.getCellC();
        float alpha = header.getCellAlpha();
        float beta = header.getCellBeta();
        float gamma = header.getCellGamma();
        store.put("cell_a", "" + cellX );
        store.put("cell_b", "" + cellY );
        store.put("cell_c", "" + cellZ );
        store.put("cell_alpha", "" + alpha );
        store.put("cell_beta", "" + beta );
        store.put("cell_gamma", "" + gamma );

        float pixelX = cellX/(float) gridX;
        float pixelY = cellY/(float) gridY;
        float pixelZ = cellZ/(float) gridZ;
        store.put("pixel_spacing_x", "" + pixelX );
        store.put("pixel_spacing_y", "" + pixelY );
        store.put("pixel_spacing_z", "" + pixelZ );

        int nFastOffset = header.getFastOffset();
        int nMediumOffset = header.getMediumOffset();
        int nSlowOffset = header.getSlowOffset();
        store.put("origin_col", "" + nFastOffset);
        store.put("origin_row", "" + nMediumOffset);
        store.put("origin_sec", "" + nSlowOffset);
        store.put("limit_col", "" + (nFastOffset + nCol -1));
        store.put("limit_row", "" + (nMediumOffset + nRow - 1));
        store.put("limit_sec", "" + (nSlowOffset + nSec -1 ));
        store.put("statistics_minimum", "" + header.getMinDensity());
        store.put("statistics_maximum", "" + header.getMaxDensity());
        store.put("statistics_average", "" + header.getMeanDensity());
        store.put("statistics_std", "" + header.getRMSD());
        store.put("symmetry_space_group", "" + header.getSpaceGroupNumber());
        store.put("format", "CCP4" );

    }

    static void StoreHeader(Map<String, String>store, MapHeader header) throws Exception{
        if(header.getEndian() == 1){
            store.put("Map endianness", "Big endian");
        }
        else{
            store.put("Map endianness", "Little endian");
        }

        String label = new String();
        int nLabel = header.getNLabels();
        if( nLabel > 10){
            warnings.add("Number of text labels surpasses the limit (10) based on the format specifications. Extra information in the header file will be lost. The FEI-MRC map format is a common case for this type of problem.");
        }

        for (int i = 0; i < nLabel; ++i){
            label += header.getLabel(i).replaceAll("\\p{Cntrl}", "");
            if (label.indexOf("Chimera rotation") != -1){
                errors.add("Chimera rotation detected in the label. This type of maps are not allowed! Please upload alternative map already translated and rotated.");
                PrintJsonAndExit(-1);
            }
            if( i < nLabel -1 ){
                label += " ";
            }
        }
        // "Map title" is the current label only - it's shown verbatim to depositors in
        // the DepUI upload summary, so it must not include changelog/depositor lines.
        // The Chimera-rotation scan above still runs over every line, unaffected.
        String currentLabel = nLabel > 0 ? header.getLabel(0).replaceAll("\\p{Cntrl}", "") : "";
        // Escape all double quotes for conversion to JSON
        store.put("Map title", currentLabel.replace("\"", "\\\""));
        
        String mode = new String();
        switch (header.getMapMode()){
            case 0: mode = "Image stored as signed bytes"; 
                    break;
            case 1: mode = "Image stored as signed integer (2 bytes)";
                    break;
            case 2: mode = "Image stored as floating point number (4 bytes)";
                    break;
            default: errors.add("Map mode not supported!");
                    PrintJsonAndExit(-1);
        }
        store.put("Map mode", mode);

        String[]  XYZmap = {"", "X", "Y", "Z"};
        store.put("Fast, medium and slow axes", XYZmap[header.getAxis1()] + ", " + XYZmap[header.getAxis2()] + ", " + XYZmap[header.getAxis3()]);

        int nCol =  header.getNoFast();
        int nRow =  header.getNoMedium();
        int nSec =  header.getNoSlow();
        store.put("Number of columns, rows, and sections", "" + nCol + ", " + nRow + ", " + nSec);
        
        int gridX = header.getGridX();
        int gridY = header.getGridY();
        int gridZ = header.getGridZ();
        store.put("Grid sampling on x, y, and z", "" + gridX + ", " + gridY + ", " + gridZ);

        float cellX = header.getCellA();
        float cellY = header.getCellB();
        float cellZ = header.getCellC();
        float alpha = header.getCellAlpha();
        float beta = header.getCellBeta();
        float gamma = header.getCellGamma();
        store.put("Cell dimensions (x, y, and z, alpha, beta, gamma)", "" + cellX + ", " + cellY + ", " + cellZ + ", " + "" + alpha+ ", " + beta+ ", " + gamma);

        float pixelX = cellX/(float) gridX;
        float pixelY = cellY/(float) gridY;
        float pixelZ = cellZ/(float) gridZ;
        store.put("Pixel sampling on x, y, and z", "" + pixelX + ", " + pixelY + ", " + pixelZ);

        store.put("Start points on columns, rows, and sections", "" + header.getFastOffset() + ", " + header.getMediumOffset() + ", " + header.getSlowOffset());
        store.put("Minimum density", "" + header.getMinDensity());
        store.put("Maximum density", "" + header.getMaxDensity());
        store.put("Average density", "" + header.getMeanDensity());
        store.put("RMS deviation from mean density", "" + header.getRMSD());
        store.put("Space group number", "" + header.getSpaceGroupNumber());

        float smallNum = 0.00001F;
        float originalX = header.getOriginXYZ(0);
        float originalY = header.getOriginXYZ(1);
        float originalZ = header.getOriginXYZ(2);
        if ( Math.abs(originalX) > smallNum || Math.abs(originalY) > smallNum || Math.abs(originalZ) > smallNum ){
            store.put("Origin in MRC format", "" + originalX + ", " + originalY + ", " + originalZ);
        }
        else {
            store.put("Origin in MRC format", "0.0, 0.0, 0.0");
        }
    }

    static void PrintJsonAndExit(int status){

        JSONObject out = new JSONObject();
        out.put("errors", errors);
        out.put("warnings", warnings);
        if (histogramInValues != null) out.put("input_histogram_values", histogramInValues);
        if (histogramInCategories != null) out.put("input_histogram_categories", histogramInCategories);
        if (histogramOutValues != null) out.put("output_histogram_values", histogramOutValues);
        if (histogramInCategories!= null) out.put("output_histogram_categories", histogramOutCategories);
        out.put("input_header", headerIn);
        out.put("output_header", headerOut);
        out.put("output_header_long", headerOutLong);

        String data = out.toString();
        // Flush before we redirect back to the original stdout
        System.out.flush(); 
        System.setOut(originalOut);

        System.out.println(data);
        System.exit(status);
    }

    static String PrintListJSon(String name, List<String> list){
        int size = list.size();
        String out = PrintInitation(name, size, true);

        for (int i = 0; i < size; i++){
            out += PrintValue( "\"" + list.get(i) + "\"", i, size);
        }

        out += PrintTermination(size, true, false);

        return out;
    }

    static String PrintListJSon(String name, int[] list){
        int size = list.length;
        String out = PrintInitation(name, size, true);

        for (int i = 0; i < size; i++){
            out += PrintValue( ""+ list[i], i, size);
        }

        out += PrintTermination(size, true, false);

        return out;
    }

    static String PrintListJSon(String name, float[] list){
        int size = list.length;
        String out = PrintInitation(name, size, true);

        for (int i = 0; i < size; i++){
            out += PrintValue( String.format("%.2f", list[i]), i, size);
        }

        out += PrintTermination(size, true, false);

        return out;
    }

    static String PrintDictJSon(String name, Map<String, String> map, boolean isLast){
        int size = map.size();
        String out = PrintInitation(name, size, false);

        int i = 0;
        for (Map.Entry<String, String> entry: map.entrySet()){
            out += PrintValue("\""+entry.getKey() + "\": \"" + entry.getValue() + "\"", i, size);
            i += 1;
        }

        out += PrintTermination(size, false, isLast);

        return out;
    }

    static String PrintInitation(String name, int size, boolean isList){
        String out = new String();
        if (size <= 0){
            if (isList){
                return "    \"" + name + "\": [";
            }
            else {
                return "    \"" + name + "\": {";
            }
        }
        else {
            if (isList){
                return "    \"" + name + "\": [\n";
            }
            else {
                return "    \"" + name + "\": {\n";
            }
        }
    }

    static String PrintTermination(int size, boolean isList, boolean isLast){
        String out = new String();

        if (size > 0){
            out = "    ";
        }

        if (isLast){
            if (isList){
                return out + "]\n";
            }
            else {
                return out + "}\n";
            }
        }
        else {
            if (isList){
                return out + "],\n";
            }
            else {
                return out + "},\n";
            }
        }
    }

    static String PrintValue(String value, int current, int size){
        String out = "        "+ value;

        if (current < size -1){
            out += ",\n";
        }
        else {
            out += "\n";
        }

        return out;
    }


    static int getGridSampling (int order, MapHeader mapHeader) throws Exception{
        switch (order){
            case 1: return mapHeader.getNoFast();
            case 2: return mapHeader.getNoMedium();
            case 3: return mapHeader.getNoSlow();
            default: throw new Exception("Invalid value for axis order");
        }
    }
}
