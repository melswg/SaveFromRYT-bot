import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.impl.TikTokDownloader;

import java.io.File;

public class TikTokDownloaderTest {
    public static void main(String[] args) {
        TikTokDownloader downloader = new TikTokDownloader();

        String testURL = "https://vt.tiktok.com/ZSP7QE5kU/";

        System.out.println("Начало загрузки: " + testURL);

        MediaContent result = downloader.download(testURL);

        if (result != null){
            System.out.println("Загрузка успешно завершена");
            System.out.println("IS VIDEO ?! - " + result.isVideo());
            System.out.println("Кол-во файлов: " + result.getFiles().size());

            for (File file : result.getFiles()){
                System.out.println("Файл сохранен тут: " + file.getAbsolutePath());
            }
        } else {
            System.out.println("Ошибка, загрузка не удалась.");
        }
    }

}
