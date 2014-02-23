/**
 * 
 */
package net.sf.milkfish.product.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

@RunWith(SWTBotJunit4ClassRunner.class)
public class AboutHandlerTest {

	private static SWTBot bot;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		bot = new SWTBot();
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		bot.sleep(2000);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link net.sf.milkfish.product.handlers.AboutHandler#execute(org.eclipse.swt.widgets.Shell)}.
	 */
	@Test
	public final void testExecute() {
		
		for(int i=0; i<5; i++){
			
			bot.sleep(2000);
			bot.menu("Help").menu("About").click();
			bot.button("OK").click();
		}
		

	
	}

}
