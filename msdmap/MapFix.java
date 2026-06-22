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

public class MapFix {

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
    getArgs.ParseArguments(arguments, "mapFix.jar");

    MapHeader mapHeader = new MapHeader();
    buff = mapHeader.open(getArgs.fin);
    if (buff != null) {
      if (mapHeader.read(buff)) {
        mapHeader.checkBadData(getArgs.fixVoxel);
        // mapHeader.print();
        MapRead mapRead = new MapRead(buff);
        mapRead.setEndian(mapHeader.getEndian());
        mapRead.setMapMode(mapHeader.getMapMode());
        byte[] data = mapRead.mapRead(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow());
        if (mapRead.getLostBytes() > 0) {
          // getSymOpBytes if the header record number
          // getLostBytes = the diffrence in the bytes read from expected
          // positive - there are extra bytes
          // negative - thee are missing bytes
          if (mapHeader.getSymOpsBytes() == mapRead.getLostBytes()) {
            System.err.println(" ********************************************");
            System.err.println(" Re-reading map and ignoring the SymOp bytes  to allow repair");
            System.err.println(" ********************************************");
            fixSymOp = true;
            mapHeader.close(buff);
            buff = mapHeader.open(getArgs.fin);
            mapHeader.read(buff,true);
            mapRead.setEndian(mapHeader.getEndian());
            mapRead = new MapRead(buff);
            mapRead.setMapMode(mapHeader.getMapMode());
            data = mapRead.mapRead(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow());
          } else {
            System.err.println(" Map data is truncated and this cannot be explained by the SymOb bytes");
            System.err.println(" Map file not repairable : NO FILE WRITTEN");
            data = null;
          }
        }
        if (data != null) {
         // moved this before calculating information materail
          if (getArgs.sign) mapRead.signedUnsigned(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),data);
          mapRead.calcInfo(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),data);
          // int[] bin = mapRead.getDistribution(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),data);
          // if (bin != null) mapRead.drawDistribution(bin,false);
          if (getArgs.specialMode > 0)mapHeader.specialFixMethod(getArgs.specialMode, getArgs.specialFix);
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
          if (getArgs.fixAngle) mapHeader.autoFixCellAngle();
          if (getArgs.fixGridStart) mapHeader.fixGridO(getArgs.ix,getArgs.iy,getArgs.iz);
          if (getArgs.fixGridCentre) mapHeader.fixGridC();
          if (getArgs.fixLabel) mapHeader.changeLabel(getArgs.text);
          if (getArgs.fixReal2Grid) mapHeader.fixR2G();
          if (getArgs.setOriginToZero) mapHeader.setOriginZero();
          if (getArgs.nWord > 0) mapHeader.setWord(getArgs.nWord,getArgs.setWord,getArgs.setVal);
          MapWrite mapWrite = new MapWrite();
          mapWrite.setEndian(mapHeader.getEndian());
          FileOutputStream out = mapWrite.open(getArgs.fout);
          mapWrite.write(out,mapHeader,data);
          mapWrite.close(out);
          data = null;
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
