/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: TextForSpecIterator.java 36 2011-09-15 11:54:05Z johann.petrak $
 */

package at.ofai.gate.virtualdocuments;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Utils;
import gate.util.GateRuntimeException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

  public class TextForSpecIterator implements Iterator<String> {

    static final String ERRMSG = "before first call of next() or after hasNext() is false";
    Document theDocument;
    AnnotationSet theAnnSet;
    AnnotationSet curOff_Set;
    int toOffset = 0; // the offset after the last character to include
    int curOffset = 0;
    List<AnnotationParm> annSpecs;
    int curAnnSpec = 0;
    private Set<String> annotationTypes = new HashSet<String>();
    boolean takeAll;
    boolean takeOverlapping;

    TextForSpecIterator(Document doc, String annSetName, List<AnnotationParm> annParms, Set<String> anntypes, boolean takeall, boolean takeoverlapping) {
      takeAll = takeall;
      takeOverlapping = takeoverlapping;
      annotationTypes = anntypes;
      annSpecs = annParms;
      init(doc,annSetName,null,null);
    }
    TextForSpecIterator(Document doc, String annSetName, Long fromOffset, Long toOffset, List<AnnotationParm> annParms, Set<String> anntypes, boolean takeall, boolean takeoverlapping) {
      takeAll = takeall;
      takeOverlapping = takeoverlapping;
      annotationTypes = anntypes;
      annSpecs = annParms;
      init(doc,annSetName,fromOffset,toOffset);
    }

    private void init(Document doc, String annSetName, Long from, Long to) {
      theDocument = doc;
      theAnnSet = theDocument.getAnnotations(annSetName);
      if(to == null) {
        toOffset = gate.Utils.length(theDocument);
      } else {
        toOffset = to.intValue();
      }
      if(from == null) {
        curOffset = 0;
      } else {
        curOffset = from.intValue();
      }
      theAnnSet = theAnnSet.get(annotationTypes);
      theAnnSet = theAnnSet.get(new Long(curOffset),new Long(toOffset));
      //logger.debug("Annotations in start set: "+theAnnSet.size());
      //logger.debug("Parameter specifications: "+annSpecs);
      curAnnSpec = 0;   // the annotation specification to process next
      curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
      determineNextMatch();
    }

    // this always contains the the next annotation or null if no more
    // annotations are available;
    protected Annotation nextAnnotation = null;

    protected Annotation currentAnnotation = null;

    // the number of the specification used for finding nextAnnotation
    // This field is set by determineNextMatch.
    // If no annotation is found, this is set to -1
    protected int nextSpecNo = -1;

    // The number of the specification used for the annotation that the last
    // call to next() returned. This is set in next()
    protected int currentSpecNo = -1;

    private String nextContent = null;
    private String currentContent = null;

    private Long currentOffset = null;
    private Long nextOffset = null;

    private String currentSpec = null;
    private String nextSpec = null;

    // this finds the next annotation if there is one and stores it in
    // nextAnnotation -- if there is none, store null.
    // All the additional information for that annotation is also stored in
    // the respective next... fields. Each call to next() sets the
    // current... fields to the next... fields and calculates the new next...
    // fields (until there is nothing more found).
    // All the get... methods return the content of the current... fields
    // so, once next() has been called if an annotation is still there,
    // all the get... methods can be called as often as wanted to return
    // the various pieces of information for the current annotation.
    protected void determineNextMatch() {
      // at any time, the variables curOffset and curAnnSpec point to the
      // offset and annotation specification to process next, except when
      // no annotation was found, then curOffset is set to toOffset

      // first set nextAnnotation to null, so if we find nothing we already
      // have the correct value
      nextContent = null;
      nextAnnotation = null;
      nextOffset = -1l;
      nextSpecNo = -1;
      nextSpec = null;
      //System.out.println("Trying to determine next content curOffset="+curOffset+", toOffset="+toOffset);
      while (curOffset < toOffset) {
        // lets see if we find an annotation related to curAnnSpec at curOffset
        //logger.debug("Checking offset/sec: "+curOffset+"/"+curAnnSpec);
        //System.out.println("Iterating at offset="+curOffset+" of "+toOffset+", curAnnSpec="+curAnnSpec+", got="+curOff_Set.size());
        AnnotationParm parm = annSpecs.get(curAnnSpec);
        String typeName = parm.getTypeName();
        Set<String>  featureSet = parm.getFeatureSet();
        String featureName = parm.getFeatureName();
        String constantValue = parm.getConstantValue();
        AnnotationSet tmpSet = null;
        //logger.debug("Checking specification type/feature: "+typeName+"/"+featureName);
        //logger.debug("Annotations in CurAnnSet: "+curOff_Set.size());
        if(typeName.equals("@STRING")) {
          // if we arrive at this spec, set the content to the character
          // at the current position, leave the ann null and advance the
          // offset by one and reset the spec number to 0
          nextAnnotation = null;
          nextSpecNo = curAnnSpec;
          nextContent = theDocument.getContent().toString().substring(curOffset,curOffset+1);
          nextOffset = new Long(curOffset);
          nextSpec = annSpecs.get(nextSpecNo).toString();
          curAnnSpec = 0;
          curOffset++;
          curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
          //System.out.println("B: Going to next offset="+curOffset+" found "+curOff_Set.size());
          break;
        }
        // TODO: when is this needed? if curOff_Set is empty, only if one
        // of the next annSpecs is @String, otherwise we will never get
        // something
        if(curOff_Set.size() == 0) {
          //System.out.println("curOff_Set is empty, get for next annspec");
          curAnnSpec++;
          if(curAnnSpec >= annSpecs.size()) {
            curAnnSpec = 0;
            curOffset++;
            curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
            //System.out.println("C: Going to next offset="+curOffset+" found "+curOff_Set.size());
          }
          continue;
        }
        if(featureSet == null) {
          tmpSet = curOff_Set.get(typeName);
        } else {
          tmpSet = curOff_Set.get(typeName,featureSet);
        }
        //logger.debug("Size of tmpSet: "+tmpSet.size());
        //System.out.println("After checking for anns, tmpSet has "+tmpSet.size());
        if(!tmpSet.isEmpty()) {
          // get the longest annotation in the set, but only if it fits
          // and ends before toOffset
          Annotation ann = null;
          int maxlength = -1;
          Iterator<Annotation> it = tmpSet.iterator();
          while(it.hasNext()) {
            Annotation tmpAnn = it.next();
            if(Utils.length(tmpAnn) > maxlength &&
               tmpAnn.getEndNode().getOffset().intValue() <= toOffset) {
              maxlength = Utils.length(tmpAnn);
              ann = tmpAnn;
            }
          }
          // ann should now contain the longest annotation, or is still null
          // if none of the ones found fitted
          if(ann == null) {
            //logger.debug("Did not find a fitting longest annotation");
            //System.out.println("Did not find a fitting longest ann");
            curAnnSpec++;
            if(curAnnSpec >= annSpecs.size()) {
              curAnnSpec = 0;
              curOffset++;
              curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
              //System.out.println("A: Going to next offset="+curOffset+" found "+curOff_Set.size());
            }
            continue;
          }
          String toAppend;
          if(constantValue != null) {
            toAppend = constantValue;
            //logger.debug("Getting constant value");
            //System.out.println("Appending constant value: >"+toAppend+"<");
          } else if(featureName != null) {
        	//logger.debug("Getting value for feature "+featureName);
            toAppend = ann.getFeatures().get(featureName).toString();
            //System.out.println("Appending for feature: "+toAppend);
          } else {
        	//logger.debug("Getting underlying string");
            toAppend = Utils.stringFor(theDocument, ann);
            //System.out.println("Appending string: "+toAppend);
          }
          //logger.debug("Found this content: "+toAppend);
          // ann is the annotation we want to pick, so set it
          nextAnnotation = ann;
          nextSpecNo = curAnnSpec;
          nextContent = toAppend;
          nextOffset = ann.getStartNode().getOffset();
          nextSpec = annSpecs.get(nextSpecNo).toString();
          // depending on the parameter settings, advance the offset
          if(takeAll) {
            // do not advance the offset, just the spec, but if we have
            // no more specs, also advance offset
            curAnnSpec++;
            if(curAnnSpec >= annSpecs.size()) {
              curAnnSpec = 0;
              curOffset++;
              curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
              //System.out.println("D: Going to next offset="+curOffset+" found "+curOff_Set.size());
            }
          } else if(takeOverlapping) {
            // go to next offset and start with parameters all over
            curAnnSpec = 0;
            curOffset++;
            curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
            //System.out.println("E: Going to next offset="+curOffset+" found "+curOff_Set.size());
          } else {
            // skip after end of currently processed annotation and start with
            // specs all over
            curOffset += gate.Utils.length(ann);
            curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
            //System.out.println("F: Going to next offset="+curOffset+" found "+curOff_Set.size());
            curAnnSpec = 0;
          }
          break;
        } else {
          // nothing found at that offset, try next spec or next offset
          curAnnSpec++;
          if(curAnnSpec >= annSpecs.size()) {
            curAnnSpec = 0;
            curOffset++;
            curOff_Set = Utils.getAnnotationsAtOffset(theAnnSet,new Long(curOffset));
            //System.out.println("G: Going to next offset="+curOffset+" found "+curOff_Set.size());
          }
        }
      }
    }

    /**
     * The number of the specification used to find the annotation returned
     * by the latest call to {@link #next()}.
     * Note that this method must be called AFTER next()!
     *
     * @return currentSpecNo - the number of the current annotation.
     */
    public int getSpecNo() {
      if(currentContent == null) {
        throw new GateRuntimeException("Method getSpecNo "+ERRMSG);
      }
      return currentSpecNo;
    }

    public String getContent() {
      if(currentContent == null) {
        throw new GateRuntimeException("Method getContent "+ERRMSG);
      }
      return currentContent;
    }

    public Long getOffset() {
      if(currentContent == null) {
        throw new GateRuntimeException("Method getOffset "+ERRMSG);
      }
      return currentOffset;
    }

    public boolean hasNext() {
      return nextContent != null;
    }

    public String getSpec() {
      if(currentContent == null) {
        throw new GateRuntimeException("Method getSpec "+ERRMSG);
      }
      return currentSpec;
    }

    public Annotation getAnnotation() {
      if(currentContent == null) {
        throw new GateRuntimeException("Method getAnnotation "+ERRMSG);
      }
      return currentAnnotation;
    }

    public String next() {
      currentAnnotation = nextAnnotation;
      currentSpecNo = nextSpecNo;
      currentContent = nextContent;
      currentOffset = nextOffset;
      currentSpec = nextSpec;
      determineNextMatch();
      return currentContent;
    }

    public void remove() {
      throw new UnsupportedOperationException("Remove is not supported!");
    }

  }
