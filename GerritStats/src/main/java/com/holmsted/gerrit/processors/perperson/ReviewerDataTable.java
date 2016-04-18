package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;
import com.holmsted.gerrit.processors.perperson.IdentityRecord.ReviewerData;

import java.util.Hashtable;

public class ReviewerDataTable extends Hashtable<Commit.Identity, ReviewerData> {
}
