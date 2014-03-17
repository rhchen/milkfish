package com.fasterxml.jackson.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
	public final void test() {
		
		try {
		    
			File f = new File("data/chromeos_system_trace.json");
		 
		    JsonFactory factory = new JsonFactory();
			
			JsonParser parser = factory.createParser(f);
			
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(parser, Object.class);
			
			String str = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
			
			System.out.println(str);
	
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
	public final void test1() {
		
		
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

}
