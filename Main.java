import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private String gameDirectory = ""; // Путь к папке с игрой
    private Stage progressStage;
    private ProgressBar progressBar;
    private Label progressLabel;

    private static final String[] FILE_URLS = {
            "https://www.dropbox.com/scl/fi/ktceu2r1k62yddtd9xqv6/ContentWosFirst_Final.mix?rlkey=f5j4ds9a8j5e6g7zkk0m6ynez&e=1&st=beho0bhg&dl=1",
            "https://www.dropbox.com/scl/fi/yi7w75607ljxcnpkgmntw/BWOS-0.8e.w3x?rlkey=48r07pagzq7yyhilsgfuujjkv&st=99j5rs14&dl=1",
            "https://www.dropbox.com/scl/fi/x1ctsst762czsqk8jhocc/WoS_Music.mix?rlkey=yp96da0tywaaz6i9dg9nff648&st=86f3lj3z&dl=1",
            "https://www.dropbox.com/scl/fi/hb7xl0lzjc4739qybkzu6/ContentWosSecond.mix?rlkey=b56rphhhu99hk7mnf8x53liw5&st=k5xv9fpd&dl=1",
    };

    private static final String[] FILE_NAMES = {
            "ContentWosFirst_Final.mix",
            "Maps/BWOS 0.8e.w3x",
            "WoS_Music.mix",
            "ContentWosSecond.mix"
    };

    @Override
    public void start(Stage primaryStage) {
        URL url = Main.class.getClassLoader().getResource("bg.png");
        System.out.println("URL: " + url);

        Image backgroundImage = new Image(Main.class.getClassLoader().getResource("bg.png").toExternalForm());
        ImageView imageView = new ImageView(backgroundImage);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);

        Button selectFolderButton = new Button("Выбрать папку");
        selectFolderButton.setStyle("-fx-font-size: 18px; -fx-pref-width: 200px; -fx-pref-height: 50px;");
        selectFolderButton.setOnAction(event -> selectGameFolder(primaryStage));

        Button updateButton = new Button("Обновить игру");
        updateButton.setStyle("-fx-font-size: 18px; -fx-pref-width: 200px; -fx-pref-height: 50px;");
        updateButton.setOnAction(event -> new Thread(this::updateMixFiles).start());

        VBox buttonBox = new VBox(20, selectFolderButton, updateButton);
        buttonBox.setTranslateY(50);
        buttonBox.setTranslateX(0);
        buttonBox.setStyle("-fx-alignment: center; -fx-spacing: 10;");// Центрируем кнопки по вертикали и горизонтали


        StackPane root = new StackPane(imageView, buttonBox);
        AnchorPane anchorPane = new AnchorPane(); // Новый контейнер для версии
        anchorPane.getChildren().add(root); // Добавляем основной контейнер в AnchorPane

// Создаем лейбл с версией
        Label versionLabel = new Label("Version 1.0.0");

// Устанавливаем стиль для лейбла
        versionLabel.getStyleClass().add("version-label");

// Размещаем лейбл в правом нижнем углу
        AnchorPane.setBottomAnchor(versionLabel, 10.0);
        AnchorPane.setRightAnchor(versionLabel, 10.0);

// Добавляем лейбл в anchorPane
        anchorPane.getChildren().add(versionLabel);

        Scene scene = new Scene(anchorPane, 800, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Загрузчик War of Souls");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);  // Окно будет иметь фиксированный размер
        primaryStage.show();

    }

    private void createProgressWindow() {
        progressStage = new Stage();
        progressLabel = new Label("Ожидание...");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        VBox vbox = new VBox(10, progressLabel, progressBar);
        Scene scene = new Scene(vbox, 350, 120);
        progressStage.setScene(scene);
        progressStage.setTitle("Загрузка файлов");
        progressStage.show();
    }

    private void closeProgressWindow() {
        if (progressStage != null) {
            Platform.runLater(progressStage::close);
        }
    }

    private void selectGameFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку с игрой");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            File war3Exe = new File(selectedDirectory, "war3.exe");
            if (!war3Exe.exists()) {
                gameDirectory = ""; // Сбрасываем путь, если war3.exe нет
                showAlert("Ошибка", "В выбранной папке нет war3.exe!");
                return;
            }
            gameDirectory = selectedDirectory.getAbsolutePath();
            showAlert("Успех", "Выбрана папка: " + gameDirectory);
        }
    }

    private void updateMixFiles() {
        if (gameDirectory.isEmpty()) {
            showAlert("Ошибка", "Папка не выбрана!");
            return;
        }

        Platform.runLater(this::createProgressWindow);

        for (int i = 0; i < FILE_URLS.length; i++) {
            final int index = i;
            try {
                File localFile = new File(gameDirectory + File.separator + FILE_NAMES[index]);
                long onlineFileSize = getFileSize(FILE_URLS[index]);
                if (localFile.exists() && localFile.length() == onlineFileSize) {
                    continue;
                }
                downloadFile(FILE_URLS[index], localFile.getAbsolutePath(), index + 1, FILE_URLS.length);
            } catch (Exception e) {
                System.out.println("Ошибка при скачивании " + FILE_NAMES[index] + ": " + e.getMessage());
            }
        }

        Platform.runLater(this::closeProgressWindow);
        showAlert("Обновление завершено", "Игра обновлена! Можно играть!");
    }

    private void downloadFile(String fileURL, String savePath, int fileIndex, int totalFiles) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        long fileSize = connection.getContentLengthLong();
        InputStream inputStream = connection.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(savePath);
        byte[] buffer = new byte[4096];
        int bytesRead;
        long downloaded = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            downloaded += bytesRead;
            double progress = (double) downloaded / fileSize;
            Platform.runLater(() -> progressBar.setProgress(progress));
        }

        outputStream.close();
        inputStream.close();
        connection.disconnect();
    }

    private long getFileSize(String fileURL) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestMethod("HEAD");
        long fileSize = connection.getContentLengthLong();
        connection.disconnect();
        return fileSize;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
