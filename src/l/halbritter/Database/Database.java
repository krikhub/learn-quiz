package l.halbritter.Database;

import l.halbritter.Model.User;
import l.halbritter.Model.Question;

import java.sql.*;
import java.util.*;

public class Database {
    private static final String URL = "jdbc:sqlite:quiz.db";
    private static Connection connection;

    public static void connect() {
        if (connection != null) return;
        try {
            connection = DriverManager.getConnection(URL);
            System.out.println("Verbindung zur SQLite-Datenbank hergestellt.");

            try (Statement pragma = connection.createStatement()) {
                // Warte bis zu 5 Sekunden, falls die DB blockiert ist
                pragma.execute("PRAGMA busy_timeout = 5000");
                // Write-Ahead Logging für bessere Concurrency
                pragma.execute("PRAGMA journal_mode = WAL");
            }

            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Fehler beim Verbinden zur Datenbank: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE
            );
        """;
        String createQuestions = """
            CREATE TABLE IF NOT EXISTS questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT NOT NULL,
                answer TEXT NOT NULL,
                topic TEXT,
                correct INTEGER NOT NULL DEFAULT 0
            );
        """;
        String createTopics = """
            CREATE TABLE IF NOT EXISTS topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            );
        """;
        String createWrong = """
            CREATE TABLE IF NOT EXISTS user_wrong_answers (
                user_id INTEGER NOT NULL,
                question_id INTEGER NOT NULL,
                wrong_count INTEGER DEFAULT 0,
                PRIMARY KEY(user_id, question_id),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY(question_id) REFERENCES questions(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createQuestions);
            stmt.execute(createTopics);
            stmt.execute(createWrong);
            // Falls bei bestehender DB die Spalte fehlt, hinzufügen
            try {
                stmt.execute("ALTER TABLE questions ADD COLUMN correct INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignore) {
                // Spalte existiert bereits
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
        }
    }

    public static synchronized void addOrUpdateUser(User user) {
        String insertUser = "INSERT INTO users (username) VALUES (?) ON CONFLICT(username) DO NOTHING";
        String upsertWrong = """
            INSERT INTO user_wrong_answers (user_id, question_id, wrong_count)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, question_id) DO UPDATE SET wrong_count = excluded.wrong_count
        """;

        try (PreparedStatement psUser = connection.prepareStatement(insertUser)) {
            psUser.setString(1, user.getUsername());
            psUser.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen/Aktualisieren des Nutzers: " + e.getMessage());
            return;
        }

        try {
            int userId = getUserId(user.getUsername());
            if (userId == -1) return;

            for (Map.Entry<Integer, Integer> entry : user.getWrongQuestionCounts().entrySet()) {
                try (PreparedStatement psWrong = connection.prepareStatement(upsertWrong)) {
                    psWrong.setInt(1, userId);
                    psWrong.setInt(2, entry.getKey());
                    psWrong.setInt(3, entry.getValue());
                    psWrong.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern der falschen Antworten: " + e.getMessage());
        }
    }

    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String sqlUsers = "SELECT id, username FROM users";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlUsers)) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                User user = new User(username);

                String sqlWrong = "SELECT question_id, wrong_count FROM user_wrong_answers WHERE user_id = ?";
                try (PreparedStatement psWrong = connection.prepareStatement(sqlWrong)) {
                    psWrong.setInt(1, userId);
                    try (ResultSet wrs = psWrong.executeQuery()) {
                        while (wrs.next()) {
                            user.getWrongQuestionCounts().put(wrs.getInt("question_id"), wrs.getInt("wrong_count"));
                        }
                    }
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
        String sql = "SELECT id, question, answer, topic, correct FROM questions";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("answer"),             // CSV-String mit allen Antworten
                        rs.getString("topic"),
                        rs.getInt("correct")                // Index der richtigen Antwort
                );
                questions.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Fragen: " + e.getMessage());
        }
        return questions;
    }

    public static synchronized void addOrUpdateQuestion(Question question) {
        String sql = """
            INSERT INTO questions (id, question, answer, topic, correct)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE
              SET question = excluded.question,
                  answer   = excluded.answer,
                  topic    = excluded.topic,
                  correct  = excluded.correct
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, question.getQuestionId());
            ps.setString(2, question.getQuestionText());
            ps.setString(3, question.getAnswerAsCSV());
            ps.setString(4, question.getTopic());
            ps.setInt(5, question.getCorrectAnswer());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen/Aktualisieren der Frage: " + e.getMessage());
        }
    }

    public static synchronized void deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der Frage: " + e.getMessage());
        }
    }

    public static List<String> loadTopics() {
        List<String> topics = new ArrayList<>();
        String sql = "SELECT name FROM topics ORDER BY name ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                topics.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Themen: " + e.getMessage());
        }
        return topics;
    }

    public static synchronized void addTopic(String topic) {
        String sql = "INSERT INTO topics (name) VALUES (?) ON CONFLICT(name) DO NOTHING";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, topic);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen des Themas: " + e.getMessage());
        }
    }

    public static synchronized void deleteTopic(String topic) {
        String sql = "DELETE FROM topics WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, topic);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen des Themas: " + e.getMessage());
        }
    }

    public static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Datenbankverbindung geschlossen.");
            } catch (SQLException e) {
                System.err.println("Fehler beim Schließen der Verbindung: " + e.getMessage());
            }
            connection = null;
        }
    }

    private static int getUserId(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }
}
