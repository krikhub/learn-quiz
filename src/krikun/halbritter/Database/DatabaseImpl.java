/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.Database;

import krikun.halbritter.Model.Question;
import krikun.halbritter.Model.User;

import java.sql.*;
import java.util.*;

public class DatabaseImpl implements DatabaseService {

    private static final String URL = "jdbc:sqlite:quiz.db";
    private Connection connection;

    @Override
    public void connect() {
        if (connection != null) return;
        try {
            connection = DriverManager.getConnection(URL);
            System.out.println("Verbindung zur SQLite-Datenbank hergestellt.");

            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA busy_timeout = 5000");
                pragma.execute("PRAGMA journal_mode = WAL");
            }

            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Fehler beim Verbinden zur Datenbank: " + e.getMessage());
        }
    }

    private void initializeDatabase() {
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE
            );
        """;
        String createQuestions = """
            CREATE TABLE IF NOT EXISTS questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question   TEXT NOT NULL,
                answer     TEXT NOT NULL,
                topic      TEXT,
                correctAnswer    INTEGER NOT NULL DEFAULT 0,
                difficulty INTEGER NOT NULL DEFAULT 1
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
        } catch (SQLException e) {
            System.err.println("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
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

    @Override
    public void addOrUpdateUser(User user) {
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
            for (var entry : user.getWrongQuestionCounts().entrySet()) {
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

    @Override
    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String sqlUsers = "SELECT id, username FROM users";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlUsers)) {
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

    @Override
    public List<Question> loadQuestions() {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT id, question, answer, topic, correctAnswer, difficulty FROM questions";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getString("topic"),
                        rs.getInt("correctAnswer"),
                        rs.getInt("difficulty")
                );
                questions.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Fragen: " + e.getMessage());
        }
        return questions;
    }
    
    @Override
    public void addOrUpdateQuestion(Question question) {
        // 1. NEUE Frage? (questionId ≤ 0)
        if (question.getQuestionId() <= 0) {
            String sqlInsert = """
            INSERT INTO questions 
               (question, answer, topic, correctAnswer, difficulty)
            VALUES (?, ?, ?, ?, ?)
        """;
            try (PreparedStatement ps = connection.prepareStatement(
                    sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, question.getQuestionText());
                ps.setString(2, question.getAnswerAsCSV());
                ps.setString(3, question.getTopic());
                ps.setInt(4, question.getCorrectAnswer());
                ps.setInt(5, question.getDifficulty());
                ps.executeUpdate();

                // Generierte ID auslesen und im Objekt speichern
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        question.setQuestionId(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Fehler beim Einfügen der Frage: " + e.getMessage());
            }
        }
        // 2. Bestands-Frage updaten
        else {
            String sqlUpdate = """
            UPDATE questions
               SET question     = ?,
                   answer       = ?,
                   topic        = ?,
                   correctAnswer= ?,
                   difficulty   = ?
             WHERE id = ?
        """;
            try (PreparedStatement ps = connection.prepareStatement(sqlUpdate)) {
                ps.setString(1, question.getQuestionText());
                ps.setString(2, question.getAnswerAsCSV());
                ps.setString(3, question.getTopic());
                ps.setInt(4, question.getCorrectAnswer());
                ps.setInt(5, question.getDifficulty());
                ps.setInt(6, question.getQuestionId());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Fehler beim Aktualisieren der Frage: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der Frage: " + e.getMessage());
        }
    }

    @Override
    public List<String> loadTopics() {
        List<String> topics = new ArrayList<>();
        String sql = "SELECT name FROM topics ORDER BY name ASC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) topics.add(rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der Themen: " + e.getMessage());
        }
        return topics;
    }

    @Override
    public void addTopic(String topic) {
        String sql = "INSERT INTO topics (name) VALUES (?) ON CONFLICT(name) DO NOTHING";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, topic);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler beim Hinzufügen des Themas: " + e.getMessage());
        }
    }

    @Override
    public void deleteTopic(String topic) {
        String deleteQuestionsSql = "DELETE FROM questions WHERE topic = ?";
        String deleteTopicSql     = "DELETE FROM topics WHERE name = ?";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(deleteQuestionsSql)) {
                ps1.setString(1, topic);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = connection.prepareStatement(deleteTopicSql)) {
                ps2.setString(1, topic);
                ps2.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback fehlgeschlagen: " + ex.getMessage());
            }
            System.err.println("Fehler beim Löschen des Themas inkl. Fragen: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Fehler beim Zurücksetzen von AutoCommit: " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteUser(String username) {
        try {
            connection.setAutoCommit(false);

            String sqlWrong = "DELETE FROM user_wrong_answers WHERE user_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlWrong)) {
                int userId = getUserId(username);
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            String sqlUser = "DELETE FROM users WHERE username = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlUser)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* log rollback-Fehler */ }
            throw new RuntimeException("Fehler beim Löschen des Users: " + e.getMessage(), e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) { /* log */ }
        }
    }

    private int getUserId(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }
}