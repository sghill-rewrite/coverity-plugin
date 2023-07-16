/*******************************************************************************
 * Copyright (c) 2018 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 *******************************************************************************/
package jenkins.plugins.coverity;

import java.io.IOException;
import java.io.PrintStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.plugins.coverity.ws.ViewIssuesReader;
import jenkins.tasks.SimpleBuildStep;

public class CoverityViewResultsPublisher extends Recorder implements SimpleBuildStep {
    private String connectInstance;
    private String connectView;
    private String projectId;
    private boolean failPipeline;
    private boolean unstable;
    private boolean abortPipeline;

    @DataBoundConstructor
    public CoverityViewResultsPublisher(String connectInstance, String connectView, String projectId) {
        this.connectInstance = connectInstance;
        this.connectView = connectView;
        this.projectId = projectId;
        failPipeline = false;
        unstable = false;
        abortPipeline = false;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();
        logger.println("[Coverity] Publish Coverity View Results { "+
            "connectInstance:'" + connectInstance + "', " +
            "projectId:'" + projectId + "', " +
            "connectView:'" + connectView +
            "}");

        CIMInstance instance = getInstance();

        if (instance == null) {
            logger.println("[Coverity] Unable to find Coverity Connect instance: " + connectInstance);
            throw new AbortException("Unable to find Coverity Connect instance: " + connectInstance);
        }

        if (StringUtils.isEmpty(projectId) || StringUtils.isEmpty(connectView)) {
            logger.println("[Coverity] Coverity Connect project and view are required. But was Project: '" + projectId +
                "' View: '" + connectView + "'");
            throw new AbortException("Coverity Connect project and view are required. But was Project: '" + projectId +
                    "' View: '" + connectView + "'");
        }

        try {
            ViewIssuesReader reader = new ViewIssuesReader(run, listener.getLogger(), this);
            final CoverityBuildAction buildAction = reader.getIssuesFromConnectView();
            if (abortPipeline && buildAction.getDefects().size() > 0) {
                logger.println("[Coverity] Coverity issues were found and abortPipeline was set to true, throwing abort exception.");
                throw new AbortException("Coverity issues were found");
            } else if (failPipeline && buildAction.getDefects().size() > 0) {
                logger.println("[Coverity] Coverity issues were found and failPipeline was set to true, the pipeline result will be marked as FAILURE.");
                run.setResult(Result.FAILURE);
            } else if (unstable && buildAction.getDefects().size() > 0) {
                logger.println("[Coverity] Coverity issues were found and unstable was set to true, the pipeline result will be marked as UNSTABLE.");
                run.setResult(Result.UNSTABLE);
            }
        } catch (Exception e) {
            logger.println("[Coverity] Error Publishing Coverity View Results");
            logger.println(e.toString());
            throw new AbortException("Error Publishing Coverity View Results");
        }

        logger.println("[Coverity] Finished Publishing Coverity View Results");
    }

    public CIMInstance getInstance() {
        return getDescriptor().getInstance(connectInstance);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getConnectInstance() {
        return connectInstance;
    }

    public String getConnectView() {
        return connectView;
    }

    @Override
    public CoverityViewResultsDescriptor getDescriptor() {
        return (CoverityViewResultsDescriptor) super.getDescriptor();
    }

    public String getProjectId() {
        return projectId;
    }

    public boolean isFailPipeline() {
        return failPipeline;
    }

    @DataBoundSetter
    public void setFailPipeline(boolean failPipeline) {
        this.failPipeline = failPipeline;
    }

    public boolean isUnstable() {
        return unstable;
    }

    @DataBoundSetter
    public void setUnstable(boolean unstable) {
        this.unstable = unstable;
    }

    @DataBoundSetter
    public void setAbortPipeline(boolean abortPipeline) {
        this.abortPipeline = abortPipeline;
    }

    public boolean isAbortPipeline() {
        return abortPipeline;
    }
}
