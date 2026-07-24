package com.wyj.kgc.utils;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class WordUtils {

    /**
     * Extracts text from a Word file (.doc or .docx).
     *
     * @param file The Word file to extract text from.
     * @return The extracted text as a String.
     * @throws IOException If an I/O error occurs or the format is unsupported.
     */
    public String extractText(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            if (fileName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(fis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else if (fileName.endsWith(".doc")) {
                try (HWPFDocument document = new HWPFDocument(fis);
                     WordExtractor extractor = new WordExtractor(document)) {
                    return extractor.getText();
                }
            } else {
                throw new IllegalArgumentException("Unsupported file format. Expected .doc or .docx: " + fileName);
            }
        }
    }
}
