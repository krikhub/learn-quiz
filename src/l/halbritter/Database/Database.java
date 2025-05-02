package l.halbritter.Database;

import l.halbritter.Model.User;
import l.halbritter.Model.Question;

import java.sql.*;
import java.util.*;

public class Database {
    private static final String URL = "jdbc:sqlite:quiz.db";
    private static Connection connection;

    public static void connect() {
        try {
            connection = DriverManager.getConnection(URL);
            System.out.println("Verbindung zur SQLite-Datenbank hergestellt.");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Fehler beim Verbinden zur Datenbank: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    topic TEXT
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS topics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_wrong_answers (
                    user_id INTEGER NOT NULL,
                    question_id INTEGER NOT NULL,
                    wrong_count INTEGER DEFAULT 0,
                    PRIMARY KEY(user_id, question_id),
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(question_id) REFERENCES questions(id) ON DELETE CASCADE
                );
            """);
        } catch (SQLException e) {
            System.err.println("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
        }
    }

    public static void addOrUpdateUser(User user) {
        try {
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (username) VALUES (?) ON CONFLICT(username) DO NOTHING");
            insert.setString(1, user.getUsername());
            insert.executeUpdate();

            int userId = getUserId(user.getUsername());
            if (userId == -1) return;

            for (Map.Entry<Integer, Integer> entry : user.getWrongQuestionCounts().entrySet()) {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO user_wrong_answers (user_id, question_id, wrong_count) " +
                                "VALUES (?, ?, ?) ON CONFLICT(user_id, question_id) DO UPDATE SET wrong_count = excluded.wrong_count"
                );
                stmt.setInt(1, userId);
                stmt.setInt(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen/Aktualisieren des Nutzers: " + e.getMessage());
        }
    }

    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                User user = new User(username);

                PreparedStatement wrongs = connection.prepareStatement(
                        "SELECT question_id, wrong_count FROM user_wrong_answers WHERE user_id = ?");
                wrongs.setInt(1, userId);
                ResultSet wrs = wrongs.executeQuery();

                while (wrs.next()) {
                    int questionId = wrs.getInt("question_id");
                    int count = wrs.getInt("wrong_count");
                    user.getWrongQuestionCounts().put(questionId, count);
                }

                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Nutzer: " + e.getMessage());
        }
        return users;
    }

    public static List<Question> loadQuestions() {
        List<Question> questions = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM questions");

            while (rs.next()) {
                int id = rs.getInt("id");
                String questionText = rs.getString("question");
                String answer = rs.getString("answer");
                String topic = rs.getString("topic");

                Question question = new Question(id, questionText, answer, topic);
                questions.add(question);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Fragen: " + e.getMessage());
        }
        return questions;
    }

    public static void addOrUpdateQuestion(Question question) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO questions (id, question, answer, topic) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET question = excluded.question, answer = excluded.answer, topic = excluded.topic");
            stmt.setInt(1, question.getQuestionId());
            stmt.setString(2, question.getQuestionText());
            stmt.setString(3, question.getAnswerAsCSV());
            stmt.setString(4, question.getTopic());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen/Aktualisieren der Frage: " + e.getMessage());
        }
    }

    public static void deleteQuestion(int questionId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM questions WHERE id = ?");
            stmt.setInt(1, questionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der Frage: " + e.getMessage());
        }
    }

    public static List<String> loadTopics() {
        List<String> topics = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM topics ORDER BY name ASC");
            while (rs.next()) {
                topics.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Themen: " + e.getMessage());
        }
        return topics;
    }

    public static void addTopic(String topic) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO topics (name) VALUES (?) ON CONFLICT(name) DO NOTHING");
            stmt.setString(1, topic);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen des Themas: " + e.getMessage());
        }
    }

    public static void deleteTopic(String topic) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM topics WHERE name = ?");
            stmt.setString(1, topic);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen des Themas: " + e.getMessage());
        }
    }

    public static void disconnect() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Datenbankverbindung geschlossen.");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Schließen der Verbindung: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    private static int getUserId(String username) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT id FROM users WHERE username = ?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }
}
