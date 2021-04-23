package eu.luminis.aws.norconex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.norconex.collector.core.doc.CrawlDocInfo;
import com.norconex.collector.http.doc.HttpDocInfo;
import eu.luminis.aws.norconex.dynamodb.ZonedDateTimeConverter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrawlerLowLevelTest {


    @Test
    public void checkCasting() {
        Gson GSON = new Gson();
        HttpDocInfo httpDocInfo = new HttpDocInfo();
        httpDocInfo.setReference("http://www.jettro.dev");

        String jsonObject =  GSON.toJson(httpDocInfo);

        CrawlDocInfo crawlDocInfo = GSON.fromJson(jsonObject, HttpDocInfo.class);

        HttpDocInfo casted = (HttpDocInfo) crawlDocInfo;

        assertEquals("http://www.jettro.dev", casted.getReference());
    }

    @Test
    public void checkParsingWithDate() {
        Gson GSON = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeConverter())
                .create();

        String example = "{\"depth\":0,\"urlRoot\":\"https://www.jettro.dev\",\"referrerReference\":\"https://www.jettro.dev\",\"referencedUrls\":[],\"redirectTrail\":[],\"state\":{\"state\":\"NEW\"},\"crawlDate\":\"2021-04-23T11:47:08.939954+02:00[Europe/Amsterdam]\",\"reference\":\"https://www.jettro.dev/\",\"contentType\":{\"type\":\"text/html\"},\"contentEncoding\":\"UTF-8\",\"embeddedParentReferences\":[]}";

        HttpDocInfo crawlDocInfo = GSON.fromJson(example, HttpDocInfo.class);

        assertEquals(0, crawlDocInfo.getDepth());
    }
}
