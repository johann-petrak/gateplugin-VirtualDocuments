/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: AnnotationParm.java 36 2011-09-15 11:54:05Z johann.petrak $
 */

package at.ofai.gate.virtualdocuments;

import java.util.HashSet;
import java.util.Set;


 public class AnnotationParm {
    private String theTypeName;
    public String getTypeName() { return theTypeName; }
    private String theFeatureName;
    public String getFeatureName() { return theFeatureName; }
    private String theConstantValue;
    public String getConstantValue() { return theConstantValue; }
    private Set<String> theFeatureSet;
    public Set<String> getFeatureSet() { return theFeatureSet; }
    @Override
    public String toString() {
      String ret = "";
      ret += getTypeName();
      if(getFeatureName() != null && !getFeatureName().equals("")) {
        ret += "." + getFeatureName();
      }
      return ret;
    }


    AnnotationParm(String typeName, String featureName, String constantValue) {
      theTypeName = typeName;
      theFeatureName = featureName;
      theConstantValue = constantValue;
      if(theFeatureName != null) {
        theFeatureSet = new HashSet<String>();
        theFeatureSet.add(theFeatureName);
      } else {
        theFeatureSet = null;
      }
    }
    AnnotationParm(String origparm) {
      // the general format is <typeName>[.<featureName>][->constantValue]
      // the special construct @STRING must occur all by itself

      // NOTE: we cannot trim, since a trailing space might be significant,
      // e.g. for a specification "SpaceToken-> " which means that each
      // space token is replaced by a literal space.
      //originalParm = originalParm.trim();
      String originalParm = origparm;
      theTypeName = null;
      String[] s1 = originalParm.split("->",2);
      if(s1.length == 1) {
        theConstantValue = null;
      } else if(s1.length == 2) {
        theConstantValue = s1[1];
        originalParm = s1[0];
      }
      s1 = originalParm.split("\\.",2);
      if(s1.length == 1) {
        theFeatureName = null;
        theFeatureSet = null;
        theTypeName = originalParm;
      } else if(s1.length == 2) {
        theFeatureName = s1[1];
        theFeatureSet = new HashSet<String>();
        theFeatureSet.add(theFeatureName);
        theTypeName = s1[0];
      }
    }
  }

