package eu.luminis.aws.norconex;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.tagger.impl.DOMTagger;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ImporterConfigFactory extends AbstractFactoryBean<ImporterConfig> {
    @Override
    public Class<?> getObjectType() {
        return ImporterConfig.class;
    }

    @Override
    protected ImporterConfig createInstance() throws Exception {
        ImporterConfig importerConfig = new ImporterConfig();
        DOMTagger domTagger = new DOMTagger();
        domTagger.addDOMExtractDetails(new DOMTagger.DOMExtractDetails("section#main_content", "content", PropertySetter.REPLACE));
        importerConfig.setPreParseHandlers(Arrays.asList(domTagger));
        return importerConfig;
    }
}
