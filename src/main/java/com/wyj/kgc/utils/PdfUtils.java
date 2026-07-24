package com.wyj.kgc.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class PdfUtils {

    /**
     * Extracts text from a PDF file.
     *
     * @param file The PDF file to extract text from.
     * @return The extracted text as a String.
     * @throws IOException If an I/O error occurs.
     */
    public String extractText(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
