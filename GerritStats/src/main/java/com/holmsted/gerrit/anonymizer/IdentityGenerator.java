package com.holmsted.gerrit.anonymizer;

import com.holmsted.RandomLists;
import com.holmsted.gerrit.Commit;
import com.holmsted.resources.ResourceReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

public class IdentityGenerator {

    private static final String[] DOMAIN_SUFFIXES = {
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

    private static final Random RANDOM_GENERATOR = new Random();

    private final List<String> domainBasenames = new ArrayList<>();
    private final List<String> firstNames = new ArrayList<>();
    private final List<String> lastNames = new ArrayList<>();

    private static class Name {
        @Nonnull
        public final String firstName;
        @Nonnull
        public final String lastName;
        @Nonnull
        public final String username;

        public Name(@Nonnull String firstName, @Nonnull String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;

            username = (firstName.substring(0, 1)
                     + lastName.substring(0, Math.min(6, lastName.length()))
                     + (RANDOM_GENERATOR.nextInt(100) > 75
                    ? String.format("%d", (int) (RANDOM_GENERATOR.nextInt(100))) : ""))
                    .toLowerCase();
        }

        public String getDisplayName() {
            return String.format("%s %s", firstName, lastName);
        }

        public String getEmail(String domainName) {
            return String.format("%s.%s@%s", firstName, lastName, domainName);
        }
    }

    @Nonnull
    public Commit.Identity generateIdentity() {
        Name generatedName = generateName();
        String domain = generateDomain();

        return new Commit.Identity(
                generatedName.getDisplayName(),
                generatedName.getEmail(domain),
                generatedName.username);
    }

    @Nonnull
    private String generateDomain() {
        if (domainBasenames.isEmpty()) {
            domainBasenames.addAll(ResourceReader.readResourceFile("name_generator/domains.txt"));
        }

        String domainSuffix = RandomLists.randomItemFrom(DOMAIN_SUFFIXES);
        return String.format("%s.%s",
                RandomLists.randomItemFrom(domainBasenames),
                domainSuffix);
    }

    @Nonnull
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
