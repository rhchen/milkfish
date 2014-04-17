package net.sf.milkfish.systrace.android.core;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static Bundle bundle;
	
	private volatile IEclipseContext eclipseContext;

	private static Activator instance;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		bundle = bundleContext.getBundle();
		instance = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		bundle = null;
		instance = null;
	}

	public IEclipseContext getEclipseContext() {
		IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundle.getBundleContext());
		return serviceContext;
	}

	public static Bundle getContext() {
		return bundle;
	}

	public static Activator getDefault() {
		return instance;
	}
}
