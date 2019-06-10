package app.util

trait AdtEmptyChecker[A] {
  def check(v: A): Boolean
}
object AdtEmptyChecker {
  private def apply[A: AdtEmptyChecker]: AdtEmptyChecker[A] = implicitly

  private def instance[A](f: A => Boolean): AdtEmptyChecker[A] = f(_)

  object instances {
    import shapeless.{HList, ::, HNil, Lazy, Generic}

    implicit def genEncoder[A, R](
        implicit
        G: Generic.Aux[A, R],
        E: Lazy[AdtEmptyChecker[R]]
    ): AdtEmptyChecker[A] = instance(x => E.value.check(G.to(x)))

    implicit def optInstance[A]: AdtEmptyChecker[Option[A]] = instance {
      case Some(_) => true
      case None    => false
    }

    implicit val hNilInstance: AdtEmptyChecker[HNil] =
      instance(_ => false)

    implicit def hListInstance[H, T <: HList](
        implicit
        H: Lazy[AdtEmptyChecker[H]],
        T: AdtEmptyChecker[T]
    ): AdtEmptyChecker[H :: T] = instance {
      case h :: t => H.value.check(h) || T.check(t)
    }
  }
  object ops {
    def isEmpty[A](a: A)(implicit A: AdtEmptyChecker[A]): Boolean = !A.check(a)
  }
}
