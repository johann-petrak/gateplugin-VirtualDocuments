# AnnotationsSpecificationsParameter

<h2>The annotationSpecifications Parameter</h2>

<p>The <code>annotationSpecifications</code> Parameter specifies together with the <code>insertSpace</code> parameter the way how a "virtual document" is constructed on which the gazetteer is run instead of the original document text. For example if the annotationSpecifications list just contains <code>MyWord.string</code> and insertSpace is set to true, a virtual document will be constructed that is made up from all the values of the feature "string" of annotations "MyWord" concatenated together and seperated by spaces.</p>

<p>The <code>annotationSpecifications</code> Parameter can take of list of specifications where the first/top specification has highest priority and the subsequent specifications have decreasing priority. If at some position in the document, several annotationSpecifications match, the one with the highest priority is used to construct the "virtual document".</p>

<p>An annotationSpecification can be:
  * an annotation type and feature name separated by a dot, e.g. "Token.root" in which case the content of the feature of an annotation of this type is used
  * just an annotation type e.g. "Token" in which case the document string covered by the annotation is used
  * an annotation type, followed by "-&gt;" followed by some string, e.g. "MyToken-&gt;bla". When the annotation type is matched, the given string is used literally, in the example, if an annotation of type "MyToken" is matched, the string "bla" is inserted in the virtual document.
  * an annotation type and feature name separated by a dot, followed by "-&gt;" and some string: as in the previous case, but the annotation must have the given feature to match.
  * the literal string "@STRING" (without the quotes). This can only be reasonably be specified as the last specification in a list (as it always matches) and indicates that if no other specification matches, individual characters of the document text should be taken until another specification matches. If this is not specified, the corresponding part of the document is just skipped.</p>

<p>The parameter insertSpace specifies if a single Space character should be inserted between subsequent strings that are inserted into the virtual document based on some annotationSpecification.</p>

<p>Note that the annotations are only picked based on the priority, not the length of the candidate annotation (as it is done with JAPE rules).</p>
