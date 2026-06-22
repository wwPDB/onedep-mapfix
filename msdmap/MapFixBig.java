package msdmap;

/* =============================================
Author  T.J.Oldfield
Date     29th April 2007
What    Control application to open a map and
        determine map problems and fix them - writing a new map
*/

import java.io.*;
import msdmap.mapread.MapHeader;
import msdmap.mapread.MapRead;
import msdmap.mapread.MapWrite;
import msdmap.Version;
import java.lang.RuntimeException;

public class MapFixBig {

  String fin = null;
  String fout = null;
  boolean fixMean = false;
  boolean fixMin = false;
  boolean fixMax = false;
  boolean fixRMS = false;
  boolean fixSymOp = false;
  boolean fixNullChar = false;

  public static void main(String arguments[]) throws Exception{

    boolean fixSymOp = false;
    FileInputStream buff = null;

    Version v = new Version();

    GetArgs getArgs = new GetArgs();
    getArgs.ParseArguments(arguments, "mapFixBig.jar");

    MapHeader mapHeader = new MapHeader();
    buff = mapHeader.open(getArgs.fin);
    if (buff != null) {
      if (mapHeader.read(buff)) {
        mapHeader.checkBadData(getArgs.fixVoxel);
        // mapHeader.print();
        MapRead mapRead = new MapRead(buff);
        mapRead.setEndian(mapHeader.getEndian());
        mapRead.setMapMode(mapHeader.getMapMode());
        boolean noError = mapRead.mapReadBig(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow());
        // cannot manage lost bytes in map
        if (noError) {
          // moved this before calculating information materail
          if (getArgs.sign) {
            System.err.println(" mapFixBig.jar does not support -sign option!");
            System.exit(-1);
            // mapRead.signedUnsigned(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),data);
          }
          mapRead.calcInfo(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),mapHeader.headerSizeInByte);
          if (getArgs.fixMean)mapHeader.setMean(mapRead.getMean());
          if (getArgs.fixMin)mapHeader.setMin(mapRead.getMin());
          if (getArgs.fixMax)mapHeader.setMax(mapRead.getMax());
          if (getArgs.fixRMS)mapHeader.setRMS(mapRead.getRMS());
          if (fixSymOp && getArgs.fixSymOp) mapHeader.setnByteForSymOp(0);
          if (getArgs.fixNullChar)mapHeader.zeroCharFix();
          if (getArgs.fixSP) mapHeader.fixSpaceGroup();
          if (getArgs.fixMapFileType) mapHeader.fixMapType();
          if (getArgs.fixVoxel) mapHeader.fixVoxel(getArgs.pixelX,getArgs.pixelY,getArgs.pixelZ);
          if (getArgs.fixCell) mapHeader.fixCell(getArgs.cellX,getArgs.cellY,getArgs.cellZ);
          if (getArgs.fixGridStart) mapHeader.fixGridO(getArgs.ix,getArgs.iy,getArgs.iz);
          if (getArgs.fixGridCentre) mapHeader.fixGridC();
          if (getArgs.fixLabel) mapHeader.changeLabel(getArgs.text);
          if (getArgs.fixReal2Grid) mapHeader.fixR2G();
          if (getArgs.setOriginToZero) mapHeader.setOriginZero();
          if (getArgs.nWord > 0) mapHeader.setWord(getArgs.nWord,getArgs.setWord,getArgs.setVal);
          MapWrite mapWrite = new MapWrite();
          mapWrite.setEndian(mapHeader.getEndian());
          FileOutputStream out = mapWrite.open(getArgs.fout);
          mapWrite.write(out,mapHeader,mapHeader.headerSizeInByte,buff, mapHeader.getMapMode());
          mapWrite.close(out);
        }
        mapRead.kill();
        mapHeader.close(buff);
        mapHeader.kill();
      } else {
        System.err.println(" Read error of header ");
        System.exit(-1);
      }
    }

    System.out.println("done");

  }

}
