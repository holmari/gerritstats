package com.holmsted.gerrit.anonymizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LoremIpsumGenerator {

    private static final String[] IPSUM_SENTENCES = {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            "Curabitur laoreet at turpis non accumsan.",
            "Maecenas in leo rhoncus, sagittis risus eu, vestibulum mauris.",
            "Aenean ac ante semper, tempor odio non, lacinia purus.",
            "Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.",
            "Aliquam erat volutpat.",
            "Nullam ut mi nibh.",
            "Donec faucibus maximus venenatis.",
            "Etiam faucibus, nisl sit amet aliquet tincidunt, turpis felis suscipit turpis, vel finibus metus metus quis mi.",
            "Sed a blandit lorem. Nam sit amet neque non urna gravida finibus sit amet dictum sapien.",
            "Mauris magna felis, mattis non congue non, blandit non ante.",
            "Nam placerat lorem vitae dignissim hendrerit.",
            "Nullam semper risus dui, in pharetra mauris tempor sed.",
            "In pellentesque, metus ac lacinia blandit, leo neque interdum diam, in lobortis nibh tortor quis nibh.",
            "Maecenas convallis felis ligula, a pellentesque quam fringilla non.",
            "Fusce facilisis felis tellus, sed eleifend neque sagittis sed.",
            "Suspendisse faucibus orci tellus, sed consequat nisl malesuada sed.",
            "Mauris molestie consectetur malesuada. Morbi iaculis non enim efficitur imperdiet.",
            "Sed quis urna tortor. Nulla iaculis neque quis lorem placerat, in rhoncus erat pulvinar.",
            "Quisque non neque vel enim dignissim luctus at et risus.",
            "Fusce in ipsum ut dui aliquam venenatis sed at arcu.",
            "Nullam et egestas ante, sed convallis tortor.",
            "Aliquam tincidunt odio ac enim porta mattis.",
            "Fusce faucibus blandit lacinia. Integer mollis tristique rutrum.",
            "Maecenas non ante porta, efficitur tellus vitae, tristique ex.",
            "Integer nec venenatis sapien.",
            "Phasellus ullamcorper, leo in suscipit efficitur, purus magna lobortis ex, vel viverra est dui ut nisi.",
            "Integer ultrices velit non risus lacinia, eu auctor velit vulputate.",
            "Donec tincidunt justo sed dapibus gravida.",
            "In quam diam, pretium vel mi ut, luctus eleifend lorem.",
            "Donec sed blandit mauris.",
            "Sed eleifend vel orci id tincidunt.",
            "Sed vitae interdum massa.",
            "Curabitur in nibh vel enim rhoncus finibus.",
            "Etiam vehicula ac magna vel ullamcorper.",
            "Sed blandit risus ac nunc venenatis ullamcorper in a turpis.",
            "Nunc pellentesque felis vitae maximus sollicitudin.",
            "Sed vehicula lectus id justo sollicitudin, at aliquam erat dignissim.",
            "Proin gravida ut ligula aliquam sagittis.",
            "Cras turpis tellus, molestie vel porttitor eget, venenatis vitae risus.",
            "Phasellus imperdiet nisl vitae tortor faucibus, eu finibus dui finibus.",
            "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas.",
            "Nulla at leo id velit porta molestie ac id leo.",
            "Nam elit leo, feugiat et nibh nec, imperdiet molestie nisl.",
            "Aenean bibendum dictum turpis eu rutrum.",
            "Duis tempus et eros scelerisque placerat.",
            "In a risus vel mauris finibus tincidunt.",
            "Nullam at sem ut ante sollicitudin faucibus quis ut eros."
    };

    @Nonnull
    public static String makeLoremIpsum(int length) {
        StringBuilder ipsum = new StringBuilder();

        int i = 0;
        while (ipsum.length() <= length && i < IPSUM_SENTENCES.length) {
            ipsum.append(IPSUM_SENTENCES[i]).append(' ');
            ++i;
        }

        return ipsum.toString();
    }

    @Nullable
    public static String makeLoremIpsum(@Nullable String stringToMatch) {
        if (stringToMatch != null) {
            return makeLoremIpsum(stringToMatch.length());
        } else {
            return null;
        }
    }

    private LoremIpsumGenerator() {
    }
}
