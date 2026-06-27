package com.zerolauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class JavaUpdater {

    public static void main(String[] args) {
        // تحسين مظهر الواجهة الرسومية لتطابق النظام
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // الانتظار لمدة ثانيتين للتأكد من إغلاق برنامج الـ Bootstrapper تماماً وتحرير الملفات
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        // التأكد من استقبال الـ 3 مسارات كاملة (الملف القديم، الملف النهائي، الملف المؤقت)
        if (args.length < 3) {
            JOptionPane.showMessageDialog(null,
                    "Missing update arguments!\nThis updater should be launched by the main Bootstrapper.",
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        File oldFile = new File(args[0]);   // مسار الملف القديم المُراد حذفه
        File newFile = new File(args[1]);   // المسار النهائي المطلوب للملف الجديد
        File tempFile = new File(args[2]);  // مسار الملف المؤقت (.tmp) المحمل حالياً

        // إظهار نافذة GUI بسيطة تبلغ المستخدم بجاري تثبيت التحديث
        JFrame frame = new JFrame("ZeroLauncher Updater");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(350, 100);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JLabel statusLabel = new JLabel("Applying updates, please wait...", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        frame.add(statusLabel, BorderLayout.CENTER);

        // إظهار النافذة على الشاشة بشكل آمن
        SwingUtilities.invokeLater(() -> frame.setVisible(true));

        try {
            // التحقق من وجود الملف المؤقت المحمل بنجاح
            if (tempFile.exists()) {

                // 1. حذف نسخة اللانشر القديمة تماماً من المجلد
                if (oldFile.exists()) {
                    boolean deleted = oldFile.delete();
                    if (!deleted) {
                        // محاولة الحذف عند إغلاق البرنامج إذا كان الملف معلقاً
                        oldFile.deleteOnExit();
                    }
                }

                // 2. نقل وتغيير اسم الملف المؤقت (.tmp) ليصبح هو الملف النهائي (.jar) بالنسخة الجديدة
                Files.move(tempFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // تحديث النص في الواجهة قبل التشغيل
                SwingUtilities.invokeLater(() -> statusLabel.setText("Update applied! Launching now..."));
                Thread.sleep(1000);

                // 3. تشغيل اللانشر المحدث فوراً بشكل مرئي وصحيح
                runUpdatedJar(newFile);
            } else {
                throw new java.io.FileNotFoundException("Downloaded update file (.tmp) was not found.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to apply update:\n" + e.getMessage(),
                    "Update Process Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // إغلاق نافذة الأبديتور وإنهاء العملية بالكامل
        SwingUtilities.invokeLater(frame::dispose);
        System.exit(0);
    }

    /**
     * دالة تشغيل متطورة تضمن فتح بيئة اللانشر الرسومية بشكل مرئي داخل مجلده الصحيح
     */
    private static void runUpdatedJar(File newFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", newFile.getName());
        pb.directory(newFile.getParentFile()); // تشغيل الجافا داخل سياق مجلد %appdata%/.zerolauncher
        pb.start();
    }
}