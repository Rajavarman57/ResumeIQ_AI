package com.resumeiq.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
public class ResumeParserService {

    private static final List<String> TECH_SKILLS = Arrays.asList(
        "java","python","javascript","typescript","react","angular","vue","spring","springboot",
        "hibernate","jpa","node","nodejs","express","django","flask","fastapi","sql","mysql",
        "postgresql","mongodb","redis","docker","kubernetes","aws","azure","gcp","git","maven",
        "gradle","jenkins","ci/cd","html","css","bootstrap","tailwind","rest","restful","api",
        "microservices","kafka","rabbitmq","elasticsearch","linux","bash","shell","c++","c#",
        ".net","php","ruby","rails","go","golang","rust","swift","kotlin","android","ios",
        "flutter","react native","tensorflow","pytorch","machine learning","deep learning",
        "data science","pandas","numpy","scikit-learn","spark","hadoop","tableau","power bi",
        "agile","scrum","jira","confluence","figma","photoshop","excel","word","powerpoint"
    );

    private static final List<String> SOFT_SKILLS = Arrays.asList(
        "leadership","communication","teamwork","problem solving","analytical","critical thinking",
        "time management","adaptability","creativity","collaboration","presentation","management",
        "mentoring","negotiation","project management","customer service","attention to detail"
    );

    public String extractText(MultipartFile file, String fileType) throws IOException {
        if ("PDF".equalsIgnoreCase(fileType)) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if ("DOCX".equalsIgnoreCase(fileType)) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                return doc.getParagraphs().stream()
                        .map(p -> p.getText())
                        .collect(Collectors.joining("\n"));
            }
        }
        return "";
    }

    public List<String> extractSkills(String text) {
        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String skill : TECH_SKILLS) {
            if (lower.contains(skill)) found.add(skill);
        }
        for (String skill : SOFT_SKILLS) {
            if (lower.contains(skill)) found.add(skill);
        }
        return found.stream().distinct().collect(Collectors.toList());
    }

    public int extractExperienceYears(String text) {
        Pattern p = Pattern.compile("(\\d+)\\s*\\+?\\s*years?\\s*(of)?\\s*(experience|exp)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        int max = 0;
        while (m.find()) {
            int val = Integer.parseInt(m.group(1));
            if (val > max && val < 50) max = val;
        }
        if (max == 0) {
            // estimate from date ranges
            Pattern dateRange = Pattern.compile("(20\\d{2})\\s*[-–]\\s*(20\\d{2}|present|current)",
                    Pattern.CASE_INSENSITIVE);
            Matcher dm = dateRange.matcher(text);
            int total = 0;
            while (dm.find()) {
                int start = Integer.parseInt(dm.group(1));
                int end = dm.group(2).matches("\\d+") ? Integer.parseInt(dm.group(2)) : 2026;
                total += (end - start);
            }
            max = Math.min(total, 40);
        }
        return max;
    }

    public String extractEducation(String text) {
        List<String> degrees = Arrays.asList(
            "b.tech","btech","b.e","be","m.tech","mtech","mba","bsc","msc","phd",
            "bachelor","master","doctorate","diploma","b.com","m.com","bca","mca"
        );
        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String deg : degrees) {
            if (lower.contains(deg)) found.add(deg.toUpperCase());
        }
        return found.isEmpty() ? "Not specified" : String.join(", ", found.stream().distinct().collect(Collectors.toList()));
    }

    public String extractEmail(String text) {
        Pattern p = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
        Matcher m = p.matcher(text);
        return m.find() ? m.group() : "";
    }

    public String extractPhone(String text) {
        Pattern p = Pattern.compile("(\\+?\\d[\\d\\s\\-().]{7,}\\d)");
        Matcher m = p.matcher(text);
        return m.find() ? m.group().trim() : "";
    }

    public String extractName(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 2 && line.length() < 60
                    && line.matches("[A-Za-z .'-]+")
                    && !line.toLowerCase().contains("resume")
                    && !line.toLowerCase().contains("curriculum")) {
                return line;
            }
        }
        return "Unknown Candidate";
    }
}
