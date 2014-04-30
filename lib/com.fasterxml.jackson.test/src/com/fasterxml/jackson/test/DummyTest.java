package com.fasterxml.jackson.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class DummyTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void test_0() {
		
		try {
		    
			File f = new File("data/chromeos_system_trace.json");
		 
		    JsonFactory factory = new JsonFactory();
			
			JsonParser parser = factory.createParser(f);
			
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(parser, Object.class);
			
			String str = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
			
			//System.out.println(str);
	
			/* Print to file */
			PrintWriter out = new PrintWriter("data/chromeos_system_trace_formated.json");
			out.println(str);
			out.close();
			
			Assert.assertNotNull(str);
			
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	@Test
	public final void test_1() {
		
		
		try {
			
			JacksonXmlModule module = new JacksonXmlModule();
			// and then configure, for example:
			module.setDefaultUseWrapper(false);
			XmlMapper xmlMapper = new XmlMapper(module);
//			xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//			xmlMapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
//			xmlMapper.disable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS );
//			xmlMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE );
			
			xmlMapper.addHandler(new DeserializationProblemHandler(){

				@Override
				public boolean handleUnknownProperty(
						DeserializationContext arg0, JsonParser arg1,
						JsonDeserializer<?> arg2, Object arg3, String arg4)
						throws IOException, JsonProcessingException {
					
					return false;
					
				}
				
			});
			
			Object str = xmlMapper.readValue("<Simple><x>1</x><y>2</y></Simple>", Object.class);
			
			Assert.assertNotNull(str);
			
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public final void test_2() {
		
		try {
			
			Document doc = Jsoup.parse(new File("data/android_systrace.html"), "UTF-8");
	        
	        Elements links = doc.select("script");
			for (Element link : links) {
	 
				// get the value from href attribute
				//System.out.println("text : " + link.data());
				PrintWriter out = new PrintWriter("data/android_systrace.txt");
				out.println(link.data());
				out.close();
			}
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}
	
	@Test
	public final void test_3() {
		
		try {
			
			Document doc = Jsoup.parse(new File("data/falcon_pro_trace.html"), "UTF-8");
	        
	        Elements links = doc.select("script");
			for (Element link : links) {
	 
				// get the value from href attribute
				//System.out.println("text : " + link.data());
				PrintWriter out = new PrintWriter("data/falcon_pro_trace.txt");
				out.println(link.data());
				out.close();
			}
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}
	
}
