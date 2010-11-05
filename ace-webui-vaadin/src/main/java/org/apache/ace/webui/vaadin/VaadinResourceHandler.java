/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ace.webui.vaadin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class VaadinResourceHandler {
    private volatile HttpService m_http;
    private HttpContext m_context;
    
    public void start() {
        m_context = m_http.createDefaultHttpContext();
        try {
            m_http.registerResources("/VAADIN", "/VAADIN", new HttpContext() {
                public String getMimeType(String name) {
                    return m_context.getMimeType(name);
                }

                public URL getResource(String name) {
                    URL resource = null;
                    String prefix = "/VAADIN/";
                    if (name.startsWith(prefix)) {
                        resource = getClass().getResource(name);
                        if (resource == null) {
                            // try to find the resource in the Vaadin bundle instead
                            resource = com.vaadin.Application.class.getResource(name);
                        }
                    }
                    return resource;
                }

                public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    return m_context.handleSecurity(request, response);
                }});
        }
        catch (NamespaceException e) {
            e.printStackTrace();
        }
    }
}
