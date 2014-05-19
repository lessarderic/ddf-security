/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.security.policy.context;

import org.apache.commons.lang.StringUtils;
import org.codice.security.policy.context.attributes.ContextAttributeMapping;
import org.codice.security.policy.context.attributes.DefaultContextAttributeMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tustisos on 5/15/14.
 */
public class PolicyManager implements ContextPolicyManager {

    private static final String AUTH_TYPES = "authenticationTypes";

    private static final String REQ_ATTRS = "requiredAttributes";

    private Map<String, ContextPolicy> policyStore = new HashMap<String, ContextPolicy>();

    private ContextPolicy defaultPolicy = new Policy("/", new ArrayList<String>(), new ArrayList<ContextAttributeMapping>());

    public PolicyManager() {
        policyStore.put("/", defaultPolicy);
    }

    @Override
    public ContextPolicy getContextPolicy(String path) {
        ContextPolicy entry = policyStore.get(path);
        if(entry != null) {
            return entry;
        } else {
            int idx = path.lastIndexOf("/");
            if(idx <= 0) {
                idx++;
            }
            String pathFragment = path.substring(0, idx);
            if(StringUtils.isNotEmpty(pathFragment)) {
                return getContextPolicy(pathFragment);
            } else {
                //this is just here for safety
                //if we get down to the point where we can never get an entry, return the default
                return policyStore.get("/");
            }
        }
    }

    @Override
    public void setContextPolicy(String path, ContextPolicy contextPolicy) {
        if (path == null) {
            throw new IllegalArgumentException("Context path cannot be null.");
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Context path must start with /");
        }
        if (contextPolicy == null) {
            throw new IllegalArgumentException("Context policy cannot be null.");
        }
        policyStore.put(path, contextPolicy);
    }

    public void setPolicies(Map<String, String> properties) {
        policyStore.clear();
        policyStore.put("/", defaultPolicy);

        String authTypes = properties.get(AUTH_TYPES);
        String reqAttrs = properties.get(REQ_ATTRS);
        if(StringUtils.isNotEmpty(authTypes) && StringUtils.isNotEmpty(reqAttrs)) {
            String[] authContexts = authTypes.split(",");
            String[] attrContexts = reqAttrs.split(",");

            Map<String, List<String>> contextToAuth = new HashMap<String, List<String>>();
            Map<String, List<ContextAttributeMapping>> contextToAttr = new HashMap<String, List<ContextAttributeMapping>>();

            for(String auth : authContexts) {
                String[] parts = auth.split("=");
                if(parts.length == 2) {
                    String[] auths = parts[1].split("\\|");
                    if(auths.length > 0) {
                        contextToAuth.put(parts[0], Arrays.asList(auths));
                    }
                }
            }

            for(String attr : attrContexts) {
                String context = attr.substring(0, attr.indexOf("="));
                String value = attr.substring(attr.indexOf("=")+1);
                if(StringUtils.isNotEmpty(context) && value != null) {
                    if(value.startsWith("{") && value.endsWith("}")) {
                        if(value.length() == 2) {
                            value = "";
                        } else {
                            value = value.substring(1, value.length() - 1);
                        }
                    }
                    String[] attributes = value.split(";");
                    List<ContextAttributeMapping> attrMaps = new ArrayList<ContextAttributeMapping>();
                    for(String attribute : attributes) {
                        String[] parts = attribute.split("=");
                        if(parts.length == 2) {
                            attrMaps.add(new DefaultContextAttributeMapping(parts[0], parts[1]));
                        }
                    }
                    contextToAttr.put(context, attrMaps);
                }
            }

            Collection<String> contexts = contextToAuth.keySet();

            for(String context : contexts) {
                List<ContextAttributeMapping> mappings = contextToAttr.get(context);
                if(mappings == null) {
                    mappings = new ArrayList<ContextAttributeMapping>();
                }
                policyStore.put(context, new Policy(context, contextToAuth.get(context), mappings));
            }
        }
    }
}
