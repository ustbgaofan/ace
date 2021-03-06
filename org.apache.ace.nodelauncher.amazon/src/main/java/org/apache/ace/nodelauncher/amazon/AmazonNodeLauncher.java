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
package org.apache.ace.nodelauncher.amazon;

import org.apache.ace.nodelauncher.NodeLauncher;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.InstanceType;
import org.jclouds.ec2.reference.EC2Constants;
import org.jclouds.scriptbuilder.domain.Statements;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.jclouds.compute.predicates.NodePredicates.runningInGroup;

/**
 * Simple NodeLauncher implementation that launches nodes based on a given AMI in Amazon EC2.
 * We expect the AMI we launch to have a java on its path, at least after bootstrap.<br><br>
 * 
 * This service is configured using Config Admin; see the constants in the class for more information
 * about this.<br><br>
 *
 * After the node has been started up, this service will install a management agent on it. For this
 * to work, there should be an ace-launcher in the OBR of the server the node should connect to.
 */
public class AmazonNodeLauncher implements NodeLauncher, ManagedService {
    public static final String PID = "org.apache.ace.nodelauncher.amazon";

    /**
     * Configuration key: The ACE server the newly started nodes should connect to.
     */
    public static final String SERVER = "server";

    /**
     * Configuration key: The ID of the AMI to use. Note that this AMI should be available
     * in the location ("availability zone") you configure.
     */
    public static final String AMI_ID = "amiId";

    /**
     * Configuration key: The location where the node should be started; this is an Amazon "availability zone",
     * something like "eu-west-1".
     */
    public static final String LOCATION = "location";

    /**
     * Configuration key: the Amazon access key ID.
     */
    public static final String ACCESS_KEY_ID = "accessKeyid";

    /**
     * Configuration key: The secret key that goes with your access key.
     */
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";

    /**
     * Configuration key: A prefix to use for all nodes launched by this service. You can use this (a) allow
     * multiple nodes with the same ID, but launcher from different NodeLauncher services, or (b) to more
     * easily identify your nodes in the AWS management console.
     */
    public static final String TAG_PREFIX = "tagPrefix";

    /**
     * Configuration key: A piece of shell script that is run <em>before</em> the management agent is started.
     */
    public static final String NODE_BOOTSTRAP = "nodeBootstrap";

    /**
     * Configuration key: A set of VM options to pass to the JVM when starting the management agent, as a single string.
     */
    public static final String VM_OPTIONS = "vmOptions";

    /**
     * Configuration key: Any command line arguments you want to pass to the launcher; see the ace-launcher for
     * the possible options.
     */
    public static final String LAUNCHER_ARGUMENTS = "launcherArguments";
    
    /**
     * Configuration key: Extra ports to open on the nodes, besides the default ones (see DEFAULT_PORTS).
     */
    public static final String EXTRA_PORTS = "extraPorts";
    
    /**
     * Configuration key: Should we run the process as root?
     */
    public static final String RUN_AS_ROOT = "runAsRoot";

    /**
     * Default set of ports to open on a node.
     */
    public static final int[] DEFAULT_PORTS = new int[] {22, 80, 8080};
    
    private URL m_server;
    private String m_amiId; 
    private String m_location;
    private String m_accessKeyId;
    private String m_secretAccessKey;
    private String m_tagPrefix;
    private String m_vmOptions;
    private String m_nodeBootstrap;
    private String m_launcherArguments;
    private String m_extraPorts;
    private boolean m_runAsRoot;

    private ComputeServiceContext m_computeServiceContext;
    
    public void start() {
        Properties props = new Properties();
        props.put(EC2Constants.PROPERTY_EC2_AMI_OWNERS, "");
        m_computeServiceContext = new ComputeServiceContextFactory().createContext("aws-ec2",
                m_accessKeyId, m_secretAccessKey, (List) Arrays.asList(new JschSshClientModule()), props);
    }

    public void start(String id) throws Exception {
        ComputeService computeService = m_computeServiceContext.getComputeService();
        Template template = computeService.templateBuilder()
                .imageId(m_location + "/" + m_amiId)
                .hardwareId(InstanceType.C1_MEDIUM)
                .locationId(m_location)
                .build();
        
        int[] extraPorts = parseExtraPorts(m_extraPorts);
        int[] inboundPorts = mergePorts(DEFAULT_PORTS, extraPorts);
        template.getOptions().as(EC2TemplateOptions.class).inboundPorts(inboundPorts);
        template.getOptions().blockOnComplete(false);
        template.getOptions().runAsRoot(m_runAsRoot);

        Set<? extends NodeMetadata> tag = computeService.createNodesInGroup(m_tagPrefix + id, 1, template);
        System.out.println("In case you need it, this is the key to ssh to " + id + ":\n"
                + tag.iterator().next().getCredentials().credential);
        computeService.runScriptOnNodesMatching(runningInGroup(m_tagPrefix + id),
                Statements.exec(buildStartupScript(id)),
                RunScriptOptions.Builder.blockOnComplete(false));
    }
    
    int[] mergePorts(int[] first, int[] last) {
        int[] result = new int[first.length + last.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (i < first.length) ? first[i] : last[i - first.length];
        }
        return result;
    }
    
    int[] parseExtraPorts(String extraPorts) {
        extraPorts = extraPorts.trim();
        if (extraPorts.isEmpty()) {
            return new int[] {};
        }
        String[] ports = extraPorts.split(",");
        int[] result = new int[ports.length];
        for (int i = 0; i < ports.length; i++) {
            result[i] = Integer.parseInt(ports[i].trim());
        }
        return result;
    }

    private String buildStartupScript(String id) throws MalformedURLException {
        StringBuilder script = new StringBuilder();
        if (m_nodeBootstrap != null) {
            script.append(m_nodeBootstrap).append(" ; ");
        }
        script.append("wget ").append(new URL(m_server, "/obr/ace-launcher.jar")).append(" ;");

        script.append("nohup java -jar ace-launcher.jar ");
        script.append("discovery=").append(m_server.toExternalForm()).append(" ");
        script.append("identification=").append(id).append(" ");
        script.append(m_vmOptions).append(" ");
        script.append(m_launcherArguments);
        return script.toString();
    }

    public void stop(String id) {
        m_computeServiceContext.getComputeService().destroyNodesMatching(runningInGroup(m_tagPrefix + id));
    }
    
    public Properties getProperties(String id) throws Exception {
        Properties result = new Properties();
        
        NodeMetadata nodeMetadata = getNodeMetadataForRunningNodeWithTag(m_tagPrefix + id);
        if (nodeMetadata == null) {
            return null;
        }
        result.put("id", id);
        result.put("node-id", nodeMetadata.getId());
        result.put("ip", nodeMetadata.getPublicAddresses().iterator().next());
        
        return result;
    }

    private NodeMetadata getNodeMetadataForRunningNodeWithTag(String tag) {
        for (ComputeMetadata node : m_computeServiceContext.getComputeService().listNodes()) {
            NodeMetadata candidate = m_computeServiceContext.getComputeService().getNodeMetadata(node.getId());
            if (tag.equals(candidate.getGroup()) && candidate.getState().equals(NodeState.RUNNING)) {
                return candidate;
            }
        }
        return null;
    }

    public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        if (properties != null) {
            URL server;
            try {
                server = new URL(getConfigProperty(properties, SERVER));
            }
            catch (MalformedURLException e) {
                throw new ConfigurationException(SERVER, getConfigProperty(properties, SERVER) + " is not a valid URL.", e);
            }
            String amiId = getConfigProperty(properties, AMI_ID);
            String location = getConfigProperty(properties, LOCATION);
            String accessKeyId = getConfigProperty(properties, ACCESS_KEY_ID);
            String secretAccessKey = getConfigProperty(properties, SECRET_ACCESS_KEY);

            String vmOptions = getConfigProperty(properties, VM_OPTIONS, "");
            String nodeBootstrap = getConfigProperty(properties, NODE_BOOTSTRAP, "");
            String tagPrefix = getConfigProperty(properties, TAG_PREFIX, "");
            String launcherArguments = getConfigProperty(properties, LAUNCHER_ARGUMENTS, "");
            String extraPorts = getConfigProperty(properties, EXTRA_PORTS, "");
            String runAsRoot = getConfigProperty(properties, RUN_AS_ROOT, "false");

            m_server = server;
            m_amiId = amiId;
            m_location = location;
            m_accessKeyId = accessKeyId;
            m_secretAccessKey = secretAccessKey;
            m_tagPrefix = tagPrefix;
            m_vmOptions = vmOptions;
            m_nodeBootstrap = nodeBootstrap;
            m_launcherArguments = launcherArguments;
            m_extraPorts = extraPorts;
            m_runAsRoot = "true".equals(runAsRoot);
        }
    }

    private String getConfigProperty(@SuppressWarnings("rawtypes") Dictionary settings, String id)
            throws ConfigurationException {
        return getConfigProperty(settings, id, null);
    }

    private String getConfigProperty(@SuppressWarnings("rawtypes") Dictionary settings, String id, String defaultValue)
            throws ConfigurationException {
        String result = (String) settings.get(id);
        if (result == null) {
            if (defaultValue == null) {
            throw new ConfigurationException(id, "key missing");
            }
            else {
                return defaultValue;
            }
        }
        return result;
    }
}
