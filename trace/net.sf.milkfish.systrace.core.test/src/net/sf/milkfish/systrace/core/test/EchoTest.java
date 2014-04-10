package net.sf.milkfish.systrace.core.test;

import static org.junit.Assert.*;

import java.io.File;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectorFactory;

import net.sf.milkfish.systrace.core.service.ISystraceService;
import net.sf.milkfish.systrace.core.service.impl.SystraceService;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoTest {

	private static IEclipseContext _context;
	
	private static ISystraceService _systraceService;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		_context = EclipseContextFactory.create();
	    
		InjectorFactory.getDefault().addBinding(ISystraceService.class).implementedBy(SystraceService.class);
	   
		_systraceService = ContextInjectionFactory.make(ISystraceService.class, _context);
		
	    
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
	
	@SuppressWarnings("restriction")
	@Test
	public final void testEcho2() {
		
		int echo = _systraceService.echo();
	    
	    Assert.assertNotNull(echo);
	}

}
