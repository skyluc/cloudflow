package sensordata;

import akka.NotUsed;
import akka.stream.javadsl.*;
import akka.kafka.ConsumerMessage.Committable;

import cloudflow.streamlets.*;
import cloudflow.streamlets.avro.*;
import cloudflow.akkastream.*;
import cloudflow.akkastream.javadsl.*;
import cloudflow.akkastream.javadsl.util.Either;
import cloudflow.akkastream.util.javadsl.*;

public class InvalidMetricLogger extends AkkaStreamlet  {
  AvroInlet<Metric> in = AvroInlet.<Metric>create("in", Metric.class);

  public StreamletShape shape() {
    return StreamletShape.createWithInlets(in);
  }
  
  public AkkaStreamletLogic createLogic() {
    return new RunnableGraphStreamletLogic(getContext()) {
      private FlowWithContext<Metric, Committable, Metric, Committable, NotUsed> flowWithContext() {
        return FlowWithCommittableContext.<Metric>create()
        .map( invalidMetric -> {
          log().warn("Invalid metric detected! " + invalidMetric);
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