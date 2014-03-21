package net.sf.milkfish.systrace.core.impl;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

import net.sf.milkfish.systrace.core.ISystraceService;

import com.fasterxml.jackson.core.JsonFactory;

public class SystraceService implements ISystraceService{

	public int echo(){
		
		JsonFactory f = new JsonFactory();
		
		return 0;
		
	}
}
