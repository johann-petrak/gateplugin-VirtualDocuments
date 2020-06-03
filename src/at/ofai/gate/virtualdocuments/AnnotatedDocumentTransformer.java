/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: $
 */

package at.ofai.gate.virtualdocuments;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Annotation;
import gate.FeatureMap;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import org.apache.log4j.Logger;

/**
 * A class that can be used to extract a string 
 * (with {@link #getStringForDocument(Document, String)}) 
 * or a sequence of annotations
 * (with {@link #getIterator(Document, String)}
 * from an annotated document based on a list of annotation specifications
 * and a list of processing options which have to be provided when an object of
 * this class is created. The annotation specifications describe
 * which annotations and features to pick in which order of preference and 
 * whether to use the document text covered by an annotation, the value
 * of a feature of an annotation, or some constant string value in case 
 * an annotation type or annotation with some feature is found.
 * The processing options specify if and how to separate the strings which are 
 * found based on the annotation specification or how to look for the next
 * matching annotation after an annotation has been found.
 * <p>  
 * The annotation specifications list consists of entries as described below,
 * where the order of the entries expresses decreasing priority:
 * <ul>
 * <li>annotationtype - if an annotation type "annotationtype"
 * is found, take the text covered by that annotation
 * <li>annotationtype->value - this matches in the same way as the previous 
 * specification but takes the value specified instead of the text covered 
 * by the annotation.
 * is found, take the value specified after the arrow
 * <li>annotationtype.featurename - if an annotation of
 * type "annotationtype" is found with a non-null value for the feature with 
 * the name "featurename", take the value of the feature.
 * <li>annotationtype.featurename->value - this matches in the same way as 
 * the previous specification but takes the value specified instead of the 
 * value of the feature.
 * <li>@STRING - if no annotation matches, take the original document text 
 * until some other higher priority specification matches. This can only be
 * reasonably be used as the last entry in the list (since it will always match).
 * </ul>
 * Note that trailing spaces are not ignored as they may be significant,
 * for example for a specification like "SpaceToken-> " which specifies that
 * for each SpaceToken annotation a literal space should be generated.
 * <p>
 * The processing option list consists of entries of the following form, 
 * in any order:
 * <ul>
 * <li>takeAll=true|false if true, apply all matching specifications at some
 * offset, if false only the one with the highest priority. If there are several
 * annotations matching a single specification, only the longest match is
 * selected, no matter what this parameter is set to.
 * <li>takeOverlapping=true|false if true, try all offsets within a range
 * that already matched, if false, proceed to the offset behind the current
 * match.
 * <li>separator=somestring how to separate the output from different offsets
 * <li>separatorSame=someString how to seperate the output that comes from
 * identical offsets. If not set, use the same string as for <code>separator</code>.
 * <li>terminator=string how to terminate the output string.
 * <li>separatorKeyValue=someString if this is specified and not empty, the
 * output will have the format "key<someString>value" where key is the
 * specification, someString what has been specified for this parameter and
 * value the text added to the output.
 * </ul>
 * <p>
 * Note that some of the annotation specifications or processing options do not
 * make sense when used with {@link #getIterator}. 
 * <p>
 * Before extracting annotations, the user can request that mappings from
 * offsets in the original document to the output string and/or mappings
 * from the offsets in the output string back to the original document are 
 * creating by calling {@link #setGenerateForwardOffsetMap} with parameter true
 * and/or {@link #setGenerateBackwardOffsetMap} with parameter true.
 * This is implemented as a setter method and not as a processing parameter
 * since it is something that the end user in general should not be able to
 * influence.
 * <p>
 * When those maps have been generated, the methods 
 * {@link #addBackMappedAnnotations} and
 * {@link #addForwardMappedAnnotations} (NOT YET IMPLEMENTED) can be used 
 * to transfer annotations from the virtual document to the original document
 * or from the original document to the virtual document.
 * <p>
 * TODOS:
 * <ul>
 * <li>IMPORTANT: transfer the code to actually create a virtual document
 * into this class!
 * </ul>
 * 
 * @author Johann Petrak
 */
public class AnnotatedDocumentTransformer {

  private List<AnnotationParm> theAnnotationParms = new ArrayList<AnnotationParm>();

  private List<String> originalAnnotationParms;

  public Set<String> annotationTypes = new HashSet<String>();
  
  // processing parms

  // take all annotations at a single position
  private Boolean takeAll = false;
  // take all new annotations encountered during annotations that have already
  // been processed (overlapping with processed but first at current)
  private Boolean takeOverlapping = false;
  // The separator between what we take from successive offsets
  private String separator = "";
  // The separator between what we take from the same offset, if we do
  // If null, same as whatever separator has been set or defaults to
  private String separatorSame = null;
  // What to append at the end of the string extracted from a document
  private String terminator = "";
  // If specified (not null) a string to insert between a key/value pair
  // generated for an annotation (this will not work with @STRING 
  // this will generate as the keyword the original annotation specification
  // and as the value the value returned for that specification
  private String separatorKeyValue = null;

  private Boolean generateForwardOffsetMap = false;
  private Boolean generateBackwardOffsetMap = false;

  public Boolean getGenerateForwardOffsetMap() {
    return generateForwardOffsetMap;
  }

  public Boolean getGenerateBackwardOffsetMap() {
    return generateBackwardOffsetMap;
  }

  
  private OffsetMapping theForwardOffsetMapping = null;
  public OffsetMapping getForwardOffsetMap() {
    return theForwardOffsetMapping;
  }

  private OffsetMapping theBackwardOffsetMapping = null;
  public OffsetMapping getBackwardOffsetMap() {
    return theBackwardOffsetMapping;
  }

  // if we do both forward and backward mapping, a set to remember which
  // annotations have been forward mapped, so we can avoid to needlessly
  // mapping them back
  private HashSet<Annotation> forwardMappedAnnotations;
  
  protected Logger logger;

  public AnnotatedDocumentTransformer(
          List<String> annotationParms,
          FeatureMap processingParms,
          boolean generateForwardOffsetMap,
          boolean generateBackwardOffsetMap)
    throws InvalidNameException {

    this.generateForwardOffsetMap = generateForwardOffsetMap;
    this.generateBackwardOffsetMap = generateBackwardOffsetMap;
    initMappings();
    
    // if the annotationParms is null, we have an error
    logger = Logger.getLogger(this.getClass().getName());
    if(annotationParms == null) {
      throw new IllegalArgumentException("annotationParms must not be null");
    }
    if(annotationParms.isEmpty()) {
      throw new IllegalArgumentException("annotationParms must not be empty");
    }
    // if the processing parms is null, create an empty list for it
    if(processingParms == null) {
      processingParms = Factory.newFeatureMap();
    }
    int i = 0;
     for(String parm : annotationParms) {
       AnnotationParm p = new AnnotationParm(parm);
       theAnnotationParms.add(p);
       if(p.getTypeName() != null) {
         annotationTypes.add(p.getTypeName());
       }
       if(p.getTypeName().equals("@STRING")) {
         if(i < annotationParms.size()-1) {
           throw new IllegalArgumentException("@STRING can only occur as the last specification");
         }
       }
       i++;
     }
     originalAnnotationParms = new ArrayList<String>(annotationParms);
     
     
     for(Object keyObject : processingParms.keySet()) {
       String key = (String)keyObject;
       String val = (String)processingParms.get(keyObject);
       if(key.equalsIgnoreCase("takeall")) {
         takeAll = Boolean.valueOf(val);
       } else if(key.equalsIgnoreCase("takeOverlapping")) {
         takeOverlapping = Boolean.valueOf(val);
       } else if(key.equalsIgnoreCase("separator")) {
         separator = val;
       } else if(key.equalsIgnoreCase("separatorSame")) {
         separatorSame = val;
       } else if(key.equalsIgnoreCase("separatorKeyValue")) {
         separatorKeyValue = val;
       } else if(key.equalsIgnoreCase("terminator")) {
         terminator = val;
       } else {
         throw new IllegalArgumentException("Unknown parameter/value: "+key+"/"+val);
       }
     }
     // now, if separatorSame is still null, set it to whatever separator
     // is set
     if(separatorSame == null) {
       separatorSame = separator;
     }
  }

  private void initMappings() {
     if(generateForwardOffsetMap) {
       theForwardOffsetMapping = new ForwardOffsetMapping();
     }
     if(generateBackwardOffsetMap) {
       theBackwardOffsetMapping = new BackwardOffsetMapping();
     }
     if(generateBackwardOffsetMap && generateForwardOffsetMap) {
       forwardMappedAnnotations = new HashSet<Annotation>();
     }
  }

  public String getStringForDocument(Document aDocument, String annSetName) {
    initMappings();
    TextForSpecIterator it =
            new TextForSpecIterator(aDocument,annSetName,theAnnotationParms, annotationTypes, takeAll, takeOverlapping);
    StringBuilder resultString =
            new StringBuilder(aDocument.getContent().size().intValue());
    boolean first = true;
    Long lastOffset = -1l;
    int outOffset = 0;
    int inOffset = 0;
    int sourceLen = 0;
    while(it.hasNext()) {
      String toAppend = it.next();
      Annotation ann = it.getAnnotation();
      if(ann == null) { // if @STRING is matched
        sourceLen = 1;
      } else {
        inOffset = ann.getStartNode().getOffset().intValue();
        sourceLen = (int)(ann.getEndNode().getOffset() - ann.getStartNode().getOffset());
        //sourceLen = Utils.length(ann);
      }
      //System.out.println("Got content >"+toAppend+"< annotation: "+ann);
      // check if we process the very first part, if yes, no separator
      // string (if any is defined) needs to be inserted, otherwise
      // insert the one that applies if we are at a new or the same
      // offset as before.
      if(first) {
        first = false;
      } else {
        if(it.getOffset().equals(lastOffset)) {
          resultString.append(separatorSame);
          addMappings(ann.getEndNode().getOffset().intValue(), outOffset, 0, separatorSame.length(), false);
          outOffset += separatorSame.length();
        } else {
          resultString.append(separator);
          // TODO: if we get the result of @STRING, ann is null!
          // In that case, the
          if(ann == null) {
            addMappings((int)(it.getOffset()+it.getContent().length()), outOffset, 0, separator.length(), false);
          } else {
            addMappings(ann.getEndNode().getOffset().intValue(), outOffset, 0, separator.length(), false);
          }
          outOffset += separator.length();
          lastOffset = it.getOffset();
        }
      }
      // insert the actual string as requested and add a mapping for it
      if(separatorKeyValue != null) {
        resultString.append(it.getSpec());
        resultString.append(separatorKeyValue);
        resultString.append(toAppend);
        int newLength = it.getSpec().length() + separatorKeyValue.length() + toAppend.length();
        addMappings(inOffset, outOffset, sourceLen, newLength, false);
      } else {
        resultString.append(toAppend);
        //!System.out.println("Appending: >"+toAppend+"< origOff="+inOffset+" virtOff="+outOffset+" srcLen="+sourceLen);
        addMappings(inOffset, outOffset, sourceLen, toAppend.length(), false);
      }
      outOffset += toAppend.length();
      inOffset += sourceLen;
    }
    
    // finally, append the terminator string, if any
    if(terminator != null && !terminator.equals("")) {
      resultString.append(terminator);
    }
    //System.out.println("Content sizes old/new: "+aDocument.getContent().size()+"/"+resultString.length());
    String tmp = aDocument.getContent().toString().replaceAll("\\n", " ");
    //System.out.println("Content old: >"+tmp+"<");
    tmp = resultString.toString().replaceAll("\\n", " ");
    //System.out.println("Content new: >"+tmp+"<");
    if(generateBackwardOffsetMap) {
      //System.out.println("backward map generated: "+theBackwardOffsetMapping);
      //System.out.println("bwmap from: "+theBackwardOffsetMapping.mapFrom);
      //System.out.println("bwmap to:   "+theBackwardOffsetMapping.mapTo);
    }
    if(generateForwardOffsetMap) {
      //System.out.println("forward map generated:  "+theForwardOffsetMapping);
      //System.out.println("fwmap from: "+theForwardOffsetMapping.mapFrom);
      //System.out.println("fwmap to:   "+theForwardOffsetMapping.mapTo);
    }
    return resultString.toString();
  }

  /** 
   * Return an iterator that can be used to access the annotations according
   * to the annotation specification in increasing offset order.
   * <p>
   * 
   * @param doc 
   * @param annSetName
   * @return
   */
  public TextForSpecIterator getIterator(Document doc, String annSetName) {
    TextForSpecIterator it = new TextForSpecIterator(doc,annSetName,theAnnotationParms,annotationTypes, takeAll, takeOverlapping);
    return it;
  }

  private void addMappedAnnotation(
          AnnotationSet targetSet,
          Annotation theAnn,
          OffsetMapping offsetMap) {
    Long newFrom = offsetMap.getFromLong(theAnn.getStartNode().getOffset());
    Long newTo = offsetMap.getToLong(Math.max(theAnn.getEndNode().getOffset()-1,0l));
    //!System.out.println("Mapping targetfrom="+newFrom+" targetto="+newTo+" virtfrom="+theAnn.getStartNode().getOffset()+" virtto="+theAnn.getEndNode().getOffset());
    try{
      targetSet.add(newFrom,newTo,
              theAnn.getType(),
              // TODO: use a deep copy clone of the original Feature Map instead!?!?
              theAnn.getFeatures());
    } catch (InvalidOffsetException ex) {
      throw new GateRuntimeException(ex);
    }
  }


  private void addMappings(int origOffset, int targetOffset, int sourceLen, int targetLen, boolean copy) {
          if(generateForwardOffsetMap) {
              //System.out.println("Adding forward mapping at "+origOffset+" sourcelength="+sourceLen+" targetLen="+targetLen);
              theForwardOffsetMapping.addMapping(
                    origOffset, sourceLen, targetLen, copy);
          }
          if(generateBackwardOffsetMap) {
            //System.out.println("Adding backward mapping at "+targetOffset+" sourcelength="+sourceLen+" targetLen="+targetLen);
            theBackwardOffsetMapping.addMapping(
                    origOffset, sourceLen, targetLen, copy);
          }

  }


  public void addBackMappedAnnotations(Document originalDoc, Document virtualDoc,
          List<String> annotationSetsTypes) {
    if(!generateBackwardOffsetMap) {
      throw new GateRuntimeException(
        "Cannot create a backward mapping when backward map creation is disabled");
    }
    // TODO: before selecting an annotation to map back, check in
    // forwardMappedAnnotations (if non-null) if this annotation is new
    // or one of the forward mapped ones. In the latter case, ignore.
    if(annotationSetsTypes == null) {
      return;
    } else {
      // go through list of sets and types and process
      for (String setType : annotationSetsTypes) {
        // if setType contains a dot, assume it is a set name followed by
        // a type, otherwise assume it is just a set name
        String[] tmp1 = setType.split("\\.",2);
        String annotationSetName = tmp1[0];
        String annotationTypeName = (tmp1.length == 2) ? tmp1[1] : null;
        if(annotationSetName.equals("")) {
          annotationSetName = null;
        }
        AnnotationSet theAnns = virtualDoc.getAnnotations(annotationSetName);
        AnnotationSet targetSet = originalDoc.getAnnotations(annotationSetName);
        if(annotationTypeName != null) {
          theAnns = theAnns.get(annotationTypeName);
        }
        for (Annotation theAnn : theAnns) {
          addMappedAnnotation(targetSet,theAnn,getBackwardOffsetMap());
        }
      }
    }
  }

  public void addForwardMappedAnnotations(Document originalDoc, Document virtualDoc,
          List<String> annotationSetsTypes) {
    if(!generateForwardOffsetMap) {
      throw new GateRuntimeException(
        "Cannot create a forward mapping when forward map creation is disabled");
    }
    // For now: if null, do not do anything!
    if(annotationSetsTypes == null) {
    } else {
      for(String setType : annotationSetsTypes) {
        // if setType contains a dot, assume it is a set name followed by
        // a type, otherwise assume it is just a set name
        String[] tmp1 = setType.split("\\.",2);
        String annotationSetName = tmp1[0];
        String annotationTypeName = (tmp1.length == 2) ? tmp1[1] : null;
        if(annotationSetName.equals("")) {
          annotationSetName = null;
        }
        AnnotationSet theAnns = originalDoc.getAnnotations(annotationSetName);
        AnnotationSet targetSet = virtualDoc.getAnnotations(annotationSetName);
        if(annotationTypeName != null) {
          theAnns = theAnns.get(annotationTypeName);
        }
        for (Annotation theAnn : theAnns) {
          addMappedAnnotation(targetSet,theAnn,getForwardOffsetMap());
        }
      }
    }
  }

}
