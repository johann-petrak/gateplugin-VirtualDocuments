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
import gate.Controller;
import gate.Corpus;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import javax.naming.InvalidNameException;


/**
 * This creates a virtual document and copies it to the output corpus.
 * @author johann
 */
@CreoleResource(name = "Copy Virtual Document PR",
        comment = "Create a virtual document and copy to another corpus")
public class CopyVirtualDocumentPR
  extends AbstractLanguageAnalyser 
  implements LanguageAnalyser, ControllerAwarePR
{
  public static final long serialVersionUID = 1L;
  
  @RunTime
  @CreoleParameter(comment = "A list of source annotation specifications")
  public void setOutputCorpus(Corpus outputCorpus) {
    this.outputCorpus = outputCorpus;
  }
  public Corpus getOutputCorpus() {
    return outputCorpus;
  }
  Corpus outputCorpus;

  @RunTime
  @Optional
  @CreoleParameter(
    comment = "URL of the directory where to write the document files to"
    )
  public void setDirectoryUrl(URL directoryUrl) {
    this.directoryUrl = directoryUrl;
  }
  public URL getDirectoryUrl() {
    return directoryUrl;
  }
  URL directoryUrl;

  File directoryFile;



  @RunTime
  @CreoleParameter(comment = "A list of source annotation specifications (document is copied as is if not given)")
  public void setSourceSpecifications(List<String> ss) {
    this.sourceSpecifications = ss;
  }
  public List<String> getSourceSpecifications() {
    return sourceSpecifications;
  }
  private List<String> sourceSpecifications;

  @RunTime
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
  @CreoleParameter(comment = "A list of Annotation Sets/Types to map to the new document; use set[.type] format")
  public void setAnnotationSetNames(List<String> as) {
    this.annotationSetNames = as;
  }
  public List<String> getAnnotationSetNames() {
    return annotationSetNames;
  }
  private List<String> annotationSetNames;

  @RunTime
  @CreoleParameter(comment = "The input annotation set",
  defaultValue = "")
  public void setInputAnnotationSetName(String ias) {
    this.inputAnnotationSetName = ias;
  }
  public String getInputAnnotationSetName() {
    return inputAnnotationSetName;
  }
  private String inputAnnotationSetName = "";


  @RunTime
  @CreoleParameter(comment = "Preserve format from 'Original Markups' or the only set given for annotation names  when saving to a file", defaultValue = "false")
  public void setSavePreservingFormat(Boolean yesno) {
    savePreservingFormat = yesno;
  }
  public Boolean getSavePreservingFormat() {
    return savePreservingFormat;
  }
  private Boolean savePreservingFormat;


  @RunTime
  @CreoleParameter(comment = "add features when preserving format", defaultValue = "false")
  public void setAddFeaturesToPreservingFormat(Boolean yesno) {
    addFeaturesToPreservingFormat = yesno;
  }
  public Boolean getAddFeaturesToPreservingFormat() {
    return addFeaturesToPreservingFormat;
  }
  private Boolean addFeaturesToPreservingFormat;

  @RunTime
  @CreoleParameter(comment = "Suffix to add to name of copied document",
  defaultValue = "")
  public void setCopiedDocNameSuffix(String suffix) {
    copiedDocNameSuffix = suffix;
  }
  public String getCopiedDocNameSuffix() {
    return copiedDocNameSuffix;
  }
  private String copiedDocNameSuffix;

  private boolean forwardcopy = false;

  AnnotatedDocumentTransformer annotatedDocumentTransformer;

  @Override
  public void execute() {
    fireStatusChanged("CopyVirtualDocumentPR processing: "
            + getDocument().getName());


    String newText = "";
    if(annotatedDocumentTransformer != null) {
      newText = annotatedDocumentTransformer.getStringForDocument(
            getDocument(), inputAnnotationSetName);
    } else {
      newText = getDocument().getContent().toString();
    }
      

      if (!(document instanceof DocumentImpl)) {
        throw new GateRuntimeException("Can only handle DocumentImpl not " + document.getClass());
      }
      String theclass = document.getClass().toString();
      FeatureMap theparms = Factory.newFeatureMap();
      theparms.put("collectRepositioningInfo", document.getCollectRepositioningInfo());
      theparms.put("encoding", ((DocumentImpl) document).getEncoding());
      theparms.put("markupAware", document.getMarkupAware());
      theparms.put("mimeType", ((DocumentImpl)document).getMimeType());
      theparms.put("preserveOriginalContent", document.getPreserveOriginalContent());
      theparms.put("stringContent", newText);
      FeatureMap thefeats = Factory.newFeatureMap();
      FeatureMap docfeats = document.getFeatures();
      for(Object k : docfeats.keySet()) {
        thefeats.put(k, docfeats.get(k));
      }

      String theName = document.getName();
      // create a copy of the current document
      Document newDoc;
      try {
        newDoc = (Document) Factory.createResource(
              //theclass,
              "gate.corpora.DocumentImpl",
              theparms,
              thefeats,
              theName+copiedDocNameSuffix
              );
      } catch (ResourceInstantiationException ex) {
        throw new GateRuntimeException(ex);
      }
      if(annotatedDocumentTransformer != null) {
        if(forwardcopy) {
          annotatedDocumentTransformer.
            addForwardMappedAnnotations(document, newDoc, annotationSetNames);
        }
      } else {
        // TODO: which annotation sets to copy to the copied doc here?
        // TODO: at least copy the ones specified!
      }

      if(directoryFile != null) {
        String out = "";
        if(getSavePreservingFormat()) {
          AnnotationSet as = newDoc.getAnnotations(annotationSetNames.get(0));
          out = newDoc.toXml(as,addFeaturesToPreservingFormat);
        } else {
          out = newDoc.toXml();
        }
        File outFile = new File(directoryFile, theName+copiedDocNameSuffix+".xml");
        PrintStream outStream;
        try {
          outStream = new PrintStream(outFile);
        } catch (FileNotFoundException ex) {
          throw new GateRuntimeException("Cannot write to file "+outFile.getAbsoluteFile(),ex);
        }
        outStream.print(out);
        outStream.close();
      }

      // we keep the document if it is added to a transient corpus, otherwise
      // we drop it.
      boolean keepDoc = false;

      if(outputCorpus != null) {
        outputCorpus.add(newDoc);
        if(outputCorpus.getLRPersistenceId() == null) {
          keepDoc = true;
        } else {
          outputCorpus.unloadDocument(newDoc);
        }
      }
      if(!keepDoc) {
        Factory.deleteResource(newDoc);
      }


    fireStatusChanged("CopyVirtualDocumentPR completed");

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
    forwardcopy = false;
    if(annotationSetNames != null && annotationSetNames.size() > 0) {
      forwardcopy = true;
    }
    if(getSavePreservingFormat()) {
      if(annotationSetNames.size() != 1) {
        throw new GateRuntimeException("Need exactly one set for annotation sets if save preserving format is true");
      }
      String annsetname = annotationSetNames.get(0);
      if(annsetname.contains(".")) {
        throw new GateRuntimeException("Annotation set must not contain a type (have a dot) if save preserving format is true");
      }
    }
    if(directoryUrl != null) {
      try {
        directoryFile = new File(directoryUrl.toURI());
      } catch (URISyntaxException ex) {
        throw new GateRuntimeException(ex);
      }
    } else {
      directoryFile = null;
    }
    if(directoryUrl == null && outputCorpus == null) {
      throw new GateRuntimeException("Output corpus and directory URL may not be both missing");
    }
    try {
      annotatedDocumentTransformer = null;
      if(getSourceSpecifications() != null && getSourceSpecifications().size() > 0) {
        annotatedDocumentTransformer =
          new AnnotatedDocumentTransformer(
          getSourceSpecifications(), getProcessingOptions(),
          forwardcopy, false);
      }
    } catch (InvalidNameException ex) {
      throw new ExecutionException(ex);
    }
  }
  
}
