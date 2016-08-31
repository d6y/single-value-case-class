import spray.json._
import shapeless._
import shapeless.ops.hlist.IsHCons
import cats.data.Xor
import cats.data.Xor.{Left, Right}

case class Currency(value: String)
case class Error(value: String)

trait OneFieldFormats {

  // Primitive readers and writers:

  implicit def writeString(v: String): JsValue = JsString(v)

  implicit def readString(js: JsValue): Xor[Error, String] = js match {
    case JsString(s) => Right(s)
    case _           => Left(Error(s"Hey! Expected JsString, but given $js"))
  }

  // Generic method to create formats for case classes with a single field:

  private implicit def makeFormat[
    P,           // Product type, eg Currency
    VN <: HList, // Generic representation of P, eg String :: HNil
    V            // Single value the product, eg String
    ](implicit
    gen    : Generic.Aux[P, VN],        // Converter from P to/from VN
    ev     : IsHCons.Aux[VN, V, HNil],  // Proof VN is V followed by HNil
    same   : (V :: HNil) =:= VN,        // Proof V :: HNil is the same as VN
    writer : V => JsValue,              // Writer for V values
    reader : JsValue => Xor[Error, V]   // Reader of JSON into values of V
  ): JsonFormat[P] =
      new RootJsonFormat[P] {
        override def write(obj: P): JsValue = writer(gen.to(obj).head)
        override def read(json: JsValue): P = reader(json) match {
          case Right(v)         => gen.from(v :: HNil)
          case Left(Error(msg)) => deserializationError(msg)
        }
      }

  // Export some formats:
  implicit val currencyFormat: JsonFormat[Currency] = makeFormat
}

object Main extends DefaultJsonProtocol with OneFieldFormats with App {

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
    Try {
      """{ "currency": 10, "cents": 10 }""".parseJson.convertTo[CompoundClass]
    }
  )

}
