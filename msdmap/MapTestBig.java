package msdmap;

/* =============================================
Author  T.J.Oldfield
Date     29th April 2007
What    Control application to open a map and 
        look at the contents - including distribution
*/

import java.io.*;
import msdmap.mapread.MapHeader;
import msdmap.mapread.MapRead;
import msdmap.mapread.MapWrite;
import msdmap.Version;

public class MapTestBig {

  public static void main(String arguments[]) throws Exception{

    FileInputStream buff = null;
    MapHeader mapHeader = new MapHeader();

    Version v = new Version();

    if (arguments != null) {
      if (arguments.length > 0 && arguments[0] != null) {
         buff = mapHeader.open(arguments[0]);
      }
    }
    if (buff != null) {
      if (mapHeader.read(buff)) {
        mapHeader.print();
        mapHeader.checkBadData(false);
        MapRead mapRead = new MapRead(buff);
        mapRead.setEndian(mapHeader.getEndian());
        System.out.println(" map mode = " + mapHeader.getMapMode());
        mapRead.setMapMode(mapHeader.getMapMode());
        boolean noError = mapRead.mapReadBig(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow());
        // Cannot manage lost bytes in map
        if (noError) {
          mapRead.calcInfo(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),mapHeader.headerSizeInByte);
          mapRead.checkMap(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),mapHeader.headerSizeInByte);
          int[] bin = mapRead.getDistribution(mapHeader.getNoFast(),mapHeader.getNoMedium(), mapHeader.getNoSlow(),mapHeader.headerSizeInByte);
          if (bin != null) mapRead.drawDistribution(bin,false);
        }
        mapRead.kill();
        mapHeader.close(buff);
        mapHeader.kill();
      } else {
        System.err.println(" Read error of header ");
        System.exit(-1);
      }
    } else {
      System.err.println(" Usage = java -jar -Xms256m -Xmx256m mapTestBig.jar <mapName>");
      System.exit(-1);
    }

    System.out.println("done");

  }

}
