package com.zerolauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class LauncherBootstrapper {

    // 🔗 GitHub Configuration
    private static final String GITHUB_USER = "MC7DZ";
    private static final String GITHUB_REPO = "ZeroLauncher-Updates";

    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/latest";
    private static final String APPDATA_FOLDER = getAppDataPath();

    // 🎨 Ultra Modern Glassmorphism Dark Theme Colors
    private static final Color GLASS_BG = new Color(25, 25, 25, 220);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 30);

    private static final Color TEXT_WHITE = new Color(245, 245, 245);
    private static final Color TEXT_GRAY = new Color(140, 140, 140);
    private static final Color ACCENT_BLUE = new Color(0, 153, 255);
    private static final Color ACCENT_GREEN = new Color(46, 204, 113);
    private static final Color ACCENT_RED = new Color(231, 76, 60);

    public static void main(String[] args) {
        configureDarkOptionPane();

        if (args.length >= 3 && args[0].equals("--self-update")) {
            handleSelfUpdateTask(args[1], args[2]);
            System.exit(0);
        }

        // Environment verified! Proceed with folder initialization and checking routines
        File folder = new File(APPDATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        new Thread(LauncherBootstrapper::checkAndManageUpdates).start();
    }

    private static void handleSelfUpdateTask(String oldJarPath, String newJarPath) {
        try {
            Thread.sleep(1500);

            File oldJar = new File(oldJarPath);
            File newJar = new File(newJarPath);
            File tempFile = new File(APPDATA_FOLDER + File.separator + "zero-launcher-new.tmp");

            if (oldJar.exists()) {
                oldJar.delete();
            }

            if (tempFile.exists()) {
                Files.move(tempFile.toPath(), newJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            showUpdateSuccessAndLaunch(newJar.getAbsolutePath());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Self-Update failed: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void checkAndManageUpdates() {
        JFrame checkingFrame = showCheckingForUpdatesGUI();

        File folder = new File(APPDATA_FOLDER);
        File currentJarFile = null;
        String currentVersion = "0.0.0";

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("zero-launcher-") && file.getName().endsWith(".jar")) {
                    currentJarFile = file;
                    currentVersion = file.getName().substring(file.getName().lastIndexOf("-") + 1, file.getName().lastIndexOf(".jar"));
                    break;
                }
            }
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed to connect to GitHub API. Code: " + conn.getResponseCode());
            }

            Scanner scanner = new Scanner(conn.getInputStream());
            String jsonResponse = scanner.useDelimiter("\\A").next();
            scanner.close();

            checkingFrame.dispose();

            String latestVersion = jsonResponse.split("\"tag_name\":\"")[1].split("\"")[0].replace("v", "").trim();

            if (currentJarFile == null) {
                String jarName = "zero-launcher-" + latestVersion + ".jar";
                String finalPath = APPDATA_FOLDER + File.separator + jarName;
                String downloadUrl = "https://github.com/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/download/v" + latestVersion + "/" + jarName;

                showDownloadGUI(downloadUrl, finalPath, "Downloading ZeroLauncher for the first time...", true);
                showUpdateSuccessAndLaunch(finalPath);
                return;
            }

            if (isNewerVersion(latestVersion, currentVersion)) {
                int response = JOptionPane.showConfirmDialog(
                        null,
                        "A new update is available (" + latestVersion + ").\nYour current version is: " + currentVersion + "\n\nWould you like to update now?",
                        "ZeroLauncher Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (response == JOptionPane.YES_OPTION) {
                    String newJarRealName = "zero-launcher-" + latestVersion + ".jar";
                    String downloadUrl = "https://github.com/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/download/v" + latestVersion + "/" + newJarRealName;
                    String tempFilePath = APPDATA_FOLDER + File.separator + "zero-launcher-new.tmp";

                    showDownloadGUI(downloadUrl, tempFilePath, "Downloading Update: " + latestVersion, false);

                    String oldJarPath = currentJarFile.getAbsolutePath();
                    String newJarPath = APPDATA_FOLDER + File.separator + newJarRealName;

                    String currentRunningJar = new File(LauncherBootstrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

                    ProcessBuilder pb = new ProcessBuilder("java", "-jar", currentRunningJar, "--self-update", oldJarPath, newJarPath);
                    pb.start();

                    System.exit(0);
                } else {
                    runJarAndMonitor(currentJarFile.getAbsolutePath());
                    System.exit(0);
                }
            } else {
                runJarAndMonitor(currentJarFile.getAbsolutePath());
                System.exit(0);
            }

        } catch (Exception e) {
            checkingFrame.dispose();
            handleNetworkError(currentJarFile);
        }
    }

    private static JFrame showCheckingForUpdatesGUI() {
        JFrame frame = new JFrame();
        frame.setSize(340, 110);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(GLASS_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(GLASS_BORDER);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel label = new JLabel("Checking for updates...", JLabel.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(TEXT_WHITE);

        ModernSpinner spinner = new ModernSpinner(24);

        panel.add(label, BorderLayout.WEST);
        panel.add(spinner, BorderLayout.EAST);
        frame.add(panel);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
        return frame;
    }

    private static void showDownloadGUI(String fileURL, String outputPath, String titleMessage, boolean showNotice) throws Exception {
        JFrame frame = new JFrame();
        frame.setSize(440, showNotice ? 150 : 110);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(GLASS_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(GLASS_BORDER);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(22, 25, 22, 25));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        JLabel statusLabel = new JLabel(titleMessage, JLabel.LEFT);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(TEXT_WHITE);

        ModernSpinner progressSpinner = new ModernSpinner(28);

        centerPanel.add(statusLabel, BorderLayout.WEST);
        centerPanel.add(progressSpinner, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        if (showNotice) {
            JLabel noticeLabel = new JLabel("<html><center>Note: If the launcher is broken, please delete the old .jar files inside the <b>.zerolauncher</b> directory.</center></html>", JLabel.CENTER);
            noticeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            noticeLabel.setForeground(TEXT_GRAY);
            noticeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            mainPanel.add(noticeLabel, BorderLayout.SOUTH);
        }

        frame.add(mainPanel);
        SwingUtilities.invokeLater(() -> frame.setVisible(true));

        HttpURLConnection httpConn = (HttpURLConnection) new URL(fileURL).openConnection();
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            int fileLength = httpConn.getContentLength();
            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputPath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                int lastReportedProgress = -1;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (fileLength > 0) {
                        final int progress = (int) ((totalBytesRead * 100) / fileLength);

                        if (progress != lastReportedProgress) {
                            lastReportedProgress = progress;
                            final int currentProgress = progress;
                            SwingUtilities.invokeLater(() -> progressSpinner.setProgressPercent(currentProgress));
                        }
                    }
                }
            }
        } else {
            frame.dispose();
            throw new RuntimeException("Server replied HTTP code: " + responseCode);
        }
        SwingUtilities.invokeLater(frame::dispose);
    }

    private static class ModernSpinner extends JComponent {
        private int angle = 0;
        private int progressPercent = 0;
        private final Timer animationTimer;
        private final int size;

        public ModernSpinner(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size + 40, size));

            animationTimer = new Timer(15, e -> {
                angle = (angle + 4) % 360;
                repaint();
            });
            animationTimer.start();
        }

        public void setProgressPercent(int percent) {
            this.progressPercent = percent;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int x = 0;
            int y = (height - size) / 2;

            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.drawOval(x + 2, y + 2, size - 4, size - 4);

            g2d.setColor(ACCENT_BLUE);
            g2d.drawArc(x + 2, y + 2, size - 4, size - 4, -angle, 100);

            if (progressPercent > 0) {
                g2d.setColor(TEXT_WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2d.drawString(progressPercent + "%", x + size + 8, (height / 2) + 4);
            }

            g2d.dispose();
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            if (animationTimer != null) animationTimer.stop();
        }
    }

    private static void showUpdateSuccessAndLaunch(String targetJarPath) {
        JFrame frame = new JFrame();
        frame.setSize(360, 110);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(GLASS_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(new Color(46, 204, 113, 100));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 25, 22, 25));

        JLabel titleLabel = new JLabel("Update Installed Successfully!", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(ACCENT_GREEN);

        JLabel descLabel = new JLabel("Launching ZeroLauncher...", JLabel.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_WHITE);

        panel.add(titleLabel);
        panel.add(descLabel);
        frame.add(panel);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));

        try {
            Thread.sleep(1200);
        } catch (InterruptedException ignored) {}

        frame.dispose();
        try {
            runJarAndMonitor(targetJarPath);
            System.exit(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Failed to start launcher automatically: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void showJavaRequiredGUI() {
        JFrame frame = new JFrame();
        frame.setSize(520, 220);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(GLASS_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(GLASS_BORDER);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("JavaFX Environment Missing!", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT_RED);

        JLabel descLabel = new JLabel("<html><center>ZeroLauncher requires a Java runtime environment bundled with <b>JavaFX</b> modules.<br>Please download or install one of our recommended packages below:</center></html>", JLabel.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_WHITE);

        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);

        JButton btnZulu = createModernButton("Azul Zulu FX", new Color(45, 45, 45, 180));
        JButton btnLiberica = createModernButton("Liberica Full JDK", new Color(45, 45, 45, 180));
        JButton btnExit = createModernButton("Exit", ACCENT_RED);

        btnZulu.addActionListener(e -> openUrlInBrowser("https://www.azul.com/downloads/?package=jdk-fx#downloads-table-zulu"));
        btnLiberica.addActionListener(e -> openUrlInBrowser("https://bell-sw.com/pages/downloads/#downloads"));
        btnExit.addActionListener(e -> System.exit(1));

        buttonPanel.add(btnZulu);
        buttonPanel.add(btnLiberica);
        buttonPanel.add(btnExit);

        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        while (frame.isVisible()) {
            try { Thread.sleep(100); } catch (Exception ignored) {}
        }
    }

    private static JButton createModernButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static void configureDarkOptionPane() {
        UIManager.put("OptionPane.background", new Color(30, 30, 30));
        UIManager.put("Panel.background", new Color(30, 30, 30));
        UIManager.put("OptionPane.messageForeground", TEXT_WHITE);
        UIManager.put("Button.background", new Color(50, 50, 50));
        UIManager.put("Button.foreground", TEXT_WHITE);
    }

    private static void handleNetworkError(File currentJarFile) {
        if (currentJarFile == null) {
            JOptionPane.showMessageDialog(null, "No internet connection detected!\nAnd no local version found.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } else {
            String[] options = {"Launch Anyway", "Exit"};
            int selection = JOptionPane.showOptionDialog(null, "No internet connection! Launch local version?", "Offline Mode", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (selection == JOptionPane.YES_OPTION) {
                try {
                    runJarAndMonitor(currentJarFile.getAbsolutePath());
                    System.exit(0);
                } catch (Exception ex) {
                    System.exit(1);
                }
            } else {
                System.exit(0);
            }
        }
    }

    private static String getAppDataPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("APPDATA") + File.separator + ".zerolauncher";
        } else {
            return System.getProperty("user.home") + File.separator + ".zerolauncher";
        }
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }

    private static void runJarAndMonitor(String jarPath) throws Exception {
        File file = new File(jarPath);
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", file.getName());
        pb.directory(file.getParentFile());

        // Redirecting error stream to input stream so we can catch any missing module issues directly
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean hasJavaFXCrash = false;

        // Active monitoring stream thread
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Check log strings for classic JavaFX runtime runtime missing errors
                if (line.contains("ClassNotFoundException: javafx.") ||
                        line.contains("JavaFX runtime components are missing") ||
                        line.contains("Error: GraphicsDevice") ||
                        line.contains("Exception in Application start method")) {
                    hasJavaFXCrash = true;
                }
            }
        }

        int exitCode = process.waitFor();

        // If it exited unexpectedly with error code or logs caught a JavaFX crash signature
        if (exitCode != 0 || hasJavaFXCrash) {
            showJavaRequiredGUI();
            System.exit(1);
        }
    }

    private static void openUrlInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {}
    }
}