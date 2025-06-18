package cn.njit.version;

import java.io.*;

public class VersionInfo {
    private String currentVersion;
    private String latestVersion;
    private String downloadUrl;
    private boolean needUpdate;

    // 构造器、getter和setter
    public VersionInfo(String currentVersion) {
        this.currentVersion = currentVersion;
        this.needUpdate = false;
    }

    // 添加公共getter方法
    public String getLatestVersion() {
        return latestVersion;
    }

    // 检测版本是否需要更新
    public boolean checkUpdate() {
        // 模拟从服务端获取最新版本信息
        // 实际项目中应通过HTTP请求获取版本文件
        this.latestVersion = "1.0.1";
        this.downloadUrl = "http://server/update/client.jar";

        if (!currentVersion.equals(latestVersion)) {
            this.needUpdate = true;
            return true;
        }
        return false;
    }

    // 下载新版本
    public void downloadUpdate() {
        System.out.println("开始下载新版本：" + latestVersion);
        try {
            // 模拟下载过程
            // 实际项目中应使用URLConnection或HttpClient下载文件
            Thread.sleep(2000);
            System.out.println("下载完成，保存到：client_new.jar");

            // 保存更新标记，下次启动时运行新程序
            saveUpdateMarker();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 保存更新标记
    private void saveUpdateMarker() {
        try (FileWriter writer = new FileWriter("update.marker")) {
            writer.write(latestVersion + "\n" + downloadUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从标记文件读取更新信息
    public static VersionInfo loadFromMarker() {
        try (BufferedReader reader = new BufferedReader(new FileReader("update.marker"))) {
            String latestVersion = reader.readLine();
            String downloadUrl = reader.readLine();
            VersionInfo info = new VersionInfo(latestVersion);
            info.latestVersion = latestVersion;
            info.downloadUrl = downloadUrl;
            info.needUpdate = true;
            return info;
        } catch (IOException e) {
            return null;
        }
    }
}
