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
package jenkins.plugins.coverity.CoverityTool;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.plugins.coverity.*;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Command {

    protected List<String> commandLine;
    protected AbstractBuild build;
    protected Launcher launcher;
    protected TaskListener listener;
    protected CoverityPublisher publisher;
    protected EnvVars envVars;

    public Command(
            @NonNull AbstractBuild<?, ?> build,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener,
            @NonNull CoverityPublisher publisher,
            @NonNull EnvVars envVars) {
        Validate.notNull(build, AbstractBuild.class.getName() + " object cannot be null");
        Validate.notNull(launcher, Launcher.class.getName() + " object cannot be null");
        Validate.notNull(listener, TaskListener.class.getName() + " object cannot be null");
        Validate.notNull(publisher, CoverityPublisher.class.getName() + " object cannot be null");
        Validate.notNull(envVars, EnvVars.class.getName() + " object cannot be null");

        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        this.publisher = publisher;
        this.envVars = envVars;

        commandLine = new ArrayList<>();
    }

    protected void addArgument(String args){
        commandLine.add(args);
    }

    protected void addArguments(List<String> args){
        commandLine.addAll(args);
    }

    public int runCommand() throws IOException, InterruptedException {
        boolean useAdvancedParser = false;
        InvocationAssistance invocationAssistance = publisher.getInvocationAssistance();
        if (invocationAssistance != null && invocationAssistance.getUseAdvancedParser()){
            useAdvancedParser = true;
        }

        if (canExecute()){
            prepareCommand();
            return CoverityUtils.runCmd(commandLine, build, launcher, listener, envVars, useAdvancedParser);
        }

        // Need to return 0 to move onto different commands, rather than marking the build to fail.
        return 0;
    }

    protected abstract void prepareCommand();

    protected abstract boolean canExecute();
}
