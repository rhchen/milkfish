package net.sf.milkfish.systrace.core.service;

import java.io.IOException;
import java.net.URI;

import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;

public interface ISystraceService {

	public int echo();
	
	public void registPipe(TracePipe pipe);
	
	public void addTrace(URI fileURI) throws IOException;
}
