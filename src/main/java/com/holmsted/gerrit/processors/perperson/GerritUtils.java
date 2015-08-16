package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import javax.annotation.Nonnull;

public class GerritUtils {

    /**
     * Creates a code comment URL.
     * <p>
     * Commit url: https://gerrit.domain.com/29251
     * Output: https://gerrit.domain.com/#/c/29251/13/examples/UiToolkitExample/res/layout/uitoolkit_dark_top_bar.xml
     *
     */
    public static String getUrlForComment(@Nonnull Commit commit,
                                          @Nonnull Commit.PatchSet patchSet,
                                          @Nonnull Commit.PatchSetComment comment) {
        String url = commit.url;
        String baseUrl = url.substring(0, url.lastIndexOf('/'));
        return String.format("%s/#/c/%d/%d/%s", baseUrl, commit.commitNumber, patchSet.number, comment.file);
    }
}
