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
package org.apache.ace.deployment.servlet;

import javax.servlet.http.HttpServlet;

import org.apache.ace.deployment.provider.DeploymentProvider;
import org.apache.ace.deployment.streamgenerator.StreamGenerator;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    public static final String PID = "org.apache.ace.deployment.servlet";

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createComponent()
            .setInterface(HttpServlet.class.getName(), null)
            .setImplementation(DeploymentServlet.class)
            .add(createServiceDependency().setService(StreamGenerator.class).setRequired(true))
            .add(createServiceDependency().setService(DeploymentProvider.class).setRequired(true))
            .add(createServiceDependency().setService(LogService.class).setRequired(false))
            .add(createConfigurationDependency().setPropagate(true).setPid(PID)));
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // do nothing
    }
}