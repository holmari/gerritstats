# GerritStats

This project provides statistics from a Gerrit repository. It provides output in both CSV and plaintext format.

The tool can be useful for analysing how efficiently code reviews are implemented in an organization, and how
often cross-team code reviews are conducted.

## How to build

```
./gradlew assemble
```

## How to execute

```
java -jar build/libs/GerritStats.jar
```

Lists all command line options.

## How to execute: saving data locally

Because fetching data from Gerrit can take a while, it's best if you first fetch the data locally, like so:

```
java -jar build/libs/GerritStats.jar --server gerrit.instance.on.inter.nets --project YourProjectName --limit 7500 --output-file gerrit-json-out.txt 
```

The above command will download all data from the given Gerrit server and given project, and store it in the given output file.

Once you have the data, you can start to play around with the output. You can e.g. include only a particular set of developers, which can be useful
when looking at e.g. team-level review practices.

```
java -jar GerritStats.jar --file ./GerritStats/gerrit-json-out.txt --branches master --include joe.developer@inter.nets,jeff@buckley.org,deep@purple.com,beastie@boys.com
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
