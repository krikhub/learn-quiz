package l.halbritter;

import l.halbritter.Controller.QuizController;
import l.halbritter.Database.Database;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizController controller = new QuizController();
            controller.startApplication();
        });
    }
}

