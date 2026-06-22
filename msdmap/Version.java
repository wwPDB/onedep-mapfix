package msdmap;

/*
Copyright [2012] EMBL - European Bioinformatics Institute
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
*/

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Version {
  
  public Version() {

    System.err.println(" ============= Version date 13th March 2014  (Production) ================== ");
    System.err.println(" Word editing option in mapFix.jar and mapFixBig.jar:");
    System.err.println("    -word1 100 #sets word 1 (NC) to 100");
    System.err.println("    -word14 10 #sets word 14 (Cell dimension angle alpha) to 10");
    System.err.println(" For detail instructions see:");
    System.err.println("    http://wiki.pdbe.ebi.ac.uk/index.php?title=The_map_header_detail");
    System.err.println("    and http://emdb-info.rutgers.edu/mapfix");
    System.err.println(" ========================================================================== ");

  }

}
