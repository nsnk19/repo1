## Instructions

Note that below instructions have been tested on MacOS (version 10.14.6)

- Install Java (if not already installed)
```
brew cask install java
```

- Download jar
```
curl -o starts_per_second_1-assembly-0.1.jar https://raw.githubusercontent.com/nsnk19/repo1/master/target/scala-2.12/starts_per_second_1-assembly-0.1.jar
```

- Execute jar
```
java -jar starts_per_second_1-assembly-0.1.jar
```

## Output
Output would look like below (note output has a field timeInSec to indicate what time in seconds
start time has been grouped over):

```
{"device":"xbox_360","sps":23,"title":"house of cards","country":"IND","timeInSec":1580284452}
{"device":"ps4","sps":13,"title":"stranger things","country":"IND","timeInSec":1580284452}
{"device":"xbox_one_x","sps":17,"title":"kublai khan","country":"BR","timeInSec":1580284452}
{"device":"xbox_360","sps":23,"title":"orange is the new black","country":"UK","timeInSec":1580284452}}
{"device":"ps3","sps":13,"title":"marco polo","country":"USA","timeInSec":1580284452}
```

## Assumptions
- Input data that is invalid or incorrectly formatted is ignored.
- Stream events that contain `"sev":"success"` or `"sev":"successinfo"` are considered successful.


## Tools / languages
- The implementation uses Scala as the programming language.
- It uses Monix (https://monix.io/), a high-performance Scala library for asynchronous, event-based programming.
- It uses Jackson, a high-performance JSON processor.


## Scalability / Reliability
- The solution as provided works reliably on a single host. If we wish to scale to a larger number of
hosts, one way to do so would be to do hash-based partitioning of the input stream data by
`(device, title, country)` attribute tuples. Each host could then independently run `ReadAndAggregateStreamData`
on a partition of the input stream. We could partition on a smaller subset of the above attributes,
however this would increase the likelihood of some partitions running hotter (thus some hosts needing
to process more stream data) than others.

- To handle varying load over a period of day, assuming we don't need to process the data stream in real-time,
we could add the stream events to a distributed queue (such as Kafka or Kinesis). A subset of the stream
partitions / shards could be assigned to each host for processing. If no host is available to immediately process
the events in partition / shard, it will remain unassigned till a host becomes available. A host will need to
checkpoint its progress periodically to avoid reprocessing events if it goes down

- Testing would include testing under heavy load and testing with uneven distribution of traffic (in both time as well
as across attributes like country, title, device)
