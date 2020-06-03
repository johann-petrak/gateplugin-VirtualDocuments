/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: OffsetMapping.java 36 2011-09-15 11:54:05Z johann.petrak $
 */

package at.ofai.gate.virtualdocuments;

import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.List;

  /**
   * This class provides a mapping between ranges of text within
   * two documents. Each offset in the source document maps to a starting
   * position and to an ending position. Several consecutive offsets can
   * map to identical starting and ending positions. Only if it is known
   * that a range in the target document is truely a copy of the source
   * document, every individual source offset will map to an individual
   * target to and from offset. Otherwise all offsets withing the range will map to
   * the same identical to and from offsets.
   *
   */
  public abstract class OffsetMapping {
    protected List<Integer> mapFrom = new ArrayList<Integer>();
    protected List<Integer> mapTo = new ArrayList<Integer>();

    // this keeps the source offset of the last mapping the addMapping
    // method was processing so we can see if the current mapping overlaps
    // or is not in increasing offset order.
    protected int lastSourceOffset = -1;

    protected int currentSourceOffset = -1;

    // this always contains the current target offset which is identical
    // to the target length.
    protected int currentTargetOffset = 0;

    @Override
    public String toString() {
      return "Mapping:"+mapFrom.size()+"/"+mapTo.size();
    }

    public abstract void addMapping(
            int sourceOffset,
            int sourceLength, int targetLength,
            boolean isCopy);

    public int getFrom(int offset) {
      return mapFrom.get(offset);
    }
    public Long getFromLong(Long offset) {
      return new Long(mapFrom.get(offset.intValue()));
    }

    public int getTo(int offset) {
      return mapTo.get(offset);
    }
    public Long getToLong(Long offset) {
      return new Long(mapTo.get(offset.intValue()));
    }

    public void addFrom(int pos, int what) {
      addToMap(mapFrom, pos, what);
    }

    public void addTo(int pos, int what) {
      addToMap(mapTo, pos, what);
    }

    private void addToMap(List<Integer> map, int pos, int what) {
      // this makes sure we cannot get a index out of bounds exception
      // - if the index pos is already part of the vector, we show a warning
      // - if the index pos is exactly one more than the current last index
      //   silently do it
      // - if the index pos is larger than one more than the current index,
      //   insert the missing elements and show a warning
      int size = map.size();
      if(pos < 0) {
        throw new GateRuntimeException("Trying to set map element at index "+pos);
      }
      if(pos == size) {
        map.add(pos,what);
      } else if(pos < size) {
        System.err.println("Trying to set map element that exists: "+pos+" size is "+size);
        map.set(pos, what);
      } else if(pos > size) {
        System.err.println("Trying to set map element after gap: "+pos+" size is "+size);
        for(int i = size; i <= pos; i++) {
          map.add(i,what);
        }
      }
    }

  }

