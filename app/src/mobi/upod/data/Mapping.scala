package mobi.upod.data

import java.net.{URL, URI}
import org.joda.time.DateTime


trait Mapping[A] {

  def read(element: DataElement): A

  def write(factory: DataElementFactory, name: String, element: A): DataElement
}

object Mapping {

  def Mapping[A](rd: DataElement => A)(wrt: (DataElementFactory, String, A) => DataElement) = new Mapping[A] {
    def read(element: DataElement): A = rd(element)

    def write(factory: DataElementFactory, name: String, element: A): DataElement = wrt(factory, name, element)
  }

  def PrimitiveMapping[A](read: DataPrimitive => A)(write: (DataElementFactory, String, A) => DataPrimitive) = Mapping[A] {
    element =>
      read(element.asDataPrimitive)
  } {
    (factory, name, element) =>
      write(factory, name, element)
  }

  private def IterableMapping[A, B <: Iterable[A]](mapping: Mapping[A])(convert: Iterable[A] => B) = Mapping[B] {
    element =>
      convert(element.asDataSequence.map(mapping.read(_)))
  } {
    (factory, name, element) =>
      factory.create(name, element.map(mapping.write(factory, "", _)))
  }

  val boolean = PrimitiveMapping(_.asBoolean)(_.create(_, _))

  val int = PrimitiveMapping(_.asInt)(_.create(_, _))

  val long = PrimitiveMapping(_.asLong)(_.create(_, _))

  val float = PrimitiveMapping(_.asFloat)(_.create(_, _))

  val string = PrimitiveMapping(_.asString)(_.create(_, _))

  val symbol = PrimitiveMapping(e => Symbol(e.asString))((factory, name, value) => factory.create(name, value.name))

  val uri = PrimitiveMapping(element => new URI(element.asString))(
    (factory, name, uri) => factory.create(name, uri.toString))

  val url = PrimitiveMapping(element => new URL(element.asString))(
    (factory, name, url) => factory.create(name, url.toString))

  val dateTime = PrimitiveMapping(element => new DateTime(element.asLong))(
    (factory, name, dt) => factory.create(name, dt.getMillis))

  def enumerated[A <: Enumeration](enumeration: A) =
    PrimitiveMapping[A#Value](d => enumeration.withName(d.asString))((factory, name, value) => factory.create(name, value.toString))

  def optional[A](mapping: Mapping[A]) = Mapping[Option[A]] {
    element =>
      element match {
        case _: NoData => None
        case data: DataElement => Some(mapping.read(data))
      }
  } {
    (factory, name, element) =>
      element match {
        case Some(e) => mapping.write(factory, name, e)
        case None => factory.none(name)
      }
  }

  def optional[A](mapping: Mapping[A], default: A) = Mapping[A] {
    element =>
      element match {
        case _: NoData => default
        case data: DataElement => mapping.read(data)
      }
  } {
    mapping.write
  }

  def seq[A](mapping: Mapping[A]) = IterableMapping[A, Seq[A]](mapping)(_.toSeq)

  def set[A](mapping: Mapping[A]) = IterableMapping[A, Set[A]](mapping)(_.toSet)

  def csv[A](fromString: String => A, toString: A => String) = Mapping[Set[A]] {
    element => element.asDataPrimitive.asString.split(",").map(_.trim).toSet.map(fromString)
  } {
    (factory, name, element) =>
      factory.create(name, element.map(toString).mkString(","))
  }

  def csvStrings: Mapping[Set[String]] = csv[String](s => s, s => s)

  //
  // type safe object mappings
  //

  private type M[A] = (String, Mapping[A])

  def ObjectMapping[A](read: DataObject => A)(write: (DataElementFactory, String, A) => Option[DataObject]) = Mapping[A] {
    element =>
      read(element.asDataObject)
  } {
    (factory, name, element) => write(factory, name, element) match {
      case Some(dataObject: DataObject) => dataObject
      case None => factory.none(name)
    }
  }

  def simpleMapping[A](m: M[A]) = map(m)(o => o)(o => Some(o))

  def map[A, M1](m1: M[M1])(construct: (M1) => A)(extract: A => Option[(M1)]) = ObjectMapping(
    o => construct(read(o, m1)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t))))

  def map[A, M1, M2](m1: M[M1], m2: M[M2])(construct: (M1, M2) => A)(extract: A => Option[(M1, M2)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2))))

  def map[A, M1, M2, M3](m1: M[M1], m2: M[M2], m3: M[M3])(construct: (M1, M2, M3) => A)(extract: A => Option[(M1, M2, M3)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3))))

  def map[A, M1, M2, M3, M4](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4])(construct: (M1, M2, M3, M4) => A)(extract: A => Option[(M1, M2, M3, M4)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4))))

  def map[A, M1, M2, M3, M4, M5](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5])(construct: (M1, M2, M3, M4, M5) => A)(extract: A => Option[(M1, M2, M3, M4, M5)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5))))

  def map[A, M1, M2, M3, M4, M5, M6](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6])(construct: (M1, M2, M3, M4, M5, M6) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6))))

  def map[A, M1, M2, M3, M4, M5, M6, M7](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7])(construct: (M1, M2, M3, M4, M5, M6, M7) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8])(construct: (M1, M2, M3, M4, M5, M6, M7, M8) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17], m18: M[M18])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17), read(o, m18)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17), write(f, m18, t._18))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17], m18: M[M18], m19: M[M19])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17), read(o, m18), read(o, m19)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17), write(f, m18, t._18), write(f, m19, t._19))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17], m18: M[M18], m19: M[M19], m20: M[M20])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17), read(o, m18), read(o, m19), read(o, m20)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17), write(f, m18, t._18), write(f, m19, t._19), write(f, m20, t._20))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17], m18: M[M18], m19: M[M19], m20: M[M20], m21: M[M21])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17), read(o, m18), read(o, m19), read(o, m20), read(o, m21)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17), write(f, m18, t._18), write(f, m19, t._19), write(f, m20, t._20), write(f, m21, t._21))))

  def map[A, M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21, M22](m1: M[M1], m2: M[M2], m3: M[M3], m4: M[M4], m5: M[M5], m6: M[M6], m7: M[M7], m8: M[M8], m9: M[M9], m10: M[M10], m11: M[M11], m12: M[M12], m13: M[M13], m14: M[M14], m15: M[M15], m16: M[M16], m17: M[M17], m18: M[M18], m19: M[M19], m20: M[M20], m21: M[M21], m22: M[M22])(construct: (M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21, M22) => A)(extract: A => Option[(M1, M2, M3, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M20, M21, M22)]) = ObjectMapping(
    o => construct(read(o, m1), read(o, m2), read(o, m3), read(o, m4), read(o, m5), read(o, m6), read(o, m7), read(o, m8), read(o, m9), read(o, m10), read(o, m11), read(o, m12), read(o, m13), read(o, m14), read(o, m15), read(o, m16), read(o, m17), read(o, m18), read(o, m19), read(o, m20), read(o, m21), read(o, m22)))(
    (f, n, e) => extract(e).map(t => f.create(n, write(f, m1, t._1), write(f, m2, t._2), write(f, m3, t._3), write(f, m4, t._4), write(f, m5, t._5), write(f, m6, t._6), write(f, m7, t._7), write(f, m8, t._8), write(f, m9, t._9), write(f, m10, t._10), write(f, m11, t._11), write(f, m12, t._12), write(f, m13, t._13), write(f, m14, t._14), write(f, m15, t._15), write(f, m16, t._16), write(f, m17, t._17), write(f, m18, t._18), write(f, m19, t._19), write(f, m20, t._20), write(f, m21, t._21), write(f, m22, t._22))))

  private def read[B](obj: DataObject, mapping: (String, Mapping[B])): B =
    mapping._2.read(obj(mapping._1))

  private def write[B](factory: DataElementFactory, mapping: (String, Mapping[B]), element: B): (String, DataElement) =
    mapping._1 -> mapping._2.write(factory, mapping._1, element)
}