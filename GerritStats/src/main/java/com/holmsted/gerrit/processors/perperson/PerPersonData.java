package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.QueryData;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import javax.annotation.Nonnull;

public class PerPersonData extends Hashtable<Commit.Identity, IdentityRecord> {

    private QueryData queryData;
    private long fromDate;
    private long toDate;

    public IdentityRecordList toOrderedList(@Nonnull Comparator<? super IdentityRecord> comparator) {
        IdentityRecordList orderedList = new IdentityRecordList();
        orderedList.addAll(values());
        Collections.sort(orderedList, comparator);
        return orderedList;
    }

    public void setQueryData(@Nonnull QueryData queryData) {
        this.queryData = queryData;
    }

    public void setFromDate(long fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(long toDate) {
        this.toDate = toDate;
    }

    @Nonnull
    public QueryData getQueryData() {
        return queryData;
    }

    public long getFromDate() {
        return fromDate;
    }

    public long getToDate() {
        return toDate;
    }
}
