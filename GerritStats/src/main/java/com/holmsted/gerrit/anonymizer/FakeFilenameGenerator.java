package com.holmsted.gerrit.anonymizer;

import com.holmsted.RandomLists;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class FakeFilenameGenerator {

    private static final String[] DOMAIN_NAME_PARTS = {
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

    private static final String[] PROJECT_BEGIN_PARTS = {
            "moustache",
            "stackoverflow",
            "copypaste",
            "nebula",
            "social",
            "particle",
            "supply_chain",
            "big_data",
            "game_theory",
            "deep_knowledge",
            "selfie_stick",
            "crowdsourcing",
            "realtime_mobility",
            "automated",
            "non_deterministic",
            "ui_spec",
            "marble",
            "audio",
            "video",
            "gpu",
            "backend",
            "middleware",
            "notification",
            "sid_meiers",
    };

    private static final String[] PROJECT_END_PARTS = {
            "3d_modeler",
            "copypaster",
            "incubator",
            "network",
            "aggregator",
            "pipeline",
            "planner",
            "graph",
            "expander",
            "facilitator",
            "pokedex",
            "lib",
            "extension",
            "effectron",
            "signaller",
            "generator",
            "game",
            "platform",
            "nanny",
            "driver",
            "reviewer",
            "automaton",
            "randomizer",
            "rasterizer",
            "stack",
            "definer",
            "robotron",
            "pinball",
            "fascinator"
    };

    private static final String[] FILENAME_BEGIN_PARTS = {
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

    private static final String[] FILENAME_END_PARTS = {
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

    private final Set<String> usedProjectNames = new HashSet<>();

    private String generateFileBasename() {
        StringBuilder builder = new StringBuilder();
        builder.append(RandomLists.randomItemFrom(FILENAME_BEGIN_PARTS))
                .append(RandomLists.randomItemFrom(FILENAME_END_PARTS))
                .append(".java");

        return builder.toString();
    }

    public String generateFilenameFromProjectName(@Nullable String projectName) {
        StringBuilder builder = new StringBuilder(20);
        String randomProjectName = projectName != null
                ? projectName
                : RandomLists.randomItemFrom(PROJECT_BEGIN_PARTS)
                + '/' + RandomLists.randomItemFrom(PROJECT_END_PARTS);

        builder.append("src/main/java/com/")
                .append(randomProjectName).append('/')
                .append(RandomLists.randomItemFrom(DOMAIN_NAME_PARTS)).append('/')
                .append(generateFileBasename());

        return builder.toString();
    }

    public String generateUniqueProjectName() {
        String candidate = null;
        int maxLoops = PROJECT_BEGIN_PARTS.length * PROJECT_END_PARTS.length;
        for (int i = 0; i < maxLoops; ++i) {
            candidate = RandomLists.randomItemFrom(PROJECT_BEGIN_PARTS)
                    + '_' + RandomLists.randomItemFrom(PROJECT_END_PARTS);
            if (!usedProjectNames.contains(candidate)) {
                usedProjectNames.add(checkNotNull(candidate));
                return "acme/" + candidate;
            }
        }

        // this is very unlikely; you need to have a lot of projects for the
        // above loop to fail max out; but, it is possible.
        candidate = String.format("%s_%d", candidate, System.currentTimeMillis());
        return "acme/" + candidate;
    }
}
