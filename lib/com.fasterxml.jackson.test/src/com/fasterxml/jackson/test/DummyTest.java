package com.fasterxml.jackson.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

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
		
		URL url;
		try {
		    
			File f = new File("data/chromeos_system_trace.json");
		 
		    JsonFactory factory = new JsonFactory();
			
			JsonParser parser = factory.createParser(f);
			
			Assert.assertNotNull(factory);
			
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

}
