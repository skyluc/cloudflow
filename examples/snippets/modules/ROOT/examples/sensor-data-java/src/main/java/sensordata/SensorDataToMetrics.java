package sensordata;

import java.util.Arrays;

import akka.stream.javadsl.*;
import akka.kafka.ConsumerMessage.Committable;

import akka.NotUsed;

import cloudflow.streamlets.*;
import cloudflow.streamlets.avro.*;
import cloudflow.akkastream.*;
import cloudflow.akkastream.javadsl.*;

public class SensorDataToMetrics extends AkkaStreamlet {
  AvroInlet<SensorData> in = AvroInlet.<SensorData>create("in", SensorData.class);
  AvroOutlet<Metric> out =
      AvroOutlet.<Metric>create("out", Metric.class)
          .withPartitioner(RoundRobinPartitioner.getInstance());

  public StreamletShape shape() {
    return StreamletShape.createWithInlets(in).withOutlets(out);
  }

  private FlowWithContext<SensorData, Committable, Metric, Committable, NotUsed> flowWithContext() {
    return FlowWithCommittableContext.<SensorData>create()
        .mapConcat(
            data ->
                Arrays.asList(
                    new Metric(
                        data.getDeviceId(),
                        data.getTimestamp(),
                        "power",
                        data.getMeasurements().getPower()),
                    new Metric(
                        data.getDeviceId(),
                        data.getTimestamp(),
                        "rotorSpeed",
                        data.getMeasurements().getRotorSpeed()),
                    new Metric(
                        data.getDeviceId(),
                        data.getTimestamp(),
                        "windSpeed",
                        data.getMeasurements().getWindSpeed())));
  }

  public AkkaStreamletLogic createLogic() {
    return new RunnableGraphStreamletLogic(getContext()) {
      public RunnableGraph createRunnableGraph() {
        return getSourceWithCommittableContext(in)
            .via(flowWithContext())
            .to(getCommittableSink(out));
      }
    };
  }
}