# ProcessingOptionsParameter

<h2>The processingOptions Parameter</h2>

<p>Options for how to create the virtual document from the original according to the annotationSpecifications. Possible options are:
  * takeAll (true/false, default=false): if true apply all matching specifications at some offset, if false only the one with the highest priority. If there are several annotations matching a single specification, only the longest match is selected.
  * takeOverlapping (true/false, default=false):
  * separator (string, default is the empty string): how to seperate the output from different offsets of the document. Default is not to use any separator (this makes it possible to e.g. use the SpaceToken annotation in the annotationSpecifications to copy the original whitespace instead)
  * separatorSame (string, default is empty string): how to separate the output that comes from the same original offset. If not set, use the value for separator.
  * separatorKeyValue (string, default is the empty string): if not empty, the output will have the format "(key)(str)(value)" where key is the specification, str is the string specified for this parameter and value is the text taken from the document.</p>
