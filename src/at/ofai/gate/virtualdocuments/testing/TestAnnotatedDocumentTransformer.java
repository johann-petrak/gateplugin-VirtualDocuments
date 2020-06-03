/*
 *  AnnotateBySpecPR.java
 *
 *  This file is is free software, licenced under the 
 *  GNU Library General Public License, Version 2, June 1991.
 *  See http://www.gnu.org/licenses/gpl-2.0.html
 * 
 *  $Id: $
 */

package at.ofai.gate.virtualdocuments.testing;

import at.ofai.gate.virtualdocuments.AnnotatedDocumentTransformer;
import at.ofai.gate.virtualdocuments.OffsetMapping;
import javax.naming.InvalidNameException;
import org.junit.* ;
import static org.junit.Assert.* ;

import gate.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestAnnotatedDocumentTransformer {

  private static File testingDirIn;
  private static File testingDirOut;
  private static Document testDoc01In = null;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    System.out.println("!!! This is oneTimeSetUp!");
    Gate.init();
    File pluginHome = new File(".").getCanonicalFile();
    System.out.println("Plugins Home is "+pluginHome);
    Gate.getCreoleRegister().registerDirectories(
            pluginHome.toURI().toURL());
    testingDirIn = new File(new File(pluginHome,"test"), "in");
    testingDirOut = new File(new File(pluginHome,"test"), "out");
    assertTrue(testingDirIn.exists());
    assertTrue(testingDirOut.exists());
    // get the testing document ./test/testDoc01.xml
    System.out.println("Loading the test document in/testDoc01.xml");
    File testDoc01FileIn = new File(testingDirIn,"testDoc01.xml");
    testDoc01In = Factory.newDocument(testDoc01FileIn.toURI().toURL());
  }

  @AfterClass
  public static void oneTimeTearDown() {
    System.out.println("!!! This is oneTimeTearDown!");
  }

  @Before
  public void setUp() {
    System.out.println("!!! This is setUp!");
  }

  @After
  public void tearDown() {
    System.out.println("!!! This is teardown!");
  }

  @Test
  public void test1() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Token.string");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator"," ");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,true,true);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("time_modifier is a simple test document for testing the PN class . stop contains token annotations in the default annotation set from person_first and some additional manual annotations .", theString1);

    OffsetMapping bom = adt.getBackwardOffsetMap();
    OffsetMapping fom = adt.getForwardOffsetMap();
    // test some backward mappings
    assertEquals("Backward mapping of 'is' start - start offset", 5, bom.getFrom(14));
    assertEquals("Backward mapping of 'is' start - end offset", 7, bom.getTo(14));
    assertEquals("Backward mapping of 'is' end - start offset", 5, bom.getFrom(15));
    assertEquals("Backward mapping of 'is' end - end offset", 7, bom.getTo(15));
    adt.getForwardOffsetMap();
  }

  @Test
  public void test2() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Person->CONSTANT");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator","/");
    procSpecs.put("terminator","EOL");
    //procSpecs.add("separatorKeyValue==");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("time_modifier/PN/stop/person_firstEOL", theString1);
  }

  @Test
  public void test3() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Person->CONSTANT");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator"," ");
    procSpecs.put("separatorKeyValue","=");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("Lookup.majorType=time_modifier Unknown.kind=PN Lookup.majorType=stop Lookup.majorType=person_first", theString1);
  }

  @Test
  public void test4() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("FirstPerson.rule");
    annSpecs.add("Person.rule1");
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Person->CONSTANT");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator",",");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("time_modifier,PN,stop,FirstName", theString1);
  }

  @Test
  public void test5() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("WhatClass");
    annSpecs.add("ClassName");
    annSpecs.add("ClassWord");
    annSpecs.add("FirstPerson.rule");
    annSpecs.add("Person.rule1");
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Person->CONSTANT");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator", ",");
    procSpecs.put("separatorSame", ";");
    procSpecs.put("separatorKeyValue", "=");
    procSpecs.put("takeOverlapping","true");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("Lookup.majorType=time_modifier,WhatClass=AnnotatedDocumentTranformer class,ClassWord=class,Lookup.majorType=stop,FirstPerson.rule=FirstName", theString1);
  }

  @Test
  public void test6() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("WhatClass");
    annSpecs.add("ClassName");
    annSpecs.add("ClassWord");
    annSpecs.add("FirstPerson.rule");
    annSpecs.add("Person.rule1");
    annSpecs.add("Unknown.kind");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("Person->CONSTANT");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator", ",");
    procSpecs.put("separatorSame", ";");
    procSpecs.put("separatorKeyValue", "=");
    procSpecs.put("takeAll", "true");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("Lookup.majorType=time_modifier,WhatClass=AnnotatedDocumentTranformer class;ClassName=AnnotatedDocumentTranformer;Unknown.kind=PN,ClassWord=class,Lookup.majorType=stop,FirstPerson.rule=FirstName;Person.rule1=GazPersonFirst;Lookup.majorType=person_first;Person=CONSTANT", theString1);
  }

  @Test
  public void test7() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("WhatClass");
    annSpecs.add("Person->CONSTANT");
    annSpecs.add("Lookup.majorType");
    annSpecs.add("SpaceToken-> ");
    annSpecs.add("@STRING");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator","");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,false,false);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    assertEquals("time_modifier is a simple test document for testing the AnnotatedDocumentTranformer class. stop contains token annotations in the default annotation set from CONSTANT and some additional manual annotations.", theString1);
  }

  // TODO: test forward and backward mapping offsets and actual forward and
  // backward mappings!
  @Test
  public void test8() throws InvalidNameException {
    List<String> annSpecs = new ArrayList<String>();
    annSpecs.add("WhatClass");
    annSpecs.add("Person->CONSTANT");
    annSpecs.add("Lookup.majorType");
    FeatureMap procSpecs = Factory.newFeatureMap();
    procSpecs.put("separator","");

    AnnotatedDocumentTransformer adt =
            new AnnotatedDocumentTransformer(annSpecs,procSpecs,true,true);
    String theString1 = adt.getStringForDocument(testDoc01In,"");
    System.out.println("String: >"+theString1+"<");
    System.out.println("Forward offset map: "+adt.getForwardOffsetMap());
    System.out.println("Backward offset map: "+adt.getBackwardOffsetMap());
  }

}