package xlike.top.kn_ai_chat.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件内容读取器
 * <p>
 * 支持多种常见文件格式的文本内容提取。
 * @author xlike
 */
public class FileContentReader {

    /**
     * 读取指定文件的文本内容
     *
     * @param file File对象
     * @return 文件内容字符串
     * @throws IOException 如果文件读取失败
     */
    public static String readFileContent(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }

        String fileName = file.getName();
        String fileExtension = getFileExtension(fileName).toLowerCase();
        
        return switch (fileExtension) {
            case "pdf" -> readPdfContent(file);
            case "docx" -> readWordContent(file);
            case "xlsx", "xls" -> readExcelContent(file);
            case "html", "htm" -> readHtmlContent(file);
            case "csv" -> readCsvContent(file);
            case "xml" -> readXmlContent(file);
            case "txt", "log", "json", "py", "java", "md" -> readTextContent(file);
            default -> throw new IOException("不支持的文件类型: " + fileExtension);
        };
    }

    private static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndex + 1);
    }

    private static String readPdfContent(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            } else {
                throw new IOException("PDF文件已加密，无法读取");
            }
        }
    }

    private static String readWordContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder content = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                content.append(para.getText()).append("\n");
            }
            return content.toString();
        }
    }

    private static String readExcelContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            String fileExtension = getFileExtension(file.getName()).toLowerCase();
            if ("xlsx".equals(fileExtension)) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            StringBuilder content = new StringBuilder();
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        content.append(cell.toString()).append("\t");
                    }
                    content.append("\n");
                }
                content.append("\n");
            }
            workbook.close();
            return content.toString();
        }
    }

    private static String readHtmlContent(File file) throws IOException {
        return Jsoup.parse(file, StandardCharsets.UTF_8.name()).text();
    }

    private static String readCsvContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (CSVReader csvReader = new CSVReader(new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))) {
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                content.append(String.join(",", row)).append("\n");
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV文件解析失败: " + e.getMessage());
        }
        return content.toString();
    }

    private static String readXmlContent(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 防止 XXE / 外部实体读取
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(file);
            StringBuilder content = new StringBuilder();
            extractTextFromNode(doc.getDocumentElement(), content);
            return content.toString();
        } catch (Exception e) {
            throw new IOException("XML文件解析失败: " + e.getMessage());
        }
    }

    private static void extractTextFromNode(Node node, StringBuilder content) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getTextContent().trim();
            if (!text.isEmpty()) {
                content.append(text).append(" ");
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            extractTextFromNode(children.item(i), content);
        }
    }

    private static String readTextContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}