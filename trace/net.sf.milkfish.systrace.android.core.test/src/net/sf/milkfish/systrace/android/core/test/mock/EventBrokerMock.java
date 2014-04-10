package net.sf.milkfish.systrace.android.core.test.mock;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.EventHandler;

@Creatable
public class EventBrokerMock implements IEventBroker {

	public EventBrokerMock() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean send(String topic, Object data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean post(String topic, Object data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean subscribe(String topic, EventHandler eventHandler) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean subscribe(String topic, String filter,
			EventHandler eventHandler, boolean headless) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unsubscribe(EventHandler eventHandler) {
		// TODO Auto-generated method stub
		return false;
	}

	
}
