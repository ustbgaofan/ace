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

import java.util.*;

import org.apache.ace.client.repository.RepositoryObject;
import org.apache.ace.client.repository.helper.PropertyResolver;
import org.apache.ace.client.repository.object.GatewayObject;

/**
 *
 * Top-level property resolver, also able to return collections
 * of distributions, features and artifacts linked to this target
 * repository object.
 */
public class GatewayPropertyResolver extends RepoPropertyResolver {

    public GatewayPropertyResolver(GatewayObject go) {
        super(go);
    }

    public Collection<PropertyResolver> getDistributions() {
        List<PropertyResolver> list = new ArrayList<PropertyResolver>();

        List<RepositoryObject> distributions = (List<RepositoryObject>)getChildren();

        for (RepositoryObject repo : distributions) {
            list.add(new RepoPropertyResolver(repo));
        }

        return list;
    }

    public Collection<PropertyResolver> getFeatures() {
        List<PropertyResolver> list = new ArrayList<PropertyResolver>();

        Set<RepositoryObject> features = new HashSet<RepositoryObject>();

        for (RepositoryObject repositoryObject : getChildren()) {
            features.addAll(getChildren(repositoryObject));
        }

        for (RepositoryObject repo : features) {
            list.add(new RepoPropertyResolver(repo));
        }
        return list;
    }

    public Collection<PropertyResolver> getArtifacts() {
        List<PropertyResolver> list = new ArrayList<PropertyResolver>();

        Set<RepositoryObject> artifacts = new HashSet<RepositoryObject>();
        Set<RepositoryObject> features = new HashSet<RepositoryObject>();

        for (RepositoryObject repositoryObject : getChildren()) {
            features.addAll(getChildren(repositoryObject));
        }

        for (RepositoryObject repositoryObject : features) {
            artifacts.addAll(getChildren(repositoryObject));
        }


        for (RepositoryObject repo : artifacts) {
            list.add(new RepoPropertyResolver(repo));
        }
        return list;
    }

}
