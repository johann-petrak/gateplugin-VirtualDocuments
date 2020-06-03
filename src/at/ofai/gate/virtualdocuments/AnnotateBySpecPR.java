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

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageAnalyser;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.InvalidNameException;
import org.apache.log4j.Logger;

/**
 * This PR takes an annotation specification list 
 * (see {@link AnnotatedDocumentTransformer}) 
 * and creates new annotations
 * according to which of the annotations in the list were found.
 * The new annotations, which have a default type "SELECTED",
 * have the feature "content" set to whatever the annotation
 * specification defines (the underlying string, the value of a feature or
 * a constant), the feature "specNo" set to the number of the matching
 * specification, the feature "spec" to the actual matching specification.
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "Annotation by Specification PR",
        comment = "Create new annotations based on annotation specifications")
public class AnnotateBySpecPR
  extends AbstractLanguageAnalyser
  implements LanguageAnalyser, ControllerAwarePR
{
  public static final long serialVersionUID = 1L;
	
  @RunTime
  @CreoleParameter(comment = "A list of source annotation specifications")
  public void setSourceSpecifications(List<String> ss) {
    this.sourceSpecifications = ss;
  }
  public List<String> getSourceSpecifications() {
    return sourceSpecifications;
  }
  private List<String> sourceSpecifications;
  private Vector<String> sourceSpecificationsVector;

  @RunTime
  @Optional
  @CreoleParameter(comment = "A list or processing options",
    defaultValue="separator=;takeAll=false;takeOverlapping=false;separatorSame=;separatorKeyValue=;terminator="
  )
  public void setProcessingOptions(gate.FeatureMap po) {
    this.processingOptions = po;
  }
  public gate.FeatureMap getProcessingOptions() {
    return processingOptions;
  }
  private gate.FeatureMap processingOptions;

  @RunTime
  @Optional
  @CreoleParameter(comment = "The input annotation set for which to process the specifications",
  defaultValue = "")
  public void setInputAnnotationSetName(String ias) {
    this.inputAnnotationSetName = ias;
  }
  public String getInputAnnotationSetName() {
    return inputAnnotationSetName;
  }
  private String inputAnnotationSetName = "";

  @RunTime
  @Optional
  @CreoleParameter(comment = "The output annotation set where to place the annotations",
  defaultValue = "")
  public void setOutputAnnotationSetName(String ias) {
    this.outputAnnotationSetName = ias;
  }
  public String getOutputAnnotationSetName() {
    return outputAnnotationSetName;
  }
  private String outputAnnotationSetName = "";

  @RunTime
  @CreoleParameter(comment = "The output annotation type name",
  defaultValue = "SELECTED")
  public void setOutputAnnotationTypeName(String iat) {
    this.outputAnnotationTypeName = iat;
  }
  public String getOutputAnnotationTypeName() {
    return outputAnnotationTypeName;
  }
  private String outputAnnotationTypeName = "";

  AnnotatedDocumentTransformer annotatedDocumentTransformer;
  
  protected Logger logger;
  
  @Override
  public Resource init() throws ResourceInstantiationException {
    super.init();
    logger = Logger.getLogger(this.getClass());
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    fireStatusChanged("AnnotatedBySpecPR processing: "
            + getDocument().getName());

    TextForSpecIterator it =
            annotatedDocumentTransformer.getIterator(getDocument(),inputAnnotationSetName);

    AnnotationSet os = getDocument().getAnnotations(outputAnnotationSetName);
    while(it.hasNext()) {
      it.next();
      Annotation ann = it.getAnnotation();
      // if the annotation is null, skip to next match. This can happen for
      // a @STRING specification which does not make sense here
      if(ann == null) {
        continue;
      }
      logger.debug("Got annotation: "+ann);
      int specNo = it.getSpecNo();
      String spec = sourceSpecificationsVector.get(it.getSpecNo());
      FeatureMap fm = Factory.newFeatureMap();
      String content = it.getContent();
      fm.put("content", content);
      fm.put("specNo",specNo+"");
      fm.put("spec", spec);
      os.add(ann.getStartNode(),ann.getEndNode(),outputAnnotationTypeName,fm);
    }
    fireStatusChanged("AnnotatedBySpecPR completed");

  }


  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1)
  		throws ExecutionException {
  }

  @Override
  public void controllerExecutionFinished(Controller arg0)
  		throws ExecutionException {
  }

  @Override
  public void controllerExecutionStarted(Controller arg0)
  		throws ExecutionException {
    startup();
  }

  public void startup() throws ExecutionException {
	if(getSourceSpecifications() == null || 
	   getSourceSpecifications().size() == 0) {
	  throw new ExecutionException("SourceSpecifications must not be empty");
	}
	sourceSpecificationsVector = new Vector<String>(sourceSpecifications);
	try {
	  annotatedDocumentTransformer =
	    new AnnotatedDocumentTransformer(
	      getSourceSpecifications(), 
              getProcessingOptions(),
              false, false);
	} catch (InvalidNameException ex) {
	  throw new ExecutionException(ex);
	}	  
  }
}
