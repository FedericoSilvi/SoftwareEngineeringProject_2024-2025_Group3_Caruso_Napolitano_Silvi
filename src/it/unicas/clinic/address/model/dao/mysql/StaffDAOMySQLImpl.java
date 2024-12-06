package it.unicas.clinic.address.model.dao.mysql;

import it.unicas.clinic.address.model.Staff;
import it.unicas.clinic.address.model.dao.StaffDAO;
import it.unicas.clinic.address.model.dao.StaffException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class StaffDAOMySQLImpl implements StaffDAO<Staff> {

    private static StaffDAO dao = null;
    private static Logger logger = null;

    private StaffDAOMySQLImpl(){}

    public static StaffDAO getInstance(){
        if (dao == null){
            dao = new StaffDAOMySQLImpl();
            logger = Logger.getLogger(StaffDAOMySQLImpl.class.getName());
        }
        return dao;
    }
    @Override
    public void update(Staff s) throws StaffException {
        verifyStaff(s);
        String sqlUpdate = "UPDATE staff SET name = ?, surname = ?, specialties = ? WHERE id = ?";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlUpdate)) {
            preparedStatement.setString(1, s.getName());
            preparedStatement.setString(2, s.getSurname());
            preparedStatement.setString(3, s.getSpecialties());
            preparedStatement.setInt(4, s.getId());

            int rowAffected=preparedStatement.executeUpdate();
            logger.info("Query executed successfully: " + sqlUpdate);
            if(rowAffected==0){
                logger.info("No staff found with id " + s.getId());
            }
        } catch (SQLException e) {
            logger.severe(("SQL: In update(): An error occurred while updating staff data"));
            throw new StaffException("SQL: In update(): An error occurred while updating staff data");
        }
    }

    @Override
    public void insert(Staff s) throws StaffException {
        // Verifica l'oggetto Staff
        verifyStaff(s);

        // Creiamo la query per l'inserimento dello Staff
        String sqlInsertStaff = "INSERT INTO staff (name, surname, specialties) VALUES(?, ?, ?)";

        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlInsertStaff, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, s.getName());
            preparedStatement.setString(2, s.getSurname());
            preparedStatement.setString(3, s.getSpecialties());

            preparedStatement.executeUpdate();

            logger.info("Query executed successfully: " + sqlInsertStaff);

        } catch (SQLException e) {
            logger.severe("SQL: In insert(): An error occurred while inserting staff data, connection problem");
            throw new StaffException("SQL: In insert(): An error occurred while inserting staff data, connection problem");
        }
    }

    /**
     * "Hard delete" of a staff member. This will delete the staff from the db.
     * Also delete all the appointments linked with him.
     * @param s
     * @throws StaffException
     */
    @Override
    public void delete(Staff s) throws StaffException {
        if(s == null || s.getId() <= 0){
            throw new StaffException("SQL: In delete(): Staff object cannot be null or with an invalid id ");
        }
        String sqlDelete = "DELETE FROM staff WHERE id = ? ";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlDelete)) {
            preparedStatement.setInt(1, s.getId());
            int rowAffected=preparedStatement.executeUpdate();
            logger.info("Query executed successfully: " + sqlDelete);
            if(rowAffected==0){
                logger.info("No staff found with id " + s.getId());
            }
        }catch(SQLException e){
            logger.severe(("SQL: In delete(): An error occurred while deleting staff data"));
            //logger.severe("SQL: In delete(): Error - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | ErrorCode: " + e.getErrorCode());

            throw new StaffException("SQL: In delete(): An error occurred while deleting staff data");
        }
    }

    /**
     * "Soft delete" of a staff member. Just set the firedData
     * @param s
     * @throws StaffException
     */
    @Override
    public void softDelete(Staff s) throws StaffException {
        if (s.getId() <= 0) {
            throw new StaffException("Invalid staff ID.");
        }
        String sqlUpdateFiredDate = "UPDATE staff SET firedDate = ? WHERE id = ?";
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(sqlUpdateFiredDate)) {

            // Set the firing data
            LocalDate today = LocalDate.now();
            preparedStatement.setDate(1, Date.valueOf(today));
            preparedStatement.setInt(2, s.getId());

            // to the update
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new StaffException("No staff found with ID " + s.getId());
            }

            logger.info("Successfully set firedDate for staff with ID: " + s.getId());
        } catch (SQLException e) {
            logger.severe("SQL: Error setting firedDate for staff with ID: " + s.getId() + ". " +
                    "Error: " + e.getMessage());
            throw new StaffException("SQL: Error setting firedDate for staff with ID: " + s.getId());
        }
    }

    /**
     * Select all the staff not fired yet
     * @param s
     * @return
     */
    @Override
    public List<Staff> select(Staff s) {
        // if the object is null, create a Staff with default values
        if (s == null) {
            s = new Staff(0, "", "", "", null); // select all
        }

        ArrayList<Staff> list = new ArrayList<>();

        // If all the fields are null or 0 => select all
        if (s.getId() <= 0 && s.getName() == null && s.getSurname() == null && s.getSpecialties() == null && s.getFiredDate() == null) {
            s = new Staff(0, null, null, null, null);
        }

        String sqlSelect = "SELECT * FROM staff WHERE firedDate IS NULL";

        // Add conditions dynamically based on the fields not null
        if (s.getId() > 0) {
            sqlSelect += " AND id = ?";
        }
        if (s.getName() != null && !s.getName().isEmpty()) {
            sqlSelect += " AND name LIKE ?";
        }
        if (s.getSurname() != null && !s.getSurname().isEmpty()) {
            sqlSelect += " AND surname LIKE ?";
        }
        if (s.getSpecialties() != null && !s.getSpecialties().isEmpty()) {
            sqlSelect += " AND specialties LIKE ?";
        }
        if (s.getFiredDate() != null) {
            sqlSelect += " AND firedDate = ?"; // Exact match for firedDate
        }

        // Log final query
        logger.info("SQL Query: " + sqlSelect);

        // Prepare PreparedStatement
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlSelect)) {

            // Set the parameters dynamically
            int index = 1;

            if (s.getId() > 0) {
                stmt.setInt(index++, s.getId());
            }
            if (s.getName() != null && !s.getName().isEmpty()) {
                stmt.setString(index++, "%" + s.getName() + "%");
            }
            if (s.getSurname() != null && !s.getSurname().isEmpty()) {
                stmt.setString(index++, "%" + s.getSurname() + "%");
            }
            if (s.getSpecialties() != null && !s.getSpecialties().isEmpty()) {
                stmt.setString(index++, "%" + s.getSpecialties() + "%");
            }
            if (s.getFiredDate() != null) {
                stmt.setDate(index++, Date.valueOf(s.getFiredDate())); // Set the firedDate
            }

            // Execute the query
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Create an object with the result
                    Staff s1 = new Staff(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("specialties"),
                            rs.getDate("firedDate") != null ? rs.getDate("firedDate").toLocalDate() : null
                    );
                    list.add(s1); // Add the object to the list
                }
            }

            if (!list.isEmpty()) {
                logger.info("Query executed successfully: " + sqlSelect + " | Number of records found: " + list.size());

            } else {
                logger.info("Query executed successfully: " + sqlSelect + " | No records found.");
                return null;
            }
        } catch (SQLException e) {
            logger.severe("SQL Error: " + e.getMessage());
            throw new StaffException("SQL: In select(): An error occurred while fetching staff data");
        }

        return list; // Return the list of results
    }


    private void verifyStaff(Staff s) throws StaffException {
        //we want all not null and with a "meaning"
            if (s == null || s.getName() == null || s.getSurname() == null
                    || s.getSpecialties() == null) {
                throw new StaffException("Can not to continue because that staff member has some null field");
            }
    }


    public static void main(String args[]) throws StaffException, SQLException{
        dao=StaffDAOMySQLImpl.getInstance();
        Staff newStaff = new Staff("John", "Doe", "Dermatology");

        // Crea una lista di orari di lavoro
        //List<Schedule> scheduleList = new ArrayList<>();
        //scheduleList.add(new Schedule(1, LocalDate.of(2024, 11, 24), LocalTime.of(9, 0), LocalTime.of(17, 0), 0));
        //scheduleList.add(new Schedule(2, LocalDate.of(2024, 11, 25), LocalTime.of(9, 0), LocalTime.of(17, 0), 0));

        //dao.insert(new Staff("Marco", "Caruso", "Nessuna"));
        //dao.insert(new Staff("Federico", "Silvi", "massaggi"));
        List<Staff> selectStuffMassage = dao.select(new Staff(1, null, null, null));
        //System.out.println(selectStuffMassage);
        //dao.softDelete(new Staff(1, "Marco", "Caruso", ""));

    }

    public Staff getLastStaff() throws SQLException {
        Staff s = new Staff(0, null, null, null);
        Connection connection = DAOMySQLSettings.getConnection();
        //Define command
        String searchUser = "select * from staff order by id desc limit 1";
        PreparedStatement command = connection.prepareStatement(searchUser);
        //Execute command
        ResultSet result = command.executeQuery();

        if(result.next()){
            s.setName(result.getString("name"));
            s.setSurname(result.getString("surname"));
            s.setSpecialties(result.getString("specialties"));
            s.setId(result.getInt("id"));
        }
        connection.close();
        return s;
    }
    @Override
    public  Staff select(int id) throws SQLException {
        if(id<=0){
            return null;
        }
        else{
            Connection connection = DAOMySQLSettings.getConnection();
            String searchStaff = "select * from staff where id = ?";
            PreparedStatement command = connection.prepareStatement(searchStaff);
            command.setInt(1, id);
            ResultSet result = command.executeQuery();
            if(result.next()){
                Staff s = new Staff();
                s.setId(result.getInt("id"));
                s.setName(result.getString("name"));
                s.setSurname(result.getString("surname"));
                s.setSpecialties(result.getString("specialties"));
                return s;
            }
            else
                return null;
        }
    }



    @Override
    public List<Staff> selectFiredBefore(LocalDate date) {
        String query = "SELECT * FROM staff WHERE firedDate IS NOT NULL AND firedDate <= ?";
        List<Staff> staffList = new ArrayList<>();
        try (Connection con = DAOMySQLSettings.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Staff staff = new Staff(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("specialties"),
                            rs.getDate("firedDate").toLocalDate()
                    );
                    staffList.add(staff);
                }
            }
        } catch (SQLException e) {
            logger.severe("Error selecting old staff: " + e.getMessage());
            throw new RuntimeException("Error selecting old staff", e);
        }
        return staffList;
    }

}
