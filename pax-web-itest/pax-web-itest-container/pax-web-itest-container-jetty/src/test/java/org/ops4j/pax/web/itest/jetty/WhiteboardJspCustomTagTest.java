/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@RunWith(PaxExam.class)
public class WhiteboardJspCustomTagTest extends ITestBase {

    private Bundle installWarBundle;
    
	@Configuration
	public static Option[] configure() {
		return combine(configureJetty());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
	    
	    String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard-jsp-custom-tags/" + VersionUtil.getProjectVersion();
        installWarBundle = installAndStartBundle(bundlePath);
	    
		initServletListener();
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
	    if (installWarBundle != null) {
            installWarBundle.stop();
            installWarBundle.uninstall();
        }
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
	    HttpTestClientFactory.createDefaultTestClient()
        .withResponseAssertion("Response must contain 'Test'",
                resp -> resp.contains("Test"))
        .doGETandExecuteTest("http://127.0.0.1:8181/sample/index");
	}

}
