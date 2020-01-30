# About

Client that connects to a stream of successful Netflix plays and consumes the events in a streaming fashion.
The client captures the stream, aggregates it on one second intervals grouped by device, title, country
and computes counts for these combinations.

# Instructions

## Pre-requisites

### Install Homebrew

- Refer to the [docs](https://brew.sh/)

Note that below instructions have been tested on MacOS (version 10.14.6)

### Install Java version 1.8 or above (if not already installed)

```
brew cask install java
```

### Download jar
```
curl -o starts_per_second_1-assembly-0.1.jar https://raw.githubusercontent.com/nsnk19/repo1/master/target/scala-2.12/starts_per_second_1-assembly-0.1.jar
```

### Execute jar
```
java -jar starts_per_second_1-assembly-0.1.jar
```

# Output

Output would look like below (note output has a field `timeInSec` to indicate what time in seconds
start time has been grouped over):

```
{"device":"xbox_360","sps":23,"title":"house of cards","country":"IND","timeInSec":1580284452}
{"device":"ps4","sps":13,"title":"stranger things","country":"IND","timeInSec":1580284452}
{"device":"xbox_one_x","sps":17,"title":"kublai khan","country":"BR","timeInSec":1580284452}
{"device":"xbox_360","sps":23,"title":"orange is the new black","country":"UK","timeInSec":1580284452}}
{"device":"ps3","sps":13,"title":"marco polo","country":"USA","timeInSec":1580284452}
```

# Code Organization

`src/main/scala/StartsPerSecond/ReadAndAggregateStreamData.scala` - main client code for parsing and aggregating stream events
`src/main/scala/StartsPerSecond/JSONUtil.scala` - utility library for encoding / decoding JSON

# Assumptions

- Input data that is invalid or incorrectly formatted is ignored.
- Stream events that contain `"sev":"success"` or `"sev":"successinfo"` are considered successful.


# Tools / languages

- The implementation uses Scala as the programming language.
- It uses Monix (https://monix.io/), a high-performance Scala library for asynchronous, event-based programming.
- It uses Jackson, a high-performance JSON processor.


# Scalability / Reliability

- The solution as provided works reliably on a single host. If we wish to scale to a larger number of
hosts, one way to do so would be to do hash-based partitioning of the input stream data by
`(device, title, country)` attribute tuples among available hosts. Each host could then independently
run `ReadAndAggregateStreamData` on a partition of the input stream. Alternatively we could route
streaming events to a cluster of hosts within a data center close to the country location of the event.
Within a cluster we could partition data across hosts based on `(device, title)` attributes.

- To handle varying load over a period of day, assuming we don't need to process the data stream in real-time,
we could add the stream events to a distributed message queue (such as Kafka or Kinesis). A subset of the stream
partitions / shards could be assigned to each host for processing. If no host is available to immediately process
the events in partition / shard, it will remain unassigned till a host becomes available. A host will need to
checkpoint its progress periodically to avoid reprocessing events if it goes down.

- Testing would include testing under heavy load and testing with uneven distribution of traffic (in
both time as well as across attributes like country, title, device). I would also test how service opeerates
in case of brief outages to ensure all events are reliably processed.
