/*
 * Copyright (C) 2016-2019 Lightbend Inc. <https://www.lightbend.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloudflow.streamlets.descriptors

import scala.collection.immutable

import org.apache.avro.SchemaBuilder

import com.typesafe.config.Config

import cloudflow.streamlets._
import cloudflow.streamlets.avro.AvroUtil

case class Coffee(espressos: Int)

object Schemas {
  val coffeeSchema = SchemaBuilder
    .record("Coffee").namespace("cloudflow.sbt")
    .fields()
    .name("expressos").`type`().nullable().intType().noDefault()
    .endRecord()
}

case object TestRuntime extends StreamletRuntime {
  override val name = "test-runtime"
}

trait TestStreamlet extends Streamlet {
  override def runtime: StreamletRuntime = TestRuntime
  def logStartRunnerMessage(buildInfo: String): Unit = ???
  override protected def createContext(config: Config): StreamletContext = ???
  override def run(config: Config): StreamletExecution = ???

}

class CoffeeIngress extends Streamlet with TestStreamlet {
  case class TestOutlet(name: String, schemaDefinition: SchemaDefinition) extends Outlet
  override val shape = StreamletShape(TestOutlet("out", AvroUtil.createSchemaDefinition(Schemas.coffeeSchema)))
  override val labels: immutable.IndexedSeq[String] = Vector("test", "coffee")
  override val description: String = "Coffee Ingress Test"
}
