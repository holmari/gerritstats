package com.holmsted.gerrit.anonymizer;

import com.holmsted.RandomLists;
import com.holmsted.gerrit.Commit;
import com.holmsted.resources.ResourceReader;

import java.util.ArrayList;
import java.util.List;

public class IdentityGenerator {

    private final static String[] DOMAIN_SUFFIXES = {
            "com",
            "org",
            "net",
            "int",
            "edu",
            "gov",
            "mil",
            "guru",
            "date",
            "coop",
            "app",
            "tv"
    };

    private static class Name {
        public final String firstName;
        public final String lastName;
        public final String username;

        public Name(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;

            username = (firstName.substring(0, 1)
                     + lastName.substring(0, Math.min(6, lastName.length()))
                     + ((Math.random() > 0.75) ? String.format("%d", (int) (100 * Math.random())) : ""))
                    .toLowerCase();
        }

        public String getDisplayName() {
            return String.format("%s %s", firstName, lastName);
        }

        public String getEmail(String domainName) {
            return String.format("%s.%s@%s", firstName, lastName, domainName);
        }
    }

    private final List<String> domainBasenames = new ArrayList<>();
    private final List<String> firstNames = new ArrayList<>();
    private final List<String> lastNames = new ArrayList<>();

    public Commit.Identity generateIdentity() {
        Commit.Identity identity = new Commit.Identity();
        Name generatedName = generateName();
        String domain = generateDomain();

        identity.name = generatedName.getDisplayName();
        identity.email = generatedName.getEmail(domain);
        identity.username = generatedName.username;

        return identity;
    }

    private String generateDomain() {
        if (domainBasenames.isEmpty()) {
            domainBasenames.addAll(ResourceReader.readResourceFile("name_generator/domains.txt"));
        }

        String domainSuffix = RandomLists.randomItemFrom(DOMAIN_SUFFIXES);
        return String.format("%s.%s",
                RandomLists.randomItemFrom(domainBasenames),
                domainSuffix);
    }

    private Name generateName() {
        if (firstNames.isEmpty() || lastNames.isEmpty()) {
            firstNames.addAll(ResourceReader.readResourceFile("name_generator/first_names.txt"));
            lastNames.addAll(ResourceReader.readResourceFile("name_generator/last_names.txt"));
        }

        String firstName = RandomLists.randomItemFrom(firstNames);
        String lastName = RandomLists.randomItemFrom(lastNames);

        return new Name(firstName, lastName);
    }
}
