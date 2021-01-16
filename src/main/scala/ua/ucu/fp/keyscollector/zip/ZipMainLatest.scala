package ua.ucu.fp.keyscollector.zip

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FanInShape2, Inlet, _}


object ZipMainLatest {

  /**
   * Create a new `ZipMainLatest`.
   */
  def apply[A, B](): ZipMainLatest[A, B] = new ZipMainLatest()
}

final class ZipMainLatest[A, B] extends ZipMainLatestWith2[A, B, (A, B)](Tuple2.apply) {
  override def toString = "ZipMainLatest"
}

class ZipMainLatestWith2[A1, A2, O](val zipper: (A1, A2) => O) extends GraphStage[FanInShape2[A1, A2, O]] {
  override def initialAttributes = Attributes.name("ZipMainLatestWith2")

  override val shape: FanInShape2[A1, A2, O] = new FanInShape2[A1, A2, O]("ZipMainLatestWith2")

  def out: Outlet[O] = shape.out

  val in0: Inlet[A1] = shape.in0
  val in1: Inlet[A2] = shape.in1

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) { outer =>
    var in1HasValue = false
    // Without this field the completion signalling would take one extra pull
    var willShutDown = false

    private var staleTupleValues = true

    val inlet1 = new ZipLatestInlet(in1, in0)

    private def pushAll(): Unit = {
      push(out, zipper(grab(in0), inlet1.value))
      if (willShutDown) completeStage()
      else {
        pull(in0)
        if (!outer.hasBeenPulled(in1)) pull(in1)
      }
    }

    override def preStart(): Unit = {
      pull(in0)
      pull(in1)
    }

    setHandler(in0, new InHandler {
      override def onPush(): Unit = {

        if (in1HasValue) {
          pushAll()
          staleTupleValues = true
        } else {
          staleTupleValues = false
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (!isAvailable(in0)) completeStage()
        willShutDown = true
      }

    })
    setHandler(in1, inlet1)

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (in1HasValue && !staleTupleValues) pushAll()
      }
    })

    private class ZipLatestInlet[T, R](in: Inlet[T], mainIn: Inlet[R]) extends InHandler {
      var value: T = _

      override def onPush() = {
        value = outer.grab(in)
        outer.in1HasValue = true
        if (!outer.hasBeenPulled(mainIn)) pull(mainIn)
      }

      override def onUpstreamFinish(): Unit = {
        completeStage()
      }
    }
  }
}