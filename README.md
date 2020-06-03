# gateplugin-VirtualDocuments

**NOTE: this is the Git version of a plugin original hosted on Google Code here 
https://code.google.com/archive/p/gateplugin-virtualdocuments/ and which has
last been updated in 2012!**

A plugin for the GATE (http://gate.ac.uk) language technology framework that provides a number of processing resources and an API that allows the construction of a new (on the fly) document from a mix of various annotation types and features in a source document. 
Optionally, original annotations from the source document can be mapped forward into the new "virtual" document and annotations created for the new virtual document can be mapped back into the original document.

This plugin provides the following processing resources: 

* Indirect Language Analyser PR: this PR will run another PR on a virtual document created from the original document according to a annotation specification list and map the annotations created back to the original document. This can be used to run any PR on a "virtual view" of some document and create annotations on the original document according to which parts of the original correspond to the virtual document. 
* Annotation by Specification PR: use an annotation specification list to pick annotations and create a new output annotation. This basically implements an annotation matching processes where priority is more important than match length. 
* Export Contained Annotations PR: a simple PR which will export the content of an annotationtype.feature to textual documents for each input document 
* Copy Virtual Document PR: created from an annotation specification list to a new corpus. This uses an annotation specification list to define which annotations, features etc to pick from the original document to create a new virtual document. 
* Replace By Virtual Document PR: this replaces each document in a corpus by the virtual document created according to an annotation specification list.

NOTE: this plugin is still in a beta stage and not all planned functionality has been implemented. Check the documentation of the individual processing resources for what is still missing or buggy.
