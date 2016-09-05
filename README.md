# GerritStats

This project provides tools to display statistics from a Gerrit repository. It generates output in HTML, CSV and plaintext formats.

The tool can be useful for analysing how efficiently code reviews are implemented in an organization, and how often cross-team code reviews are conducted.

There are two separate tools:

* GerritDownloader, which downloads JSON data from the server
* GerritStats, which parses the json output provided by GerritDownloader.

## How to build


```
# Prerequisite: install brew from http://brew.sh
brew install npm
./gradlew assemble
```

## How to execute

```
./gerrit_downloader.sh

```

Lists all command line options for the data download tool.

```
./gerrit_stats.sh
```

Lists all command line options for the statistics generator.

## How to execute: saving data locally

Fetching data from Gerrit can take a while. Use GerritDownloader to get the JSON output from the server so that you can parse it later:

```
./gerrit_downloader.sh --server gerrit.instance.on.inter.nets --project YourProjectName --limit 7500 --output-dir gerrit_out/
```

The above command will download all data from the given Gerrit server and given project, and store the data in .json format in the given output directory.

If you have multiple projects in Gerrit and want to take a look at all the data, omit the --project argument. This will download data for all projects you have
access to:

```
./gerrit_downloader.sh --server gerrit.instance.on.inter.nets --output-dir gerrit_out/
```

## How to execute: plaintext output

Once you have the data, you can start to play around with the output. You can e.g. include only a particular set of developers, which can be useful
when looking at e.g. team-level review practices.

```
./gerrit_stats.sh --file ./GerritStats/gerrit-json-out.txt --branches master --include joe.developer@inter.nets,jeff@buckley.org,deep@purple.com,beastie@boys.com --output-type plain
```

The above command will give you output as illustrated below:

```
joe.developer@inter.nets
  Commits: 1072
  Comments written: 748
  Comments received: 444
  Commit/comment ratio: 0.41417912
  Added as reviewer: 1939
  Review comment ratio: 0.38576585
  Avg. patch set count: 1.4906716
  Max patch set count: 11
  +2 reviews given: 944
  +1 reviews given: 1115
  -1 reviews given: 285
  -2 reviews given: 111
  # of people added as reviewers: 26
  Adds them as reviewers: jeff@buckley.org (900), deep@purple.com (875), beastie@boys.com (1)
  They add this person as reviewer: jeff@buckley.org (300), rolling@stones.com (217), beastie@boys.com (1)
```

## How to execute: CSV output

If you want to do some processing on the data with Excel or similar tools, a CSV format might be convenient for import.

```
./gerrit_stats.sh --file gerrit-json-out.txt --branches master --include developer1@domain.com,developer2@domain.com,...developer5@domain.com --output-type csv
```

The output of the command looks like this:

```
Project: all data from file /Users/holmsted/dev/GerritStats/out.txt
Branches: master
From: 2014-10-23
To: 2015-08-21

Identity	            Commits	Comments written	Comments received	Commit/comment ratio	Added as reviewer	Review comment ratio	Avg. patch set count	Max patch set count	+2 reviews given	+1 reviews given	-1 reviews given	-2 reviews given	# of people added as reviewers
developer1@domain.com	     80	               1	               14	            0.175000	              335	            0.002985	            1.487500	                 8	             35	             100	             5	             1	                                   18
developer2@domain.com	      8	               4	                2	            0.250000	              167	            0.023952	            1.250000	                 3	             23	              23	             6	             0	                                    6
developer3@domain.com	     50	               8	                7	            0.140000	              366	            0.021858	            1.760000	                 5	            101	              77	             7	             9	                                   11
developer4@domain.com	    276	              19	               57	            0.206522	             1377	            0.013798	            2.246377	                13	             174	         225	            17	             8	                                   27
developer5@domain.com	   1293	             328	              257	            0.198763	             2537	            0.129287	            1.486466	                11	            1231	        1330	           384	           136	                                   26
```

## How to execute: HTML output

The default output of the tool is HTML.

This will generate a set of HTML files that include graphs and charts, such as a proximity graph of the developers,
based on how many review comments they write to each other, as well as a per-developer page that lists all their review
comments and links back to the reviews.

```
./gerrit_stats.sh --file gerrit-json-out.txt --branches master --include developer1@domain.com,developer2@domain.com,...developer5@domain.com --list-commits-exceeding-patch-set-count 5
```

However, you don't need all the arguments to generate output. Just pass the directory with downloaded json data to -f, and you're ready to go:

```
./gerrit_stats.sh --f gerrit_out/
```

The index page will provide you with a sortable overview table of some of the core statistics, similar to the plaintext CSV output.

![Overview table of all developers](doc/overview_table.png)

The index page also contais a graph that illustrates how developers are connected to each other:

![Proximity graph for the given branch and given set of identities](doc/proximity_graph.png)

And on the per-person page, you'll see a chart of review comments per day, a configurable list of commits that have a high number of patch sets, et cetera:

![Review comment statistic written by a developer](doc/review_comments.png)
