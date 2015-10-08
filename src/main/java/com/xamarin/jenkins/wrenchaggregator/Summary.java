package com.xamarin.jenkins.wrenchaggregator;

import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.plugins.git.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.TransientProjectActionFactory;
import hudson.util.RunList;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildSummaryAction;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Summary extends InvisibleAction {

    final private AbstractProject<?, ?> project;

    public Summary(@SuppressWarnings("rawtypes") AbstractProject project) {

        this.project = project;
    }

    public RunList<?> getBuilds() {

        return project.getBuilds();
    }
    
    public String getSha1(AbstractBuild<?, ?> target) {
        return ((GitSCM)(project.getScm())).getBuildData(target).getLastBuiltRevision().getSha1String();
    }
    
    public String getRepoUrl() {
        return ((GitSCM)(project.getScm())).getBrowser().getRepoUrl();
    }
    
    public String getSummary(AbstractBuild<?, ?> target) {
        String rawStatus = ((GroovyPostbuildSummaryAction)(target.getActions(GroovyPostbuildSummaryAction.class).toArray()[0])).getText();
        rawStatus = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + rawStatus.substring(rawStatus.indexOf("</h1>") + 5);
        try {
            StringBuilder result = new StringBuilder();
            XPath xpath = XPathFactory.newInstance().newXPath();
            InputSource inputSource = new InputSource( new StringReader( rawStatus ) );
            NodeList nodes = (NodeList) xpath.evaluate("/table/tr/td[position()=4]", inputSource, XPathConstants.NODESET);
            for(int i=0; i < nodes.getLength(); i++) {
                result.append(nodes.item(i).getNodeValue());
            }
            return result.toString();
        } catch(Exception e) {
            return "<td style=\"background-color: #ff0000;\">ERROR</td>";
        }
    }
    
    public String getColor(AbstractBuild<?, ?> target) {
        if(target.getResult() != null && target.getResult().isCompleteBuild()) {
            if(target.getResult().equals(Result.SUCCESS))
                    return "00ff7f";
            if(target.getResult().equals(Result.UNSTABLE))
                    return "ffa500";
            if(target.getResult().equals(Result.FAILURE))
                    return "ff0000";
            return "d3d3d3";
        }
        return "ffffff";
    }

    @Override
    public String toString() {

        return "Wrench aggregation for " + project.toString();
    }

    @Extension
    public static class SummaryFactory extends TransientProjectActionFactory {

        /**
         * For matrix projects parameter actions are attached to the MatrixProject
         */
        @Override
        public Collection<? extends Action> createFor(
                @SuppressWarnings("rawtypes") AbstractProject target
        ) {

            if (target instanceof MatrixConfiguration) {

                target = ((MatrixConfiguration) target).getParent();
            }
            
            return Arrays.asList(new Summary(target));
        }
    }
}
