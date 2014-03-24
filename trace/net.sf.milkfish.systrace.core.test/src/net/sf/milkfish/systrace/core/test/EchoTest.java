package net.sf.milkfish.systrace.core.test;

import static org.junit.Assert.*;
import net.sf.milkfish.systrace.core.service.impl.SystraceService;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoTest {

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
	public final void testEcho() {
		
		int r = new SystraceService().echo();
		
		Assert.assertNotNull(r);
	}

}
