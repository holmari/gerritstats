package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.QueryData;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class PerPersonData {

    private final Map<Commit.Identity, IdentityRecord> records = new Hashtable<>();

    private QueryData queryData;
    private long fromDate;
    private long toDate;

    @Nonnull
    public IdentityRecordList toOrderedList(@Nonnull Comparator<? super IdentityRecord> comparator) {
        IdentityRecordList orderedList = new IdentityRecordList();
        orderedList.addAll(records.values());
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

    public IdentityRecord get(@Nonnull Commit.Identity identity) {
        return records.get(identity);
    }

    public void clear() {
        records.clear();
    }

    public void put(@Nonnull Commit.Identity identity, IdentityRecord identityRecord) {
        records.put(identity, identityRecord);
    }

    public Set<Commit.Identity> keySet() {
        return records.keySet();
    }
}
