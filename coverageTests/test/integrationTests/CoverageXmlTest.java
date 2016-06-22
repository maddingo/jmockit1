package integrationTests;

import integrationTests.data.ClassWithFields;
import mockit.coverage.CodeCoverage;
import mockit.coverage.Configuration;
import mockit.coverage.XmlFile;
import mockit.coverage.data.CoverageData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class CoverageXmlTest extends CoverageTest
{
   ClassWithFields tested;

   @Test
   public void verifyXmlOutputGenerator()
   {
      File outputFile = new File("target", "coverage.xml");
      if (outputFile.exists()) {
         outputFile.delete();
      }
      assumeThat(Configuration.getProperty("output"), is(equalTo("xml")));
      assumeThat(Configuration.getProperty("outputDir"), is(equalTo("target")));

      CodeCoverage.generateOutput(false);

      assertTrue(outputFile.exists());

      outputFile.delete();
   }

   @Test
   public void verifyXmlFile() throws IOException, ParserConfigurationException, SAXException
   {
      File tempFile = File.createTempFile("coverage_", ".xml", new File("target/"));
      tempFile.deleteOnExit();

      // We want to override the default output file
      class TestXmlFile extends XmlFile {
         public TestXmlFile() {
            super(tempFile, null, CoverageData.instance());
         }
      }
      new TestXmlFile().generate();

      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(tempFile);

      Element documentElem = doc.getDocumentElement();
      assertThat(documentElem.getTagName(), is(equalTo("coverage")));
      assertThat(documentElem.getAttribute("version"), is(equalTo("1")));

      NodeList fileNodeList = documentElem.getElementsByTagName("file");
      int nboNodes = fileNodeList.getLength();
      assertTrue(nboNodes > 0);

      // find the file element for the tested class
      Node testClassNode = null;
      for (int i = 0; i < nboNodes; i++) {
         Node fileNode = fileNodeList.item(i);
         assertTrue(fileNode.getParentNode().isEqualNode(documentElem));
         Node pathAttr = fileNode.getAttributes().getNamedItem("path");
         if ("integrationTests/data/ClassWithFields.java".equals(pathAttr.getNodeValue())) {
            testClassNode = fileNode;
         }
      }

      assertThat(testClassNode, is(notNullValue()));

      NodeList linesToCoverList = ((Element) testClassNode).getElementsByTagName("lineToCover");
      int nboLinesToCover = linesToCoverList.getLength();
      assertTrue(nboLinesToCover > 0);

      for(int i = 0; i < nboLinesToCover; i++) {
         Node item = linesToCoverList.item(i);
         assertTrue(item.getParentNode().isEqualNode(testClassNode));
         // Does the lineToCover node have a lineNumber and covered attribute?
         Node lineNumber = item.getAttributes().getNamedItem("lineNumber");
         assertThat(lineNumber, is(notNullValue()));

         Node covered = item.getAttributes().getNamedItem("covered");
         assertThat(covered, is(notNullValue()));

         assertThat(covered.getNodeValue(), either(is(equalTo("true"))).or(is(equalTo("false"))));
      }
   }
}
