
package msdmap.mapread;

/*
Author    T.J.Oldfield
Version   1.0
Date      23-April-2007
Action    To write the CCP4 map 
*/

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.channels.FileChannel;
import java.lang.Exception;

/* class to read the CCP4 map 

Read a map from disk - whole map for now

*/

public class MapWrite {

// endian : order of bytes
// 0 = big endian,  1 = little endian
  private int endian = 1;
  private int BAD = -10000000;

  private int mapMode = 2;
  private long nRead = 0;

// dataType definition
  private int BYTE = 0;
  private int INT2 = 1;
  private int REAL4 = 2;
  private int COMPLEX2 = 3; //  complex integer*2
  private int COMPLEX4 = 4; //  complex real*4
  private FileInputStream buff = null;
  private boolean validCalc = false;

  private double mean = 0.0;
  private double rms = 0.0;
  private double max = -1.0e36;
  private double min = 1e36;
  
  public MapWrite() { 
  }

  public FileOutputStream open(String fileName) {

    try {
      FileOutputStream file = new FileOutputStream(fileName);
      return file;
    } catch (FileNotFoundException e) {
      System.err.println("Could not open file " + fileName);
      return null;
    }
 
  }

  public void close(FileOutputStream buff) {
    try {
      buff.close();
    } catch (Exception e) {
      System.out.println(" Close error on write ");
    }
  }
 
  public void write(FileOutputStream buff, MapHeader header, long headerSize, FileInputStream inbuff, int mapMode) throws Exception{

    if (buff == null) {
System.out.println(" No file open for writing");
      return;
    }

     writeInteger(buff,header.getNoFast());
     writeInteger(buff,header.getNoMedium());
     writeInteger(buff,header.getNoSlow());
     writeInteger(buff,header.getMapMode());
     writeInteger(buff,header.getFastOffset());
     writeInteger(buff,header.getMediumOffset());
     writeInteger(buff,header.getSlowOffset());
     writeInteger(buff,header.getGridX());
     writeInteger(buff,header.getGridY());
     writeInteger(buff,header.getGridZ());
     writeFloat(buff,header.getCellA());
     writeFloat(buff,header.getCellB());
     writeFloat(buff,header.getCellC());
     writeFloat(buff,header.getCellAlpha());
     writeFloat(buff,header.getCellBeta());
     writeFloat(buff,header.getCellGamma());
     writeInteger(buff,header.getAxis1());
     writeInteger(buff,header.getAxis2());
     writeInteger(buff,header.getAxis3());
     writeFloat(buff,header.getMinDensity());
     writeFloat(buff,header.getMaxDensity());
     writeFloat(buff,header.getMeanDensity());
     writeInteger(buff,header.getSpaceGroupNumber());
     writeInteger(buff,header.getSymOpsBytes());
     writeInteger(buff,header.getSkew());
     for (int i = 0; i < 9; i++) writeFloat(buff,header.getSkewMatrix(i));
     for (int i = 0; i < 3; i++) writeFloat(buff,header.getSkewTransformation(i));
// changed for the EM reserved words
     for (int i = 0; i < 12; i++) writeInteger(buff,header.getReserved(i));
     for (int i = 0; i < 3; i++) writeFloat(buff,header.getOriginXYZ(i));
     writeWord(buff,header.getFileTypeCheck());
     writeData(buff,header.getMachineStamp());
     writeFloat(buff,header.getRMSD());
     writeInteger(buff,header.getNLabels());
     for (int j = 0; j < 10; j++) {
       if (j < header.getNLabels()) 
         writeWord20(buff,header.getLabel(j));
       else {
         for (int i = 0; i < 20; i++) writeWord(buff,"    ");
       }
     }
     if (header.getSymOpsBytes() > 0) {
       writeLine(buff,header.getSymop(),header.getSymOpsBytes());
     }

System.out.println(" Written header ");

    int word = 0;
    setMapMode(mapMode);
    if ((word = bytesPerWord()) == 0){
      System.out.println(" Did not write data = wrong (0) bytes/word ");
      return;
    }
    byte[] data = null;
    long nbyte = (long)header.getNoFast() * (long)header.getNoMedium() * (long)word;
    try {
      data = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      throw new Exception();
    }

    try {
      FileChannel fileChannel = inbuff.getChannel();
      fileChannel.position(0L);
      byte[] junk = new byte[(int)headerSize];
      int nread = inbuff.read(junk, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }

     for (int s = 0; s < header.getNoSlow(); s++) {
        int n = 0;
        try {
          nRead = inbuff.read(data, 0,(int)nbyte);
          buff.write(data);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
      }

      System.out.println(" Written data ");
  }

  public void write(FileOutputStream buff, MapHeader header, byte[] data) {

    if (buff == null) {
System.out.println(" No file open for writing");
      return;
    }

     writeInteger(buff,header.getNoFast());
     writeInteger(buff,header.getNoMedium());
     writeInteger(buff,header.getNoSlow());
     writeInteger(buff,header.getMapMode());
     writeInteger(buff,header.getFastOffset());
     writeInteger(buff,header.getMediumOffset());
     writeInteger(buff,header.getSlowOffset());
     writeInteger(buff,header.getGridX());
     writeInteger(buff,header.getGridY());
     writeInteger(buff,header.getGridZ());
     writeFloat(buff,header.getCellA());
     writeFloat(buff,header.getCellB());
     writeFloat(buff,header.getCellC());
     writeFloat(buff,header.getCellAlpha());
     writeFloat(buff,header.getCellBeta());
     writeFloat(buff,header.getCellGamma());
     writeInteger(buff,header.getAxis1());
     writeInteger(buff,header.getAxis2());
     writeInteger(buff,header.getAxis3());
     writeFloat(buff,header.getMinDensity());
     writeFloat(buff,header.getMaxDensity());
     writeFloat(buff,header.getMeanDensity());
     writeInteger(buff,header.getSpaceGroupNumber());
     writeInteger(buff,header.getSymOpsBytes());
     writeInteger(buff,header.getSkew());
     for (int i = 0; i < 9; i++) writeFloat(buff,header.getSkewMatrix(i));
     for (int i = 0; i < 3; i++) writeFloat(buff,header.getSkewTransformation(i));
// changed for the EM reserved words
     for (int i = 0; i < 12; i++) writeInteger(buff,header.getReserved(i));
     for (int i = 0; i < 3; i++) writeFloat(buff,header.getOriginXYZ(i));
     writeWord(buff,header.getFileTypeCheck());
     writeData(buff,header.getMachineStamp());
     writeFloat(buff,header.getRMSD());
     writeInteger(buff,header.getNLabels());
     for (int j = 0; j < 10; j++) {
       if (j < header.getNLabels()) 
         writeWord20(buff,header.getLabel(j));
       else {
         for (int i = 0; i < 20; i++) writeWord(buff,"    ");
       }
     }
     if (header.getSymOpsBytes() > 0) {
       writeLine(buff,header.getSymop(),header.getSymOpsBytes());
     }

System.out.println(" Written header ");

     writeData(buff,data);
System.out.println(" Written data ");
  }

  public void writeInteger(FileOutputStream buff, int value) {
    
    byte[] out = new byte[4];
    try {
      if (endian == 1) {
        out[0] = (byte)(value & 0x000000FF);
        out[1] = (byte)((value >> 8) & 0x000000FF);
        out[2] = (byte)((value >> 16) & 0x000000FF);
        out[3] = (byte)((value >> 24)  & 0x000000FF);
        buff.write(out);
      } else {
        out[3] = (byte)(value & 0x000000FF);
        out[2] = (byte)((value >> 8) & 0x000000FF);
        out[1] = (byte)((value >> 16) & 0x000000FF);
        out[0] = (byte)((value >> 24)  & 0x000000FF);
        buff.write(out);
      }
    } catch (Exception e) {
System.out.println(" Failed to write integer " + value);
    }
  }

  public void writeData(FileOutputStream buff, byte[] data) {
    try {
      buff.write(data);
    } catch (Exception e) {
      System.out.println(" Failed to write bytes " + e);
    }
  }

  public void writeFloat(FileOutputStream buff, float value) {

    writeInteger(buff,Float.floatToIntBits(value));

  }

  public void writeLine(FileOutputStream buff, String line,int length) {
    try {
      buff.write(line.getBytes(),0,length);
    } catch (Exception e) {
      System.out.println(" Failed to write bytes " + e);
    }
  }
  public void writeWord20(FileOutputStream buff, String word20) {
    writeLine(buff,word20,80);
  }
  public void writeWord(FileOutputStream buff, String word) {
    writeLine(buff,word,4);
  }

  public void setEndian(int mode) {
    endian = mode;
// 0 : set to little endian, 1 : set to big endian
  }

  public void setMapMode(int mode) {
// set the data type, 0 = byte, 1 = int2, 2 = real4, 3/4 = complex
   mapMode = mode;
  }

  private int bytesPerWord() {
    switch (mapMode) {
      case 0: return 1;
      case 1: return 2;
      case 2: return 4;
      case 3: return 8;
      case 4: return 8;
      default: return 0;
    }
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
   buff = null;
  }

}
