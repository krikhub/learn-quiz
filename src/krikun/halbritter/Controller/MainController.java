/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter.Controller;

import krikun.halbritter.Database.DatabaseService;
import krikun.halbritter.View.QuizUI;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainController {

    private final QuizUI view;
    private final DatabaseService db;

    private final QuizController quizController;
    private final QuestionController questionController;
    private final StatsController statsController;

    public MainController(DatabaseService db) {
        this.db = db;
        this.view = new QuizUI();

        this.quizController = new QuizController(db, view, this);
        this.questionController = new QuestionController(db, view, this);
        this.statsController = new StatsController(db, view, this);
    }

    public void start() {
        db.connect();
        JFrame frame = view.getFrame();
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int ans = JOptionPane.showConfirmDialog(
                        frame,
                        "Möchtest du wirklich beenden?",
                        "Beenden",
                        JOptionPane.YES_NO_OPTION
                );
                if (ans == JOptionPane.YES_OPTION) {
                    exitApplication();
                }
            }
        });
        showMainMenu();
    }

    public void showMainMenu() {
        view.showMainMenu(
                e -> questionController.showQuestionsForEditing(),
                e -> quizController.showPlayerSelection(),
                e -> statsController.showUserSelectionForWorstQuestions(),
                e -> {
                    db.disconnect();
                    System.exit(0);
                }
        );
    }

    private void exitApplication() {
        db.disconnect();
        System.exit(0);
    }
}