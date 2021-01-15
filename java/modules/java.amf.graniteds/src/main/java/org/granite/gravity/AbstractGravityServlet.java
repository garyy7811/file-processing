/**
 *   GRANITE DATA SERVICES
 *   Copyright (C) 2006-2015 GRANITE DATA SERVICES S.A.S.
 *
 *   This file is part of the Granite Data Services Platform.
 *
 *   Granite Data Services is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   Granite Data Services is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 *   General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 *   USA, or see <http://www.gnu.org/licenses/>.
 */
package org.granite.gravity;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.granite.util.ContentType;

import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

/**
 * @author Franck WOLFF
 */
public class AbstractGravityServlet extends HttpServlet {

	///////////////////////////////////////////////////////////////////////////
	// Fields.
	
	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////////////////////////
	// Initialization.
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		GravityServletUtil.init(config);
	}

	///////////////////////////////////////////////////////////////////////////
	// Connect messages management (request attribute).
	
	public static void setConnectMessage(HttpServletRequest request, Message connect) {
		GravityServletUtil.setConnectMessage(request, connect);
	}
	
	public static CommandMessage getConnectMessage(HttpServletRequest request) {
		return GravityServletUtil.getConnectMessage(request);
	}
	
	public static void removeConnectMessage(HttpServletRequest request) {
		GravityServletUtil.removeConnectMessage(request);
	}

	///////////////////////////////////////////////////////////////////////////
	// Long polling timeout.
	
	protected long getLongPollingTimeout() {
		return GravityServletUtil.getLongPollingTimeout(getServletContext());
	}

	///////////////////////////////////////////////////////////////////////////
	// AMF (de)serialization methods.
	
	protected GravityInternal initializeRequest(GravityInternal gravity, HttpServletRequest request, HttpServletResponse response) {
		return GravityServletUtil.initializeRequest(getServletConfig(), gravity, request, response);
	}

	protected Message[] deserialize(GravityInternal gravity, HttpServletRequest request) throws ClassNotFoundException, IOException {
		return GravityServletUtil.deserialize(gravity, request);
	}
	
	protected Message[] deserialize(GravityInternal gravity, HttpServletRequest request, InputStream is) throws ClassNotFoundException, IOException {
		return GravityServletUtil.deserialize(gravity, request, is);
	}
	
	protected void serialize(GravityInternal gravity, HttpServletResponse response, Message[] messages, ContentType contentType) throws ServletException, IOException {
		GravityServletUtil.serialize(gravity, response, messages, contentType);
	}
	
	protected void cleanupRequest(HttpServletRequest request) {
		GravityServletUtil.cleanupRequest(request);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Unsupported HTTP methods.

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		throw new ServletException("Unsupported operation: " + req.getMethod());
	}
}
