/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: BackwardOffsetMapping.java 40 2011-09-15 13:25:31Z johann.petrak $
 */
package at.ofai.gate.virtualdocuments;

import gate.util.GateRuntimeException;


class BackwardOffsetMapping extends OffsetMapping {
    public void addMapping(
          int sourceOffset,
          int sourceLength, int targetLength,
          boolean isCopy) {
      // each output range maps back to exactly the offsets of the input range
      // no difficulties like with the forward mapping here as there cannot
      // be overlaps in the output ranges
      if(currentTargetOffset != mapFrom.size() || currentTargetOffset != mapTo.size()) {
        throw new GateRuntimeException("Problem adding backward mapping: currentTargetOffset="+currentTargetOffset+" mapFrom.size="+mapFrom.size()+" mapTo.size="+mapTo.size());
      }
      for(int i=0; i<targetLength; i++) {
        mapFrom.add(currentTargetOffset+i, sourceOffset);
        //!System.out.println("to mapFrom: "+(currentTargetOffset+i)+"--"+sourceOffset);
        mapTo.add(currentTargetOffset+i, sourceOffset+sourceLength);
        //!System.out.println("to mapTo:  "+(currentTargetOffset+i)+"--"+(sourceOffset+sourceLength));
      }
      // adapt
      currentTargetOffset += targetLength;
    }
  }
