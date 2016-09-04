package com.holmsted.gerrit.anonymizer;

import com.holmsted.RandomLists;

import javax.annotation.Nullable;

public class FakeFilenameGenerator {

    private static String[] DOMAIN_NAME_PARTS = {
            "models",
            "widget",
            "output",
            "handlers",
            "moustachemodels",
            "incubators",
            "input",
            "threading",
            "exceptions",
            "factories",
            "context",
            "json",
            "processors",
            "resources"
    };

    private static String[] SOFTWARE_BASE_NAMES = {
            "moustache_3d_modeler",
            "stackoverflow_copypaster",
            "nebula_incubator",
            "social_network",
            "supply_chain_aggregator",
            "big_data_pipeline",
            "game_theory_planner",
            "deep_knowledge_graph",
            "selfie_stick_expander",
            "crowdsourcing_facilitator",
            "realtime_mobility_pokedex",
    };

    private static String[] FILENAME_BEGIN_PARTS = {
            "Factory",
            "Generator",
            "Data",
            "Formatter",
            "Identity",
            "Map",
            "Proguard",
            "Rule",
            "Event",
            "Forwarding",
            "Removal",
            "Runtime",
            "Unchecked",
            "Abstract",
            "Long",
            "Short",
            "Double",
            "Bit",
            "More",
            "Less",
            "Utf8",
            "SmallChar",
            "Finalizable",
            "Iterable",
            "Common",
            "Immutable",
            "Network"
    };

    private static String[] FILENAME_END_PARTS = {
            "Handler",
            "Helper",
            "Utils",
            "Proguard",
            "Factory",
            "Builder",
            "Generator",
            "Data",
            "Formatter",
            "Main",
            "Connections",
            "Mapper",
            "List",
            "HashMap",
            "Set",
            "Deque",
            "Bus",
            "Attributes",
            "Signal",
            "Cache",
            "Cause",
            "Exception",
            "Listener",
            "Singleton",
            "Multiton",
            "Document",
            "Support",
            "Library",
            "Reference",
            "Predicate"
    };

    private String generateFileBasename() {
        StringBuilder builder = new StringBuilder();
        builder.append(RandomLists.randomItemFrom(FILENAME_BEGIN_PARTS));
        builder.append(RandomLists.randomItemFrom(FILENAME_END_PARTS));
        builder.append(".java");

        return builder.toString();
    }

    public String generateFilenameFromProjectName(@Nullable String projectName) {
        StringBuilder builder = new StringBuilder();
        projectName = projectName != null  ? projectName : RandomLists.randomItemFrom(SOFTWARE_BASE_NAMES);

        builder.append("src/main/java/com/");
        builder.append(projectName).append('/');
        builder.append(RandomLists.randomItemFrom(DOMAIN_NAME_PARTS)).append('/');
        builder.append(generateFileBasename());

        return builder.toString();
    }

    public String generateProjectName() {
        return "acme/" + RandomLists.randomItemFrom(SOFTWARE_BASE_NAMES);
    }
}
