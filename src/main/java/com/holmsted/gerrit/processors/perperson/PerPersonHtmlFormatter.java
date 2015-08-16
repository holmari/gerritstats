package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.processors.CommitDataProcessor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.annotation.Nonnull;

import file.FileWriter;

class PerPersonHtmlFormatter implements CommitDataProcessor.OutputFormatter<PerPersonData> {
    private static final String DEFAULT_OUTPUT_DIR = "out";
    private static final String TEMPLATES_RES_PATH = "templates";
    private static final String VM_PERSON_PROFILE = TEMPLATES_RES_PATH + File.separator + "person_profile.vm";
    private OutputRules outputRules;

    public PerPersonHtmlFormatter(@Nonnull OutputRules outputRules) {
        this.outputRules = outputRules;
    }

    @Override
    public void format(@Nonnull PerPersonData data) {
        File outputDir = new File(DEFAULT_OUTPUT_DIR);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOError(new IOException("Cannot create output directory " + outputDir.getAbsolutePath()));
        }
        VelocityEngine velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocity.init();

        List<IdentityRecord> orderedList = data.toOrderedList(new AlphabeticalOrderComparator());

        for (IdentityRecord record : orderedList) {
            String outputFilename = getOutputFilenameForIdentity(record.identity);
            System.out.println("Creating " + outputFilename);

            Context context = new VelocityContext();
            context.put("outputRules", outputRules);
            context.put("identity", record.identity);
            context.put("record", record);
            context.put("Gerrit", GerritUtils.class);

            StringWriter writer = new StringWriter();
            velocity.mergeTemplate(VM_PERSON_PROFILE, "UTF-8", context, writer);

            FileWriter.writeFile(outputDir.getPath() + File.separator + outputFilename, writer.toString());
        }

        System.out.println("Output written to " + outputDir.getAbsolutePath());
    }

    private static String getOutputFilenameForIdentity(@Nonnull Commit.Identity identity) {
        return identity.username + ".html";
    }
}
