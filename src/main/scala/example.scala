import spray.json._
import shapeless._
import shapeless.ops.hlist.IsHCons
import cats.data.Xor
import cats.data.Xor.{Left, Right}

case class Currency(value: String)
case class Error(value: String)

object Main extends DefaultJsonProtocol with App {

  implicit def writeString(v: String): JsValue = JsString(v)
  
  implicit def readString(js: JsValue): Xor[Error, String] = js match {
    case JsString(s) => Right(s)
    case _           => Left(Error(s"That's no string! $js"))
  }

  implicit def jsonTinyFormat1[P, VN <: HList, V, X <: HList](implicit 
    gen    : Generic.Aux[P, VN],
    ev     : IsHCons.Aux[VN, V, X],
    eve    : (V :: HNil) =:= VN,
    writer : V => JsValue,
    reader : JsValue => Xor[Error, V]
  ): JsonFormat[P] =
      new RootJsonFormat[P] {
        override def write(obj: P): JsValue = writer(gen.to(obj).head)
        override def read(json: JsValue): P = reader(json) match {
          case Right(v)         => gen.from(v :: HNil) 
          case Left(Error(msg)) => deserializationError(msg)
        }
      }

  val g = implicitly[Generic[Currency]]
  val a = implicitly[Generic.Aux[Currency, String :: HNil]]
  val w = implicitly[String => JsValue]
  val r = implicitly[JsValue => Xor[Error, String]]

  //val fmt: spray.json.JsonFormat[Currency] = jsonTinyFormat1(a,e1,e2,w,r)

  val cf = implicitly[JsonFormat[Currency]]
  val currencyFormat: spray.json.JsonFormat[Currency] = jsonTinyFormat1

}
