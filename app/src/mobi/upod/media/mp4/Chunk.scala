package mobi.upod.media.mp4

import com.coremedia.iso.boxes.SampleTableBox

import scala.collection.JavaConverters._

private[mp4] case class Chunk(firstSample: Long, lastSample: Long, offset: Long)

private[mp4] object Chunk {

  def listFrom(sampleTable: SampleTableBox): IndexedSeq[Chunk] = {
    val chunkOffsets = sampleTable.getChunkOffsetBox.getChunkOffsets
    val maxChunkIndex = chunkOffsets.size - 1
    val chunkRangeToSampleCount = {
      val sampleToChunkBox = sampleTable.getSampleToChunkBox.getEntries.asScala
      val firstChunkToSampleCount = sampleToChunkBox.map(e => e.getFirstChunk - 1 -> e.getSamplesPerChunk).toMap

      val firstChunkIndices = firstChunkToSampleCount.keys.toSeq.sorted
      val chunkIndexRangeBorders = firstChunkIndices :+ maxChunkIndex + 1l
      val chunkIndexRanges = chunkIndexRangeBorders.sliding(2)
      chunkIndexRanges.map(a => ChunkRangeToSampleCount(a.head, a.last - 1, firstChunkToSampleCount(a.head))).toIndexedSeq
    }

    val sampleCountAndOffset = for (chunkIndex <- 0l to maxChunkIndex)
      yield chunkRangeToSampleCount.find(range => chunkIndex >= range.firstChunkIndex && chunkIndex <= range.lastChunkIndex).get.sampleCount -> chunkOffsets(chunkIndex.toInt)

    def sampleCountAndOffsetToChunk(remaining: List[(Long, Long)], sampleIndex: Long): List[Chunk] = remaining match {
      case x :: tail => Chunk(sampleIndex, sampleIndex + x._1 - 1, x._2) :: sampleCountAndOffsetToChunk(tail, sampleIndex + x._1)
      case Nil => Nil
    }

    sampleCountAndOffsetToChunk(sampleCountAndOffset.toList, 0).toIndexedSeq
  }

  private case class FirstChunkToSampleCount(firstChunkIndex: Long, sampleCount: Long)
  private case class ChunkRangeToSampleCount(firstChunkIndex: Long, lastChunkIndex: Long, sampleCount: Long)
}
