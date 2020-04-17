package com.holmsted.gerrit;

public enum GerritStatsVersion {

	LEGACY, // ordinal 0; this format may be extinct
	SSH, // ordinal 1, SSH query result
	REST_CHANGE, // ordinal 2, REST API result for change
	REST_CHANGE_DETAIL, // ordinal 2, REST API result for change detail
}
