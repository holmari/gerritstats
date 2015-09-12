package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.ArrayList;

import javax.annotation.Nonnull;

public class IdentityRecordList extends ArrayList<IdentityRecord> {

    public int getIndexOfIdentity(@Nonnull Commit.Identity identity) {
        for (int i = 0; i < size(); ++i) {
            IdentityRecord record = get(i);
            if (record.identity.equals(identity)) {
                return i;
            }
        }
        return -1;
    }
}
