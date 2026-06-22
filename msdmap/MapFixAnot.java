package msdmap;

/* =============================================
Author  Eduardo Sanz
Date    19 January 2015
What    Interface for the Annotation module in the D&A project
*/

import java.io.*;
import java.lang.RuntimeException;

public class MapFixAnot {
  
    public static void main(String arguments[]) throws Exception{
        
        GetArgs getArgs = new GetArgs();
        DAInternals.setup();
        String programName = "mapFixAnot.jar";
        try{
            getArgs.ParseArguments(arguments, programName);
            if (getArgs.sign) {
                DAInternals.errors.add("mapFixAnot.jar does not support -sign option.");
                DAInternals.PrintJsonAndExit(-1);
            }
            if (getArgs.fixVoxel == false) {
                DAInternals.errors.add("-voxel option is mandatory.");
                DAInternals.PrintJsonAndExit(-1);
            }
        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            DAInternals.errors.add("Problem parsing the command line arguments.");
            for (StackTraceElement ste : e.getStackTrace()) {
                System.err.println(ste);
            }
            DAInternals.PrintJsonAndExit(-1);
        }

        DAInternals.convert(getArgs, programName);
    }
}
