package sensordata;

import akka.stream.javadsl.*;

import akka.NotUsed;
import akka.actor.*;
import akka.kafka.ConsumerMessage.Committable;
import akka.stream.*;

import com.typesafe.config.Config;

import cloudflow.streamlets.*;
import cloudflow.streamlets.avro.*;
import cloudflow.akkastream.*;
import cloudflow.akkastream.javadsl.*;
import cloudflow.akkastream.javadsl.util.Either;
import cloudflow.akkastream.util.javadsl.*;

public class MetricsValidation extends AkkaStreamlet {
  AvroInlet<Metric> inlet = AvroInlet.<Metric>create("in", Metric.class);
  AvroOutlet<InvalidMetric> invalidOutlet =
      AvroOutlet.<InvalidMetric>create(
          "invalid", m -> m.getMetric().toString(), InvalidMetric.class);
  AvroOutlet<Metric> validOutlet =
      AvroOutlet.<Metric>create(
          "valid", m -> m.getDeviceId().toString() + m.getTimestamp().toString(), Metric.class);

  public StreamletShape shape() {
    return StreamletShape.createWithInlets(inlet).withOutlets(invalidOutlet, validOutlet);
  }

  public AkkaStreamletLogic createLogic() {
    return new RunnableGraphStreamletLogic(getContext()) {
      public RunnableGraph createRunnableGraph() {
        return getSourceWithCommittableContext(inlet)
            .to(Splitter.sink(createFlow(), invalidOutlet, validOutlet, getContext()));
      }
    };
  }

  private FlowWithContext<Metric, Committable, Either<InvalidMetric, Metric>, Committable, NotUsed>
      createFlow() {
    return FlowWithCommittableContext.<Metric>create()
        .map(
            metric -> {
              if (!SensorDataUtils.isValidMetric(metric))
                return Either.left(
                    new InvalidMetric(metric, "All measurements must be positive numbers!"));
              else return Either.right(metric);
            });
  }
}