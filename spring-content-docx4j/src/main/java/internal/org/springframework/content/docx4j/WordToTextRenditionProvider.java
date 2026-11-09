package internal.org.springframework.content.docx4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.model.text.TextOptions;
import org.docx4j.model.text.Texts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import static java.lang.String.format;

@Service
public class WordToTextRenditionProvider implements RenditionProvider {

    private static final Log logger = LogFactory.getLog(WordToTextRenditionProvider.class);

    @Override
    public String consumes() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    @Override
    public String[] produces() {
        return new String[]{"text/plain"};
    }

    @Override
    public InputStream convert(InputStream fromInputSource, String toMimeType) {
        try {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(fromInputSource);

            File tmpFile = Files.createTempFile(null, null, new FileAttribute<?>[]{}).toFile();
            String text = Texts.plainText(pkg, new TextOptions());
            Files.writeString(tmpFile.toPath(), text);

            if (pkg.getMainDocumentPart().getFontTablePart() != null) {
                pkg.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
            }

            return new ObservableInputStream(new FileInputStream(tmpFile), new FileRemover(tmpFile));
        } catch (Exception e) {
            logger.warn(format("%s rendition failed", toMimeType), e);
        }

        return null;
    }

}
