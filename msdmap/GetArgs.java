package msdmap;

import java.lang.RuntimeException;

public class GetArgs {
  
  String fin = null;
  String fout = null;
  String programName = null;
  boolean fixMean = false;
  boolean fixMin = false;
  boolean fixMax = false;
  boolean fixRMS = false;
  boolean fixSymOp = false;
  boolean fixNullChar = false;
  boolean fixSP = false;
  boolean fixVoxel = false;
  boolean fixCell = false;
  boolean fixAngle = false;
  boolean fixGridStart = false;
  boolean fixGridSampling = false;
  boolean fixGridCentre = false;
  boolean fixReal2Grid = false;
  boolean fixLabel = false;
  boolean fixMapFileType = false;
  boolean sign = false;
  boolean setOriginToZero = false;
  float cellX,cellY,cellZ;
  float pixelX,pixelY,pixelZ;
  int ix,iy,iz;
  int sx,sy,sz;
  int specialFix = 0;
  int specialMode = 0;
  String text = null;
  int nWord = 0;
  int[] setWord = null;
  String[] setVal = null;
  String[] defaultOptions = {
    " -in  <filename>        : input map",
    " -out <filename>        : output map",
    " -all                   : fix everything : NOT anything to do with map origin",
    " -angle                 : auto fix cell angle if < 0.0001",
    " -cell <x> <y> <z>      : set x/y/z-length x/y/z",
    " -label <string>        : write new label",
    " -mapType               : fix the map Type -> MAP^ ",
    " -max                   : fix the max value ",
    " -mean                  : fix the mean value ",
    " -min                   : fix only the min value ",
    " -null                  : fix the zero bytes in strings",
    " -gridstart <x> <y> <z> : set x/y/z- grid start point",
    " -gridcentre            : set x/y/z-grid centre point based on the grid extent",
    " -originZero            : Zero words 50-52 : the EM origin card",
    " -realtogrid            : x/y/Z grid using the real value grid in words 50-52 ",
    " -rms                   : fix the rms value ",
    " -sign                  : Convert signed-unsigned bytes/int2",
    " -sp                    : fix the 0 value space group unless NS = 1",
    " -sym                   : fix the symop records",
    " -voxel <x> <y> <z>     : set x/y/z-length values to N[X/Y/Z]-length"
  };

  public void ParseArguments(String arg[], String programName) {

    if (programName.equals("mapFixDep.jar")){
        String[] newOptions = {
            " -in  <filename>           : input map",
            " -out <filename>           : output map",
            " -label <DepCode>          : write new label",
            " -voxel <x> <y> <z>        : set x y z pixel spacing"
        };
        defaultOptions =  newOptions;
    }

    if (programName.equals("mapFixAnot.jar")){
        String[] newOptions = {
            " -in  <filename>           : input map",
            " -out <filename>           : output map",
            " -cell <x> <y> <z>         : set x/y/z-length x/y/z",
            " -label <DepCode>          : write new label",
            " -gridsampling <x> <y> <z> : set x/y/z- grid sampling",
            " -gridstart <x> <y> <z>    : set x/y/z- grid start point",
            " -voxel <x> <y> <z>        : set x y z pixel spacing"
        };
        defaultOptions =  newOptions;
    }
    
    // Remove the -sign option
    if (programName.equals("mapFixBig.jar")){
        String[] newOptions = new String[defaultOptions.length -1];
        for (int i=0, j=0; i < defaultOptions.length; ++i){
            String term = defaultOptions[i];
            if (!term.startsWith(" -sign")){
                newOptions[j] =  defaultOptions[i];
                 ++j;   
            }
        }
        defaultOptions =  newOptions;
    }

    String temp;
    for (int i = 0; i < arg.length; i++) {
      if (arg[i].startsWith("-")) {
        temp = arg[i].toUpperCase();
        if (temp.startsWith("-H")) {
          PrintOptions(defaultOptions, programName);
        } else if (temp.startsWith("-WOR")) {
           if (setWord == null) setWord = new int[10];
           if (setVal == null) setVal = new String[10];
           setWord[nWord] = getWord(temp);
           setVal[nWord] = getVal(setWord[nWord],arg[i+1]);
           nWord = nWord + 1;
        } else if (temp.startsWith("-SIG")) {
           sign = true;
        } else if (temp.startsWith("-IN")) {
           fin = arg[++i];
        } else if (temp.startsWith("-OUT")) {
           fout = arg[++i];
        } else if (temp.startsWith("-MIN")) {
           fixMin = true;
        } else if (temp.startsWith("-MAX")) {
           fixMax = true;
        } else if (temp.startsWith("-MEAN")) {
           fixMean = true;
        } else if (temp.startsWith("-RMS")) {
           fixRMS = true;
        } else if (temp.startsWith("-SYM")) {
           fixSymOp = true;
        } else if (temp.startsWith("-MAPT")) {
           fixMapFileType = true;
        } else if (temp.startsWith("-SP")) {
           fixSP = true;
        } else if (temp.startsWith("-ANG")) {
           fixAngle = true;
        } else if (temp.startsWith("-LAB")) {
           fixLabel = true;
           text = arg[++i];
        } else if (temp.startsWith("-VOX")) {
           fixVoxel = true;
           pixelX = Float.valueOf(arg[++i]).floatValue();
           pixelY = Float.valueOf(arg[++i]).floatValue();
           pixelZ = Float.valueOf(arg[++i]).floatValue();
        } else if (temp.startsWith("-CEL")) {
           fixCell = true;
           cellX = Float.valueOf(arg[++i]).floatValue();
           cellY = Float.valueOf(arg[++i]).floatValue();
           cellZ = Float.valueOf(arg[++i]).floatValue();
        } else if (temp.startsWith("-GRIDST")) {
           fixGridStart = true;
           ix = Integer.valueOf(arg[++i]).intValue();
           iy = Integer.valueOf(arg[++i]).intValue();
           iz = Integer.valueOf(arg[++i]).intValue();
        } else if (temp.startsWith("-GRIDSA")) {
           fixGridSampling = true;
           sx = Integer.valueOf(arg[++i]).intValue();
           sy = Integer.valueOf(arg[++i]).intValue();
           sz = Integer.valueOf(arg[++i]).intValue();
        } else if (temp.startsWith("-GRIDC")) {
           fixGridCentre = true;
        } else if (temp.startsWith("-ORIG")) {
           setOriginToZero = true;
        } else if (temp.startsWith("-REAL")) {
           fixReal2Grid = true;
        } else if (temp.startsWith("-ZZZZ")) {
           specialMode = Integer.valueOf(arg[++i]).intValue();
           System.err.println(" WARNING : special fix mode " + specialMode);
           specialFix = 121;
        } else if (temp.startsWith("-NULL")) {
           fixNullChar = true;
        } else if (temp.startsWith("-ALL")) {
           fixMin = true;
           fixMax = true;
           fixMean = true;
           fixRMS = true;
           fixSymOp = true;
           fixAngle = true;
  //         fixNullChar = true;
           fixSP = true;
           fixMapFileType = true;
        } else{
           System.err.println("Error, unknown option: "+ arg[i] + ".");
           PrintOptions(defaultOptions, programName);
        }
      }
    }

    if (fin == null || fout == null) {
      System.err.println("Both options -in and -out must be specified.");
      PrintOptions(defaultOptions, programName);
    }
  }

  private void PrintOptions(String[] options, String programName) {
    for (int i=0; i < options.length; ++i){
      System.err.println(options[i]);
    }

    System.err.print(" Recommend : java -Xms256m -Xmx256m -jar "+ programName +" -in <filein> -out <fileout>");
    if (programName.equals("mapFixDep.jar")){
        System.err.println(" -voxel X Y Z -label 'D_120001'");
    }
    else if (programName.equals("mapFixAnot.jar")){
        System.err.println("");
    }
    else{
        System.err.println(" -all");
    }
    System.exit(-1);
  }

  private String getVal(int record, String word) {
    // all testing of which word is in the setter routine of getHeader
    return word;
  }

  private int getWord(String word) {

    int iret = -1;
    
    String ret = word.substring(5);

    try {
      iret = Integer.parseInt(ret);
      if (iret < 0 || iret > 256) {
        System.err.println(" Word access OUB " + iret);
        throw new RuntimeException();
      }
    } catch (NumberFormatException ex) {
      System.err.println(" Invalid word access (not an integer) " + ret);
      throw new RuntimeException();
    }

    return iret;

  }

}
