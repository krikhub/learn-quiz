package l.halbritter;

import l.halbritter.Controller.QuizController;
import l.halbritter.Database.DatabaseImpl;
import l.halbritter.Database.DatabaseService;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseService db = new DatabaseImpl();
            QuizController controller = new QuizController(db);
            controller.startApplication();
        });
    }
}

