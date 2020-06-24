/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.jetty.internal.web;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletContext;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.util.Path;

/**
 * Extension of Jetty's <em>default servlet</em> to satisfy the resource contract from Http Service and Whiteboard
 * Service specifications.
 */
public class JettyResourceServlet extends DefaultServlet {

	/** If specified, this is the directory to fetch resource files from */
	private final PathResource baseUrlResource;

	/**
	 * If {@link #baseUrlResource} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	public JettyResourceServlet(PathResource baseUrlResource, String chroot) {
		this.baseUrlResource = baseUrlResource;
		this.chroot = chroot;
	}

	@Override
	protected ContextHandler initContextHandler(ServletContext servletContext) {
		// necessary for super.init()
		if (servletContext instanceof ContextHandler.Context) {
			return ((ContextHandler.Context) servletContext).getContextHandler();
		}
		return ((ContextHandler.Context)((OsgiScopedServletContext)servletContext).getContainerServletContext()).getContextHandler();
	}

	@Override
	public Resource getResource(String pathInContext) {
		// our (commons-io) normalized path
		String childPath = Path.securePath(pathInContext);
		if (childPath == null) {
			return null;
		}
		if (childPath.startsWith("/")) {
			childPath = childPath.substring(1);
		}

		try {
			if (baseUrlResource != null) {
				// Pax Web special - direct access to configured directory with proper metadata handling
				// (size, lastModified) for caching purposes
				if ("".equals(childPath)) {
					// root directory access. We want 404.
					return null;
				}
				return baseUrlResource.addPath(childPath);
			} else {
				// HttpService/Whiteboard behavior - resourceBase is prepended to argument for context resource
				// remember - under ServletContext there should be WebContainerContext that wraps
				// HttpContext or ServletContextHelper
				URL url = getServletContext().getResource(chroot + "/" + childPath);

				// TOCHECK: I see Felix is returning proper time from org.osgi.framework.Bundle.getLastModified()
//				if (Utils.isBundleProtocol(resource)) {
//					// let's return URLResource, but with tweaked "last modified"
//				}
				if (url != null && url.getProtocol().equals("file")) {
					if (new File(url.getPath()).isDirectory()) {
						// we want 404, not 403
						return null;
					}
				}

				// resource can be provided by custom HttpContext/ServletContextHelper, so we can't really
				// affect lastModified for caching purposes
				Resource resource = Resource.newResource(url);
				if (resource != null && resource.isDirectory()) {
					// we want 404, not 403
					return null;
				}
				return resource;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
