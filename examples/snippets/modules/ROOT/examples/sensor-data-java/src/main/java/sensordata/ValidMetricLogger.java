package sensordata;

import java.rmi.UnexpectedException;

import akka.NotUsed;
import akka.stream.javadsl.*;
import akka.kafka.ConsumerMessage.Committable;

import cloudflow.streamlets.*;
import cloudflow.streamlets.avro.*;
import cloudflow.akkastream.*;
import cloudflow.akkastream.javadsl.*;
import cloudflow.akkastream.util.javadsl.*;

public class ValidMetricLogger extends AkkaStreamlet {

  AvroInlet<Metric> in = AvroInlet.<Metric>create("in", Metric.class);

  public StreamletShape shape() {
    return StreamletShape.createWithInlets(in);
  }

  RegExpConfigParameter LogLevel = RegExpConfigParameter.create(
    "log-level",
    "Provide one of the following log levels, debug, info, warning or error",
    "^debug|info|warning|error$")
    .withDefaultValue("debug");

  StringConfigParameter MsgPrefix = StringConfigParameter.create(
    "msg-prefix",
    "Provide a prefix for the log lines")
    .withDefaultValue("valid-logger");

  public ConfigParameter[] defineConfigParameters() {
    return new ConfigParameter[]{LogLevel, MsgPrefix};
  }

  interface Logging {
    public void log(String s);
  }
  
  public AkkaStreamletLogic createLogic() {
    return new RunnableGraphStreamletLogic(getContext()) {

      Logging logger = createLogging();
      
      private Logging createLogging() {
        Logging l;
        switch(getStreamletConfig().getString(LogLevel.getKey()).toLowerCase()) {
          case "debug":
            l = s -> log().debug(s);
          case "info":
            l = s -> log().info(s);
          case "warning":
            l = s -> log().warn(s);
          default: // including "error"
            l = s -> log().error(s);
        }
        return l;
      };

      String msgPrefix = getStreamletConfig().getString(MsgPrefix.getKey());

      private FlowWithContext<Metric, Committable, Metric, Committable, NotUsed> flowWithContext() {
        return FlowWithCommittableContext.<Metric>create()
        .map( invalidMetric -> {
          logger.log(" " + invalidMetric);
          return invalidMetric;
        });
      }
      public RunnableGraph createRunnableGraph() {
        return getSourceWithCommittableContext(in)
            .via(flowWithContext())
            .to(getCommittableSink());
      }
    };
  }

}