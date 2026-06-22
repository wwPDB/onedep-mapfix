
package msdmap.mapread;

/*
Author    T.J.Oldfield
Version   1.0
Date      23-April-2007
Action    To read the CCP4 map 
*/

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.channels.FileChannel;

/* class to read the CCP4 map 

Read a map from disk - whole map for now

*/

public class MapRead {

// endian : order of bytes
// 0 = big endian,  1 = little endian
  private int endian = 1;
  private int BAD = -10000000;

  private int mapMode = 2;
  private long nRead = 0;
  private int status = 0;
  private int lostBytes = 0;

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
  
  public MapRead(FileInputStream file) { 
    this.buff = file;
  }

  public void setEndian(int mode) {
    endian = mode;
// 0 : set to little endian, 1 : set to big endian
  }

  public void setMapMode(int mode) {
// set the data type, 0 = byte, 1 = int2, 2 = real4, 3/4 = complex
   mapMode = mode;
  }

  public float getMean() { return (float)mean; }
  public float getMin() { return (float)min; }
  public float getMax() { return (float)max; }
  public float getRMS() { return (float)rms; }

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

  public int getLostBytes() { return lostBytes; }
   
  public int signedUnsigned(int nfast, int nmedium, int nslow, byte[] map) {

    int n = 0;
    int word = 0;

    if ((word = bytesPerWord()) == 0) return 0;

     System.out.println("converting signed<->unsigned");

      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            if (mapMode == 0) {
              if (map[n] >= 0) {
// shift data by 128 to smaller value
                map[n] = (byte)((short)map[n] - (short)128);
              } else {
// wrap data by subtracting from 256
                map[n] = (byte)(128 + (short)map[n]);
              }
            } else if (mapMode == 1) {
/*              if (dat > 0) {
// shift data by 32768 to smaller value
                dat2 = (short)((int)map[n] - (int)32768);
              } else {
// wrap data by subtracting from 256
                dat2 = (short)(65536 - (int)map[n]);
              } */
            }
            n += word;
          }
        }
      }

     if (mapMode == 0) {
       System.out.println(" done  <dat = dat & 0xff>");
     } else {
       System.out.println(" Done nothing - cannot do mapMode = " + mapMode);
     }
     return 1;

  }
  public int signedUnsigned(int nfast, int nmedium, int nslow, long headerSize) throws Exception{

    int word = 0;
    byte[] map = null;

    if ((word = bytesPerWord()) == 0) return 0;

     System.out.println("converting signed<->unsigned");

    long nbyte = (long)nfast * (long)nmedium * (long)word;
    try {
      map = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      throw new Exception();
    }

    try {
      FileChannel fileChannel = buff.getChannel();
      fileChannel.position(0L);
      byte[] header = new byte[(int)headerSize];
      int nread = buff.read(header, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }


      for (int s = 0; s < nslow; s++) {
        int n = 0;
        try {
          nRead = buff.read(map, 0,(int)nbyte);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            if (mapMode == 0) {
              if (map[n] >= 0) {
// shift data by 128 to smaller value
                map[n] = (byte)((short)map[n] - (short)128);
              } else {
// wrap data by subtracting from 256
                map[n] = (byte)(128 + (short)map[n]);
              }
            } else if (mapMode == 1) {
/*              if (dat > 0) {
// shift data by 32768 to smaller value
                dat2 = (short)((int)map[n] - (int)32768);
              } else {
// wrap data by subtracting from 256
                dat2 = (short)(65536 - (int)map[n]);
              } */
            }
            n += word;
          }
        }
      }

     if (mapMode == 0) {
       System.out.println(" done  <dat = dat & 0xff>");
     } else {
       System.out.println(" Done nothing - cannot do mapMode = " + mapMode);
     }
     return 1;

  }
/* this versin did not work
  public int signedUnsigned(int nfast, int nmedium, int nslow, byte[] map) {

    int n = 0;
    int word = 0;

    if ((word = bytesPerWord()) == 0) return 0;

     System.out.println("converting signed<->unsigned");

      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            if (mapMode == 0) {
              if (map[n] > 0) {
// shift data by 128 to smaller value
                map[n] = (byte)((short)map[n] - (short)128);
              } else {
// wrap data by subtracting from 256
                map[n] = (byte)(256 - (short)map[n]);
              }
            } else if (mapMode == 1) {
//              if (dat > 0) {
// shift data by 32768 to smaller value
                dat2 = (short)((int)map[n] - (int)32768);
              } else {
// wrap data by subtracting from 256
                dat2 = (short)(65536 - (int)map[n]);
 //             } 
            }
            n += word;
          }
        }
      }

     if (mapMode == 0) {
       System.out.println(" done  <dat = dat & 0xff>");
     } else {
       System.out.println(" Done nothing - cannot do mapMode = " + mapMode);
     }
     return 1;

  } */

  public int[] getDistribution(int nfast, int nmedium, int nslow, long headerSize) throws Exception{
      return getDistribution(nfast, nmedium, nslow, headerSize, 30);
  }

  public int[] getDistribution(int nfast, int nmedium, int nslow, long headerSize, int nbin) throws Exception{

    byte[] map = null;
    int word = 0;

    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return null;

    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;

    long nbyte = (long)nfast * (long)nmedium * (long)word;
    try {
      map = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      return null;
    }

    try {
      FileChannel fileChannel = buff.getChannel();
      fileChannel.position(0L);
      byte[] header = new byte[(int)headerSize];
      int nread = buff.read(header, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }

//    if (calcInfoMean(nfast,nmedium,nslow)) {
    if (true) {
      int[] bin = new int[nbin];
      for (int s = 0; s < nslow; s++) {
        int n = 0;
        try {
          nRead = buff.read(map, 0,(int)nbyte);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) break;
// needs adapting for complex numbers
            int k = (int)((number-min)/(max-min) * (float)nbin);
            if (k >= 0 && k < nbin) bin[k]++;
            n += word;
            count += 1.0;
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }


      return bin;
    } else return null;
      
    }

  public int[] getDistribution(int nfast, int nmedium, int nslow, byte[] map) {
    return getDistribution(nfast, nmedium, nslow, map, 30);
  }

  public int[] getDistribution(int nfast, int nmedium, int nslow, byte[] map, int nbin) {

    int word = 0;

    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return null;

    int n = 0;
    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;

    if (calcInfoMean(nfast,nmedium,nslow,map)) {
      int[] bin = new int[nbin];
      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) break;
// needs adapting for complex numbers
            int k = (int)((number-min)/(max-min) * (float)nbin);
            if (k >= 0 && k < nbin) bin[k]++;
            n += word;
            count += 1.0;
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }
      return bin;
    } else return null;
      
    }

    public void drawDistribution(int[] data, boolean log) {

      int largest = 0;
      int[] bin = null;

      if (log) {
        bin = new int[data.length];
        for (int i = 0; i < bin.length; i++) {
          if (data[i] > 0)bin[i] = (int)Math.log((double)data[i]);
        }
      } else {
        bin = data;
      }
      
      for (int i = 0; i < bin.length; i++) { if (bin[i] > largest) largest = bin[i]; }

      for (int i = 0; i < bin.length; i++) { 
        bin[i] = (bin[i] * 50) / largest; 
        double r = min + ((max-min) / 30.0) * (double)i;
        System.out.print(formatDouble(r,8,3) + "  ");
        for (int j = 0; j < bin[i]; j++) System.out.print("*");
        System.out.println(" ");
      }

      if (log) bin = null;

  }

  public String formatDouble(double d, int len, int dp) {

    String fx = (dp <= 0) ? "" + Math.round(d) : "" + trim(d, dp);
    while (fx.substring(fx.indexOf(".") + 1, fx.length()).length() < dp) fx += "0";
    while (fx.length() < len) fx = " " + fx;
    return fx;
  }

        public static double trim(double d, int dp) {

        double factor = Math.pow(10.0, dp);
        return (int)(d * factor) / factor;

        }

  public boolean calcInfoMean(int nfast, int nmedium, int nslow, long headerSize) throws Exception{

    int word = 0;
    byte[] map = null;

    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return false;

    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;

    long nbyte = (long)nfast * (long)nmedium * (long)word;
    try {
      map = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      return false;
    }

    try {
      FileChannel fileChannel = buff.getChannel();
      fileChannel.position(0L);
      byte[] header = new byte[(int)headerSize];
      int nread = buff.read(header, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }

      for (int s = 0; s < nslow; s++) {
        int n = 0;
        try {
          nRead = buff.read(map, 0,(int)nbyte);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) break;
// needs adapting for complex numbers
            mean += number;
            if (number < min) min = number;
            if (number > max) max = number;
            n += word;
            count += 1.0;
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }


    mean = mean / count;

    return true;

  }
  public boolean calcInfoMean(int nfast, int nmedium, int nslow, byte[] map) {

    int word = 0;
    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return false;

    int n = 0;
    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;

      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) break;
// needs adapting for complex numbers
            mean += number;
            if (number < min) min = number;
            if (number > max) max = number;
            n += word;
            count += 1.0;
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }

    mean = mean / count;

    return true;

  }

  public boolean calcInfoRMS(int nfast, int nmedium, int nslow, long headerSize) throws Exception{

    int word = 0;
    byte[] map = null;

    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return false;

    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;
    rms = 0.0;
  
    long nbyte = (long)nfast * (long)nmedium * (long)word;
    try {
      map = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      return false;
    }

    try {
      FileChannel fileChannel = buff.getChannel();
      fileChannel.position(0L);
      byte[] header = new byte[(int)headerSize];
      int nread = buff.read(header, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }

      for (int s = 0; s < nslow; s++) {
        int n = 0;
        try {
          nRead = buff.read(map, 0, (int)nbyte);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) {
              System.out.println(" A bad number was found " );
              break;
            }
//            System.out.println( " count " + count + " number " + number);
// needs adapting for complex numbers
            n += word;
            count += 1.0;
            rms = rms + (number - mean) * (number - mean);
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }


//    System.out.println(" debug " + count + "  " + rms);
    rms = rms / count;
    if (rms > 0.0) rms = Math.sqrt(rms);


    return true;

  }
  public boolean calcInfoRMS(int nfast, int nmedium, int nslow, byte[] map) {

    int word = 0;
    if (buff == null) status = -1;
    if ((word = bytesPerWord()) == 0) return false;

    int n = 0;
    double number = BAD;
    double cmplx = BAD;
    double count = 0.0;
    rms = 0.0;

      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            if (number == BAD) break;
//            System.out.println( " count " + count + " number " + number);
// needs adapting for complex numbers
            n += word;
            count += 1.0;
            rms = rms + (number - mean) * (number - mean);
          }
          if (number == BAD) break;
        }
        if (number == BAD) break;
      }

//    System.out.println(" debug " + count + "  " + rms);
    rms = rms / count;
    if (rms > 0.0) rms = Math.sqrt(rms);


    return true;

  }

  public boolean calcInfo(int nfast, int nmedium, int nslow, byte[] map) {


    if (calcInfoMean(nfast,nmedium,nslow,map)) {

System.out.println(" Mean = " + mean);
System.out.println(" Min  = " + min);
System.out.println(" Max  = " + max);
       
      if (calcInfoRMS(nfast,nmedium,nslow,map)) {
System.out.println(" RMSD  = " + rms);
        validCalc = true;
      } else return false;
    } else return false;

    return true;

  }

  public boolean calcInfo(int nfast, int nmedium, int nslow, long headerSize) throws Exception{


    if (calcInfoMean(nfast,nmedium,nslow,headerSize)) {

System.out.println(" Mean = " + mean);
System.out.println(" Min  = " + min);
System.out.println(" Max  = " + max);
       
      if (calcInfoRMS(nfast,nmedium,nslow,headerSize)) {
System.out.println(" RMSD  = " + rms);
        validCalc = true;
      } else return false;
    } else return false;

    return true;

  }

  public void checkMap(int nfast, int nmedium, int nslow, long headerSize) throws Exception{

    int word;
    byte[] map = null;
    if ((word = bytesPerWord()) == 0) return ;
  
    long nbyte = (long)nfast * (long)nmedium * (long)word;
    try {
      map = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
    }

    try {
      FileChannel fileChannel = buff.getChannel();
      fileChannel.position(0L);
      byte[] header = new byte[(int)headerSize];
      int nread = buff.read(header, 0,(int)headerSize);
    } catch (Exception e) {
      System.out.println(" Failed to remove header for huge map handling ");
      throw new Exception();
    }

     long count = 0;
     long zero = 0;
     double number = BAD;
     double cmplx = BAD;
  
      count = 0;
      for (int s = 0; s < nslow; s++) {
        int n = 0;
        try {
          nRead = buff.read(map, 0,(int)nbyte);
        } catch (Exception e) {
          System.out.println("Error reading map - big map handling" + e);
          throw new Exception();
        }
        for (int i = 0; i < nbyte; i++) {
          if (map[i] != 0) zero++;
        }
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            n = n += word;
            if (Math.abs(number) > 0.0001) count++;
          }
        }
      }


     System.out.println(" mapmode = " + mapMode);
     System.out.println(" word = " + word);
     System.out.println(" Number of non-zero bytes = " + zero);

     System.out.println(" Number of non-zero numbers = " + count);

  }

  public void checkMap(int nfast, int nmedium, int nslow, byte[] map) {

    int word;
    if ((word = bytesPerWord()) == 0) return ;
  
    long nbyte = nfast * nmedium * nslow * word;

     int count = 0;
     for (int i = 0; i < nbyte; i++) {
       if (map[i] != 0) count++;
     }
     System.out.println(" mapmode = " + mapMode);
     System.out.println(" word = " + word);
     System.out.println(" Number of non-zero bytes = " + count);

    int n = 0;
    double number = BAD;
    double cmplx = BAD;
    count = 0;
  
      count = 0;
      for (int s = 0; s < nslow; s++) {
        for (int m = 0; m < nmedium; m++) {
          for (int f = 0; f < nfast; f++) {
            switch (mapMode) {
              case 0 : number = (float)map[n]; break;
              case 1 : number = (float)getShort(map[n],map[n+1]); break;
              case 2 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); break;
              case 3 : number = (float)getShort(map[n],map[n+1]);
                       cmplx =  (float)getShort(map[n+2],map[n+3]); break;
              case 4 : number = getFloat(map[n],map[n+1],map[n+2],map[n+3]); 
                       cmplx = getFloat(map[n+4],map[n+5],map[n+6],map[n+7]); break;
              default : number = BAD; break;
            }
            n = n += word;
            if (Math.abs(number) > 0.0001) count++;
          }
        }
      }


     System.out.println(" N = " + n);
     System.out.println(" Number of non-zero numbers = " + count);

  }

  public boolean mapReadBig(int nfast, int nmedium, int nslow) {

/* Read only as bytes as this give the best performance due
   to full buffering. */
    int word = 0;
    status = 0;

    if ((word = bytesPerWord()) == 0) return false;
    
    long nbyte = (long)nfast * (long)nmedium *  (long)word;

    if (buff == null) status = -1;
    if (nbyte < 0) status = -2;
    if (status != 0) return false;

/*      try {
        FileChannel fileChannel = buff.getChannel();
        long size = fileChannel.position();
      } catch (Exception e) {
        System.out.println(" Unable to reposition file pointer ");
        throw new Exception();
      } */

    return true;
 
  }
  public byte[] mapRead(int nfast, int nmedium, int nslow) {

/* Read only as bytes as this give the best performance due
   to full buffering. */
    int word = 0;
    status = 0;

    if ((word = bytesPerWord()) == 0) return null;
    
    long nbyte = (long)nfast * (long)nmedium * (long)nslow * (long)word;

    if (buff == null) status = -1;
    if (nbyte < 0) status = -2;
    if (status != 0) return null;

    byte[] ret = null;
    try {
      ret = new byte[(int)nbyte];
    }catch (Throwable e) {
      System.out.println("======================================");
      System.out.println("You need to increase the run time heap");
      System.out.println("using java -Xms256m -Xmx256m <filename> ");
      System.out.println("======================================");
      return null;
    }

    try {
      nRead = buff.read(ret, 0,(int)nbyte);
    } catch (Exception e) {
      System.out.println("Error reading map " + e);
      status = -3;
      return null;
    }
      
    System.out.println(" Bytes requested from map = " + nbyte);
    System.out.println(" Bytes read from map      = " + nRead);
    lostBytes = (int)(nbyte - nRead);
    if (lostBytes > 0) {
      System.out.println(" ========== Warning ===============");
      System.out.println(" Map is truncated by " + lostBytes + " bytes");
      System.out.println(" ==================================");
      for (int i = (int)nRead; i < (int)nbyte; i++) ret[i] = 0;
      status = 1;
    }

    return ret;
 
  }
    
  public String readWordOfLength(int len, FileInputStream buff) {

// get a word as a string from the file.
// notice that the word is protected for special characters by default
   
   byte[] buf = new byte[len];

     try {
       buff.read(buf, 0,len);
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

  private float getFloat(byte b0, byte b1, byte b2, byte b3) {

    int x = getInteger(b0,b1,b2,b3);
    if (x != BAD) 
       return Float.intBitsToFloat(x);
     else
       return BAD;
  }

  private short getShort(byte b0, byte b1) {
    
    short x = 0;
    if (endian == 0) 
      x = (short)((((int)b0&0xff)<<8)+((int)b1&0xff));
//      x = (short)((short)(((b0 << 8) + ((b1)))));
    else 
      x = (short)((((int)b1&0xff)<<8)+((int)b0&0xff));
//      x = (short)((short)(((b1 << 8) + ((b0)))));

    return x;
  }

  private int getInteger(byte b0, byte b1, byte b2, byte b3) {

    int x = 0;
    if (endian == 0) 
      x = (int)((((int)b0&0xff)<<24)+(((int)b1&0xff)<<16)+(((int)b2&0xff)<<8)+((int)b3&0xff));
    else 
      x = (int)((((int)b3&0xff)<<24)+(((int)b2&0xff)<<16)+(((int)b1&0xff)<<8)+((int)b0&0xff));
  
    return x;
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
       if (endian == 0) 
         x = (int)((((int)buf[0]&0xff)<<24)+(((int)buf[1]&0xff)<<16)+(((int)buf[2]&0xff)<<8)+((int)buf[3]&0xff));
       else
         x = (int)((((int)buf[3]&0xff)<<24)+(((int)buf[2]&0xff)<<16)+(((int)buf[1]&0xff)<<8)+((int)buf[0]&0xff));
       return x;
     } catch (Exception e) {
       System.out.println("READ " + e);
       return BAD;
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
