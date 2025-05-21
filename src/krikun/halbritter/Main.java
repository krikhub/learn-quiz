/*
 * © 2025 Luca Halbritter und Alexander Krikun
 * IU Internationale Hochschule, Hamburg
 * SPDX-License-Identifier: MIT
 */
package krikun.halbritter;

import krikun.halbritter.Controller.QuizController;
import krikun.halbritter.Database.DatabaseImpl;
import krikun.halbritter.Database.DatabaseService;

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

