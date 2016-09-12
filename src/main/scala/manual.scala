import spray.json._
import shapeless._
import shapeless.ops.hlist.IsHCons
import cats.data.Xor
import cats.data.Xor.{Left, Right}

object ManualMain extends DefaultJsonProtocol with App {

  // Primitive readers and writers again:

  implicit def writeString(v: String): JsValue = JsString(v)

  implicit def readString(js: JsValue): Xor[Error, String] = js match {
    case JsString(s) => Right(s)
    case _           => Left(Error(s"Hey! Expected JsString, but given $js"))
  }

  // Method to create formats for case classes with a single field:
  def mkFormat[P, V](
    construct   : V => P,
    deconstruct : P => V
  )(implicit
    writer : V => JsValue,
    reader : JsValue => Xor[Error, V]
  ): JsonFormat[P] =
      new RootJsonFormat[P] {
        override def write(obj: P): JsValue = writer(deconstruct(obj))
        override def read(json: JsValue): P = reader(json) match {
          case Right(v)         => construct(v)
          case Left(Error(msg)) => deserializationError(msg)
        }
      }


  // Export a format:
  implicit val currencyFormat: JsonFormat[Currency] = mkFormat(Currency.apply, _.value)

 def example(c: Currency): String = {
    val js = c.toJson
    val c2 = js.convertTo[Currency]
    s"$c -> ${js.prettyPrint} -> $c2"
  }

  // Happy example:
  println(
    example(Currency("USD"))
  )

  // Example failure message:
  case class CompoundClass(currency: Currency, cents: Int)
  import scala.util.Try
  implicit val ccFormat = jsonFormat2(CompoundClass)
  println(
    // Expect failure as currency should be a String such as USD not an integer
    Try {
      """{ "currency": 10, "cents": 10 }""".parseJson.convertTo[CompoundClass]
    }
  )

}
