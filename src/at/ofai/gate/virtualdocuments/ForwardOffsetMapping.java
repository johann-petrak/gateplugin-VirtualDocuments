/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: ForwardOffsetMapping.java 36 2011-09-15 11:54:05Z johann.petrak $
 */

package at.ofai.gate.virtualdocuments;


  /**
   * This class represents a mapping from a source document to a target
   * document where the target document was created from annotations in the
   * source document with these possible properties:
   * <ul>
   * <li>Source annotations might not be directly following each other (there
   * may be gaps between them)
   * <li>Source annotations may overlap
   * <li>only part of the document is covered by the source annotation
   * specifications.
   * </ul>
   * Since several specifications can map a single offset with processing
   * modes takeAll or takeOverlapping set to true, it should be possible to
   * map a single input offset to several from/to output offsets.
   * However, for simplicity,
   * we do not do that for now. Instead, each offset will map to that to-range
   * that is first created for that offset.
   *
   * In other words, as soon as there is a from and to offset for a given
   * input offset, that value will not be overwritten.
   */
 class ForwardOffsetMapping extends OffsetMapping {

    public ForwardOffsetMapping() {
      lastSourceOffset = 0;
      currentSourceOffset = 0;
    }
    public void addMapping(
          int sourceOffset,
          int sourceLength, int targetLength,
          boolean isCopy) {
      //System.out.println("addMapping: "+sourceOffset+"/"+sourceLength+"/"+targetLength+"/"+isCopy+" -- "+lastSourceOffset+"/"+currentSourceOffset);
      if(sourceOffset < lastSourceOffset) {
        //throw new IllegalArgumentException("sourceOffset "+sourceOffset+" is smaller than last sourceOffset "+lastSourceOffset);
        System.err.println("sourceOffset "+sourceOffset+" is smaller than last sourceOffset "+lastSourceOffset);
      }

      if(sourceOffset < currentSourceOffset) {
        // we have got overlap!
        // if there is a part of the source that does not overlap,
        // that part should get mapped, otherwise set the source length
        // to 0
        if(sourceOffset+sourceLength > currentSourceOffset) {
          int nonOverlappingLength = sourceOffset+sourceLength-currentSourceOffset;
          // create mappings for the nonoverlapping length, starting at
          // currentSourceOffset
          sourceOffset = currentSourceOffset;
          sourceLength = nonOverlappingLength;
          isCopy = false; // even if it was true, reset to false since we modified
        } else {
          sourceLength = 0;
        }
      } else if(sourceOffset > currentSourceOffset) {
        // we have a range that comes after a gap, need to map the gap
        // so all the offsets in the gap need to have their from and too
        // mappings set to the current target offset
        int gapLength = sourceOffset - currentSourceOffset;
        for(int i=0; i<gapLength; i++) {
          //mapFrom.add(currentSourceOffset+i, currentTargetOffset);
          addFrom(currentSourceOffset+i,currentTargetOffset);
          //mapTo.add(currentSourceOffset+i, currentTargetOffset);
          addTo(currentSourceOffset+i, currentTargetOffset);
        }
        // do the actual mapping normally later
        currentSourceOffset += gapLength;
        isCopy = false;
      }
      // do the mapping: if isCopy is true for each offset individually,
      // otherwise for the whole range
      for(int i=0; i<sourceLength; i++) {
        if(isCopy) {
          //mapFrom.add(currentSourceOffset+i, currentTargetOffset+i);
          addFrom(currentSourceOffset+i, currentTargetOffset+i);
          //mapTo.add(currentSourceOffset+i, currentTargetOffset+i+1);
          addTo(currentSourceOffset+i, currentTargetOffset+i+1);
        } else {
          //mapFrom.add(currentSourceOffset+i, currentTargetOffset);
          addFrom(currentSourceOffset+i, currentTargetOffset);
          //mapTo.add(currentSourceOffset+i, currentTargetOffset+targetLength);
          addTo(currentSourceOffset+i, currentTargetOffset+targetLength);
        }
      } // for
      // adjust
      lastSourceOffset = currentSourceOffset;
      currentSourceOffset += sourceLength;
      currentTargetOffset += targetLength;
    }
  }

