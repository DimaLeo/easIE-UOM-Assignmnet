package certh.iti.mklab.easie.extractors.staticpages;

import certh.iti.mklab.easie.URLPatterns;
import certh.iti.mklab.easie.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pagination {

    private int startPage;
    private String next_page_selector;
    private final int numThreads = 20;
    private int step;

    public Pagination(String next_page_selector) {
        this.next_page_selector = next_page_selector;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public Collection<? extends HashMap> multiThreadPagination(String frontPattern, String rearPattern, String tableSelector, List<Configuration.ScrapableField> cfields, List<Configuration.ScrapableField> sfields) throws InterruptedException {
        ArrayList extractedFields = new ArrayList();
        boolean existNext = true;
        int i = 0;

        while (existNext) {
            List<Future<Object>> handles = executePageExtractionThreads(frontPattern, rearPattern, tableSelector, cfields, sfields, i);

            for (int t = 0, n = handles.size(); t < n; t++) {
                try {
                    existNext = processExtractionResult(handles, t, extractedFields);
                } catch (ExecutionException | NoSuchElementException ex) {
                    handleException(ex);
                }
            }

            i++;
        }

        return extractedFields;
    }

    private List<Future<Object>> executePageExtractionThreads(String frontPattern, String rearPattern, String tableSelector, List<Configuration.ScrapableField> cfields, List<Configuration.ScrapableField> sfields, int iteration) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<Object>> handles = new ArrayList<>();

        for (int j = this.startPage; j <= 100 * step; j = j + step) {
            int pagenum = iteration * 100 * step + j;

            handles.add(executorService.submit(new SingleStaticPageExtractor(
                    frontPattern + pagenum + rearPattern,
                    tableSelector,
                    cfields,
                    sfields
            )));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);

        return handles;
    }

    private Boolean processExtractionResult(List<Future<Object>> handles, int index, ArrayList extractedFields) throws ExecutionException {
        Object result = handles.get(index);

        if (result instanceof ArrayList) {
            return handleArrayListResult((ArrayList) result, extractedFields);
        } else {
            return handlePairResult((Pair) result, extractedFields);
        }
    }

    private Boolean handleArrayListResult(ArrayList list, ArrayList extractedFields) {
        if (list.size() == 0 || list == null) {
            return   false;
        } else {
            extractedFields.addAll(list);
            return true;
        }
    }

    private Boolean handlePairResult(Pair pair, ArrayList extractedFields) {
        if (pair != null && ((ArrayList) pair.getKey()).size() > 0) {
            extractedFields.add(pair);
            return true;
        } else {
            return false;
        }
    }

    private void handleException(Exception ex) {
        Logger.getLogger(PaginationIterator.class.getName()).log(Level.SEVERE, null, ex);
    }

    public ArrayList singleThreadPagination(SingleStaticPageExtractor singlePageExtractor, String base_url) throws URISyntaxException, IOException, KeyManagementException {
        ArrayList extractedFields = new ArrayList();
        Object result = singlePageExtractor.call();
        if (result instanceof ArrayList) {
            extractedFields.addAll((ArrayList) singlePageExtractor.call());
        } else {
            extractedFields.add((Pair) singlePageExtractor.call());
        }
        while (!singlePageExtractor.document.select(next_page_selector).attr("href").equals("")) {
            singlePageExtractor.page = base_url + singlePageExtractor.document.select(next_page_selector).attr("href").replace(base_url, "");
            result = singlePageExtractor.call();
            if (result instanceof ArrayList) {
                extractedFields.addAll((ArrayList) result);
            } else {
                extractedFields.add((Pair) result);
            }
        }
        return extractedFields;
    }

    public boolean thereisPattern(String base_url, String relative_url) throws IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException {
        try {
            StaticHTMLExtractor wrapper = new StaticHTMLExtractor(base_url, relative_url);
            Document document = (Document) wrapper.fetcher.getHTMLDocument();
            if (!document.select(next_page_selector).attr("href").equals("")) {
                String url1 = base_url + document.select(next_page_selector).attr("href").replace(base_url, "");
                document = Jsoup.connect(new URI(base_url + document.select(next_page_selector).attr("href").replace(wrapper.base_url, "")).toASCIIString())
                        .userAgent("Mozilla/37.0").timeout(60000).get();
                String url2 = base_url + document.select(next_page_selector).attr("href").replace(base_url, "");
                String frontPattern = URLPatterns.frontPattern(url1, url2);
                String rearPattern = URLPatterns.rearPattern(url1, url2);
                String replaceUrl2 = url2.replace(frontPattern, "").replace(rearPattern, "");
                if (URLPatterns.isInteger(replaceUrl2)) {
                    int temp1 = Integer.parseInt(url1.replace(frontPattern, "").replace(rearPattern, ""));
                    int temp2 = Integer.parseInt(replaceUrl2);
                    step = temp2 - temp1;
                    startPage = temp2 - 2 * step;

                }
                return URLPatterns.isInteger(replaceUrl2);
            } else {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


}
