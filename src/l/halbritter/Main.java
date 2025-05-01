package l.halbritter;

import l.halbritter.View.QuizView;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            QuizView quizView = new QuizView();
            quizView.createAndShowGUI();
        });
    }
}
