package mobi.upod.media.mp4

import java.io.RandomAccessFile

class SampleTable[A <: SampleTime](val samples: IndexedSeq[A]) {

  def sampleAt(millis: Long): Option[A] =
    samples.find(s => s.startMillis <= millis && s.endMillis > millis)

  def sampleStartingAt(millis: Long): Option[A] =
    samples.find(s => s.startMillis == millis)

  def sampleStartingAt(millis: Long, toleranceMillis: Long): Option[A] =
    samples.find(s => s.startMillis - toleranceMillis <= millis && s.startMillis + toleranceMillis >= millis)
}

object SampleTable {

  def apply[A <: SampleTime](samples: IndexedSeq[A]): SampleTable[A] =
    new SampleTable(samples)

  def read[A <: SampleTime](src: RandomAccessFile, track: Track, sampleReader: SampleReader[A]): SampleTable[A] = {
    val samples = track.sampleInfoTable.samples.map(_.read(src, sampleReader))
    new SampleTable(samples)
  }
}