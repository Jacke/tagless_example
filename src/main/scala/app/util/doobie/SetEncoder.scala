package app.util.doobie
import doobie.util.fragment.Fragment

trait SetEncoder[A] {
  def encode(v: A): List[String]
}

object SetEncoder {
  def apply[A: SetEncoder]: SetEncoder[A] = implicitly

  def instance[A](f: A => List[String]): SetEncoder[A] = f(_)

  object instance {
    import shapeless.{HList, ::, HNil, Lazy, LabelledGeneric, Witness}
    import shapeless.labelled.FieldType

    private def stdAList[A](a: A): List[String] = List(s"$a")

    implicit val intInstance: SetEncoder[Int] = stdAList
    implicit val longInstance: SetEncoder[Long] = stdAList
    implicit val shortInstance: SetEncoder[Short] = stdAList
    implicit val byteInstance: SetEncoder[Byte] = stdAList
    implicit val boolInstance: SetEncoder[Boolean] = stdAList
    implicit val floatInstance: SetEncoder[Float] = stdAList
    implicit val doubleInstance: SetEncoder[Double] = stdAList
    implicit val stringInstance: SetEncoder[String] = a => List(s"'$a'")
    implicit def listStringEncoder: SetEncoder[List[String]] = instance {
      case Nil => Nil
      case lst => List(s"'{${lst.mkString(",")}}'")
    }

    implicit def pairInstance[A, B](
        implicit
        A: SetEncoder[A],
        B: SetEncoder[B]
    ): SetEncoder[(A, B)] = instance {
      case (a, b) => A.encode(a) ++ B.encode(b)
    }

    implicit def optionInstance[A](
        implicit A: SetEncoder[A]): SetEncoder[Option[A]] = instance {
      case Some(a) => A.encode(a)
      case None    => Nil
    }

    implicit def listInstance[A](
        implicit A: SetEncoder[A]): SetEncoder[List[A]] = instance {
      case Nil => Nil
      case lst =>
        List(s"'{${lst.map(x => A.encode(x).mkString(",")).mkString(",")}}'")
    }

    implicit val hNilInstance: SetEncoder[HNil] = instance(_ => Nil)

    implicit def labelGenericEncoder[A, R](
        implicit
        G: LabelledGeneric.Aux[A, R],
        E: Lazy[SetEncoder[R]]
    ): SetEncoder[A] =
      instance(x => E.value.encode(G.to(x)))

    implicit def hListInstance[K <: Symbol, H, T <: HList](
        implicit
        W: Witness.Aux[K],
        H: Lazy[SetEncoder[H]],
        T: SetEncoder[T]
    ): SetEncoder[FieldType[K, H] :: T] = instance {
      case h :: t =>
        H.value.encode(h).mkString("") match {
          case m if !m.isEmpty => {
            val mappedName = W.value.name.toCharArray.toList.flatMap { x =>
              if (x.isUpper && !x.isSpaceChar) '_' :: x.toLower :: Nil
              else x :: Nil
            }.mkString
            List(s"$mappedName = $m") ++ T.encode(t)
          }
          case _ => T.encode(t)
        }
    }

  }
  object ops {

    def maybeSet[A](value: A)(implicit A: SetEncoder[A]): List[Fragment] =
      maybeSetL(List(value))

    def maybeSetL[A](values: List[A])(
        implicit A: SetEncoder[A]): List[Fragment] =
      values.flatMap(x => A.encode(x)).map(x => Fragment.const(x))

  }
}
