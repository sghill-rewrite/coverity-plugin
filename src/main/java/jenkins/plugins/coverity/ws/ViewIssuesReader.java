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
package jenkins.plugins.coverity.ws;


import java.io.PrintStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.plugins.coverity.CIMInstance;
import jenkins.plugins.coverity.CoverityBuildAction;
import jenkins.plugins.coverity.CoverityDefect;
import jenkins.plugins.coverity.CoverityViewResultsPublisher;

/**
 * Class responsible for reading issues from Coverity Connect instance {@link CIMInstance} for a {@link CoverityViewResultsPublisher}.
 * The issues will be added to the pipeline run as a {@link CoverityBuildAction}.
 */
public class ViewIssuesReader {
    private Run<?, ?> run;
    private PrintStream outputLogger;
    private CoverityViewResultsPublisher publisher;

    public ViewIssuesReader(@NonNull Run<?, ?> run, PrintStream outputLogger, CoverityViewResultsPublisher publisher) {
        this.run = run;
        this.outputLogger = outputLogger;
        this.publisher = publisher;
    }

    public CoverityBuildAction getIssuesFromConnectView() throws Exception {
        CIMInstance instance = publisher.getInstance();

        if (instance != null) {
            List<CoverityDefect> issuesFromView = instance.getIssuesVorView(publisher.getProjectId(), publisher.getConnectView(), outputLogger);

            CoverityBuildAction action = new CoverityBuildAction(run, publisher.getProjectId(), publisher.getConnectView(), publisher.getConnectInstance(), issuesFromView);
            run.addAction(action);

            String rootUrl = Jenkins.getInstance().getRootUrl();
            if(StringUtils.isNotEmpty(rootUrl)) {
                outputLogger.println("Coverity details: " + rootUrl + run.getUrl() + action.getUrlName());
            }

            return action;
        }
        return run.getAction(CoverityBuildAction.class);
    }
}
