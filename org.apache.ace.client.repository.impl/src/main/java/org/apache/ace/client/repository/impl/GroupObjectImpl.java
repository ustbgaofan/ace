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
package org.apache.ace.client.repository.impl;

import java.util.List;
import java.util.Map;

import org.apache.ace.client.repository.object.Artifact2GroupAssociation;
import org.apache.ace.client.repository.object.ArtifactObject;
import org.apache.ace.client.repository.object.Group2LicenseAssociation;
import org.apache.ace.client.repository.object.GroupObject;
import org.apache.ace.client.repository.object.LicenseObject;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Implementation class for the GroupObject. For 'what it does', see GroupObject,
 * for 'how it works', see RepositoryObjectImpl.
 */
public class GroupObjectImpl extends RepositoryObjectImpl<GroupObject> implements GroupObject {
    private final static String XML_NODE = "group";

    GroupObjectImpl(Map<String, String> attributes, Map<String, String> tags, ChangeNotifier notifier) {
        super(checkAttributes(attributes, KEY_NAME), tags, notifier, XML_NODE);
    }

    GroupObjectImpl(Map<String, String> attributes, ChangeNotifier notifier) {
        super(checkAttributes(attributes, KEY_NAME), notifier, XML_NODE);
    }

    GroupObjectImpl(HierarchicalStreamReader reader, ChangeNotifier notifier) {
        super(reader, notifier, XML_NODE);
    }

    public List<ArtifactObject> getArtifacts() {
        return getAssociations(ArtifactObject.class);
    }

    public List<LicenseObject> getLicenses() {
        return getAssociations(LicenseObject.class);
    }

    public String getDescription() {
        return getAttribute(KEY_DESCRIPTION);
    }

    public String getName() {
        return getAttribute(KEY_NAME);
    }

    public void setDescription(String description) {
        addAttribute(KEY_DESCRIPTION, description);
    }

    public void setName(String name) {
        addAttribute(KEY_NAME, name);
    }

    public List<Artifact2GroupAssociation> getAssociationsWith(ArtifactObject artifact) {
        return getAssociationsWith(artifact, ArtifactObject.class, Artifact2GroupAssociation.class);
    }

    public List<Group2LicenseAssociation> getAssociationsWith(LicenseObject license) {
        return getAssociationsWith(license, LicenseObject.class, Group2LicenseAssociation.class);
    }

    private static String[] DEFINING_KEYS = new String[] {KEY_NAME};
    @Override
    String[] getDefiningKeys() {
        return DEFINING_KEYS;
    }
}
