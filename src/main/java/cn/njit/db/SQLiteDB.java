package cn.njit.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteDB {
    private Connection connection;
    private static final String DB_PATH = "data.db";

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            System.out.println("数据库连接成功");
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 创建表
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS files (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "filename TEXT," +
                    "upload_time TEXT," +
                    "description TEXT)";
            statement.executeUpdate(sql);
            System.out.println("表创建成功");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 插入文件记录
    public long insertFile(String filename, String description) {
        long id = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO files (filename, upload_time, description) VALUES (?, datetime('now'), ?)")) {
            pstmt.setString(1, filename);
            pstmt.setString(2, description);
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                id = generatedKeys.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }



    // 查询所有文件记录
    public List<FileRecord> queryAllFiles() {
        List<FileRecord> records = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM files")) {
            while (rs.next()) {
                FileRecord record = new FileRecord();
                record.id = rs.getLong("id");
                record.filename = rs.getString("filename");
                record.uploadTime = rs.getString("upload_time");
                record.description = rs.getString("description");
                records.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    // 文件记录类
    public static class FileRecord {
        public long id;
        public String filename;
        public String uploadTime;
        public String description;

        @Override
        public String toString() {
            return "ID: " + id + ", 文件名: " + filename + ", 时间: " + uploadTime;
        }
    }

    public List<Map<String, Object>> getAllFileRecords() throws SQLException {
        List<Map<String, Object>> records = new ArrayList<>();
        String sql = "SELECT id, filename, description, upload_time FROM files";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("filename", rs.getString("filename"));
                record.put("description", rs.getString("description"));
                record.put("upload_time", rs.getString("upload_time"));
                records.add(record);
            }
        }
        return records;
    }



    // 测试方法
    public static void main(String[] args) {
        SQLiteDB db = new SQLiteDB();
        db.connect();

        // 插入测试数据
        long id = db.insertFile("test.txt", "测试文件");
        System.out.println("插入记录ID: " + id);

        // 查询数据
        List<FileRecord> records = db.queryAllFiles();
        for (FileRecord record : records) {
            System.out.println(record);
        }

        db.disconnect();
    }
}
