/*
 * #%L
 * Talend :: ESB :: Job :: Controller
 * %%
 * Copyright (C) 2011 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.job.controller.internal;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import routines.system.api.TalendESBJob;
import routines.system.api.TalendESBRoute;
import routines.system.api.TalendJob;

/**
 * Tracks registration and unregistration of all type of jobs and notifies a corresponding listener.
 */
public class JobTracker {

    public static final Logger LOG =
        Logger.getLogger(JobTracker.class.getName());

    public static final String FILTER =
        "(objectClass=" + TalendJob.class.getName() + ")";

    public static final String PROPERTY_KEY_NAME = "name";

    private BundleContext context;

    private JobListener listener;

    private ServiceTracker tracker;


    public void setJobListener(JobListener listener) {
        this.listener = listener;
    }

    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    public void bind() {
        LOG.fine("bind calling, creating and opening ServiceTracker...");
        Filter filter = null;
        try {
            filter = context.createFilter(FILTER);
        } catch (InvalidSyntaxException e) {
            LOG.throwing(this.getClass().getName(), "bind", e);
            
        }
        tracker = new ServiceTracker(context, filter, new Customizer());
        tracker.open();
    }

    public void unbind() {
        LOG.fine("unbind calling, closing ServiceTracker...");
        if (tracker != null) {
            tracker.close();
        }
    }

    private String getValue(String name, ServiceReference sRef) {
        Object val = sRef.getProperty(name);
        if (name == null || !(name instanceof String)) {
            throw new IllegalArgumentException(
                    name + " property of TalendJob either not defined or not of type String");
        }
        return (String) val;
    }
    
    private class Customizer implements ServiceTrackerCustomizer {

        @Override
        public Object addingService(ServiceReference reference) {
            LOG.info("Service with reference " + reference + " added");
            Object service = context.getService(reference);
            if (service != null) {
                String name = getValue(PROPERTY_KEY_NAME, reference);
                if (service instanceof TalendESBJob) {
                    listener.esbJobAdded((TalendESBJob) service, name);
                } else if (service instanceof TalendESBRoute) {
                    listener.routeAdded((TalendESBRoute) service, name);
                } else if (service instanceof TalendJob) {
                    listener.jobAdded((TalendJob)service, name);
                }

            }
            return service;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object job) {
            LOG.info("Service " + job + " modified");
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            LOG.info("Service " + service + " removed");
            String name = getValue(PROPERTY_KEY_NAME, reference);
            if (service instanceof TalendESBJob) {
                listener.esbJobRemoved((TalendESBJob) service, name);
            } else if (service instanceof TalendESBRoute) {
                listener.routeRemoved((TalendESBRoute) service, name);
            } else if (service instanceof TalendJob) {
                listener.jobRemoved((TalendJob)service, name);
            }
            context.ungetService(reference);
            
        }
    }
}
