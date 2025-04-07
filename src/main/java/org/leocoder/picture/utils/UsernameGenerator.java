package org.leocoder.picture.utils;

import java.util.Random;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 16:22
 * @description : 用户名生成工具类
 */
public class UsernameGenerator {

    /**
     * 生成智能云图库风格的用户名 - 中文前缀 + 四个大写字母
     * @return 生成的用户名
     */
    public static String generateGalleryUsername() {
        // 图库相关的中文前缀
        String[] galleryPrefixes = {
            "云图", "影像", "瞬间", "光影", "色彩", "画卷", "镜像", "相册", 
            "影集", "图像", "照片", "景观", "视界", "画面", "艺术", "美学",
            "摄影", "创意", "图库", "像素", "构图", "风景", "记忆", "印象"
        };
        
        // 从前缀数组中随机选择一个
        String prefix = galleryPrefixes[new Random().nextInt(galleryPrefixes.length)];
        
        // 生成四个随机字母
        String letters = generatePatternedLetters();
        
        // 返回组合后的用户名
        return prefix + letters;
    }

    /**
     * 生成有意义或随机的四个大写字母
     * @return 四个大写字母字符串
     */
    private static String generatePatternedLetters() {
        // 一些有意义的四字母组合模式
        String[] letterPatterns = {
            "FOTO", "SNAP", "PICS", "VIEW", "LENS", "ZOOM", "SHOT", "FILM",
            "LIFE", "TIME", "LOOK", "EYES", "SEEN", "ARTE", "LITE", "VIBE"
        };

        // 随机使用一个有意义的组合，或完全随机生成
        boolean usePattern = new Random().nextBoolean();

        if (usePattern) {
            return letterPatterns[new Random().nextInt(letterPatterns.length)];
        } else {
            // 完全随机生成四个字母
            StringBuilder letters = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                char randomChar = (char) (65 + new Random().nextInt(26));
                letters.append(randomChar);
            }
            return letters.toString();
        }
    }
}