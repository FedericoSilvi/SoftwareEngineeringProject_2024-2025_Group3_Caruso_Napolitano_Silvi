package it.unicas.clinic.address.model.dao.mysql;

import it.unicas.clinic.address.model.Schedule;
import it.unicas.clinic.address.model.Staff;
import it.unicas.clinic.address.model.dao.ScheduleDAO;
import it.unicas.clinic.address.model.dao.ScheduleException;
import it.unicas.clinic.address.model.dao.StaffDAO;
import it.unicas.clinic.address.model.dao.StaffException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ScheduleDAOMySQLImpl implements ScheduleDAO<Schedule> {
    private static ScheduleDAO dao = null;
    private static Logger logger = null;

    private ScheduleDAOMySQLImpl(){}

    public static ScheduleDAO getInstance(){
        if (dao == null){
            dao = new ScheduleDAOMySQLImpl();
            logger = Logger.getLogger(ScheduleDAOMySQLImpl.class.getName());
        }
        return dao;
    }

    @Override
    public List<Schedule> select(Schedule s) {
        // if the object is null, create a Schedule with default value (0, null, null, null, 0)
        if (s == null) {
            s = new Schedule(0, null, null, null, 0); //select all
        }

        ArrayList<Schedule> list = new ArrayList<>();

        // If all the fields are null o 0 => select all
        if (s.getId() <= 0 && s.getDay() == null && s.getStartTime() == null && s.getStopTime() == null && s.getStaffId() <= 0) {
            s = new Schedule(0, null, null, null, 0);
        }


        String sqlSelect = "SELECT * FROM schedule WHERE 1=1";

        // Add dynamicly the condition of the fields not null
        if (s.getId() > 0) {
            sqlSelect += " AND id = ?";
        }
        if (s.getDay() != null) {
            sqlSelect += " AND day = ?";
        }
        if (s.getStartTime() != null) {
            sqlSelect += " AND start_time >= ?";
        }
        if (s.getStopTime() != null) {
            sqlSelect += " AND stop_time <= ?";
        }
        if (s.getStaffId() > 0) {
            sqlSelect += " AND staff_id = ?";
        }

        // Log final query
        logger.info("SQL Query: " + sqlSelect);

        // Prepare PreparedStatement
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlSelect)) {

            // Set the params dinamicly
            int index = 1; // index of the parms of the preparedStatement

            if (s.getId() > 0) {
                stmt.setInt(index++, s.getId());  //set ID
            }
            if (s.getDay() != null) {
                stmt.setDate(index++, Date.valueOf(s.getDay()));  // set the day
            }
            if (s.getStartTime() != null) {
                stmt.setTime(index++, Time.valueOf(s.getStartTime()));  // set start time
            }
            if (s.getStopTime() != null) {
                stmt.setTime(index++, Time.valueOf(s.getStopTime()));  // set final time
            }
            if (s.getStaffId() > 0) {
                stmt.setInt(index++, s.getStaffId());  // set staffId
            }

            // execute the query
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // create an object with the result
                    Schedule s1 = new Schedule(
                            rs.getInt("id"),
                            rs.getDate("day").toLocalDate(),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("stop_time").toLocalTime(),
                            rs.getInt("staff_id")
                    );
                    list.add(s1);  // add the object in the list
                }
            }

            if (!list.isEmpty()) {
                logger.info("Query executed successfully: " + sqlSelect + " | Number of records found: " + list.size());
            } else {
                logger.info("Query executed successfully: " + sqlSelect + " | No records found.");
            }
        } catch (SQLException e) {
            logger.severe("SQL Error: " + e.getMessage());
            throw new ScheduleException("SQL: In select(): An error occurred while fetching schedule data");
        }

        return list;
    }


    @Override
    public void update(Schedule s) throws StaffException {
        if (s == null || !(staffExists(s.getStaffId())) || s.getDay() == null) {
            throw new StaffException("SQL: In update(): Schedule object cannot be null, invalid staffId or day.");
        }
        String sqlUpdateSchedule = "UPDATE schedule SET day = ?, start_time = ?, stop_time = ? WHERE staff_id = ?";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlUpdateSchedule)) {
            preparedStatement.setDate(1, Date.valueOf(s.getDay()));
            preparedStatement.setTime(2, Time.valueOf(s.getStartTime()));
            preparedStatement.setTime(3, Time.valueOf(s.getStopTime()));
            preparedStatement.setInt(4, s.getStaffId());

            int rowAffected=preparedStatement.executeUpdate();
            logger.info("Query executed successfully: " + sqlUpdateSchedule);
            if(rowAffected==0){
                logger.info("No staff found with id " + s.getId());
            }
        } catch (SQLException e) {
            logger.severe(("SQL: In update(): An error occurred while updating scheduling data"));
            throw new StaffException("SQL: In update(): An error occurred while updating scheduling data");
        }
    }

    @Override
    public void insert(Schedule s) throws StaffException {
        if(s==null)
            throw new ScheduleException("Null Schedule");
        //check on the values
        if (s.getDay() == null || s.getStartTime() == null || s.getStopTime() == null) {
            throw new ScheduleException("Some fields are null!!");
        }
        // check if the starttime and finalTime are coherent
        if (s.getStartTime().isAfter(s.getStopTime())) {
            throw new ScheduleException("The starting hour must me before than the ending hour!");
        }
        if(!staffExists(s.getStaffId()))
            throw new StaffException("Staff member does not exist!");

        String sqlInsertSchedule = "INSERT INTO schedule (day, start_time, stop_time, staff_id) VALUES (?, ?, ?, ?)";


        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement scheduleStmt = con.prepareStatement(sqlInsertSchedule)) {


            scheduleStmt.setDate(1, Date.valueOf(s.getDay()));
            scheduleStmt.setTime(2, Time.valueOf(s.getStartTime()));
            scheduleStmt.setTime(3, Time.valueOf(s.getStopTime()));
            scheduleStmt.setInt(4, s.getStaffId());


            int rowsAffected = scheduleStmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Query executed successfully: " + sqlInsertSchedule);
            } else {
                logger.warning("No rows affected in the insert statement.");
            }

        } catch (SQLException e) {
            logger.severe("SQL: An error occurred while inserting schedule data: " + e.getMessage());
            throw new StaffException("SQL: An error occurred while inserting schedule data.");
        }


    }

    @Override
    public void delete(Schedule s) throws StaffException {
        if (s == null || !(staffExists(s.getStaffId())) || s.getDay() == null) {
            throw new StaffException("SQL: In delete(): Schedule object cannot be null, invalid staffId or day.");
        }
        String sqlDeleteSchedule = "DELETE FROM schedule WHERE staff_id = ? AND day = ? AND start_time = ? AND stop_time = ?";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlDeleteSchedule)) {

            preparedStatement.setInt(1, s.getStaffId());
            preparedStatement.setDate(2, Date.valueOf(s.getDay()));
            preparedStatement.setTime(3, Time.valueOf(s.getStartTime()));
            preparedStatement.setTime(4, Time.valueOf(s.getStopTime()));

            int rowsAffected = preparedStatement.executeUpdate();
            logger.info("Query executed successfully: " + sqlDeleteSchedule);
            if (rowsAffected > 0) {
                logger.info("Schedule deleted successfully" );
            } else {
                logger.info("No schedule found for the given staff and day.");
            }

        } catch (SQLException e) {
            logger.severe("SQL: An error occurred while deleting schedule data: " + e.getMessage());
            throw new StaffException("SQL: An error occurred while deleting schedule data.");
        }
    }

    @Override
    public  boolean isAvailable(LocalDate day, LocalTime time, int staff_id){
        if(day==null || time==null || staff_id==0){
            throw new IllegalArgumentException("Day, time or staff id are not valid!");
        }
        List<Schedule> schedulesOfDay = select(new Schedule(0,day,null,null,staff_id));
        if(schedulesOfDay.isEmpty())
            return false;
        for(Schedule s: schedulesOfDay){
            if(time.isAfter(s.getStopTime()) || time.isBefore(s.getStartTime())){
                return false;
            }
        }
        return true;
    }
    //method to check if a staffId exists (needs for CRUD on Schedule). Return false if not exists.
    private boolean staffExists(int staffId) throws ScheduleException {
        String sqlCheckStaff = "SELECT COUNT(*) FROM staff WHERE id = ?";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlCheckStaff)) {
            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error during the checking of staff id");
            throw new ScheduleException("Error during the checking of staff id");
        }
        return false;
    }



    public static void main(String args[]) throws StaffException, SQLException{
        dao=ScheduleDAOMySQLImpl.getInstance();

        //dao.insert(new Schedule(1, LocalDate.of(2024, 11, 24), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));
        //dao.insert(new Schedule(2, LocalDate.of(2024, 11, 25), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));

        // Inserisci il nuovo staff con i suoi orari di lavoro
        //dao.insert(newStaff, scheduleList);
        //dao.delete(new Schedule(2, LocalDate.of(2024, 11, 25), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));
        //dao.update(new Schedule(1, LocalDate.of(2024, 12, 24), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));
        //dao.insert(new Schedule(4, LocalDate.of(2024, 11, 24), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));
        //dao.insert(new Schedule(5, LocalDate.of(2024, 11, 25), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));
        //dao.insert(new Schedule(6, LocalDate.of(2023, 11, 25), LocalTime.of(9, 0), LocalTime.of(17, 0), 11));

        // Cerca orari per il 2023-11-25 gennaio 2023 e staff con ID 101
        Schedule scheduleFilter = new Schedule(LocalDate.of(2023,1,1), 11);
        System.out.println(scheduleFilter);
        //List<Schedule> schedulesall = dao.select(new Schedule(0, null, null, null,0));
        //System.out.println(schedulesall);
    }
}
