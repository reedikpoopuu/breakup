package com.example.demo.contract;

import jakarta.annotation.PreDestroy;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Extracts raw text from an uploaded contract PDF - step 1 of contract field extraction.
 * The 10MB request-size cap (application.properties) only bounds the file as uploaded -
 * PDF's internal stream compression means a small file can still expand into a large or
 * pathologically complex in-memory document, a known class of PDF-parser
 * denial-of-service. Both a page-count ceiling and a wall-clock timeout guard against
 * that; neither is meant to reject a real contract, only something crafted to hang or
 * balloon the parser.
 */
@Component
public class ContractPdfTextExtractor {

    private static final int MAX_PAGES = 100;
    private static final long TIMEOUT_SECONDS = 20;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public String extractText(byte[] pdfBytes) {
        Future<String> future = executor.submit(() -> extractTextUninterruptibly(pdfBytes));
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ContractPdfUnreadableException("PDF took too long to process - it may be too large or too complex");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new ContractPdfUnreadableException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ContractPdfUnreadableException("Interrupted while processing the PDF");
        }
    }

    private String extractTextUninterruptibly(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new ContractPdfUnreadableException(
                        "PDF has " + document.getNumberOfPages() + " pages, over the " + MAX_PAGES + "-page limit for a contract upload");
            }
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new ContractPdfUnreadableException(e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
