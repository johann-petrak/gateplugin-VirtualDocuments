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

import gate.Controller;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageAnalyser;
import gate.corpora.DocumentImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;

/**
 * This Language Analyser Processing Resource creates a virtual document
 * based on the annotation specification and processing options, runs some
 * language analyser PR on the virtual document and maps the annotations
 * created for the virtual document back to the original document.
 * 
 * TODO:
 * A subset of existing annotations can optionally be mapped to the virtual
 * document. All new annotations (i.e. all except those that were mapped
 * from the original document) are mapped back to the original document.
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "Indirect Language Analyser PR",
        comment = "Create a virtual document, run PR and map back annotations to original")
public class IndirectLanguageAnalyserPR
  extends AbstractLanguageAnalyser
  implements LanguageAnalyser, ControllerAwarePR
{

  public static final long serialVersionUID = 1L;
  
  @RunTime
  @CreoleParameter(comment = "A list of annotation specifications",
		  defaultValue = "")
  public void setAnnotationSpecifications(List<String> ss) {
    this.annotationSpecifications = ss;
  }
  
  public List<String> getAnnotationSpecifications() {
    return annotationSpecifications;
  }
  private List<String> annotationSpecifications;

  @RunTime
  @Optional
  @CreoleParameter(comment = "A list or processing options", 
    defaultValue="separator=;takeAll=false;takeOverlapping=false;separatorSame=;separatorKeyValue=;terminator="
    )
  public void setProcessingOptions(FeatureMap po) {
    this.processingOptions = po;
  }
  public FeatureMap getProcessingOptions() {
    return processingOptions;
  }
  private FeatureMap processingOptions;

  @RunTime
  @CreoleParameter(comment = "If true, keep virtual document", defaultValue = "false")
  public void setDebug(Boolean debug) {
    this.debug = debug;
  }
  public Boolean getDebug() {
    return this.debug;
  }
  private Boolean debug = false;

  @RunTime
  @Optional
  @CreoleParameter(comment = "(NOT IMPLEMENTED YET!) A list of Annotation set/type names to map to the virtual document (default: none)")
  public void setMapForwardAnnotations(List<String> as) {
    this.mapForwardAnnotations = as;
  }
  public List<String> getMapForwardAnnotations() {
    return mapForwardAnnotations;
  } 
  private List<String> mapForwardAnnotations;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "A list of Annotation Set/type names to map back, default: all",
    defaultValue = "")
  public void setMapBackAnnotations(List<String> as) {
    this.mapBackAnnotations = as;
  }
  public List<String> getMapBackAnnotations() {
    return mapBackAnnotations;
  }
  private List<String> mapBackAnnotations;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The input annotation set for which to process the specifications",
  defaultValue = "")
  public void setInputAnnotationSetName(String ias) {
    this.inputAnnotationSetName = ias;
  }
  public String getInputAnnotationSetName() {
    return inputAnnotationSetName;
  }
  private String inputAnnotationSetName = "";

  @RunTime
  @CreoleParameter(
    comment = "A Language Analyser PR that will be run on the virtual document")
  public void setLanguageAnalyser(LanguageAnalyser theLA) {
    this.languageAnalyser = theLA;
  }
  public LanguageAnalyser getLanguageAnalyser() {
    return languageAnalyser;
  }
  LanguageAnalyser languageAnalyser;


  AnnotatedDocumentTransformer annotatedDocumentTransformer;


  @Override
  public void execute() throws ExecutionException {
	if(corpus == null) {
	  startup();
	}
    fireStatusChanged("IndirectLanguageAnalyserPR processing: "
            + getDocument().getName());


    if (!(document instanceof DocumentImpl)) {
      throw new GateRuntimeException("Can only handle DocumentImpl not " + 
    		  document.getClass());
    }
    String newText = annotatedDocumentTransformer.getStringForDocument(
            getDocument(), inputAnnotationSetName);
    FeatureMap theparms = Factory.newFeatureMap();
    theparms.put("collectRepositioningInfo", document.getCollectRepositioningInfo());
    theparms.put("encoding", ((DocumentImpl) document).getEncoding());
    theparms.put("markupAware", document.getMarkupAware());
    theparms.put("mimeType", ((DocumentImpl) document).getMimeType());
    theparms.put("preserveOriginalContent", document.getPreserveOriginalContent());
    theparms.put("stringContent", newText);
    FeatureMap thefeats = Factory.newFeatureMap();
    FeatureMap docfeats = document.getFeatures();
    thefeats.putAll(docfeats);

    String theName = document.getName();
    // create a copy of the current document
    Document newDoc;
    try {
      newDoc = (Document) Factory.createResource(
              "gate.corpora.DocumentImpl",
              theparms,
              thefeats,
              theName+"_virtual");
    } catch (ResourceInstantiationException ex) {
      throw new GateRuntimeException(ex);
    }

    /* no forward annotation mappig yet ...
    if(annotatedDocumentTransformer.getGenerateForwardOffsetMap()) {
      annotatedDocumentTransformer.addForwardMappedAnnotations(
              document, newDoc,
              mapBackAnnotations);
    }
    */

    languageAnalyser.setDocument(newDoc);
    languageAnalyser.execute();

    if(annotatedDocumentTransformer.getGenerateBackwardOffsetMap()) {
      // figure out the annotation set names to map back
      List<String> effectiveMapFromAnnsetNames = new ArrayList<String>();
      if(mapBackAnnotations == null || mapBackAnnotations.size() == 0) {
        effectiveMapFromAnnsetNames.add("");
        Set<String> setnames = newDoc.getAnnotationSetNames();
        if(setnames != null) {
          for(String sn : setnames) {
            effectiveMapFromAnnsetNames.add(sn);
          }
        }
      } else {
    	  for(String sn : mapBackAnnotations) {
    		if(sn == null) {
    	      effectiveMapFromAnnsetNames.add("");
    		} else {
    		  effectiveMapFromAnnsetNames.add(sn);
    		}
    	  }
      }
      if(debug) {
        System.out.println("Mapping back from annotation sets: "+effectiveMapFromAnnsetNames);
      }
      annotatedDocumentTransformer.addBackMappedAnnotations(
              document, newDoc,
              effectiveMapFromAnnsetNames);
    }

    if(!debug) {
      Factory.deleteResource(newDoc);
    }
    fireStatusChanged("IndirectLanguageAnalyserPR completed");

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
    if(getAnnotationSpecifications() == null || getAnnotationSpecifications().size() == 0) {
        throw new ExecutionException("SrouceSpecifications must not be empty");
      }
      try {
        annotatedDocumentTransformer =
                new AnnotatedDocumentTransformer(
                getAnnotationSpecifications(), getProcessingOptions(),
                false,true);
        // For now we explicitly define that we do the backward mapping but 
        // not the forward mapping.
        // TODO: Once we do optional forward mapping of annotations we have to make
        // this dependent on whether we map forward annotations.
      } catch (InvalidNameException ex) {
        throw new ExecutionException(ex);
      }
      if(languageAnalyser == null) {
        throw new ExecutionException("Language Analyser PR not set!");
      }
	
}
  
}
