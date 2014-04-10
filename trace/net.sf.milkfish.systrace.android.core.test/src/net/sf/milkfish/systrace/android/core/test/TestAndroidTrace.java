package net.sf.milkfish.systrace.android.core.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import net.sf.milkfish.systrace.android.core.Activator;
import net.sf.milkfish.systrace.android.core.AndroidTrace;
import net.sf.milkfish.systrace.android.core.test.mock.EventBrokerMock;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;
import net.sf.milkfish.systrace.core.service.impl.SystraceService;

import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.InjectorFactory;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.osgi.service.event.EventHandler;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class TestAndroidTrace {

private static IEclipseContext _context;
	
	private static ISystraceService _systraceService;
	
	private static AndroidTrace _androidTrace;
	
	private static final File _file = new File("testdata/android_systrace.txt");
	
	
	@SuppressWarnings("restriction")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		_context = EclipseContextFactory.create();
	   
		InjectorFactory.getDefault().addBinding(IEventBroker.class).implementedBy(EventBrokerMock.class);
		
		InjectorFactory.getDefault().addBinding(TracePipe.class).implementedBy(TracePipe.class);
		
		InjectorFactory.getDefault().addBinding(ISystraceService.class).implementedBy(SystraceService.class);
	   
		_systraceService = ContextInjectionFactory.make(ISystraceService.class, _context);
		
		//ContextInjectionFactory.make(TracePipe.class, _context);
		
		//ContextInjectionFactory.make(IEventBroker.class, _context);
		
		
		_androidTrace = ContextInjectionFactory.make(AndroidTrace.class, _context);
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
	public final void testEcho2() throws IOException, ExecutionException {
		
		int echo = _systraceService.echo();
	    
		Assert.assertNotNull(echo);
		
		_systraceService.addTrace(_file.toURI(), _androidTrace);
		
		
		ITmfEvent e_0 = _systraceService.getTmfEvent(_file.toURI(), 0);
		
		Assert.assertNotNull(e_0);
		
		ITmfEvent e_1 = _systraceService.getTmfEvent(_file.toURI(), 1);
		
		Assert.assertNotNull(e_1);
		
	}
	
	
	
}
