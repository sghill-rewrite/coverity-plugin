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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import jenkins.plugins.coverity.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.net.URL;

public abstract class CoverityCommand extends Command {

    protected static final String intermediateDirArguments = "--dir";
    private static final String covIdirEnvVar = "COV_IDIR";
    protected static final String useSslArg = "--ssl";
    private static final String onNewCertArg = "--on-new-cert";
    private static final String trustArg = "trust";
    private static final String certArg = "--certs";
    private static final String portArg = "--port";
    private static final String hostArg = "--host";

    public CoverityCommand(@NonNull String command, AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener, CoverityPublisher publisher, String home, EnvVars envVars) {
        super(build, launcher, listener, publisher, envVars);

        Validate.notEmpty(command, "Command cannot be null empty or null");

        addCommand(command, home);
        addIntermediateDir();
    }

    private void addCommand(String cmd, String home){
        String command = cmd;

        if (!StringUtils.isEmpty(home)){
            command = new FilePath(launcher.getChannel(), home).child("bin").child(command).getRemote();
        }
        commandLine.add(command);
    }

    protected void addIntermediateDir(){
        commandLine.add(intermediateDirArguments);
        String idir = envVars.get(covIdirEnvVar);
        if (!StringUtils.isEmpty(idir)){
            commandLine.add(idir);
        }else{
            CoverityTempDir tempDir = build.getAction(CoverityTempDir.class);
            if (tempDir != null) {
                commandLine.add(tempDir.getTempDir().getRemote());
            } else {
                // This is fall-back logic since when the command is invoked through launcher,
                // the launcher checks the intermediate directory with the $COV_IDIR environment variable
                commandLine.add("$COV_IDIR");
            }

        }
    }

    protected void addTaCommandArgs(){
        if (publisher == null){
            return;
        }
        TaOptionBlock taOptionBlock = publisher.getTaOptionBlock();
        if (taOptionBlock != null){
            addArguments(taOptionBlock.getTaCommandArgs());
        }
    }

    protected void addSslConfiguration() {
        boolean isTrustNewSelfSignedCert = false;
        String certFileName = null;
        SSLConfigurations sslConfigurations = publisher.getDescriptor().getSslConfigurations();
        if(sslConfigurations != null) {
            isTrustNewSelfSignedCert = sslConfigurations.isTrustNewSelfSignedCert();
            certFileName = sslConfigurations.getCertFileName();

            if (isTrustNewSelfSignedCert) {
                addArgument(onNewCertArg);
                addArgument(trustArg);
            }
            if (certFileName != null) {
                addArgument(certArg);
                addArgument(certFileName);
            }
        }
    }

    protected void addCustomTestCommand(){
        TaOptionBlock taOptionBlock = publisher.getTaOptionBlock();
        try{
            if (!StringUtils.isEmpty(taOptionBlock.getCustomTestCommand())){
                addArguments(EnvParser.tokenize(taOptionBlock.getCustomTestCommand()));
            }
        }catch(ParseException parseException){
            throw new RuntimeException("ParseException occurred during tokenizing the cov capture custom test command.");
        }
    }

    protected boolean isSslConfigured(URL url, CIMInstance cimInstance){
        if ((url == null && cimInstance.isUseSSL())
                || url != null && url.getProtocol().equalsIgnoreCase("https")){
            return true;
        }

        return false;
    }

    protected void addPort(URL url, CIMInstance cimInstance) {
        addArgument(portArg);
        if (url == null){
            addArgument(Integer.toString(cimInstance.getPort()));
        }else{
            String portNumber = Integer.toString(url.getPort());
            if (portNumber.equals("-1")){
                addArgument("80");
            }else{
                addArgument(Integer.toString(url.getPort()));
            }
        }
    }

    protected void addHost(URL url, CIMInstance cimInstance){
        addArgument(hostArg);
        if (url == null){
            addArgument(cimInstance.getHost());
        }else{
            addArgument(url.getHost());
        }
    }
}
