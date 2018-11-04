package mobi.upod.media.mp4

import java.io.RandomAccessFile

import com.coremedia.iso.boxes.SampleTableBox

import scala.collection.JavaConverters._

trait SampleTime {
  val startMillis: Long
  val endMillis: Long
}

case class RawSample(startMillis: Long, endMillis: Long, bytes: Array[Byte]) extends SampleTime

case class StringSample(startMillis: Long, endMillis: Long, str: String) extends SampleTime

case class LinkSample(startMillis: Long, endMillis: Long, ankerText: String, href: String) extends SampleTime

trait SampleReader[A <: SampleTime] {
  def readSample(stream: SampleStream, length: Long, startMillis: Long, endMillis: Long): A
}

object RawSampleReader extends SampleReader[RawSample] {

  override def readSample(stream: SampleStream, length: Long, startMillis: Long, endMillis: Long): RawSample =
    RawSample(startMillis, endMillis, stream.readBytes(length.toInt))
}

object StringSampleReader extends SampleReader[StringSample] {

  override def readSample(stream: SampleStream, length: Long, startMillis: Long, endMillis: Long): StringSample = {
    val len = stream.readUnsignedShort()
    StringSample(startMillis, endMillis, stream.readString(len))
  }
}

object LinkSampleReader extends SampleReader[LinkSample] {

  override def readSample(stream: SampleStream, length: Long, startMillis: Long, endMillis: Long): LinkSample = {

    def skipSampleType(): Unit = {
      stream.skipBytes(4)
      val sampleType = stream.readString(4)
      require(sampleType == "href", s"expected sample type 'href' but got '$sampleType'")
      stream.skipBytes(4)
    }

    def readHref(): String = {
      val strLen = stream.readUnsignedByte()
      stream.readString(strLen)
    }

    val ankerTextLen = stream.readUnsignedShort()
    val ankerText = stream.readString(ankerTextLen)
    val readBytes = 2 + ankerTextLen
    val href = if (readBytes < length) {
      skipSampleType()
      readHref()
    } else ""
    LinkSample(startMillis, endMillis, ankerText, href)
  }
}

case class SampleInfo(startMillis: Long, endMillis: Long, fileOffset: Long, length: Long) extends SampleTime {

  def read[A <: SampleTime](file: RandomAccessFile, reader: SampleReader[A]): A = {
    file.seek(fileOffset)
    reader.readSample(new SampleStream(file), length, startMillis, endMillis)
  }
}

private[mp4] object SampleInfo {

  def listFrom(timeScale: Long, sampleTable: SampleTableBox): IndexedSeq[SampleInfo] = {
    val chunksBySampleIndex = {
      val chunks = Chunk.listFrom(sampleTable)
      val maxSampleIndex = chunks.last.lastSample
      for (sample <- 0l to maxSampleIndex)
        yield chunks.find(chunk => sample >= chunk.firstSample && sample <= chunk.lastSample).get
    }

    val sampleTimes = {
      val timeToSampleEntries = sampleTable.getTimeToSampleBox.getEntries.asScala.toSeq
      val sampleDeltas = timeToSampleEntries.
        flatMap(e => Seq.fill(e.getCount.toInt)(e.getDelta)).
        map(_ / (timeScale / 1000.0) toLong)
      deltaMillisToMediaRange(sampleDeltas)
    }

    val sampleSizes = {
      val sampleSizeBox = sampleTable.getSampleSizeBox
      if (sampleSizeBox.getSampleSize > 0)
        Array.fill(sampleSizeBox.getSampleCount.toInt)(sampleSizeBox.getSampleSize)
      else
        sampleSizeBox.getSampleSizes
    }
    if (sampleSizes.size == chunksBySampleIndex.size && sampleSizes.size == sampleTimes.size) {
      val sampleAndChunkInfo = chunksBySampleIndex.zip(sampleSizes).zip(sampleTimes).map(x => SampleAndChunkInfo(x._2._1, x._2._2, x._1._1, x._1._2))
      val samplesGroupedByChunk = sampleAndChunkInfo.groupBy(_.chunk.firstSample).values
      val samples = samplesGroupedByChunk.flatMap(sampleChunkAndSizeToSample)
      samples.toIndexedSeq.sortBy(_.startMillis)
    } else {
      IndexedSeq()
    }
  }

  private def deltaMillisToMediaRange(deltas: Seq[Long]): Seq[(Long, Long)] = {

    def sumUp(remaining: List[Long], headSum: Long): List[(Long, Long)] = remaining match {
      case x :: tail => headSum -> (x + headSum) :: sumUp(tail, x + headSum)
      case Nil => Nil
    }

    sumUp(deltas.toList, 0)
  }

  private def sampleChunkAndSizeToSample(sampleChunkAndSize: Seq[SampleAndChunkInfo]): Seq[SampleInfo] = {

    def withAbsolutePosition(remaining: List[SampleAndChunkInfo], offsetInsideChunk: Long): List[SampleInfo] = remaining match {
      case x :: tail =>
        SampleInfo(x.startMillis, x.endMillis, x.chunk.offset + offsetInsideChunk, x.byteLength) ::
          withAbsolutePosition(tail, offsetInsideChunk + x.byteLength)
      case Nil => Nil
    }

    withAbsolutePosition(sampleChunkAndSize.toList, 0)
  }

  private case class SampleAndChunkInfo(startMillis: Long, endMillis: Long, chunk: Chunk, byteLength: Long)
}
