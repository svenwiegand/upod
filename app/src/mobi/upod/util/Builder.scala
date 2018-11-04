package mobi.upod.util

trait Builder[A] {

  def build: A
}

class BuilderObject[A] {

  implicit def builderToBuildee(builder: Builder[A]): A =
    builder.build
}
