package dao


object CalculatorRepository {
  import scalikejdbc._

  def initDataBase: Unit = {
    Class.forName("org.postgresql.Driver")
    val poolSettings = ConnectionPoolSettings(initialSize = 1, maxSize = 10)
    ConnectionPool.singleton(
      "jdbc:postgresql://localhost:5432/demo",
      "docker",
      "docker",
      poolSettings
    )
  }

  def getLatestOffsetAndResult: (Int, Double) = {
    val entities =
      DB readOnly { session =>
        session.list("select * from public.result where id = 1;") { row =>
          (row.int("write_side_offset"), row.double("calculated_value"))
        }
      }
    entities.head
  }

  def updateResultAndOfsset(calculated: Double, offset: Long): Unit = {
    using(DB(ConnectionPool.borrow())) { db =>
      db.autoClose(true)
      db.localTx {
        _.update(
          "update public.result set calculated_value = ?, write_side_offset = ? where id = ?",
          calculated,
          offset,
          1
        )
      }
    }
  }
}