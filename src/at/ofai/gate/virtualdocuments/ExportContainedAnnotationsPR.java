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
import gate.LanguageAnalyser;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import gate.util.OffsetComparator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * This PR takes the name of a containing annotation type,
 * a contained annotation type name
 * and a feature name.
 * It will find for each containing annotation type, the contained annotations
 * and output a single line with all the values of what the specified feature
 * contains for each of these annotations.
 * The PR also takes a suffix (which could be an extension) and a directory URL
 * and will create a new file for each document processed, storing the file
 * in the directory and forming the file name by appending the suffix to the
 * document name
 * @author Johann Petrak
 */
@CreoleResource(name = "Export Contained Annotations PR",
        comment = "Export lines of text based on annotations contained in other annotations")
public class ExportContainedAnnotationsPR
  extends AbstractLanguageAnalyser
  implements LanguageAnalyser, ControllerAwarePR
{
  // parms input annotation set, containing annotation type name,
  // contained annotation type name, feature name
  // If feature name = blank, take string
  // If containing annotation type name is blank, use whole document
  // Contained annotation type name must not be blank
  // Directory URL, must not be null
  // suffix, if blank do not use any suffix

  private static final long serialVersionUID = 1L;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "Name of the input annotation set",
    defaultValue = "")
  public void setInputAnnotationSetName(String annotationSetName) {
    this.inputAnnotationSetName = annotationSetName;
  }
  public String getInputAnnotationSetName() {
    return inputAnnotationSetName;
  }
  protected String inputAnnotationSetName;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "Name of the containing annotations",
    defaultValue = "Sentence")
  public void setContainingAnnotationTypeName(String annotationType) {
    this.containingAnnotationTypeName = annotationType;
  }
  public String getContainingAnnotationTypeName() {
    return containingAnnotationTypeName;
  }
  String containingAnnotationTypeName;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "Name of the contained annotations",
    defaultValue = "Token"
    )
  public void setContainedAnnotationTypeName(String annotationType) {
    this.containedAnnotationTypeName = annotationType;
  }
  public String getContainedAnnotationTypeName() {
    return containedAnnotationTypeName;
  }
  String containedAnnotationTypeName;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "Name of the feature to use for the contained annotation (default: underlying text)",
    defaultValue = ""
    )
  public void setFeatureName(String name) {
    this.featureName = name;
  }
  public String getFeatureName() {
    return featureName;
  }
  String featureName;
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "Suffix to use to form file names from document names",
    defaultValue = ".txt"
    )
  public void setFileNameSuffix(String name) {
    this.fileNameSuffix = name;
  }
  public String getFileNameSuffix() {
    return fileNameSuffix;
  }
  String fileNameSuffix;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "URL of the directory where to write the document files to",
    defaultValue = "."
    )
  public void setDirectoryUrl(URL directoryUrl) {
    this.directoryUrl = directoryUrl;
  }
  public URL getDirectoryUrl() {
    return directoryUrl;
  }
  URL directoryUrl;

  File directoryFile;

  protected Logger logger;
  
  @Override
  public Resource init() throws ResourceInstantiationException {
    super.init();
    logger = Logger.getLogger(this.getClass());

    if(getContainedAnnotationTypeName() == null || getContainedAnnotationTypeName().equals("")) {
      throw new ResourceInstantiationException("Contained Annotation Type Name must be specified");
    }
    try {
      directoryFile = new File(directoryUrl.toURI());
    } catch (URISyntaxException ex) {
      throw new ResourceInstantiationException(ex);
    }
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    fireStatusChanged("ExportContainedAnnotationsPR processing: "
            + getDocument().getName());

    //check the input
    if(document == null) {
      throw new ExecutionException(
        "No document to process!"
      );
    }

    String fileName;
    fileName = getDocument().getName() + getFileNameSuffix();

    // 1) File to write the export stuff to
    File outFile = new File(directoryFile,fileName);
    PrintStream outStream;
    try {
      outStream = new PrintStream(outFile);
    } catch (FileNotFoundException ex) {
      throw new GateRuntimeException("Cannot write to file "+outFile.getAbsoluteFile(),ex);
    }

    // if a contained annotation name is specified, get those anns,
    // sort by offset and then loop through them, otherwise just get
    // all annotations
    AnnotationSet inputAS = document.getAnnotations(inputAnnotationSetName);


    if(containingAnnotationTypeName != null && !containingAnnotationTypeName.equals(""))  {
      AnnotationSet containingAnnotations = inputAS.get(containingAnnotationTypeName);
      //Iterator<Annotation> it = inputAnnotations.iterator();
      //String content = document.getContent().toString();

      List<Annotation> containingList = new ArrayList<Annotation>(containingAnnotations);
      Collections.sort(containingList,new OffsetComparator());
      for(Annotation containing : containingList) {
        // export the contained for the span covered by containing
        exportContainedAnnotations(outStream,containing.getStartNode().getOffset(),
                containing.getEndNode().getOffset(),inputAS);
      }
    } else {
      // export the contained for the whole document span
      exportContainedAnnotations(outStream,0L,Utils.lengthLong(document),
              inputAS);
    }
    outStream.close();
    fireStatusChanged("ExportContainedAnnotationsPR completed");

  }

  protected boolean exportContainedAnnotations(PrintStream outStream, Long from, Long to,
          AnnotationSet inputAS) {
    AnnotationSet filteredAS = inputAS.get(containedAnnotationTypeName,from,to);
    List<Annotation> filteredList = new ArrayList<Annotation>(filteredAS);
    Collections.sort(filteredList,new OffsetComparator());
    boolean haveSomething = false;
    for(Annotation ann : filteredList) {
      String toExport;
      if(featureName == null || featureName.equals("")) {
        toExport = Utils.stringFor(document, ann);
      } else {
        toExport = ann.getFeatures().get(featureName).toString();
      }
      outStream.print(toExport+" ");
      haveSomething = true;
    }
    if(haveSomething) { outStream.println(); }
    return haveSomething;
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
    if(getContainedAnnotationTypeName() == null || 
       getContainedAnnotationTypeName().equals("")) {
	  throw new ExecutionException("Contained Annotation Type Name must be specified");
	}
    // TODO: more parameter checking!
	try {
	  directoryFile = new File(directoryUrl.toURI());
	} catch (URISyntaxException ex) {
	  throw new ExecutionException(ex);
	}  
  }
  
}
