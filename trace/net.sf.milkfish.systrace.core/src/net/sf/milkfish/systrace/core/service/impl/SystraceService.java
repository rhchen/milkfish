package net.sf.milkfish.systrace.core.service.impl;

import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.di.annotations.Creatable;

import com.fasterxml.jackson.core.JsonFactory;

public class SystraceService implements ISystraceService{

	public int echo(){
		
		JsonFactory f = new JsonFactory();
		
		return 0;
		
	}
}
