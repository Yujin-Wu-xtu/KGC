package com.wyj.kgc.utils;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class for extracting text from PowerPoint files.
 * Supports both .ppt (HSLF) and .pptx (XSLF) formats.
 */
@Component
public class PptUtils {

    /**
     * Extracts text from a PowerPoint file (.ppt or .pptx).
     *
     * @param file The PowerPoint file to extract text from.
     * @return The extracted text as a String.
     * @throws IOException If an I/O error occurs or the format is unsupported.
     */
    public String extractText(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            SlideShow<?, ?> slideShow;
            if (fileName.endsWith(".pptx")) {
                slideShow = new XMLSlideShow(fis);
            } else if (fileName.endsWith(".ppt")) {
                slideShow = new HSLFSlideShow(fis);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Expected .ppt or .pptx: " + fileName);
            }

            try (SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
                extractor.setSlidesByDefault(true);
                extractor.setNotesByDefault(false);
                extractor.setMasterByDefault(false);
                return extractor.getText();
            } finally {
                // Ensure slideShow is closed even if extractor doesn't close it (though it usually does)
                slideShow.close();
            }
        }
    }
}
