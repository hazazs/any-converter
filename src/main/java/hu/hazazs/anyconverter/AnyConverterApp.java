package hu.hazazs.anyconverter;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class AnyConverterApp extends Application {

    private final Button convertButton = new Button("JFIF → JPG");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        convertButton.setMinWidth(180);
        convertButton.setPrefHeight(42);
        convertButton.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; "
                        + "-fx-background-radius: 7px; -fx-cursor: hand;");
        convertButton.setOnAction(event -> convert());

        progressBar.setPrefWidth(310);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(16);

        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");

        VBox root = new VBox(17, convertButton, progressBar, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: white;");

        stage.setTitle("Any Converter");
        stage.getIcons().add(IconData.loadIcon());
        stage.setScene(new Scene(root, 380, 200));
        stage.setResizable(false);
        stage.show();
    }

    private void convert() {
        Task<JfifConverter.Summary> task = new Task<>() {
            @Override
            protected JfifConverter.Summary call() throws Exception {
                updateProgress(0, 1);
                updateMessage("Scanning D:\\downloadz...");
                return JfifConverter.convertAll(JfifConverter.DEFAULT_DIRECTORY, (done, total) -> {
                    updateProgress(done, total);
                    updateMessage("Processed " + done + " / " + total + " files");
                });
            }
        };

        convertButton.setDisable(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            convertButton.setDisable(false);

            JfifConverter.Summary result = task.getValue();
            if (result.total() == 0) {
                progressBar.setProgress(0);
                statusLabel.setText("No .jfif files found in D:\\downloadz");
            } else {
                progressBar.setProgress(1);
                statusLabel.setText("Converted: " + result.converted()
                        + "   Skipped: " + result.skipped()
                        + "   Failed: " + result.failed());
            }
        });

        task.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            convertButton.setDisable(false);
            progressBar.setProgress(0);
            Throwable cause = task.getException();
            statusLabel.setText(cause != null ? cause.getMessage() : "Conversion failed");
        });

        Thread worker = new Thread(task, "jfif-to-jpg-converter");
        worker.setDaemon(true);
        worker.start();
    }
}
