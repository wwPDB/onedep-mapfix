package msdmap.mapread;

/*
Author    T.J.Oldfield
Version   1.0
Date      23-April-2007
Action    To read the CCP4 map header and hold this as a object

Updates -- 
         28-May-2014 jdw remove '-' characters from print statements -- 
*/

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.channels.FileChannel;
import java.lang.Exception;

/* class to read the CCP4 map header

  This should be a bunch of holorith constants with fortran separater
 characters.  The map file is sequential read access binary.

*/

public class MapHeader {

// endian : order of bytes
// 0 = big endian,  1 = little endian
  private boolean firstRead = true;
  private int endian = 1;
  private int BAD = -10000000;
  private boolean stripBumCharacters = false;
  private boolean symWrong = false;
  private int nNullInString = 0;

  public long headerSizeInByte = 0;

// dataType definition
  private int BYTE = 0;
  private int INT2 = 1;
  private int REAL4 = 2;
  private int COMPLEX2 = 3; //  complex integer*2
  private int COMPLEX4 = 4; //  complex real*4

  private int nFast = 0;   // 0 
  private int nMedium = 0;
  private int nSlow = 0;
  private int mode = -1;   // 3
  private int nFastOffset = 0;
  private int nMediumOffset = 0;
  private int nSlowOffset = 0;  // 6
  private int nX = 0;      // depends on the axis order (iuvw)
  private int nY = 0;      // for mapping to nFast etc
  private int nZ = 0;      // 9
  private float cellX = 0.0F;  // the 6 cell dimension
  private float cellY = 0.0F;
  private float cellZ = 0.0F; // 12
  private float alpha = 0.0F;
  private float beta = 0.0F;
  private float gamma = 0.0F; // 15
  private int[] iuvw = new int[3];  // axis mapping order;   // 16-18
  private float minDensity = 0.0F;
  private float maxDensity = 0.0F;
  private float meanDensity = 0.0F;
  private float RMS = 0.0F;             // RMSD from mean; 22
  private int spaceGroup = 0;
  private int nByteForSymOp = 0;
  private int flagSkew = 0;             // 25
  float[] skewMatrix = new float[9];    // 26-34
  float[] skewTrans = new float[3];     // 35-37
  float[] originXYZ = new float[3];    //  38-40
  int[] reserved = new int[15];        //  41-55
  String fileTypeCheck = null; // should be MAP
  byte[] machineStamp = new byte[4];  // machine that wrote the file stamp
  private int nLabel = 0;              // number of label lines
  String label[] = null;       // map label header
  String symop = null;
  
  public MapHeader() { 
  }

  public void setWord(int nWord, int[] record, String[] val) throws Exception{

    int iret = 0;
    float dret = 0.0F;
    int ok = -1;
    int i;

    for (i = 0; i < nWord; i++) {
    ok = -1;
//    System.out.println(" Setting word " + record[i] + " to " + val[i]);
 
    try {
      iret = Integer.parseInt(val[i]);
//      System.out.println(" Setting word " + record[i] + " to an integer (or double) " + iret);
      ok = 1;
    } catch (NumberFormatException ex) {
      ;
    }

    if (ok < 0 ) {
      try {
        dret = (float)(Double.parseDouble(val[i]));
//        System.out.println(" Setting word " + record[i] + " to an double " + dret);
        ok = 2;
      } catch (NumberFormatException ex) {
        ;
      }
    }

    if (ok < 0) {
//        System.out.println(" Setting word " + record[i] + " to a string " + val[i]);
        ok = 3;
    }

    if (record[i] < 1 || record[i] > 256) {
      System.out.println(" You cannot modify words outside the header " + record[i]);
      throw new Exception();
    }

    switch(record[i]){
      case 1: { System.out.print(" Setting nFast to " + val[i]); if (ok == 1) { nFast = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 2: { System.out.print(" Setting nMedium to " + val[i]); if (ok == 1) { nMedium = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 3: { System.out.print(" Setting nSLow to " + val[i]); if (ok == 1) { nSlow = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 4: { System.out.print(" Setting mapMode to " + val[i]); if (ok == 1) { mode = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 5: { System.out.print(" Setting nFastOffset to " + val[i]); if (ok == 1) { nFastOffset = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 6: { System.out.print(" Setting nMediumOffset to " + val[i]); if (ok == 1) { nMediumOffset = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 7: { System.out.print(" Setting nSlowOffset to " + val[i]); if (ok == 1) { nSlowOffset = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 8: { System.out.print(" Setting nX to " + val[i]); if (ok == 1) { nX = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 9: { System.out.print(" Setting nY to " + val[i]); if (ok == 1) { nY = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 10: { System.out.print(" Setting nZ to " + val[i]); if (ok == 1) { nZ = iret; System.out.println(" : done "); } else { System.out.println(" with non-integer :invalid "); throw new Exception(); } break; }
      case 11: { System.out.print(" Setting CellX to " + val[i]); if (ok == 1 || ok == 2) { cellX = dret; System.out.println(" : done "); } else { System.out.println(" with string :invalid "); throw new Exception(); } break; }
      case 12: { System.out.print(" Setting CellY to " + val[i]); if (ok == 1 || ok == 2) { cellY = dret; System.out.println(" : done "); } else { System.out.println(" with string :invalid "); throw new Exception(); } break; }
      case 13: { System.out.print(" Setting cellZ to " + val[i]); if (ok == 1  || ok == 2) { cellZ = dret; System.out.println(" : done "); } else { System.out.println(" with string :invalid "); throw new Exception(); } break; }
      case 14: { System.out.print(" Setting alpha to " + val[i]); if (ok == 1 || ok == 2) { alpha = dret; System.out.println(" : done "); } else { System.out.println(" with string :invalid "); throw new Exception(); } break; }
      case 15: { System.out.print(" Setting beta to " + val[i]); if (ok == 1 || ok == 2) { beta = dret; System.out.println(" : done "); } else { System.out.println("  with string : invalid"); throw new Exception(); } break; }
      case 16: { System.out.print(" Setting gamma to " + val[i]); if (ok == 1 || ok == 2) { gamma = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 17: { System.out.print(" Setting axis-order-1 to " + val[i]); if (ok == 1) { iuvw[0] = iret; System.out.println(" : done "); } else { System.out.println(" with string :invalid"); throw new Exception(); } break; }
      case 18: { System.out.print(" Setting axis-order-2 to " + val[i]); if (ok == 1) { iuvw[1] = iret; System.out.println(" : done "); } else { System.out.println(" with string :invalid"); throw new Exception(); } break; }
      case 19: { System.out.print(" Setting axis-order-3 to " + val[i]); if (ok == 1) { iuvw[2] = iret; System.out.println(" : done "); } else { System.out.println(" with string :invalid"); throw new Exception(); } break; }
      case 20: { System.out.print(" Setting min-density to " + val[i]); if (ok == 1 || ok == 2) { minDensity = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 21: { System.out.print(" Setting max-density to " + val[i]); if (ok == 1 || ok == 2) { maxDensity = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 22: { System.out.print(" Setting mean-density to " + val[i]); if (ok == 1 || ok == 2) { meanDensity = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 23: { System.out.print(" Setting space-group to " + val[i]); if (ok == 1) { spaceGroup = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 24: { System.out.print(" Setting num-Byte-For-SymOp to " + val[i]); if (ok == 1) { nByteForSymOp = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 25: { System.out.print(" Setting skew-flag to " + val[i]); if (ok == 1) { flagSkew = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 26: { System.out.print(" Setting skew-matrix[1,1] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[0] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 27: { System.out.print(" Setting skew-matrix[1,2] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[1] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 28: { System.out.print(" Setting skew-matrix[1,3] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[2] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 29: { System.out.print(" Setting skew-matrix[2,1] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[3] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 30: { System.out.print(" Setting skew-matrix[2,2] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[4] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 31: { System.out.print(" Setting skew-matrix[2,3] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[5] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 32: { System.out.print(" Setting skew-matrix[3,1] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[6] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 33: { System.out.print(" Setting skew-matrix[3,2] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[7] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 34: { System.out.print(" Setting skew-matrix[3,3] to " + val[i]); if (ok == 1 || ok == 2) { skewMatrix[8] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 35: { System.out.print(" Setting skew-translation[1] to " + val[i]); if (ok == 1 || ok == 2) { skewTrans[0] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 36: { System.out.print(" Setting skew-translation[2] to " + val[i]); if (ok == 1 || ok == 2) { skewTrans[1] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 37: { System.out.print(" Setting skew-translation[3] to " + val[i]); if (ok == 1 || ok == 2) { skewTrans[2] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 38: { System.out.print(" Setting reserved[1] to " + val[i]); if (ok == 1 || ok == 2) { reserved[0] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 39: { System.out.print(" Setting reserved[2] to " + val[i]); if (ok == 1 || ok == 2) { reserved[1] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 40: { System.out.print(" Setting reserved[3] to " + val[i]); if (ok == 1 || ok == 2) { reserved[2] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 41: { System.out.print(" Setting reserved[4] to " + val[i]); if (ok == 1 || ok == 2) { reserved[3] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 42: { System.out.print(" Setting reserved[5] to " + val[i]); if (ok == 1 || ok == 2) { reserved[4] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 43: { System.out.print(" Setting reserved[6] to " + val[i]); if (ok == 1 || ok == 2) { reserved[5] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 44: { System.out.print(" Setting reserved[7] to " + val[i]); if (ok == 1 || ok == 2) { reserved[6] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 45: { System.out.print(" Setting reserved[8] to " + val[i]); if (ok == 1 || ok == 2) { reserved[7] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 46: { System.out.print(" Setting reserved[9] to " + val[i]); if (ok == 1 || ok == 2) { reserved[8] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 47: { System.out.print(" Setting reserved[10] to " + val[i]); if (ok == 1 || ok == 2) { reserved[9] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 48: { System.out.print(" Setting reserved[11] to " + val[i]); if (ok == 1 || ok == 2) { reserved[10] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 49: { System.out.print(" Setting reserved[12] to " + val[i]); if (ok == 1 || ok == 2) { reserved[11] = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 50: { System.out.print(" Setting origin-float[1] to " + val[i]); if (ok == 1 || ok == 2) { originXYZ[0] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 51: { System.out.print(" Setting origin-float[2] to " + val[i]); if (ok == 1 || ok == 2) { originXYZ[1] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 52: { System.out.print(" Setting origin-float[3] to " + val[i]); if (ok == 1 || ok == 2) { originXYZ[2] = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 53: { System.out.print(" Setting file-type to " + val[i]); if (ok == 1 || ok == 2 || ok ==3) { fileTypeCheck = val[i]; System.out.println(" : done "); } else { System.out.println(" with something odd : invalid"); throw new Exception(); } break; }
//      case 54: { System.out.print(" Setting machine-type to " + val[i]); if (ok == 1 || ok == 2 || ok ==3) { machineStamp = (byte[])val[i]; System.out.println(" : done "); } else { System.out.println(" with something odd : invalid"); throw new Exception(); } break; }
      case 55: { System.out.print(" Setting RMS to " + val[i]); if (ok == 1 || ok == 2) { RMS = dret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      case 56: { System.out.print(" Setting number of title lines to " + val[i]); if (ok == 1) { nLabel = iret; System.out.println(" : done "); } else { System.out.println(" with string : invalid"); throw new Exception(); } break; }
      default : { System.out.println(" Sorry - not done the header label " + record[i]); throw new Exception(); } 

    }
    }
  }

  public void setEndian(int mode) {
    endian = mode;
// 0 : set to little endian, 1 : set to big endian
  }
  public int getEndian() { return endian; }

  public int getNoFast() { return nFast; }
  public int getNoMedium() { return nMedium; }
  public int getNoSlow() { return nSlow; }
  public int getMapMode() { return mode; }  // ie byte/int2/int/real
  public int getFastOffset() { return nFastOffset; }
  public int getMediumOffset() { return nMediumOffset; }
  public int getSlowOffset() { return nSlowOffset; }
  public int getGridX() { return nX; }
  public int getGridY() { return nY; }
  public int getGridZ() { return nZ; }
  public int getMaxX() { return (nFast+nFastOffset-1); }
  public int getMaxY() { return (nMedium+nMediumOffset-1); }
  public int getMaxZ() { return (nSlow+nSlowOffset-1); }
  public float getCellA() { return cellX; }
  public float getCellB() { return cellY; }
  public float getCellC() { return cellZ; }
  public float getCellAlpha() { return alpha; }
  public float getCellBeta() { return beta; }
  public float getCellGamma() { return gamma; }
  public int[] getAxisOrder() { return iuvw; }
  public int   getAxis1() { return iuvw[0]; }
  public int   getAxis2() { return iuvw[1]; }
  public int   getAxis3() { return iuvw[2]; }
  public float getMinDensity() { return minDensity; }
  public float getMaxDensity() { return maxDensity; }
  public float getMeanDensity() { return meanDensity; }
  public float getRMSD() { return RMS; }
  public int   getSpaceGroupNumber() { return spaceGroup; }
  public boolean isSkew() { if (flagSkew == 0) return false; else return true; }
  public int getSkew() { return flagSkew; }
  public float[] getSkewMatrix() { return skewMatrix; }
  public float getOriginXYZ(int n) { return originXYZ[n]; }
  public float getSkewMatrix(int n) { return skewMatrix[n]; }
  public float[] getSkewTransformation() { return skewTrans; }
  public float getSkewTransformation(int n) { return skewTrans[n]; }
  public int[] getReserved() { return reserved; }
  public int getReserved(int n) { return reserved[n]; }
  public String getFileTypeCheck() { return fileTypeCheck; } // should return "MAP "
  public byte[] getMachineStamp() { return machineStamp; } 
  public int   getNLabels() { return nLabel; }
  public String getLabel(int n) { 
    if (label == null) return "No labels in file";
    if (n < 0) return "label " + n + " is too small";
    else if (n >= nLabel) return "label " + n + " is large";
    else return label[n];
  }
  public boolean isSymOpListed() { if (nByteForSymOp > 0) return true; else return false; }
  public int   getNumberSymOps() { return nByteForSymOp/80; } // hum...
  public int   getSymOpsBytes() { return nByteForSymOp; } // hum...
  public String getSymop() { return symop; } 

  public FileInputStream open(String filename) {
    return open(filename,false);
  }

  public FileInputStream open(String filename, boolean ignoreSymOpBytes) {
//  method to open a map, returns the bufferedReader or NULL

    try {
      FileInputStream file = new FileInputStream(filename);
      return file;
    } catch (FileNotFoundException e) {
      System.err.println("Could not open file " + filename);
      return null;
    }

  }

  public boolean read(FileInputStream buff) {
     return read(buff,false);
  }

  public boolean read(FileInputStream buff, boolean ignoreSymOpBytes) {
// Reads the words from the header as defined in the CCP4 documentation
//  http://www.ccp4.ac.uk/dist/html/maplib.html#mrdhdr

      int i,j;
      String temp = null;
      symWrong = false;
      nNullInString = 0;

     if ((nFast = readInteger(buff)) == BAD) return false;
     if (endian == 0) 
       System.out.println(" Map Endian is big");
     else
       System.out.println(" Map Endian is little");
     if ((nMedium = readInteger(buff)) == BAD) return false;
     if ((nSlow = readInteger(buff)) == BAD) return false;
     if ((mode = readInteger(buff)) == BAD) return false;
     if ((nFastOffset = readInteger(buff)) == BAD) return false;
     if ((nMediumOffset = readInteger(buff)) == BAD) return false;
     if ((nSlowOffset = readInteger(buff)) == BAD) return false;
     if ((nX = readInteger(buff)) == BAD) return false;
     if ((nY = readInteger(buff)) == BAD) return false;
     if ((nZ = readInteger(buff)) == BAD) return false;
     if ((cellX = readFloat(buff)) == BAD) return false;
     if ((cellY = readFloat(buff)) == BAD) return false;
     if ((cellZ = readFloat(buff)) == BAD) return false;
     if ((alpha = readFloat(buff)) == BAD) return false;
     if ((beta = readFloat(buff)) == BAD) return false;
     if ((gamma = readFloat(buff)) == BAD) return false;
     if ((iuvw[0] = readInteger(buff)) == BAD) { iuvw[0] = 0; return false; }
     if ((iuvw[1] = readInteger(buff)) == BAD) { iuvw[1] = 0; return false; }
     if ((iuvw[2] = readInteger(buff)) == BAD) { iuvw[2] = 0; return false; }
     if ((minDensity = readFloat(buff)) == BAD) return false;
     if ((maxDensity = readFloat(buff)) == BAD) return false;
     if ((meanDensity = readFloat(buff)) == BAD) return false;
     if ((spaceGroup = readInteger(buff)) == BAD) return false;
     if ((nByteForSymOp = readInteger(buff)) == BAD) return false;
     if ((flagSkew = readInteger(buff)) == BAD) return false; // up to 25
     for (i = 0; i < 9; i++) skewMatrix[i] = readFloat(buff); // up to 34
     for (i = 0; i < 3; i++) skewTrans[i] = readFloat(buff); // up to 37
     for (i = 0; i < 12; i++) reserved[i] = readInteger(buff);   // expansion for future
     for (i = 0; i < 3; i++) originXYZ[i] = readFloat(buff);   // Looks like EM have expanded this to use as a real origin mark


     fileTypeCheck = readWord(buff);
     readMachineStamp(buff);
     if ((RMS = readFloat(buff)) == BAD) return false;
     if ((nLabel = readInteger(buff)) == BAD) return false;
     if (nLabel > 10) {
       System.out.println(" nLabel is too large (" + nLabel + ") fix = 10");
       nLabel = 10;
     }
//  just make it 10  incase we make up  a new label
     label = new String[10];
//  read the header title cards , notice we must read the words anyway
     for (j = 0; j < 10; j++) {
       for (i = 0; i < 20; i++) {
         temp = readWord(buff);
         if (j < nLabel) {
           if (i == 0) label[j] = temp;
           else label[j] = label[j].concat(temp);
         } 
       }
     }
//  read the symop lines 
// no other program appears to get this far - integrity check

     headerSizeInByte = 4 * 56 + 10 * 80;
     if (!ignoreSymOpBytes) {
       if (nByteForSymOp > 0){
          symop = readWordOfLength(nByteForSymOp,buff);
          headerSizeInByte += nByteForSymOp;
       }
      }

      System.out.println(" Header size = " + headerSizeInByte);

/*    try {
      FileChannel fileChannel = buff.getChannel();
      headerSizeInByte = fileChannel.position();
      System.out.println(" Header size = " + headerSizeInByte);
    } catch (Exception e) {
      System.out.println(" Unable to reposition file pointer ");
      throw new Exception();
    } */
 

     System.out.println(" Completed read of Header successfully");
     if (symWrong) {
       if (nByteForSymOp > 0) {
         System.out.println(" =================== WARNING ====================");
         System.out.println(" The Header word 24 (No of symop bytes) indicates there ");
         System.out.println(" are " + nByteForSymOp + " bytes for the symop data. Invalid");
         System.out.println(" data was read here - probably map data : the map maybe truncated");
         System.out.println(" =================== WARNING ====================");
       } else {
         System.out.println(" =================== WARNING ====================");
         System.out.println(" Either map-type, machine-type or header contains");
         System.out.println(" invalid character data - this has been set to a <space>");
         System.out.println(" =================== WARNING ====================");
       }
     }
     if (nNullInString > 0) {
       System.out.println(" =================== WARNING ====================");
       System.out.println(" The byte = 0 was found " + nNullInString + " times in ");
       System.out.println(" the textual data (header etc).  This is not valid");
       System.out.println(" but most programs/computer language will not mind");
       System.out.println(" =================== WARNING ====================");
     }
     System.out.println(" ");
     
     return true;
  }

  public void specialFixMethod(int specialMode, int specialFix) throws Exception{

      if (specialMode == 1) {
        System.out.println(" Set the cell dimensions to 275.275");
        cellX = 275.275F;
        cellY = 275.275F;
        cellZ = 275.275F;
        nX = 121;
        nY = 121;
        nZ = 121;
      } else {
        System.out.println(" Undefined special fix " + specialMode);
        throw new Exception();
      }
  }
         
 
  public void checkBadData(boolean fixVoxel) throws Exception{

     if (Math.abs(originXYZ[0]) > 0.001 || Math.abs(originXYZ[1]) > 0.001 || Math.abs(originXYZ[2]) > 0.001) {
       System.out.println(" WARNING : input map has MRC map origin encoded in words 50-52");
       System.out.println("    This OK if this is a MRC map, and probably wrong if this is a CCP4 map");
     }

     if (fixVoxel) {
// Provided voxel fix - so attempt to fix things
       if (cellX < 1.0 || cellY < 1.0 || cellZ < 1.0 || nX < 1 || nY < 1 || nZ < 1) {
         System.out.println(" WARNING - Attempting to recover cell data as something is bad- CHECK PLEASE ");
         System.out.println(" cellX/Y/Z " + cellX + ", " + cellY + ", " + cellZ);
         System.out.println(" nX/Y/Z " + nX + ", " + nY + ", " + nZ);
         if (iuvw[0] == 1 && iuvw[1] == 2 && iuvw[2] == 3) {
// Only works for mapmode = 1 -   ahhh....  
           if (nFast < 1 || nMedium < 1 || nSlow < 1) {
             System.out.println(" Cannot repair as nFast/nMedium/nSlow are bad " + nFast + ", " + nMedium + ", " + nSlow);
             throw new Exception();
           }
// fix the dimensions based on the grid dimensions in slow/medium/fast
// the voxel size will be fixed in the explicit voxel routine
           nX = nFast;
           nY = nMedium;
           nZ = nSlow;
           System.out.println(" Have set nX,nY,nZ - check voxel fix too " + nX + ", " + nY + ", " + nZ);
         } else {
           System.out.println(" Sorry to difficult - iuvw is not 1/2/3 " + iuvw[0] + ", " + iuvw[1] + ", " + iuvw[2]);
           throw new Exception();
         }
       }
     } else {
// no voxel fix, just return exception for bad cell data
       if (cellX < 1.0 || cellY < 1.0 || cellZ < 1.0 || nX < 1 || nY < 1 || nZ < 1) {
         System.out.println(" ERROR - Must stop as the header cell data is bad ");
         System.out.println(" This can be fixed using -voxel   as this resets the cell dimensions ");
         System.out.println(" The cell dimensions and grid sizes must be sensible found one value < 1.0 " );
         System.out.println(" cellX/Y/Z " + cellX + ", " + cellY + ", " + cellZ);
         System.out.println(" nX/Y/Z " + nX + ", " + nY + ", " + nZ);
         throw new Exception();
       }
     }

     System.out.println(" Voxel sizes = " + cellX / (float)(nX) + ", " + cellY/(float)nY + ", " + cellZ/(float)nZ);

  }

  public void readMachineStamp(FileInputStream buff) {

// read the machine stamp as bytes

    try {
      buff.read(machineStamp, 0,4);
    } catch (Exception e) {
       System.out.println("readMachineStamp " + e);
    }

// 0 = big endian,  1 = little endian
    if (endian == 1) {
      if (machineStamp[0] != 68 || machineStamp[1] != 65 || machineStamp[2] != 0|| machineStamp[3] != 0) {
        System.out.println(" setting machine stamp to <little-endian>:44,41,00,00,  it was " + Integer.toString(machineStamp[0],16) +  "," +Integer.toString(machineStamp[1],16) +  "," +Integer.toString(machineStamp[2],16) + "," + Integer.toString(machineStamp[3],16));

        machineStamp[0] = 68;
        machineStamp[1] = 65;
        machineStamp[2] = 00;
        machineStamp[3] = 00;
      }
    } else if (endian == 0) {
      if (machineStamp[0] != 17 || machineStamp[1] != 17 || machineStamp[2] != 0|| machineStamp[3] != 0) {
        System.out.println(" setting machine stamp to <big-endian>:11,11,00,00,  it was " + Integer.toString(machineStamp[0],16) +  "," +Integer.toString(machineStamp[1],16) +  "," +Integer.toString(machineStamp[2],16) + "," + Integer.toString(machineStamp[3],16));
        machineStamp[0] = 17;
        machineStamp[1] = 17;
        machineStamp[2] = 00;
        machineStamp[3] = 00;
      }
    } else {
      System.out.println(" Unknown endian " + endian);
    }

  }
 
  public void fixVoxel(float x, float y, float z){
    System.out.println(" Fixing voxel using parameters " + x + "," + y + "," + z);
// changes the cellX/Y/Z to match EM voxel size with nx/y/z constant

    System.out.println(" Changing cell dimensions based on voxel sizes : ");
    System.out.println(" CellX " + cellX + " changed to " +  (nX * x));
    System.out.println(" CellY " + cellY + " changed to " +  (nY * y));
    System.out.println(" CellZ " + cellZ + " changed to " +  (nZ * z));
    cellX = nX * x;
    cellY = nY * y;
    cellZ = nZ * z;
    
  }

  public void changeLabel(String text) {
    System.out.println(" Changing title by prepending line = <" + text + ">");

    if (label == null) label = new String[10];

    String newLabel = formatLabel(text);
    String newLabelTrimmed = newLabel.trim();
    String oldLabel[] = label;
    int oldNLabel = nLabel;

    label = new String[10];
    label[0] = newLabel;
    nLabel = 1;

    for (int i = 0; i < oldNLabel && nLabel < 10; i++) {
      if (oldLabel[i] == null) continue;
      String oldLabelTrimmed = oldLabel[i].trim();
      if (oldLabelTrimmed.length() == 0) continue;
      if (oldLabelTrimmed.equals(newLabelTrimmed)) continue;
      label[nLabel] = formatLabel(oldLabelTrimmed);
      nLabel++;
    }
  }

  private String formatLabel(String text) {
    if (text == null) text = "";
    if (text.length() > 80) text = text.substring(0,80);
    for (int i = text.length(); i < 80; i++) text = text + ' ';
    return text;
  }

  public void fixGridO(int x, int y, int z){
    System.out.println(" Fixing grid origin " + x + "," + y + "," + z);
    System.out.println(" WARNING: CCP4 fastStart " + nFastOffset + " changed to " + x);
    System.out.println(" WARNING: CCP4 mediumStart " + nMediumOffset + " changed to " + y);
    System.out.println(" WARNING: CCP4 slowStart " + nSlowOffset + " changed to " + z);
    nFastOffset = x;
    nMediumOffset = y;
    nSlowOffset = z;
  }
  public void fixGridC() throws Exception{
  
    if (nFast % 2 == 0 && nMedium % 2 == 0 && nSlow % 2 == 0) {
      System.out.println(" Fixing grid centre  to origin");
      System.out.println(" Changing fastStart " + nFastOffset + " to " + nFast / (-2));
      System.out.println(" Changing mediumStart " + nMediumOffset + " to " + nMedium / (-2));
      System.out.println(" Changing slowStart " + nSlowOffset + " to " + nSlow / (-2));
      nFastOffset = nFast / (-2);
      nMediumOffset = nMedium / (-2);
      nSlowOffset = nSlow / (-2);
    } else {
      System.out.println(" GridCentre setting requires all the map dimensions to be even - must stop " + nFast + ", " + nMedium + ", " + nSlow);
      throw new Exception();
    }
  }

  public void fixR2G() throws Exception{

    float[] cell = new float[3];
    int[] grid = new int[3];
    int[] offset = new int[3];

    System.out.println(" Changing grid origin based on the closest real origin ");
    System.out.println(" WARNING: MRC Changing X " + nFastOffset + " to " + originXYZ[0] + " * " + nFast + " / " + cellX);
    System.out.println(" WARNING: MRC Changing Y " + nMediumOffset + " to " + originXYZ[1] + " * " + nMedium + " / " + cellY);
    System.out.println(" WARNING: MRC Changing Z " + nSlowOffset + " to " + originXYZ[2] + " * " + nSlow + " / " + cellZ);
    if (Math.abs(alpha - 90.0) < 0.0001 && Math.abs(beta -90.0) < 0.0001 && Math.abs(gamma - 90.0) < 0.0001) {
// I did not store these sensibly
      grid[0] = nFast; grid[1] = nMedium; grid[2] = nSlow;
      cell[0] = cellX; cell[1] = cellY;   cell[2] = cellZ;
    
      if (iuvw[0] != 1 && iuvw[1] != 2 && iuvw[2] != 3) {
        System.out.println(" Careful - the axis row order is not 0/1/2 " + iuvw[0] + ", " + iuvw[1] + ", " + iuvw[2]);
      }
// Notice that originXYZ is a real space orthogonal XYZ and NOT RCS
      float x = originXYZ[0] * grid[iuvw[0]-1] / cell[iuvw[0]-1];
      offset[iuvw[0]-1] = Math.round(x);
      float y = originXYZ[iuvw[1]-1] * grid[iuvw[1]-1] / cell[iuvw[1]-1];
      offset[iuvw[1]-1] = Math.round(y);
      float z = originXYZ[iuvw[2]-1] * grid[iuvw[2]-1] / cell[iuvw[2]-1];
      offset[iuvw[2]-1] = Math.round(z);

      if (Math.abs(nFastOffset-offset[0]) > 0.5) {
        System.out.println(" WARNING (realtogrid): CCP4 : nRowStart has been changed ***************");
      }
      if (Math.abs(nMediumOffset-offset[1]) > 0.5) {
        System.out.println(" WARNING (realtogrid): CCP4 : nColumnStart has been changed ***************");
      }
      if (Math.abs(nSlowOffset-offset[2]) > 0.5) {
        System.out.println(" WARNING (realtogrid): CCP4 : nSectionStart has been changed ***************");
      }
      nFastOffset = offset[0]; nMediumOffset = offset[1]; nSlowOffset = offset[2];
      System.out.println(" Precision of the origin : " + ((float)offset[iuvw[0]-1] - x) + ", " + ((float)offset[iuvw[1]-1] - y) + ", " + ((float)offset[iuvw[2]-1] - z));

    } else {
      System.out.println(" Fixing the grid origin based on the real origin records can only be done with orthogonal cells " + cellX + ", " + cellY + ", " + cellZ);
      throw new Exception();
    }
  }

  public void setOriginZero() {

    System.out.println(" WARNING : MRC - setting words 50-52 to zero ");
    originXYZ[0] = 0.0F;
    originXYZ[1] = 0.0F;
    originXYZ[2] = 0.0F;
  }

  public void autoFixCellAngle() {

    if (alpha < 0.0001) {
     System.out.println("WARNING : Cell angle alpha was < 0.0001 : set to 90.0");
     alpha = 90.0F;
    }
    if (beta < 0.0001) {
     System.out.println("WARNING : Cell angle beta was < 0.0001 : set to 90.0");
     beta = 90.0F;
    }
    if (gamma < 0.0001) {
     System.out.println("WARNING : Cell angle gamma was < 0.0001 : set to 90.0");
     gamma = 90.0F;
    }

    if (alpha >= 180.0) {
      System.out.println("WARNING : Cell angle alpha > 180.0 : IMPOSSIBLE");
    }
    if (beta >= 180.0) {
      System.out.println("WARNING : Cell angle beta > 180.0 : IMPOSSIBLE");
    }
    if (gamma >= 180.0) {
      System.out.println("WARNING : Cell angle gamma > 180.0 : IMPOSSIBLE");
    }

  }

  public void fixCell(float x, float y, float z){
    System.out.println(" Fixing cell size using parameters " + x + "," + y + "," + z);
    System.out.println(" Changing cell dimensions to supplied parameters: ");
    System.out.println(" CellX " + cellX + " changed to " + x);
    System.out.println(" CellY " + cellY + " changed to " + y);
    System.out.println(" CellZ " + cellZ + " changed to " + z);
    cellX = x;
    cellY = y;
    cellZ = z;
  }

  public void fixSpaceGroup(){
    System.out.println(" Current space group = " + spaceGroup);
    if (spaceGroup <= 0) {
      if (nSlow > 1) {
        System.out.println(" Fix space group 0 -> 1" );
        spaceGroup = 1;
      } else {
        System.out.println(" Space group = 0 because Nslow = 1");
      }
    } else if (spaceGroup > 230) {
      spaceGroup = 1;
      System.out.println(" Space-group record fixed : set to 1 : " + spaceGroup);
    }
  }

  public void fixMapType() {
    
    if (!fileTypeCheck.equals("MAP ")) {
      System.out.println(" File Type is wrong " + fileTypeCheck + " reset to MAP");
      fileTypeCheck = "MAP ";
    }
  }

  public void setRMS(float rms) { 
    this.RMS = rms; 
System.out.println(" Changed RMS to " + rms);
  }
  public void setMean(float mean) { 
    this.meanDensity = mean; 
System.out.println(" Changed Mean to " + mean);
  }
  public void setMin(float min) { 
    this.minDensity = min; 
System.out.println(" Changed Min to " + min);
  }
  public void setMax(float max) { 
    this.maxDensity = max; 
System.out.println(" Changed Max to " + max);
  }
  public void setnByteForSymOp(int nbyte) { 
    if (nbyte == 0) {
       symop = null;
    } else if (nbyte < nByteForSymOp) {
       symop = symop.substring(0,nbyte);
    } else if (nbyte >  nByteForSymOp) {
       for (int i = nByteForSymOp; i < nbyte; i++) symop = symop.concat(" ");
    }
    this.nByteForSymOp = nbyte; 
System.out.println(" Changed SymOp Byte number " + nbyte);
  }

  public void zeroCharFix() {

     int i,j,n;
     char nul = 0;
     char space = 32;

     n = 0;
     if (fileTypeCheck != null) {
      for (i = 0; i < 4; i++) { if (fileTypeCheck.charAt(i) == nul) n++; }
      fileTypeCheck = fileTypeCheck.replace(nul,space);
     }
// no    if (symop != null) machineStamp = machineStamp.replace(nul,space);
     if (symop != null) {
       for (i = 0; i < 4; i++) { if (symop.charAt(i) == nul) n++; }
       symop = symop.replace(nul,space);
     }
     for (i = 0; i < nLabel; i++) {
       for (j = 0; j < 80; j++) { if (label[i].charAt(j) == nul) n++; }
       label[i] = label[i].replace(nul,space);
     }

System.out.println(" Fixed Null characters " + n);

  }
    
  public String readWordOfLength(int len, FileInputStream buff) {

// get a word as a string from the file.
// notice that the word is protected for special characters by default
   
   byte[] buf = new byte[len];

     try {
       buff.read(buf, 0,len);
       for (int i = 0; i < len; i++) {
         if (stripBumCharacters) {
           if (buf[i] == 0) {
              nNullInString++;
              buf[i] = 32;
           }
           if (buf[i] < 31 || buf[i] > 127) {
             symWrong = true;
             buf[i] = 32;
           }
         }
       }
       return new String(buf);
     } catch (Exception e) {
       System.out.println("READWORD " + e);
       return null;
     }
  }

  public String readWord(FileInputStream buff) {
// read a word as 4 characters
    return readWordOfLength(4, buff);
  }

  public float readFloat(FileInputStream buff) {
// erad a word as a floating point number

    int x = readInteger(buff);
    if (x != BAD) 
       return Float.intBitsToFloat(x);
     else
       return BAD;

  }

  public int readInteger(FileInputStream buff) {
// erad a word as a integer depending on endian
    int x;
    byte[] buf = new byte[8];

     try {
       buff.read(buf, 0,4);
//       if (endian == 0) 
//         x = (int)((((int)buf[0]&0xff)<<24)+(((int)buf[1]&0xff)<<16)+(((int)buf[2]&0xff)<<8)+((int)buf[3]&0xff));
//       else
//         x = (int)((((int)buf[3]&0xff)<<24)+(((int)buf[2]&0xff)<<16)+(((int)buf[1]&0xff)<<8)+((int)buf[0]&0xff));

// auto select endian from first read
       if (endian == 0) {
         x = (int)((((int)buf[0]&0xff)<<24)+(((int)buf[1]&0xff)<<16)+(((int)buf[2]&0xff)<<8)+((int)buf[3]&0xff));
         if (firstRead) {
           firstRead = false;
           if (x < -100000 || x > 100000) {
System.out.println(" Changed Endian to Little ");
             endian = 1;
             x = (int)((((int)buf[3]&0xff)<<24)+(((int)buf[2]&0xff)<<16)+(((int)buf[1]&0xff)<<8)+((int)buf[0]&0xff));
           }
         }
       } else {
         x = (int)((((int)buf[3]&0xff)<<24)+(((int)buf[2]&0xff)<<16)+(((int)buf[1]&0xff)<<8)+((int)buf[0]&0xff));
         if (firstRead) {
           firstRead = false;
           if (x < -100000 || x > 100000) {
System.out.println(" Changed Endian to Big ");
             endian = 0;
             x = (int)((((int)buf[0]&0xff)<<24)+(((int)buf[1]&0xff)<<16)+(((int)buf[2]&0xff)<<8)+((int)buf[3]&0xff));
           }
         }
       }
       return x;
     } catch (Exception e) {
       System.out.println("READ " + e);
       return BAD;
     }

  }

  public void print() {
// dump the data to system.out

  char[] axes = { '?', 'X','Y','Z' };

  System.out.println(" Check for file type = " + fileTypeCheck );

  System.out.println("Number of columns, rows, sections ............... " + nFast + ", " + nMedium + ", " + nSlow);
  System.out.println("Map mode ........................................ " + mode);  
  System.out.println("Start and stop points on columns, rows, sections. " + nFastOffset + ", " + (nFast+nFastOffset-1) + ", " + nMediumOffset + ", " + (nMedium+nMediumOffset-1) + ", " + nSlowOffset + ", " + (nSlow+nSlowOffset-1));
  System.out.println("Grid sampling on x, y, z ........................ " + nX + ", " + nY + ", " + nZ);
  System.out.println("Cell dimensions ................................. " + cellX + ", " + cellY + ", " + cellZ + ", " + alpha + ", " + beta + ", " + gamma);
  System.out.println("Fast, medium, slow axes ......................... " + axes[iuvw[0]] + ", " + axes[iuvw[1]] + ", " + axes[iuvw[2]]);
  System.out.println("Minimum density ................................. " +  minDensity);
  System.out.println("Maximum density ................................. " +  maxDensity);
  System.out.println("Mean density .................................... " +  meanDensity);
  System.out.println("RMS density ..................................... " +  RMS);
  System.out.println("Space group ..................................... " +  spaceGroup);
  System.out.println("Number of lines of titles ....................... " + nLabel);
  System.out.println("--------------------------------------------------");
  for (int i = 0; i < nLabel; i++) System.out.println(label[i]);
  System.out.println("--------------------------------------------------");
  System.out.println(" Symops follow of " + nByteForSymOp + " bytes");
 
  if ((nByteForSymOp % 80) == 0) {
    for (int i = 0; i < nByteForSymOp/80; i++) {
      //System.out.println(symop.substring(i*80,i*80+79));
      //System.out.println("HERE: '"+symop+"'");
      //System.out.println("HERE: '"+symop.length()+"'");
    }
  } else {
    System.out.println(" The extra header bytes are not divisible by 80 : not printable");
  }
  System.out.println("--------------------------------------------------");
  System.out.println("Real values for OriginXYZ ....................... " + originXYZ[0] + ", " + originXYZ[1] + ", " + originXYZ[2]);


  }

  public boolean close(FileInputStream buff) {
//  method to close a map, returns a status of sucess of not
      if (buff != null) { 
        try {
          buff.close(); 
          return true; 
        } catch (Exception e) {
          System.out.println("CLOSE " + e);
          return false; 
        }
      } else return false;
  }


  protected void finalize() throws Throwable {
// do NOT call this - for garbage collection

    try {
       kill();
    } finally {
       super.finalize();
    }
  }

  public void kill() {
// call this though and it will help with garbage collection
//   I use "if (mapHeader != null) mapHeader.kill(); mapHeader = null; 

    skewMatrix = null;
    skewTrans= null;
    iuvw = null;
    label = null;
  }

}
