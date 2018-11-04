package mobi.upod.util

import java.util.{TimerTask => JTimerTask}

object TimerTask {

  def apply(action: => Unit): JTimerTask = new JTimerTask {
    def run() {
      action
    }
  }

}
