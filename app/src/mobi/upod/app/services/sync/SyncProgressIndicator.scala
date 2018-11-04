package mobi.upod.app.services.sync

trait SyncProgressIndicator {
  private var progress = 0
  private var maxProgress = 0
  private var taskDescription = ""

  def updateProgress(progress: Int, max: Int, taskDescription: String): Unit = {
    this.progress = progress
    this.maxProgress = max
    this.taskDescription = taskDescription
  }

  def addMaxProgress(addedMax: Int): Unit =
    updateProgress(progress, maxProgress + addedMax, taskDescription)

  def initProgress(max: Int, taskDescription: String): Unit =
    updateProgress(0, max, taskDescription)

  def increaseProgress(): Unit =
    updateProgress(progress + 1, maxProgress, taskDescription)

  def increaseProgress(taskDescription: String): Unit =
    updateProgress(progress + 1, maxProgress, taskDescription)

  def updateProgress(progress: Int): Unit =
    updateProgress(progress, maxProgress, taskDescription)

  def updateProgress(progress: Int, taskDescription: String): Unit =
    updateProgress(progress, maxProgress, taskDescription)

  def updateProgress(progress: Int, max: Int): Unit =
    updateProgress(progress, max, taskDescription)

  def updateProgress(taskDescription: String): Unit =
    updateProgress(progress, maxProgress, taskDescription)

  def updateProgressToMax(): Unit =
    updateProgress(maxProgress)
}
