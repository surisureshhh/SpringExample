package com.sample.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HelloWorld {
	@Autowired
	ResourceLoader resourceLoader;
	@Autowired
	 protected CamelContext camelContext;
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public void printHello() {
		System.out.println("Hello ! " + name);
	}

	private static String inputFile;
	private static File outputFile = new File("src/main/resources/bpmn_context.xml");
	private static File outputFileTemplate = new File("src/main/resources/bpmn_context.xml");
	private static XPath xPath;
	private static Document inputDoc;
	private static Document outputDoc;
	private static String startSequence = null;
	private static String endSequence = null;
	private static NodeList sequenceList;
	private static NodeList scriptTaskList;
	private static HashMap<String, String> handlerAPI;
	private static String defaultPackage = new String("com.pas.protection.platform");

	public boolean generateDSL() throws Exception {
		DocumentBuilderFactory outputDBFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder outputDBuilder = outputDBFactory.newDocumentBuilder();
		outputDoc = outputDBuilder.parse(outputFileTemplate);
		outputDoc.getDocumentElement().normalize();
		boolean status = false;
		handlerAPI = new HashMap<String, String>();
			try {
				
				File file = new File("src/main/resources/bpmn_context.xml");
				InputStream is = new FileInputStream(file);
				BufferedReader buf = new BufferedReader(new InputStreamReader(is));
				String line = buf.readLine();
				StringBuilder sb = new StringBuilder();
				while (line != null) {
					sb.append(line).append("\n");
					line = buf.readLine();
				}
				String fileAsString = sb.toString();
				System.out.println("Contents : " + fileAsString);
				if (null != file) {
					inputFile = fileAsString;
					xPath = XPathFactory.newInstance().newXPath();
					DocumentBuilderFactory inputDBFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder inputDBuilder = inputDBFactory.newDocumentBuilder();
					inputDoc = inputDBuilder.parse(new ByteArrayInputStream(inputFile.getBytes("utf-8")));
					inputDoc.getDocumentElement().normalize();
					initialize();
					String startScriptID = null;
					Node nNode = (Node) xPath.compile("/definitions/process/startEvent").evaluate(inputDoc,
							XPathConstants.NODE);
					for (int i = 0; i < sequenceList.getLength(); i++) {
						Node node = sequenceList.item(i);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							Element eElement = (Element) node;
							if (eElement.getAttribute("id").equalsIgnoreCase(startSequence)) {
								startScriptID = eElement.getAttribute("targetRef");
								break;
							}
						}
					}
					mapAllSequence(startScriptID, "sample");
				}
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		generateDSLFile();
		status = true;
		return status;
	}

	private static void generateDSLFile() throws Exception {
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty(
				"{https://urldefense.proofpoint.com/v2/url?u=http-3A__xml.apache.org_xslt-257Dindent-2Damount&d=DwIGAg&c=gtIjdLs6LnStUpy9cTOW9w&r=cX2-tB2HgItkEVl2vxfIN9M11AGubrz1HYZnsesurzM&m=dn9SNttfIGt-ZV6nmAUkibXrz9Ly-BPv8pvEbQD-LaE&s=RJvgneqsH8hFWc51ml3yeegbYS6o2s0S4Ebl8FuUxqw&e=",
				"3");
		xformer.transform(new DOMSource(outputDoc), new StreamResult(outputFile));
	}

	private static void initialize() throws Exception {
		Node nNode = (Node) xPath.compile("/definitions/process/startEvent").evaluate(inputDoc, XPathConstants.NODE);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			startSequence = eElement.getElementsByTagName("bpmn:outgoing").item(0).getTextContent();
		}
		Node endNode = (Node) xPath.compile("/definitions/process/endEvent").evaluate(inputDoc, XPathConstants.NODE);
		if (endNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) endNode;
			endSequence = eElement.getAttribute("id");
		}
		sequenceList = (NodeList) xPath.compile("/definitions/process/sequenceFlow").evaluate(inputDoc,
				XPathConstants.NODESET);
		scriptTaskList = (NodeList) xPath.compile("/definitions/process/scriptTask").evaluate(inputDoc,
				XPathConstants.NODESET);
	}

	private static void mapAllSequence(String startScriptID, String businessURI) throws Exception {
		createCamelRoot(businessURI);
		for (int i = 0; i < sequenceList.getLength(); i++) {
			Node node = sequenceList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) node;
				if (eElement.getAttribute("sourceRef").equalsIgnoreCase(startScriptID)) {
					if (!startScriptID.equalsIgnoreCase(endSequence)) {
						mapToSpringDSL(startScriptID, scriptTaskList);
						startScriptID = eElement.getAttribute("targetRef");
						i = 0;
					} else {
						break;
					}
				}
			}
		}
	}

	private static void createCamelRoot(String businessURI) throws Exception {
		Node camelNode = (Node) xPath.compile("/process/startEvent/startEvent/outgoing").evaluate(outputDoc, XPathConstants.NODE);
		if (camelNode.getNodeType() == Node.ELEMENT_NODE) {
			Element camelElement = (Element) camelNode;
			Element rootElement = outputDoc.createElement("root");
			rootElement.setAttribute("id", businessURI);
			camelElement.appendChild(rootElement);
			Element rootFromElement = outputDoc.createElement("from");
			rootFromElement.setAttribute("uri", generateStartURI(businessURI).toString());
			rootElement.appendChild(rootFromElement);
		}
	}

	private static StringBuffer generateStartURI(String businessURI) {
		StringBuffer startURI = new StringBuffer();
		startURI.append("direct:");
		if (businessURI != null) {
			String[] businessURIElements = businessURI.split("/");
			for (int i = 1; i < businessURIElements.length; i++) {
				startURI.append(businessURIElements[i]);
				if (i != (businessURIElements.length - 1)) {
					startURI.append("-");
				}
			}
		}
		return startURI;
	}

	public static void mapToSpringDSL(String scriptID, NodeList scriptTaskList) throws Exception {
		for (int j = 0; j < scriptTaskList.getLength(); j++) {
			Node jNode = scriptTaskList.item(j);
			if (jNode.getNodeType() == Node.ELEMENT_NODE) {
				Element jElement = (Element) jNode;
				if (jElement.getAttribute("id").equalsIgnoreCase(scriptID)) {
					createCamelHandelerForARoot(jElement.getAttribute("magic:handler_api_name"),
							jElement.getAttribute("magic:handler_name"));
				}
			}
		}
	}

	private static void createCamelHandelerForARoot(String apiName, String methodName) throws Exception {
		Node camelNode = (Node) xPath.compile("/beans/camelContext").evaluate(outputDoc, XPathConstants.NODE);
		Node beansNode = (Node) xPath.compile("/beans").evaluate(outputDoc, XPathConstants.NODE);
		if (camelNode.getNodeType() == Node.ELEMENT_NODE) {
			Element rootElement = (Element) camelNode.getLastChild();
			Element beanElement = outputDoc.createElement("bean");
			beanElement.setAttribute("method", methodName);
			beanElement.setAttribute("ref", generateRefKey(apiName));
			rootElement.appendChild(beanElement);
			// Spring beans
			String className = new StringBuffer().append(defaultPackage).append(".").append(apiName).toString();
			String id = generateRefKey(apiName);
			if (handlerAPI.get(id) == null) {
				Element springBeanElement = outputDoc.createElement("bean");
				springBeanElement.setAttribute("class", className);
				springBeanElement.setAttribute("id", id);
				beansNode.appendChild(springBeanElement);
				handlerAPI.put(id, className);
			}
		}
	}

	private static String generateRefKey(String apiName) {
		char[] apiNameArray = apiName.toCharArray();
		apiNameArray[0] = Character.toLowerCase((apiName.charAt(0)));
		return new String(apiNameArray);
	}
}
