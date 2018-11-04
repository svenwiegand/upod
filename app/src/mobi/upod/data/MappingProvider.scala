package mobi.upod.data

trait MappingProvider[A] {

  val mapping: Mapping[A]
}

object MappingProvider {
  implicit def mappingProviderToMapping[A](provider: MappingProvider[A]): Mapping[A] = provider.mapping
}
