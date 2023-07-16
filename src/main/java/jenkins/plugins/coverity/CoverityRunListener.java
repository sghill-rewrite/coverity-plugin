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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;

/**
 * CoverityRunListener injects the Coverity pre-build check into all builds where Coverity build/analysis/commit is
 * enabled. The check runs before code is checked out.
 */
@Extension
public class CoverityRunListener extends RunListener<Run> {
    @Override
    public void onCompleted(Run run, @NonNull TaskListener listener) {
        super.onCompleted(run, listener);
    }

    @Override
    public void onFinalized(Run run) {
        super.onFinalized(run);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        super.onStarted(run, listener);
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        AbstractProject project = build.getProject();

        //only watch coverity builds that are enabled
        CoverityPublisher publisher = (CoverityPublisher) project.getPublishersList().get(CoverityPublisher.class);
        if(publisher == null || publisher.getInvocationAssistance() == null) {
            return super.setUpEnvironment(build, launcher, listener);
        }

        //TODO: use ConsoleNote and ConsoleAnnotator to improve output appearance

        listener.getLogger().println("\nChecking Coverity configuration...");

        CheckConfig cc = new CheckConfig(publisher, build, launcher, listener);
        cc.check();

        for(CheckConfig.Status s : cc.getStatus()) {
            listener.getLogger().println(s.getStatus());
        }

        if(cc.isValid()) {
            listener.getLogger().println("Configuration is valid.\n");
            return super.setUpEnvironment(build, launcher, listener);
        } else {
            listener.getLogger().println("Configuration is invalid. Aborting build.\n");
            throw new Run.RunnerAbortedException();
        }
    }

    @Override
    public void onDeleted(Run run) {
        super.onDeleted(run);
    }
}
