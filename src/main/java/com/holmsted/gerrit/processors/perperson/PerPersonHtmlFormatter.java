package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.OutputRules;
import com.holmsted.gerrit.processors.CommitDataProcessor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.ComparisonDateTool;

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
    private static final String VM_INDEX = TEMPLATES_RES_PATH + File.separator + "index.vm";
    private static final String INDEX_OUTPUT_NAME = "index.html";

    private VelocityEngine velocity = new VelocityEngine();
    private Context baseContext = new VelocityContext();
    private File outputDir;

    public PerPersonHtmlFormatter(@Nonnull OutputRules outputRules) {
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocity.init();

        baseContext.put("Gerrit", GerritUtils.class);
        baseContext.put("date", new ComparisonDateTool());
        baseContext.put("outputRules", outputRules);

        outputDir = new File(DEFAULT_OUTPUT_DIR);
    }

    @Override
    public void format(@Nonnull PerPersonData data) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOError(new IOException("Cannot create output directory " + outputDir.getAbsolutePath()));
        }

        List<IdentityRecord> orderedList = data.toOrderedList(new AlphabeticalOrderComparator());

        createIndex(data, orderedList);
        createPerPersonFiles(orderedList);

        System.out.println("Output written to " + outputDir.getAbsolutePath() + File.separator + INDEX_OUTPUT_NAME);
    }

    private void createIndex(@Nonnull PerPersonData data, @Nonnull List<IdentityRecord> identities) {
        Context context = new VelocityContext(baseContext);
        context.put("identities", identities);
        context.put("perPersonData", data);
        writeTemplate(context, VM_INDEX, INDEX_OUTPUT_NAME);
    }

    private void createPerPersonFiles(@Nonnull List<IdentityRecord> orderedList) {
        for (IdentityRecord record : orderedList) {
            String outputFilename = getOutputFilenameForIdentity(record.identity);

            Context context = new VelocityContext(baseContext);
            context.put("identity", record.identity);
            context.put("record", record);

            writeTemplate(context, VM_PERSON_PROFILE, outputFilename);
        }
    }

    private void writeTemplate(@Nonnull Context context, String templateName, String outputFilename) {
        System.out.println("Creating " + outputFilename);

        StringWriter writer = new StringWriter();
        velocity.mergeTemplate(templateName, "UTF-8", context, writer);
        FileWriter.writeFile(outputDir.getPath() + File.separator + outputFilename, writer.toString());
    }

    private static String getOutputFilenameForIdentity(@Nonnull Commit.Identity identity) {
        return identity.username + ".html";
    }
}
