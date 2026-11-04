package hu.hazazs.anyconverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
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

    private final Button jfifButton = new Button("JFIF → JPG");
    private final Button webpButton = new Button("WEBP → JPG");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready");

    @FunctionalInterface
    private interface Conversion {
        JfifConverter.Summary run(
                Path directory, BiConsumer<Integer, Integer> progress) throws IOException;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        styleButton(jfifButton);
        styleButton(webpButton);

        jfifButton.setOnAction(event -> convert(".jfif", JfifConverter::convertAll));
        webpButton.setOnAction(event -> convert(".webp", WebpConverter::convertAll));

        progressBar.setPrefWidth(310);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(16);

        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");

        VBox root = new VBox(13, jfifButton, webpButton, progressBar, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: white;");

        stage.setTitle("Any Converter");
        stage.getIcons().add(IconData.loadIcon());
        stage.setScene(new Scene(root, 380, 260));
        stage.setResizable(false);
        stage.show();
    }

    private static void styleButton(Button button) {
        button.setMinWidth(180);
        button.setPrefHeight(42);
        button.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; "
                        + "-fx-background-radius: 7px; -fx-cursor: hand;");
    }

    private void convert(String extension, Conversion conversion) {
        Task<JfifConverter.Summary> task = new Task<>() {
            @Override
            protected JfifConverter.Summary call() throws Exception {
                updateProgress(0, 1);
                updateMessage("Scanning D:\\downloadz for " + extension + " files...");
                return conversion.run(JfifConverter.DEFAULT_DIRECTORY, (done, total) -> {
                    updateProgress(done, total);
                    updateMessage("Processed " + done + " / " + total + " files");
                });
            }
        };

        jfifButton.setDisable(true);
        webpButton.setDisable(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            resetControls();
            JfifConverter.Summary result = task.getValue();
            if (result.total() == 0) {
                progressBar.setProgress(0);
                statusLabel.setText("No " + extension + " files found in D:\\downloadz");
            } else {
                progressBar.setProgress(1);
                statusLabel.setText("Converted: " + result.converted()
                        + "   Skipped: " + result.skipped()
                        + "   Failed: " + result.failed());
            }
        });

        task.setOnFailed(event -> {
            resetControls();
            progressBar.setProgress(0);
            Throwable cause = task.getException();
            statusLabel.setText(cause != null ? cause.getMessage() : "Conversion failed");
        });

        Thread worker = new Thread(task, "file-converter");
        worker.setDaemon(true);
        worker.start();
    }

    private void resetControls() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        jfifButton.setDisable(false);
        webpButton.setDisable(false);
    }
}
