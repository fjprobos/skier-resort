import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import entity.LiftRide;
import entity.Year;


public class ResortDao {
  private static BasicDataSource dataSource;

  public ResortDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  public void createNewSeason(Year newYear) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO Resort (ResortId, Year) " +
            "VALUES (?,?)";

    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(insertQueryStatement);
      preparedStatement.setInt(1, newYear.getResortId());
      preparedStatement.setInt(2, newYear.getYear());

      // execute insert SQL statement
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }
}