# Overview

<h1>Basic Idea</h1>

<p>The traditional way of processing documents in GATE is to create and modify annotations for a static document text. While the original text never changes, the annotations reflect the stages of processing and extraction. This is an extremely flexible and powerful paradigm and exactly what is needed in most situations.</p>

<p>However, sometimes it can be useful to also change the text of the document, either temporarily or finally. For example, when running a gazetteer on a document, it might be preferable to run the gazetteer on the lemmata of the words in the document. So, just for the Gazetteer PR we would like a way to create a temporary document that replaces all words with their lemmas. Then, the Lookup annotations created for the lemmas should get added to the original corresponding words in the original documents. For this specific case, there is already a processing resource in GATE: the FlexibleGazetteer. However the FlexibleGazetteer PR only allows gazetteers to be run on the temporary document and creates the temporary document in a very specific way.</p>

<p>The VirtualDocuments plugin generalizes this functionality in several ways and provides a number of additional functions.</p>

<h1>Annotation Specification</h1>
